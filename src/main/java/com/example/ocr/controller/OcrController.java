package com.example.ocr.controller;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.core.engine.PaddleOcrEngine;
import com.example.ocr.core.engine.RoutingOcrEngine;
import com.example.ocr.core.model.OcrModelManager;
import com.example.ocr.dto.ApiResponse;
import com.example.ocr.dto.Base64OcrRequest;
import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTaskResponse;
import com.example.ocr.dto.OcrTaskStatus;
import com.example.ocr.dto.OcrTaskStatusResponse;
import com.example.ocr.dto.WrongQuestionResult;
import com.example.ocr.exception.InvalidOcrInputException;
import com.example.ocr.service.OcrAsyncService;
import com.example.ocr.service.OcrService;
import com.example.ocr.service.OcrTaskStore;
import com.example.ocr.service.WrongQuestionExtractor;
import com.example.ocr.util.Base64Utils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;
    private final OcrAsyncService ocrAsyncService;
    private final OcrTaskStore taskStore;
    private final WrongQuestionExtractor wrongQuestionExtractor;
    private final RoutingOcrEngine ocrEngine;
    private final PaddleOcrEngine paddleOcrEngine;
    private final OcrModelManager modelManager;
    private final OcrProperties properties;

    public OcrController(
            OcrService ocrService,
            OcrAsyncService ocrAsyncService,
            OcrTaskStore taskStore,
            WrongQuestionExtractor wrongQuestionExtractor,
            RoutingOcrEngine ocrEngine,
            PaddleOcrEngine paddleOcrEngine,
            OcrModelManager modelManager,
            OcrProperties properties
    ) {
        this.ocrService = ocrService;
        this.ocrAsyncService = ocrAsyncService;
        this.taskStore = taskStore;
        this.wrongQuestionExtractor = wrongQuestionExtractor;
        this.ocrEngine = ocrEngine;
        this.paddleOcrEngine = paddleOcrEngine;
        this.modelManager = modelManager;
        this.properties = properties;
    }

    @Operation(summary = "Recognize a multipart image or PDF synchronously")
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<OcrResult>> recognize(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute OcrOptions options
    ) {
        return ApiResponse.ok(ocrService.recognize(readFile(file), options));
    }

    @Operation(summary = "Recognize a Base64 image or PDF synchronously")
    @PostMapping("/recognize/base64")
    public ApiResponse<List<OcrResult>> recognizeBase64(@Valid @RequestBody Base64OcrRequest request) {
        return ApiResponse.ok(ocrService.recognize(Base64Utils.decode(request.getData()), request.getOptions()));
    }

    @Operation(summary = "Extract wrong questions from a multipart image or PDF")
    @PostMapping(value = "/wrong-questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WrongQuestionResult> wrongQuestions(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute OcrOptions options
    ) {
        return ApiResponse.ok(wrongQuestionExtractor.extract(ocrService.recognize(readFile(file), options), options));
    }

    @Operation(summary = "Extract wrong questions from a Base64 image or PDF")
    @PostMapping("/wrong-questions/base64")
    public ApiResponse<WrongQuestionResult> wrongQuestionsBase64(@Valid @RequestBody Base64OcrRequest request) {
        return ApiResponse.ok(wrongQuestionExtractor.extract(ocrService.recognize(Base64Utils.decode(request.getData()), request.getOptions()), request.getOptions()));
    }

    @Operation(summary = "Submit a multipart OCR task")
    @PostMapping(value = "/recognize-async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OcrTaskResponse> recognizeAsync(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute OcrOptions options
    ) {
        String taskId = ocrAsyncService.submit(readFile(file), options);
        return ApiResponse.ok(new OcrTaskResponse(taskId, OcrTaskStatus.PENDING));
    }

    @Operation(summary = "Submit a Base64 OCR task")
    @PostMapping("/recognize-async/base64")
    public ApiResponse<OcrTaskResponse> recognizeBase64Async(@Valid @RequestBody Base64OcrRequest request) {
        String taskId = ocrAsyncService.submit(Base64Utils.decode(request.getData()), request.getOptions());
        return ApiResponse.ok(new OcrTaskResponse(taskId, OcrTaskStatus.PENDING));
    }

    @Operation(summary = "Get OCR task status")
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<OcrTaskStatusResponse> getTask(@PathVariable String taskId) {
        return ApiResponse.ok(taskStore.find(taskId)
                .orElseThrow(() -> new InvalidOcrInputException("OCR task not found")));
    }

    @Operation(summary = "List OCR engine information")
    @GetMapping("/engines")
    public ApiResponse<Map<String, Object>> engines() {
        return ApiResponse.ok(Map.of(
                "defaultEngine", ocrEngine.defaultEngine(),
                "availableEngines", ocrEngine.availableEngineNames(),
                "defaultLanguage", modelManager.defaultLanguage(),
                "configuredLanguages", modelManager.configuredLanguages(),
                "tessdataPathExists", modelManager.tessdataPathExists(),
                "paddleEnabled", properties.getPaddle().isEnabled(),
                "paddleConfigured", paddleOcrEngine.isConfigured(),
                "paddleAvailable", paddleOcrEngine.isAvailable(),
                "paddleLanguage", properties.getPaddle().getLanguage(),
                "paddleUseAngleCls", properties.getPaddle().isUseAngleCls()
        ));
    }

    private byte[] readFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOcrInputException("Uploaded file is required");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidOcrInputException("Failed to read uploaded file", e);
        }
    }
}
