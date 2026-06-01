package com.example.ocr.service;

import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import com.example.ocr.dto.QuestionRegion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionLayoutAnalyzerTest {

    private final QuestionLayoutAnalyzer analyzer = new QuestionLayoutAnalyzer();

    @Test
    void aggregatesMultipleQuestionsByPageColumnAndYOrder() {
        OcrResult result = result(1, List.of(
                block("2.", 450, 10, 20, 16, 92, 1),
                block("右栏题", 480, 10, 80, 16, 92, 1),
                block("右栏说明", 480, 34, 90, 16, 90, 1),
                block("1.", 10, 20, 20, 16, 95, 1),
                block("左栏题", 40, 20, 80, 16, 95, 1),
                block("左栏说明", 40, 44, 90, 16, 93, 1)
        ));

        List<QuestionRegion> regions = analyzer.analyze(result);

        assertThat(regions).extracting(QuestionRegion::getQuestionId).containsExactly("1", "2");
        assertThat(regions.get(0).getText()).contains("1. 左栏题", "左栏说明");
        assertThat(regions.get(1).getText()).contains("2. 右栏题", "右栏说明");
        assertThat(regions.get(0).getPage()).isEqualTo(1);
        assertThat(regions.get(1).getPage()).isEqualTo(1);
        assertThat(regions.get(0).isNeedsReview()).isFalse();
        assertThat(regions.get(1).isNeedsReview()).isFalse();
    }

    @Test
    void appendsNeighboringBlockWithoutQuestionNumberToPreviousQuestion() {
        OcrResult result = result(1, List.of(
                block("（1）", 10, 10, 32, 16, 91, 1),
                block("计算 12 + 8", 48, 10, 120, 16, 91, 1),
                block("请写出过程", 48, 36, 120, 16, 89, 1),
                block("(2)", 10, 90, 30, 16, 93, 1),
                block("选择正确答案", 48, 90, 120, 16, 93, 1)
        ));

        List<QuestionRegion> regions = analyzer.analyze(result);

        assertThat(regions).hasSize(2);
        assertThat(regions.get(0).getQuestionId()).isEqualTo("1");
        assertThat(regions.get(0).getText()).contains("计算 12 + 8", "请写出过程");
        assertThat(regions.get(0).getBlocks()).hasSize(3);
        assertThat(regions.get(1).getQuestionId()).isEqualTo("2");
    }

    @Test
    void marksLowConfidenceOrMissingQuestionNumberRegionForReview() {
        OcrResult result = result(1, List.of(
                block("题干没有题号", 20, 10, 120, 16, 88, 1),
                block("第3题", 20, 120, 42, 16, 55, 1),
                block("低置信度题干", 70, 120, 120, 16, 50, 1)
        ));

        List<QuestionRegion> regions = analyzer.analyze(result);

        assertThat(regions).hasSize(2);
        assertThat(regions.get(0).getQuestionId()).isNull();
        assertThat(regions.get(0).isNeedsReview()).isTrue();
        assertThat(regions.get(0).getReviewReasons())
                .containsExactly(QuestionLayoutAnalyzer.REVIEW_REASON_MISSING_QUESTION_NUMBER);
        assertThat(regions.get(1).getQuestionId()).isEqualTo("3");
        assertThat(regions.get(1).isNeedsReview()).isTrue();
        assertThat(regions.get(1).getReviewReasons())
                .containsExactly(QuestionLayoutAnalyzer.REVIEW_REASON_LOW_CONFIDENCE);
    }

    @Test
    void mergesCoordinateBoundsAcrossAllBlocksInQuestion() {
        OcrResult result = result(1, List.of(
                block("1", 25, 18, 12, 14, 90, 1),
                block("证明题", 60, 20, 80, 20, 90, 1),
                block("解答区域", 40, 58, 220, 32, 90, 1)
        ));

        List<QuestionRegion> regions = analyzer.analyze(result);

        assertThat(regions).hasSize(1);
        QuestionRegion region = regions.get(0);
        assertThat(region.getQuestionId()).isEqualTo("1");
        assertThat(region.getX()).isEqualTo(25);
        assertThat(region.getY()).isEqualTo(18);
        assertThat(region.getWidth()).isEqualTo(235);
        assertThat(region.getHeight()).isEqualTo(72);
        assertThat(region.getConfidence()).isEqualTo(90);
        assertThat(region.getBlocks()).hasSize(3);
        assertThat(region.getText()).contains("1", "证明题", "解答区域");
    }

    @Test
    void recognizesChineseNumberQuestionMarkers() {
        OcrResult result = result(2, List.of(
                block("十、", 12, 10, 28, 16, 86, 2),
                block("应用题", 46, 10, 80, 16, 86, 2),
                block("十一", 12, 80, 30, 16, 88, 2),
                block("下一题", 46, 80, 80, 16, 88, 2)
        ));

        List<QuestionRegion> regions = analyzer.analyze(result);

        assertThat(regions).extracting(QuestionRegion::getQuestionId).containsExactly("十", "十一");
        assertThat(regions.get(0).getText()).contains("十、应用题");
        assertThat(regions.get(1).getText()).contains("十一", "下一题");
    }

    private OcrResult result(int page, List<OcrTextBlock> blocks) {
        OcrResult result = new OcrResult();
        result.setPage(page);
        result.setBlocks(blocks);
        return result;
    }

    private OcrTextBlock block(String text, int x, int y, int width, int height, double confidence, int page) {
        return new OcrTextBlock(text, x, y, width, height, confidence, page);
    }
}
