package com.example.ocr.core.postprocess;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import org.springframework.stereotype.Component;

@Component
public class ConfidenceFilterPostprocessor implements OcrPostprocessor {

    @Override
    public OcrResult process(OcrResult result, OcrOptions options) {
        if (options == null || options.getMinConfidence() == null) {
            return result;
        }
        result.setBlocks(result.getBlocks().stream()
                .filter(block -> block.getConfidence() >= options.getMinConfidence())
                .toList());
        return result;
    }

    @Override
    public int order() {
        return 10;
    }
}
