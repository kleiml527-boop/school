package com.example.ocr.dto;

import java.util.ArrayList;
import java.util.List;

public class OcrResult {

    private String text;
    private List<OcrTextBlock> blocks = new ArrayList<>();
    private int page;
    private String language;
    private long durationMillis;
    private String engine;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<OcrTextBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<OcrTextBlock> blocks) {
        this.blocks = blocks;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }
}
