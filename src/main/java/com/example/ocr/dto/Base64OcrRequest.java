package com.example.ocr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class Base64OcrRequest {

    @NotBlank
    private String data;

    @Valid
    private OcrOptions options = new OcrOptions();

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public OcrOptions getOptions() {
        return options;
    }

    public void setOptions(OcrOptions options) {
        this.options = options;
    }
}
