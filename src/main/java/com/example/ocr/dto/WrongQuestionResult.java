package com.example.ocr.dto;

import java.util.ArrayList;
import java.util.List;

public class WrongQuestionResult {

    private List<WrongQuestionItem> questions = new ArrayList<>();
    private List<QuestionRegion> questionRegions = new ArrayList<>();
    private String rawText;
    private int pageCount;
    private boolean needsReview;
    private List<String> reviewReasons = new ArrayList<>();
    private String ocrEngine;
    private String preprocessProfile;

    public List<WrongQuestionItem> getQuestions() {
        return questions;
    }

    public void setQuestions(List<WrongQuestionItem> questions) {
        this.questions = questions == null ? new ArrayList<>() : questions;
    }

    public List<QuestionRegion> getQuestionRegions() {
        return questionRegions;
    }

    public void setQuestionRegions(List<QuestionRegion> questionRegions) {
        this.questionRegions = questionRegions == null ? new ArrayList<>() : questionRegions;
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

    public boolean isNeedsReview() {
        return needsReview;
    }

    public boolean getNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
    }

    public List<String> getReviewReasons() {
        return reviewReasons;
    }

    public void setReviewReasons(List<String> reviewReasons) {
        this.reviewReasons = reviewReasons == null ? new ArrayList<>() : reviewReasons;
    }

    public String getOcrEngine() {
        return ocrEngine;
    }

    public void setOcrEngine(String ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    public String getPreprocessProfile() {
        return preprocessProfile;
    }

    public void setPreprocessProfile(String preprocessProfile) {
        this.preprocessProfile = preprocessProfile;
    }
}
