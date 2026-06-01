package com.example.ocr.core.engine;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;

import java.util.List;

public interface OcrEngine {

    OcrResult recognize(byte[] imageData, OcrOptions options);

    List<OcrResult> recognizePdf(byte[] pdfData, OcrOptions options);

    String engineName();
}
