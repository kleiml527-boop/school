package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;

@Component
public class ImageResizePreprocessor implements ImagePreprocessor {

    private final OcrProperties properties;

    public ImageResizePreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public BufferedImage process(BufferedImage image, OcrOptions options) {
        OcrProperties.Preprocessing preprocessing = properties.getPreprocessing();
        int targetWidth = image.getWidth();
        int targetHeight = image.getHeight();

        double scale = Math.min(
                (double) properties.getMaxImageWidth() / image.getWidth(),
                (double) properties.getMaxImageHeight() / image.getHeight()
        );
        if (scale < 1.0) {
            targetWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            targetHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        } else if (preprocessing.isUpscaleSmallImages()
                && image.getWidth() < preprocessing.getMinImageWidth()
                && image.getHeight() < preprocessing.getMinImageHeight()) {
            scale = Math.min(
                    Math.min((double) preprocessing.getMinImageWidth() / image.getWidth(),
                            (double) preprocessing.getMinImageHeight() / image.getHeight()),
                    Math.min((double) properties.getMaxImageWidth() / image.getWidth(),
                            (double) properties.getMaxImageHeight() / image.getHeight())
            );
            targetWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            targetHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        }

        if (targetWidth == image.getWidth() && targetHeight == image.getHeight()) {
            return image;
        }
        try {
            return Thumbnails.of(image)
                    .size(targetWidth, targetHeight)
                    .asBufferedImage();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resize image", e);
        }
    }

    @Override
    public int order() {
        return 10;
    }
}
