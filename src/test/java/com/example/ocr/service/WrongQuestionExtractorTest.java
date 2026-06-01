package com.example.ocr.service;

import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import com.example.ocr.dto.WrongQuestionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WrongQuestionExtractorTest {

    private final WrongQuestionExtractor extractor = new WrongQuestionExtractor();

    @Test
    void extractsWrongQuestionFromChineseAnswerText() {
        OcrResult ocrResult = new OcrResult();
        ocrResult.setPage(1);
        ocrResult.setText("""
                7. 一个长方体的底面是面积为4平方分米的正方形
                A. 6 B. 8 C. 10 D. 12
                学生答案：B 正确答案：C
                错误类型：计算错误
                知识点：长方体表面积
                """);
        ocrResult.setBlocks(List.of(
                new OcrTextBlock("7", 0, 0, 10, 10, 90, 1),
                new OcrTextBlock("答案", 0, 10, 10, 10, 80, 1)
        ));

        WrongQuestionResult result = extractor.extract(List.of(ocrResult));

        assertThat(result.getPageCount()).isEqualTo(1);
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().get(0).getQuestionId()).isEqualTo("7");
        assertThat(result.getQuestions().get(0).getType()).isEqualTo("选择题");
        assertThat(result.getQuestions().get(0).getStudentAnswer()).isEqualTo("B");
        assertThat(result.getQuestions().get(0).getCorrectAnswer()).isEqualTo("C");
        assertThat(result.getQuestions().get(0).getWrong()).isTrue();
        assertThat(result.getQuestions().get(0).getErrorType()).isEqualTo("计算错误");
        assertThat(result.getQuestions().get(0).getKnowledgePoint()).isEqualTo("长方体表面积");
        assertThat(result.getQuestions().get(0).getMarkingStatus()).isEqualTo("unknown");
        assertThat(result.getQuestions().get(0).getConfidence()).isEqualTo(85);
    }

    @Test
    void marksQuestionCorrectWhenAnswersMatch() {
        OcrResult ocrResult = new OcrResult();
        ocrResult.setPage(1);
        ocrResult.setText("（8）判断题 地球是圆的\n学生答案：对\n正确答案：对");

        WrongQuestionResult result = extractor.extract(List.of(ocrResult));

        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().get(0).getQuestionId()).isEqualTo("8");
        assertThat(result.getQuestions().get(0).getWrong()).isFalse();
    }

    @Test
    void leavesWrongStatusUnknownWhenAnswersAreMissing() {
        OcrResult ocrResult = new OcrResult();
        ocrResult.setPage(1);
        ocrResult.setText("1、计算 12 + 8 = ?");

        WrongQuestionResult result = extractor.extract(List.of(ocrResult));

        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().get(0).getWrong()).isNull();
        assertThat(result.getQuestions().get(0).getContent()).contains("12 + 8");
    }
}
