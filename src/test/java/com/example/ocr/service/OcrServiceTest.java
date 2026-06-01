package com.example.ocr.service;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.core.engine.OcrEngine;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.service.impl.OcrServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OcrServiceTest {

    @Test
    void routesImageInputToImageRecognition() {
        OcrService service = new OcrServiceImpl(new OcrProperties(), new StubOcrEngine());
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00};

        List<OcrResult> results = service.recognize(pngHeader, new OcrOptions());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getText()).isEqualTo("image");
    }

    @Test
    void routesPdfInputToPdfRecognition() {
        OcrService service = new OcrServiceImpl(new OcrProperties(), new StubOcrEngine());
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x00};

        List<OcrResult> results = service.recognize(pdfHeader, new OcrOptions());

        assertThat(results).hasSize(2);
    }

    private static class StubOcrEngine implements OcrEngine {
        @Override
        public OcrResult recognize(byte[] imageData, OcrOptions options) {
            OcrResult result = new OcrResult();
            result.setText("image");
            return result;
        }

        @Override
        public List<OcrResult> recognizePdf(byte[] pdfData, OcrOptions options) {
            return List.of(new OcrResult(), new OcrResult());
        }

        @Override
        public String engineName() {
            return "stub";
        }
    }
}
