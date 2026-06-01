package com.example.ocr.service;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;

import java.util.List;

public interface OcrService {

    List<OcrResult> recognize(byte[] data, OcrOptions options);
}
