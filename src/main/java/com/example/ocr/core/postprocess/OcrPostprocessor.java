package com.example.ocr.core.postprocess;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;

public interface OcrPostprocessor {

    OcrResult process(OcrResult result, OcrOptions options);

    int order();
}
