package com.example.ocr.core.postprocess;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class TextNormalizePostprocessor implements OcrPostprocessor {

    @Override
    public OcrResult process(OcrResult result, OcrOptions options) {
        result.getBlocks().forEach(block -> block.setText(normalize(block.getText())));
        String text = result.getBlocks().stream()
                .filter(block -> block.getText() != null && !block.getText().isBlank())
                .sorted(Comparator.comparingInt(OcrTextBlock::getPage)
                        .thenComparingInt(OcrTextBlock::getY)
                        .thenComparingInt(OcrTextBlock::getX))
                .map(OcrTextBlock::getText)
                .collect(Collectors.joining(" "));
        result.setText(text.isBlank() ? normalize(result.getText()) : text);
        return result;
    }

    @Override
    public int order() {
        return 20;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }
}
