package com.example.ocr.service.impl;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.core.engine.OcrEngine;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.exception.InvalidOcrInputException;
import com.example.ocr.service.OcrService;
import com.example.ocr.util.FileTypeDetector;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OcrServiceImpl implements OcrService {

    private final OcrProperties properties;
    private final OcrEngine ocrEngine;

    public OcrServiceImpl(OcrProperties properties, OcrEngine ocrEngine) {
        this.properties = properties;
        this.ocrEngine = ocrEngine;
    }

    @Override
    public List<OcrResult> recognize(byte[] data, OcrOptions options) {
        validateSize(data);
        FileTypeDetector.FileType type = FileTypeDetector.detect(data);
        if (type == FileTypeDetector.FileType.PDF) {
            return ocrEngine.recognizePdf(data, options);
        }
        return List.of(ocrEngine.recognize(data, options));
    }

    private void validateSize(byte[] data) {
        if (data == null || data.length == 0) {
            throw new InvalidOcrInputException("Input file is required");
        }
        if (data.length > properties.getMaxFileSize().toBytes()) {
            throw new InvalidOcrInputException("Input file exceeds max size: " + properties.getMaxFileSize());
        }
    }
}
