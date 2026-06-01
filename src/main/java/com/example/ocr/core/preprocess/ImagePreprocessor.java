package com.example.ocr.core.preprocess;

import com.example.ocr.dto.OcrOptions;

import java.awt.image.BufferedImage;

public interface ImagePreprocessor {

    BufferedImage process(BufferedImage image, OcrOptions options);

    int order();
}
