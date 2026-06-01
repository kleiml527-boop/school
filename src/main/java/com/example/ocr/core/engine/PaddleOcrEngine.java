package com.example.ocr.core.engine;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.core.postprocess.PostprocessPipeline;
import com.example.ocr.core.preprocess.PreprocessPipeline;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.exception.InvalidOcrInputException;
import com.example.ocr.exception.OcrException;
import com.example.ocr.exception.OcrTimeoutException;
import com.example.ocr.util.ImageUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class PaddleOcrEngine implements OcrEngine {

    private final OcrProperties properties;
    private final PreprocessPipeline preprocessPipeline;
    private final PostprocessPipeline postprocessPipeline;
    private final PaddleOcrResultParser resultParser;

    public PaddleOcrEngine(
            OcrProperties properties,
            PreprocessPipeline preprocessPipeline,
            PostprocessPipeline postprocessPipeline,
            PaddleOcrResultParser resultParser
    ) {
        this.properties = properties;
        this.preprocessPipeline = preprocessPipeline;
        this.postprocessPipeline = postprocessPipeline;
        this.resultParser = resultParser;
    }

    @Override
    public OcrResult recognize(byte[] imageData, OcrOptions options) {
        return recognizeImage(ImageUtils.read(imageData), options, 1);
    }

    @Override
    public List<OcrResult> recognizePdf(byte[] pdfData, OcrOptions options) {
        try (PDDocument document = PDDocument.load(pdfData)) {
            if (document.getNumberOfPages() > properties.getMaxPdfPages()) {
                throw new InvalidOcrInputException("PDF exceeds max page limit: " + properties.getMaxPdfPages());
            }
            PDFRenderer renderer = new PDFRenderer(document);
            List<OcrResult> results = new ArrayList<>();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, properties.getPdfRenderDpi(), ImageType.RGB);
                results.add(recognizeImage(image, options, i + 1));
            }
            return results;
        } catch (IOException e) {
            throw new InvalidOcrInputException("Failed to read PDF", e);
        }
    }

    @Override
    public String engineName() {
        return "paddle";
    }

    public boolean isConfigured() {
        return properties.getPaddle().isEnabled() && !properties.getPaddle().getCommand().isBlank();
    }

    private OcrResult recognizeImage(BufferedImage source, OcrOptions options, int page) {
        if (!isConfigured()) {
            throw new OcrException("PaddleOCR is not enabled or command is not configured");
        }
        long start = System.nanoTime();
        BufferedImage image = preprocessPipeline.process(source, options);
        Path imagePath = writeTempImage(image);
        try {
            String output = runPaddle(imagePath);
            long durationMillis = (System.nanoTime() - start) / 1_000_000;
            OcrResult result = resultParser.parse(output, page, language(options), durationMillis);
            return postprocessPipeline.process(result, options);
        } finally {
            deleteTempImage(imagePath);
        }
    }

    private Path writeTempImage(BufferedImage image) {
        try {
            Path imagePath = Files.createTempFile("paddle-ocr-", ".png");
            Files.write(imagePath, ImageUtils.toPngBytes(image));
            return imagePath;
        } catch (IOException e) {
            throw new OcrException("Failed to prepare PaddleOCR image", e);
        }
    }

    private String runPaddle(Path imagePath) {
        List<String> command = command(imagePath);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        if (!properties.getPaddle().getWorkingDirectory().isBlank()) {
            builder.directory(Path.of(properties.getPaddle().getWorkingDirectory()).toFile());
        }
        try {
            Process process = builder.start();
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(process));
            boolean finished = process.waitFor(timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new OcrTimeoutException("PaddleOCR recognition timed out");
            }
            String stdout = output.join();
            if (process.exitValue() != 0) {
                throw new OcrException("PaddleOCR command failed: " + stdout);
            }
            return stdout;
        } catch (IOException e) {
            throw new OcrException("Failed to run PaddleOCR command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrTimeoutException("PaddleOCR recognition was interrupted");
        }
    }

    private String readOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new OcrException("Failed to read PaddleOCR output", e);
        }
    }

    private List<String> command(Path imagePath) {
        List<String> command = new ArrayList<>();
        String configured = properties.getPaddle().getCommand().trim();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < configured.length(); i++) {
            char ch = configured.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(ch) && !quoted) {
                if (!current.isEmpty()) {
                    command.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            command.add(current.toString());
        }
        command.add(imagePath.toAbsolutePath().toString());
        return command;
    }

    private Duration timeout() {
        return Duration.ofSeconds(properties.getPaddle().getTimeoutSeconds());
    }

    private String language(OcrOptions options) {
        if (options != null && options.getLanguage() != null && !options.getLanguage().isBlank()) {
            return options.getLanguage();
        }
        return properties.getLanguage();
    }

    private void deleteTempImage(Path imagePath) {
        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException ignored) {
        }
    }
}
