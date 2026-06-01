package com.example.ocr.dto;

import java.time.Instant;
import java.util.List;

public class OcrTaskStatusResponse {

    private String taskId;
    private OcrTaskStatus status;
    private List<OcrResult> results;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public OcrTaskStatus getStatus() {
        return status;
    }

    public void setStatus(OcrTaskStatus status) {
        this.status = status;
    }

    public List<OcrResult> getResults() {
        return results;
    }

    public void setResults(List<OcrResult> results) {
        this.results = results;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
