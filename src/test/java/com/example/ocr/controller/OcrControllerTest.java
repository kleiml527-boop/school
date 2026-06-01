package com.example.ocr.controller;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.core.engine.PaddleOcrEngine;
import com.example.ocr.core.engine.RoutingOcrEngine;
import com.example.ocr.core.model.OcrModelManager;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.WrongQuestionItem;
import com.example.ocr.dto.WrongQuestionResult;
import com.example.ocr.service.OcrAsyncService;
import com.example.ocr.service.OcrService;
import com.example.ocr.service.OcrTaskStore;
import com.example.ocr.service.WrongQuestionExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OcrController.class)
class OcrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OcrService ocrService;

    @MockBean
    private OcrAsyncService ocrAsyncService;

    @MockBean
    private OcrTaskStore taskStore;

    @MockBean
    private WrongQuestionExtractor wrongQuestionExtractor;

    @MockBean
    private RoutingOcrEngine ocrEngine;

    @MockBean
    private PaddleOcrEngine paddleOcrEngine;

    @MockBean
    private OcrModelManager modelManager;

    @MockBean
    private OcrProperties properties;

    @Test
    void recognizesBase64Request() throws Exception {
        OcrResult result = new OcrResult();
        result.setText("hello");
        when(ocrService.recognize(any(), any(OcrOptions.class))).thenReturn(List.of(result));
        String body = objectMapper.writeValueAsString(new Request("iVBORw0KGgo="));

        mockMvc.perform(post("/api/ocr/recognize/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].text").value("hello"));
    }

    @Test
    void extractsWrongQuestionsFromBase64Request() throws Exception {
        OcrResult ocrResult = new OcrResult();
        ocrResult.setText("7. 题目 学生答案：B 正确答案：C");
        WrongQuestionItem item = new WrongQuestionItem();
        item.setQuestionId("7");
        item.setStudentAnswer("B");
        item.setCorrectAnswer("C");
        item.setWrong(true);
        WrongQuestionResult result = new WrongQuestionResult();
        result.setQuestions(List.of(item));
        when(ocrService.recognize(any(), any(OcrOptions.class))).thenReturn(List.of(ocrResult));
        when(wrongQuestionExtractor.extract(any())).thenReturn(result);
        String body = objectMapper.writeValueAsString(new Request("iVBORw0KGgo="));

        mockMvc.perform(post("/api/ocr/wrong-questions/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questions[0].questionId").value("7"))
                .andExpect(jsonPath("$.data.questions[0].studentAnswer").value("B"))
                .andExpect(jsonPath("$.data.questions[0].wrong").value(true));
    }

    private record Request(String data, OcrOptions options) {
        private Request(String data) {
            this(data, new OcrOptions());
        }
    }
}
