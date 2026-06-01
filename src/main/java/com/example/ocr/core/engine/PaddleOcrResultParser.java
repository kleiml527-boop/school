package com.example.ocr.core.engine;

import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import com.example.ocr.exception.OcrException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class PaddleOcrResultParser {

    private final ObjectMapper objectMapper;

    public PaddleOcrResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OcrResult parse(String output, int page, String language, long durationMillis) {
        try {
            JsonNode root = objectMapper.readTree(output);
            OcrResult result = new OcrResult();
            result.setText(text(root));
            result.setBlocks(blocks(root, page));
            result.setPage(page);
            result.setLanguage(language);
            result.setEngine("paddle");
            result.setDurationMillis(durationMillis);
            return result;
        } catch (IOException e) {
            throw new OcrException("Failed to parse PaddleOCR output", e);
        }
    }

    private String text(JsonNode root) {
        JsonNode text = root.get("text");
        if (text != null && text.isTextual()) {
            return text.asText();
        }
        List<String> lines = new ArrayList<>();
        for (OcrTextBlock block : blocks(root, 0)) {
            if (block.getText() != null && !block.getText().isBlank()) {
                lines.add(block.getText());
            }
        }
        return String.join("\n", lines);
    }

    private List<OcrTextBlock> blocks(JsonNode root, int page) {
        List<OcrTextBlock> blocks = new ArrayList<>();
        for (JsonNode node : blockNodes(root)) {
            JsonNode text = node.get("text");
            if (text == null || !text.isTextual()) {
                continue;
            }
            int[] box = box(node);
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0;
            blocks.add(new OcrTextBlock(text.asText(), box[0], box[1], box[2], box[3], confidence, page));
        }
        blocks.sort(Comparator.comparingInt(OcrTextBlock::getY).thenComparingInt(OcrTextBlock::getX));
        return blocks;
    }

    private Iterable<JsonNode> blockNodes(JsonNode root) {
        JsonNode blocks = root.get("blocks");
        if (blocks != null && blocks.isArray()) {
            return blocks;
        }
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            return results;
        }
        return List.of();
    }

    private int[] box(JsonNode node) {
        JsonNode box = node.get("box");
        if (box != null && box.isArray() && box.size() >= 4) {
            if (box.get(0).isArray()) {
                return polygonBox(box);
            }
            return new int[]{box.get(0).asInt(), box.get(1).asInt(), box.get(2).asInt(), box.get(3).asInt()};
        }
        return new int[]{0, 0, 0, 0};
    }

    private int[] polygonBox(JsonNode box) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (JsonNode point : box) {
            if (!point.isArray() || point.size() < 2) {
                continue;
            }
            int x = point.get(0).asInt();
            int y = point.get(1).asInt();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (minX == Integer.MAX_VALUE) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{minX, minY, maxX - minX, maxY - minY};
    }
}
