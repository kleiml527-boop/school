package com.example.ocr.core.postprocess;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizePostprocessorTest {

    private final TextNormalizePostprocessor postprocessor = new TextNormalizePostprocessor();

    @Test
    void preservesExistingOcrTextLinesInsteadOfReplacingWithWordBlocks() {
        OcrResult result = new OcrResult();
        result.setText("1. 第一题  \n  学生答案：A  正确答案：B\n\n2. 第二题");
        result.setBlocks(List.of(
                new OcrTextBlock("第二题", 100, 50, 40, 10, 90, 1),
                new OcrTextBlock("第一题", 100, 10, 40, 10, 90, 1)
        ));

        OcrResult processed = postprocessor.process(result, new OcrOptions());

        assertThat(processed.getText()).isEqualTo("1. 第一题\n学生答案：A 正确答案：B\n2. 第二题");
        assertThat(processed.getBlocks()).extracting(OcrTextBlock::getText).containsExactly("第一题", "第二题");
    }

    @Test
    void rebuildsTextFromBlocksWithLineBreaksOnlyWhenOriginalTextIsBlank() {
        OcrResult result = new OcrResult();
        result.setBlocks(List.of(
                new OcrTextBlock("1.", 10, 10, 10, 10, 90, 1),
                new OcrTextBlock("第一题", 30, 12, 40, 10, 90, 1),
                new OcrTextBlock("2.", 10, 40, 10, 10, 90, 1),
                new OcrTextBlock("第二题", 30, 42, 40, 10, 90, 1)
        ));

        OcrResult processed = postprocessor.process(result, new OcrOptions());

        assertThat(processed.getText()).isEqualTo("1. 第一题\n2. 第二题");
    }
}
