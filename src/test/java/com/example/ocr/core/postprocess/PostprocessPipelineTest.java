package com.example.ocr.core.postprocess;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostprocessPipelineTest {

    @Test
    void filtersByConfidenceAndNormalizesText() {
        PostprocessPipeline pipeline = new PostprocessPipeline(List.of(new ConfidenceFilterPostprocessor(), new TextNormalizePostprocessor()));
        OcrResult result = new OcrResult();
        result.setBlocks(new ArrayList<>(List.of(
                new OcrTextBlock(" hello\n", 10, 10, 20, 10, 90, 1),
                new OcrTextBlock("ignored", 30, 10, 20, 10, 20, 1),
                new OcrTextBlock("world", 40, 10, 20, 10, 95, 1)
        )));
        OcrOptions options = new OcrOptions();
        options.setMinConfidence(50.0);

        OcrResult processed = pipeline.process(result, options);

        assertThat(processed.getBlocks()).hasSize(2);
        assertThat(processed.getText()).isEqualTo("hello world");
    }
}
