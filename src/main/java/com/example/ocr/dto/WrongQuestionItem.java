package com.example.ocr.dto;

import java.util.ArrayList;
import java.util.List;

public class WrongQuestionItem {

    private String questionId;
    private String type;
    private String content;
    private String studentAnswer;
    private String correctAnswer;
    private Boolean wrong;
    private String errorType;
    private String knowledgePoint;
    private String imagePath;
    private String markingStatus;
    private int page;
    private double confidence;
    private String rawText;
    private QuestionRegion region;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private boolean needsReview;
    private List<String> reviewReasons = new ArrayList<>();
    private String ocrEngine;
    private String preprocessProfile;
    private Boolean confirmed;
    private Boolean unrecognized;
    private String correctedText;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Boolean getWrong() {
        return wrong;
    }

    public void setWrong(Boolean wrong) {
        this.wrong = wrong;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getMarkingStatus() {
        return markingStatus;
    }

    public void setMarkingStatus(String markingStatus) {
        this.markingStatus = markingStatus;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public QuestionRegion getRegion() {
        return region;
    }

    public void setRegion(QuestionRegion region) {
        this.region = region;
        if (region != null) {
            this.x = region.getX();
            this.y = region.getY();
            this.width = region.getWidth();
            this.height = region.getHeight();
        }
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
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

    public void addReviewReason(String reviewReason) {
        if (reviewReason == null || reviewReason.isBlank()) {
            return;
        }
        if (!reviewReasons.contains(reviewReason)) {
            reviewReasons.add(reviewReason);
        }
        needsReview = true;
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

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Boolean getUnrecognized() {
        return unrecognized;
    }

    public void setUnrecognized(Boolean unrecognized) {
        this.unrecognized = unrecognized;
    }

    public String getCorrectedText() {
        return correctedText;
    }

    public void setCorrectedText(String correctedText) {
        this.correctedText = correctedText;
    }
}
