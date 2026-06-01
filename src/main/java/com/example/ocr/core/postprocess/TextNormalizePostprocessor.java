package com.example.ocr.core.postprocess;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TextNormalizePostprocessor implements OcrPostprocessor {

    @Override
    public OcrResult process(OcrResult result, OcrOptions options) {
        List<OcrTextBlock> blocks = normalizedBlocks(result.getBlocks());
        result.setBlocks(blocks);

        String text = normalizeFullText(result.getText());
        result.setText(text.isBlank() ? rebuildTextFromBlocks(blocks) : text);
        return result;
    }

    @Override
    public int order() {
        return 20;
    }

    private List<OcrTextBlock> normalizedBlocks(List<OcrTextBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return new ArrayList<>();
        }
        return blocks.stream()
                .peek(block -> block.setText(normalizeBlockText(block.getText())))
                .sorted(Comparator.comparingInt(OcrTextBlock::getPage)
                        .thenComparingInt(OcrTextBlock::getY)
                        .thenComparingInt(OcrTextBlock::getX))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String rebuildTextFromBlocks(List<OcrTextBlock> blocks) {
        List<OcrTextBlock> textBlocks = blocks.stream()
                .filter(block -> block.getText() != null && !block.getText().isBlank())
                .toList();
        if (textBlocks.isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        List<OcrTextBlock> currentLine = new ArrayList<>();
        int currentPage = textBlocks.get(0).getPage();
        int currentY = textBlocks.get(0).getY();
        int currentHeight = Math.max(1, textBlocks.get(0).getHeight());

        for (OcrTextBlock block : textBlocks) {
            int tolerance = Math.max(8, Math.max(currentHeight, block.getHeight()) / 2);
            if (block.getPage() != currentPage || Math.abs(block.getY() - currentY) > tolerance) {
                lines.add(joinLine(currentLine));
                currentLine = new ArrayList<>();
                currentPage = block.getPage();
                currentY = block.getY();
                currentHeight = Math.max(1, block.getHeight());
            } else {
                currentY = (currentY * currentLine.size() + block.getY()) / (currentLine.size() + 1);
                currentHeight = Math.max(currentHeight, block.getHeight());
            }
            currentLine.add(block);
        }
        if (!currentLine.isEmpty()) {
            lines.add(joinLine(currentLine));
        }
        return lines.stream()
                .filter(line -> !line.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String joinLine(List<OcrTextBlock> line) {
        return line.stream()
                .sorted(Comparator.comparingInt(OcrTextBlock::getX))
                .map(OcrTextBlock::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining(" "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeFullText(String text) {
        if (text == null) {
            return "";
        }
        return text.lines()
                .map(line -> line.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim())
                .filter(line -> !line.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String normalizeBlockText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }
}
