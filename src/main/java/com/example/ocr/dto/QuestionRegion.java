package com.example.ocr.dto;

import java.util.ArrayList;
import java.util.List;

public class QuestionRegion {

    private String questionId;
    private int page;
    private int x;
    private int y;
    private int width;
    private int height;
    private String text;
    private double confidence;
    private List<OcrTextBlock> blocks = new ArrayList<>();
    private boolean needsReview;
    private List<String> reviewReasons = new ArrayList<>();
    private String ocrEngine;
    private String preprocessProfile;
    private String markingStatus;

    public QuestionRegion() {
    }

    public QuestionRegion(String questionId, int page, int x, int y, int width, int height, double confidence, String text) {
        this.questionId = questionId;
        this.page = page;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = confidence;
        this.text = text;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<OcrTextBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<OcrTextBlock> blocks) {
        this.blocks = blocks == null ? new ArrayList<>() : blocks;
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

    public String getMarkingStatus() {
        return markingStatus;
    }

    public void setMarkingStatus(String markingStatus) {
        this.markingStatus = markingStatus;
    }
}
