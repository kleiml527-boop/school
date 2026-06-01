package com.example.ocr.core.postprocess;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PostprocessPipeline {

    private final List<OcrPostprocessor> postprocessors;

    public PostprocessPipeline(List<OcrPostprocessor> postprocessors) {
        this.postprocessors = postprocessors.stream()
                .sorted(Comparator.comparingInt(OcrPostprocessor::order))
                .toList();
    }

    public OcrResult process(OcrResult result, OcrOptions options) {
        OcrResult current = result;
        for (OcrPostprocessor postprocessor : postprocessors) {
            current = postprocessor.process(current, options);
        }
        return current;
    }
}
