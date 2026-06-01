package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

@Component
public class PreprocessPipeline {

    private final OcrProperties properties;
    private final List<ImagePreprocessor> preprocessors;

    public PreprocessPipeline(OcrProperties properties, List<ImagePreprocessor> preprocessors) {
        this.properties = properties;
        this.preprocessors = preprocessors.stream()
                .sorted(Comparator.comparingInt(ImagePreprocessor::order))
                .toList();
    }

    public BufferedImage process(BufferedImage image, OcrOptions options) {
        boolean enabled = options != null && options.getPreprocess() != null
                ? options.getPreprocess()
                : properties.getPreprocessing().isEnabled();
        if (!enabled) {
            return image;
        }
        BufferedImage current = image;
        for (ImagePreprocessor preprocessor : preprocessors) {
            current = preprocessor.process(current, options);
        }
        return current;
    }
}
