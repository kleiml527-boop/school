package com.example.ocr.service;

import com.example.ocr.dto.OcrOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
public class OcrAsyncService {

    private final OcrService ocrService;
    private final OcrTaskStore taskStore;
    private final Executor ocrTaskExecutor;

    public OcrAsyncService(
            OcrService ocrService,
            OcrTaskStore taskStore,
            @Qualifier("ocrTaskExecutor") Executor ocrTaskExecutor
    ) {
        this.ocrService = ocrService;
        this.taskStore = taskStore;
        this.ocrTaskExecutor = ocrTaskExecutor;
    }

    public String submit(byte[] data, OcrOptions options) {
        String taskId = taskStore.create();
        ocrTaskExecutor.execute(() -> recognize(taskId, data, options));
        return taskId;
    }

    private void recognize(String taskId, byte[] data, OcrOptions options) {
        taskStore.markRunning(taskId);
        try {
            taskStore.markSucceeded(taskId, ocrService.recognize(data, options));
        } catch (RuntimeException e) {
            taskStore.markFailed(taskId, e.getMessage());
        }
    }
}
