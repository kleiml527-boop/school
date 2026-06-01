package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeskewPreprocessor implements ImagePreprocessor {

    private final OcrProperties properties;

    public DeskewPreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public BufferedImage process(BufferedImage image, OcrOptions options) {
        if (!deskewEnabled(options)) {
            return image;
        }
        double angle = estimateAngle(toGray(image));
        if (Math.abs(angle) < 0.2) {
            return image;
        }
        return rotate(image, -angle);
    }

    @Override
    public int order() {
        return 12;
    }

    private boolean deskewEnabled(OcrOptions options) {
        if (options != null && options.getDeskew() != null) {
            return options.getDeskew();
        }
        return properties.getPreprocessing().isDeskew();
    }

    private double estimateAngle(BufferedImage image) {
        double maxAngle = properties.getPreprocessing().getDeskewMaxAngle();
        double bestAngle = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (double angle = -maxAngle; angle <= maxAngle; angle += 0.5) {
            double score = projectionScore(image, angle);
            if (score > bestScore) {
                bestScore = score;
                bestAngle = angle;
            }
        }
        return bestAngle;
    }

    private double projectionScore(BufferedImage image, double angle) {
        double radians = Math.toRadians(angle);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        int centerX = image.getWidth() / 2;
        int centerY = image.getHeight() / 2;
        int[] rows = new int[image.getHeight()];
        WritableRaster raster = image.getRaster();
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                if (raster.getSample(x, y, 0) > 180) {
                    continue;
                }
                int rotatedY = (int) Math.round((x - centerX) * sin + (y - centerY) * cos + centerY);
                if (rotatedY >= 0 && rotatedY < rows.length) {
                    rows[rotatedY]++;
                }
            }
        }
        double score = 0;
        for (int i = 1; i < rows.length; i++) {
            double diff = rows[i] - rows[i - 1];
            score += diff * diff;
        }
        return score;
    }

    private BufferedImage rotate(BufferedImage image, double angle) {
        double radians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int width = image.getWidth();
        int height = image.getHeight();
        int newWidth = (int) Math.floor(width * cos + height * sin);
        int newHeight = (int) Math.floor(height * cos + width * sin);
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType());
        Graphics2D graphics = rotated.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, newWidth, newHeight);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        AffineTransform transform = new AffineTransform();
        transform.translate((newWidth - width) / 2.0, (newHeight - height) / 2.0);
        transform.rotate(radians, width / 2.0, height / 2.0);
        graphics.drawRenderedImage(image, transform);
        graphics.dispose();
        return rotated;
    }

    private BufferedImage toGray(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return image;
        }
        return new GrayscalePreprocessor().process(image, null);
    }
}
