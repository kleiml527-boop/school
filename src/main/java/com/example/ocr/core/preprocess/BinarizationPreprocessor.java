package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

@Component
public class BinarizationPreprocessor implements ImagePreprocessor {

    private final OcrProperties properties;

    public BinarizationPreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public BufferedImage process(BufferedImage image, OcrOptions options) {
        if (!binarizationEnabled(options)) {
            return image;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster source = image.getRaster();
        WritableRaster target = binary.getRaster();
        long[][] integral = integralImage(source, width, height);
        int radius = Math.max(1, normalizedWindowSize() / 2);
        int bias = properties.getPreprocessing().getAdaptiveThresholdBias();

        for (int y = 0; y < height; y++) {
            int top = Math.max(0, y - radius);
            int bottom = Math.min(height - 1, y + radius);
            for (int x = 0; x < width; x++) {
                int left = Math.max(0, x - radius);
                int right = Math.min(width - 1, x + radius);
                long sum = sum(integral, left, top, right, bottom);
                int area = (right - left + 1) * (bottom - top + 1);
                int threshold = Math.max(0, (int) (sum / area) - bias);
                target.setSample(x, y, 0, source.getSample(x, y, 0) > threshold ? 1 : 0);
            }
        }
        return binary;
    }

    @Override
    public int order() {
        return 30;
    }

    private long[][] integralImage(WritableRaster source, int width, int height) {
        long[][] integral = new long[height + 1][width + 1];
        for (int y = 1; y <= height; y++) {
            long rowSum = 0;
            for (int x = 1; x <= width; x++) {
                rowSum += source.getSample(x - 1, y - 1, 0);
                integral[y][x] = integral[y - 1][x] + rowSum;
            }
        }
        return integral;
    }

    private long sum(long[][] integral, int left, int top, int right, int bottom) {
        return integral[bottom + 1][right + 1]
                - integral[top][right + 1]
                - integral[bottom + 1][left]
                + integral[top][left];
    }

    private boolean binarizationEnabled(OcrOptions options) {
        if (options != null && options.getBinarization() != null) {
            return options.getBinarization();
        }
        return properties.getPreprocessing().isBinarizationEnabled();
    }

    private int normalizedWindowSize() {
        int size = properties.getPreprocessing().getAdaptiveThresholdWindowSize();
        return size % 2 == 0 ? size + 1 : size;
    }
}
