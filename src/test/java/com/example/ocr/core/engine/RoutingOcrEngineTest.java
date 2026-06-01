package com.example.ocr.core.engine;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.exception.OcrException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingOcrEngineTest {

    @Test
    void usesConfiguredDefaultEngine() {
        OcrProperties properties = new OcrProperties();
        properties.setEngine("paddle");
        RoutingOcrEngine engine = new RoutingOcrEngine(properties, engines(new StubEngine("tess4j", false), new StubEngine("paddle", false)));

        OcrResult result = engine.recognize(new byte[0], new OcrOptions());

        assertThat(result.getEngine()).isEqualTo("paddle");
    }

    @Test
    void requestOptionOverridesDefaultEngine() {
        OcrProperties properties = new OcrProperties();
        RoutingOcrEngine engine = new RoutingOcrEngine(properties, engines(new StubEngine("tess4j", false), new StubEngine("paddle", false)));
        OcrOptions options = new OcrOptions();
        options.setEngine("paddle");

        OcrResult result = engine.recognize(new byte[0], options);

        assertThat(result.getEngine()).isEqualTo("paddle");
    }

    @Test
    void autoFallsBackToTess4jWhenPaddleFails() {
        OcrProperties properties = new OcrProperties();
        RoutingOcrEngine engine = new RoutingOcrEngine(properties, engines(new StubEngine("paddle", true), new StubEngine("tess4j", false)));
        OcrOptions options = new OcrOptions();
        options.setEngine("auto");

        OcrResult result = engine.recognize(new byte[0], options);

        assertThat(result.getEngine()).isEqualTo("tess4j");
    }

    private Map<String, OcrEngine> engines(OcrEngine first, OcrEngine second) {
        return Map.of(first.engineName(), first, second.engineName(), second);
    }

    private static class StubEngine implements OcrEngine {
        private final String name;
        private final boolean fail;

        private StubEngine(String name, boolean fail) {
            this.name = name;
            this.fail = fail;
        }

        @Override
        public OcrResult recognize(byte[] imageData, OcrOptions options) {
            if (fail) {
                throw new OcrException("failed");
            }
            OcrResult result = new OcrResult();
            result.setEngine(name);
            return result;
        }

        @Override
        public List<OcrResult> recognizePdf(byte[] pdfData, OcrOptions options) {
            return List.of(recognize(pdfData, options));
        }

        @Override
        public String engineName() {
            return name;
        }
    }
}
