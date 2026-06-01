package com.example.ocr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class OcrOptions {

    @Pattern(regexp = "^(tess4j|paddle|auto)?$", message = "engine must be tess4j, paddle, or auto")
    private String engine;

    @Pattern(regexp = "^[a-zA-Z0-9_+.-]*$", message = "language contains unsupported characters")
    private String language;

    @Min(0)
    @Max(13)
    private Integer psm;

    @Min(0)
    @Max(100)
    private Double minConfidence;

    private Boolean preprocess;

    @Pattern(regexp = "^(scan|photo|chinese-math)?$", message = "profile must be scan, photo, or chinese-math")
    private String profile;

    private Boolean binarization;

    private Boolean denoise;

    private Boolean deskew;

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPsm() {
        return psm;
    }

    public void setPsm(Integer psm) {
        this.psm = psm;
    }

    public Double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(Double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public Boolean getPreprocess() {
        return preprocess;
    }

    public void setPreprocess(Boolean preprocess) {
        this.preprocess = preprocess;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getPreprocessProfile() {
        return profile;
    }

    public void setPreprocessProfile(String preprocessProfile) {
        this.profile = preprocessProfile;
    }

    public Boolean getBinarization() {
        return binarization;
    }

    public void setBinarization(Boolean binarization) {
        this.binarization = binarization;
    }

    public Boolean getDenoise() {
        return denoise;
    }

    public void setDenoise(Boolean denoise) {
        this.denoise = denoise;
    }

    public Boolean getDeskew() {
        return deskew;
    }

    public void setDeskew(Boolean deskew) {
        this.deskew = deskew;
    }
}
