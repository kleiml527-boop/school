package com.example.ocr.service;

import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTaskStatusResponse;

import java.util.List;
import java.util.Optional;

public interface OcrTaskStore {

    String create();

    void markRunning(String taskId);

    void markSucceeded(String taskId, List<OcrResult> results);

    void markFailed(String taskId, String errorMessage);

    Optional<OcrTaskStatusResponse> find(String taskId);
}
