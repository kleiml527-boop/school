package com.example.ocr.core.preprocess;

import com.example.ocr.dto.OcrOptions;
import org.springframework.stereotype.Component;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

@Component
public class GrayscalePreprocessor implements ImagePreprocessor {

    @Override
    public BufferedImage process(BufferedImage image, OcrOptions options) {
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return image;
        }
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return gray;
    }

    @Override
    public int order() {
        return 20;
    }
}
