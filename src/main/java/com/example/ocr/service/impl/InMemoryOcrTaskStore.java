package com.example.ocr.service.impl;

import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTaskStatus;
import com.example.ocr.dto.OcrTaskStatusResponse;
import com.example.ocr.service.OcrTaskStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryOcrTaskStore implements OcrTaskStore {

    private final ConcurrentMap<String, OcrTaskStatusResponse> tasks = new ConcurrentHashMap<>();

    @Override
    public String create() {
        String taskId = UUID.randomUUID().toString();
        OcrTaskStatusResponse task = new OcrTaskStatusResponse();
        task.setTaskId(taskId);
        task.setStatus(OcrTaskStatus.PENDING);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(task.getCreatedAt());
        tasks.put(taskId, task);
        return taskId;
    }

    @Override
    public void markRunning(String taskId) {
        update(taskId, task -> task.setStatus(OcrTaskStatus.RUNNING));
    }

    @Override
    public void markSucceeded(String taskId, List<OcrResult> results) {
        update(taskId, task -> {
            task.setStatus(OcrTaskStatus.SUCCEEDED);
            task.setResults(results);
        });
    }

    @Override
    public void markFailed(String taskId, String errorMessage) {
        update(taskId, task -> {
            task.setStatus(OcrTaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
        });
    }

    @Override
    public Optional<OcrTaskStatusResponse> find(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    private void update(String taskId, TaskUpdater updater) {
        tasks.computeIfPresent(taskId, (id, task) -> {
            updater.update(task);
            task.setUpdatedAt(Instant.now());
            return task;
        });
    }

    private interface TaskUpdater {
        void update(OcrTaskStatusResponse task);
    }
}
