package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Component
public class ContrastEnhancementPreprocessor implements ImagePreprocessor {

    private final OcrProperties properties;

    public ContrastEnhancementPreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public BufferedImage process(BufferedImage image, OcrOptions options) {
        OcrProperties.Preprocessing preprocessing = properties.getPreprocessing();
        if (!preprocessing.isEnhanceContrast()) {
            return image;
        }
        BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        double factor = preprocessing.getContrastFactor();
        int offset = preprocessing.getBrightnessOffset();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = adjust((rgb >> 16) & 0xff, factor, offset);
                int green = adjust((rgb >> 8) & 0xff, factor, offset);
                int blue = adjust(rgb & 0xff, factor, offset);
                enhanced.setRGB(x, y, (red << 16) | (green << 8) | blue);
            }
        }
        return enhanced;
    }

    @Override
    public int order() {
        return 15;
    }

    private int adjust(int value, double factor, int offset) {
        int adjusted = (int) Math.round((value - 128) * factor + 128 + offset);
        return Math.max(0, Math.min(255, adjusted));
    }
}
