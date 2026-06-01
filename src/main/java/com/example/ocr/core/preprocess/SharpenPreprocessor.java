package com.example.ocr.core.preprocess;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

@Component
public class SharpenPreprocessor implements ImagePreprocessor {

    private static final Kernel SHARPEN_KERNEL = new Kernel(3, 3, new float[]{
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0
    });

    private final OcrProperties properties;

    public SharpenPreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public BufferedImage process(BufferedImage image, OcrOptions options) {
        if (!properties.getPreprocessing().isSharpen()) {
            return image;
        }
        BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(), image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType());
        return new ConvolveOp(SHARPEN_KERNEL, ConvolveOp.EDGE_NO_OP, null).filter(image, target);
    }

    @Override
    public int order() {
        return 25;
    }
}
