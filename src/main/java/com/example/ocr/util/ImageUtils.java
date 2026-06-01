package com.example.ocr.util;

import com.example.ocr.exception.InvalidOcrInputException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImageUtils {

    private ImageUtils() {
    }

    public static BufferedImage read(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new InvalidOcrInputException("Input data is not a readable image");
            }
            return image;
        } catch (IOException e) {
            throw new InvalidOcrInputException("Failed to read image", e);
        }
    }

    public static byte[] toPngBytes(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode image", e);
        }
    }
}
