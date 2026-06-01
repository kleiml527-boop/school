package com.example.ocr.core.engine;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.exception.InvalidOcrInputException;
import com.example.ocr.exception.OcrException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Primary
@Component
public class RoutingOcrEngine implements OcrEngine {

    public static final String AUTO = "auto";

    private final OcrProperties properties;
    private final Map<String, OcrEngine> engines;

    @Autowired
    public RoutingOcrEngine(OcrProperties properties, Tess4jOcrEngine tess4jOcrEngine, PaddleOcrEngine paddleOcrEngine) {
        this(properties, Map.of(
                tess4jOcrEngine.engineName(), tess4jOcrEngine,
                paddleOcrEngine.engineName(), paddleOcrEngine
        ));
    }

    RoutingOcrEngine(OcrProperties properties, Map<String, OcrEngine> engines) {
        this.properties = properties;
        this.engines = engines;
    }

    @Override
    public OcrResult recognize(byte[] imageData, OcrOptions options) {
        return route(options).recognize(imageData, options);
    }

    @Override
    public List<OcrResult> recognizePdf(byte[] pdfData, OcrOptions options) {
        return route(options).recognizePdf(pdfData, options);
    }

    @Override
    public String engineName() {
        return defaultEngine();
    }

    public List<String> availableEngineNames() {
        return engines.keySet().stream().sorted().toList();
    }

    public String defaultEngine() {
        return normalize(properties.getEngine());
    }

    private OcrEngine route(OcrOptions options) {
        String selected = options != null && options.getEngine() != null && !options.getEngine().isBlank()
                ? normalize(options.getEngine())
                : defaultEngine();
        if (AUTO.equals(selected)) {
            return new AutoFallbackEngine(require("paddle"), require("tess4j"));
        }
        return require(selected);
    }

    private OcrEngine require(String name) {
        OcrEngine engine = engines.get(name);
        if (engine == null) {
            throw new InvalidOcrInputException("Unsupported OCR engine: " + name);
        }
        return engine;
    }

    private String normalize(String engine) {
        return engine == null || engine.isBlank() ? "tess4j" : engine.trim().toLowerCase();
    }

    private record AutoFallbackEngine(OcrEngine primary, OcrEngine fallback) implements OcrEngine {
        @Override
        public OcrResult recognize(byte[] imageData, OcrOptions options) {
            try {
                return primary.recognize(imageData, options);
            } catch (OcrException e) {
                return fallback.recognize(imageData, options);
            }
        }

        @Override
        public List<OcrResult> recognizePdf(byte[] pdfData, OcrOptions options) {
            try {
                return primary.recognizePdf(pdfData, options);
            } catch (OcrException e) {
                return fallback.recognizePdf(pdfData, options);
            }
        }

        @Override
        public String engineName() {
            return AUTO;
        }
    }
}
