package com.example.ocr.core.engine;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.core.postprocess.PostprocessPipeline;
import com.example.ocr.core.preprocess.PreprocessPipeline;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import com.example.ocr.exception.InvalidOcrInputException;
import com.example.ocr.exception.OcrException;
import com.example.ocr.util.ImageUtils;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class Tess4jOcrEngine implements OcrEngine {

    private final OcrProperties properties;
    private final TesseractInstancePool pool;
    private final PreprocessPipeline preprocessPipeline;
    private final PostprocessPipeline postprocessPipeline;

    public Tess4jOcrEngine(
            OcrProperties properties,
            TesseractInstancePool pool,
            PreprocessPipeline preprocessPipeline,
            PostprocessPipeline postprocessPipeline
    ) {
        this.properties = properties;
        this.pool = pool;
        this.preprocessPipeline = preprocessPipeline;
        this.postprocessPipeline = postprocessPipeline;
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
        return "tess4j";
    }

    private OcrResult recognizeImage(BufferedImage source, OcrOptions options, int page) {
        long start = System.nanoTime();
        BufferedImage image = preprocessPipeline.process(source, options);
        Tesseract tesseract = pool.borrowInstance();
        try {
            String language = language(options);
            tesseract.setLanguage(language);
            tesseract.setPageSegMode(psm(options));
            String text = tesseract.doOCR(image);
            List<OcrTextBlock> blocks = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD).stream()
                    .map(word -> toBlock(word, page))
                    .toList();
            OcrResult result = new OcrResult();
            result.setText(text);
            result.setBlocks(blocks);
            result.setPage(page);
            result.setLanguage(language);
            result.setEngine(engineName());
            result.setPreprocessProfile(preprocessProfile(options));
            result.setDurationMillis((System.nanoTime() - start) / 1_000_000);
            return postprocessPipeline.process(result, options);
        } catch (TesseractException e) {
            throw new OcrException("OCR recognition failed", e);
        } finally {
            pool.returnInstance(tesseract);
        }
    }

    private OcrTextBlock toBlock(Word word, int page) {
        Rectangle box = word.getBoundingBox();
        return new OcrTextBlock(
                word.getText(),
                box.x,
                box.y,
                box.width,
                box.height,
                word.getConfidence(),
                page
        );
    }

    private String language(OcrOptions options) {
        if (options != null && options.getLanguage() != null && !options.getLanguage().isBlank()) {
            return options.getLanguage();
        }
        return properties.getLanguage();
    }

    private String preprocessProfile(OcrOptions options) {
        if (options != null && options.getPreprocessProfile() != null && !options.getPreprocessProfile().isBlank()) {
            return options.getPreprocessProfile();
        }
        return properties.getPreprocessing().getProfile();
    }

    private int psm(OcrOptions options) {
        if (options != null && options.getPsm() != null) {
            return options.getPsm();
        }
        return properties.getPsm();
    }
}
