package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreprocessPipelineTest {

    @Test
    void processesImageWhenEnabled() {
        OcrProperties properties = new OcrProperties();
        PreprocessPipeline pipeline = new PreprocessPipeline(properties, List.of(new GrayscalePreprocessor(), new BinarizationPreprocessor(properties)));
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 20, 20);
        graphics.dispose();

        BufferedImage processed = pipeline.process(image, new OcrOptions());

        assertThat(processed.getType()).isEqualTo(BufferedImage.TYPE_BYTE_BINARY);
    }

    @Test
    void skipsProcessingWhenRequestDisablesIt() {
        OcrProperties properties = new OcrProperties();
        PreprocessPipeline pipeline = new PreprocessPipeline(properties, List.of(new GrayscalePreprocessor()));
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        OcrOptions options = new OcrOptions();
        options.setPreprocess(false);

        BufferedImage processed = pipeline.process(image, options);

        assertThat(processed).isSameAs(image);
    }

    @Test
    void adaptiveBinarizationHandlesUnevenBackground() {
        OcrProperties properties = new OcrProperties();
        properties.getPreprocessing().setAdaptiveThresholdWindowSize(7);
        properties.getPreprocessing().setAdaptiveThresholdBias(5);
        BinarizationPreprocessor preprocessor = new BinarizationPreprocessor(properties);
        BufferedImage image = new BufferedImage(30, 10, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int background = x < 15 ? 150 : 230;
                image.getRaster().setSample(x, y, 0, background);
            }
        }
        image.getRaster().setSample(7, 5, 0, 100);
        image.getRaster().setSample(22, 5, 0, 180);

        BufferedImage processed = preprocessor.process(image, new OcrOptions());

        assertThat(processed.getRaster().getSample(7, 5, 0)).isZero();
        assertThat(processed.getRaster().getSample(22, 5, 0)).isZero();
        assertThat(processed.getRaster().getSample(7, 1, 0)).isOne();
        assertThat(processed.getRaster().getSample(22, 1, 0)).isOne();
    }

    @Test
    void upscalesSmallImagesWithinConfiguredBounds() {
        OcrProperties properties = new OcrProperties();
        properties.setMaxImageWidth(1000);
        properties.setMaxImageHeight(900);
        properties.getPreprocessing().setMinImageWidth(800);
        properties.getPreprocessing().setMinImageHeight(600);
        ImageResizePreprocessor preprocessor = new ImageResizePreprocessor(properties);
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);

        BufferedImage processed = preprocessor.process(image, new OcrOptions());

        assertThat(processed.getWidth()).isEqualTo(800);
        assertThat(processed.getHeight()).isEqualTo(600);
    }

    @Test
    void doesNotUpscalePastMaximumDimensions() {
        OcrProperties properties = new OcrProperties();
        properties.setMaxImageWidth(700);
        properties.setMaxImageHeight(700);
        properties.getPreprocessing().setMinImageWidth(1200);
        properties.getPreprocessing().setMinImageHeight(1200);
        ImageResizePreprocessor preprocessor = new ImageResizePreprocessor(properties);
        BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);

        BufferedImage processed = preprocessor.process(image, new OcrOptions());

        assertThat(processed.getWidth()).isEqualTo(700);
        assertThat(processed.getHeight()).isEqualTo(700);
    }
}
