package com.example.ocr.dto;

import java.util.ArrayList;
import java.util.List;

public class WrongQuestionResult {

    private List<WrongQuestionItem> questions = new ArrayList<>();
    private String rawText;
    private int pageCount;

    public List<WrongQuestionItem> getQuestions() {
        return questions;
    }

    public void setQuestions(List<WrongQuestionItem> questions) {
        this.questions = questions;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}
