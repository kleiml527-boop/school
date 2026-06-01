package com.example.ocr.core.engine;

import com.example.ocr.dto.OcrResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaddleOcrResultParserTest {

    private final PaddleOcrResultParser parser = new PaddleOcrResultParser(new ObjectMapper());

    @Test
    void parsesTextAndRectangleBlocks() {
        String output = """
                {
                  "text": "hello",
                  "blocks": [
                    {"text": "hello", "confidence": 96.5, "box": [1, 2, 30, 10]}
                  ]
                }
                """;

        OcrResult result = parser.parse(output, 2, "eng", 123);

        assertThat(result.getText()).isEqualTo("hello");
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getLanguage()).isEqualTo("eng");
        assertThat(result.getEngine()).isEqualTo("paddle");
        assertThat(result.getDurationMillis()).isEqualTo(123);
        assertThat(result.getBlocks()).hasSize(1);
        assertThat(result.getBlocks().get(0).getConfidence()).isEqualTo(96.5);
    }

    @Test
    void derivesTextAndBoundingRectangleFromPolygonResults() {
        String output = """
                {
                  "results": [
                    {"text": "hello", "confidence": 0.9, "box": [[1, 2], [11, 2], [11, 7], [1, 7]]},
                    {"text": "world", "confidence": 0.8, "box": [[3, 4], [13, 4], [13, 9], [3, 9]]}
                  ]
                }
                """;

        OcrResult result = parser.parse(output, 1, "chi_sim", 10);

        assertThat(result.getText()).isEqualTo("hello\nworld");
        assertThat(result.getBlocks()).hasSize(2);
        assertThat(result.getBlocks().get(0).getX()).isEqualTo(1);
        assertThat(result.getBlocks().get(0).getY()).isEqualTo(2);
        assertThat(result.getBlocks().get(0).getWidth()).isEqualTo(10);
        assertThat(result.getBlocks().get(0).getHeight()).isEqualTo(5);
    }
}
