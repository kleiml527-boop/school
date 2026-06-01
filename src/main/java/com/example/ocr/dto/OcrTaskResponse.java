package com.example.ocr.dto;

public class OcrTaskResponse {

    private String taskId;
    private OcrTaskStatus status;

    public OcrTaskResponse() {
    }

    public OcrTaskResponse(String taskId, OcrTaskStatus status) {
        this.taskId = taskId;
        this.status = status;
    }

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
}
