package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

@Component
public class DenoisePreprocessor implements ImagePreprocessor {

    private final OcrProperties properties;

    public DenoisePreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public BufferedImage process(BufferedImage image, OcrOptions options) {
        if (!denoiseEnabled(options)) {
            return image;
        }
        BufferedImage source = toGray(image);
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster sourceRaster = source.getRaster();
        WritableRaster targetRaster = target.getRaster();
        int threshold = properties.getPreprocessing().getDenoiseThreshold();

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int center = sourceRaster.getSample(x, y, 0);
                int median = median3x3(sourceRaster, x, y, source.getWidth(), source.getHeight());
                targetRaster.setSample(x, y, 0, Math.abs(center - median) > threshold ? median : center);
            }
        }
        return target;
    }

    @Override
    public int order() {
        return 22;
    }

    private boolean denoiseEnabled(OcrOptions options) {
        if (options != null && options.getDenoise() != null) {
            return options.getDenoise();
        }
        return properties.getPreprocessing().isDenoise();
    }

    private BufferedImage toGray(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return image;
        }
        return new GrayscalePreprocessor().process(image, null);
    }

    private int median3x3(WritableRaster raster, int x, int y, int width, int height) {
        int[] values = new int[9];
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            int sampleY = Math.max(0, Math.min(height - 1, y + dy));
            for (int dx = -1; dx <= 1; dx++) {
                int sampleX = Math.max(0, Math.min(width - 1, x + dx));
                values[count++] = raster.getSample(sampleX, sampleY, 0);
            }
        }
        for (int i = 1; i < values.length; i++) {
            int value = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > value) {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
        return values[values.length / 2];
    }
}
