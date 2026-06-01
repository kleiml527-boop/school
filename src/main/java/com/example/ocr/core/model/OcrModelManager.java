package com.example.ocr.core.model;

import com.example.ocr.config.OcrProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Component
public class OcrModelManager {

    private final OcrProperties properties;

    public OcrModelManager(OcrProperties properties) {
        this.properties = properties;
    }

    public String defaultLanguage() {
        return properties.getLanguage();
    }

    public List<String> configuredLanguages() {
        return Arrays.stream(properties.getLanguage().split("\\+"))
                .filter(language -> !language.isBlank())
                .toList();
    }

    public boolean tessdataPathExists() {
        return Files.isDirectory(Path.of(properties.getTessdataPath()));
    }

    public boolean languageDataExists(String language) {
        return Files.isRegularFile(Path.of(properties.getTessdataPath(), language + ".traineddata"));
    }
}
