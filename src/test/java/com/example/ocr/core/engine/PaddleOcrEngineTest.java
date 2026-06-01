package com.example.ocr.core.engine;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.core.postprocess.PostprocessPipeline;
import com.example.ocr.core.preprocess.PreprocessPipeline;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PaddleOcrEngineTest {

    @Test
    void commandPassesLanguageAndAngleClsAsSeparateArguments() {
        OcrProperties properties = new OcrProperties();
        properties.getPaddle().setCommand("python src/main/resources/paddle_ocr_json.py");
        properties.getPaddle().setLanguage("en");
        properties.getPaddle().setUseAngleCls(false);
        PaddleOcrEngine engine = new PaddleOcrEngine(
                properties,
                mock(PreprocessPipeline.class),
                mock(PostprocessPipeline.class),
                new PaddleOcrResultParser(new ObjectMapper())
        );

        List<String> command = engine.command(Path.of("sample.png"), properties.getPaddle().getLanguage());

        assertThat(command).containsExactly(
                "python",
                "src/main/resources/paddle_ocr_json.py",
                "--image",
                Path.of("sample.png").toAbsolutePath().toString(),
                "--lang",
                "en",
                "--use-angle-cls",
                "false"
        );
    }

    @Test
    void availabilityRequiresBothEnabledAndConfiguredCommand() {
        OcrProperties properties = new OcrProperties();
        PaddleOcrEngine engine = new PaddleOcrEngine(
                properties,
                mock(PreprocessPipeline.class),
                mock(PostprocessPipeline.class),
                new PaddleOcrResultParser(new ObjectMapper())
        );

        assertThat(engine.isConfigured()).isFalse();
        assertThat(engine.isAvailable()).isFalse();

        properties.getPaddle().setCommand("python paddle_ocr_json.py");
        assertThat(engine.isConfigured()).isTrue();
        assertThat(engine.isAvailable()).isFalse();

        properties.getPaddle().setEnabled(true);
        assertThat(engine.isAvailable()).isTrue();
    }
}
