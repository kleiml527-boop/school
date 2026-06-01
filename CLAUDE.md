# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This is a Java 17 Spring Boot 3.3.5 OCR service built around Tess4J/Tesseract. It exposes synchronous and asynchronous OCR endpoints for image and PDF inputs, supports Base64 and multipart uploads, renders PDFs with PDFBox, and preprocesses images before recognition.

## Common commands

```bash
# Build and run all tests
mvn clean test

# Package the Spring Boot application
mvn clean package

# Run the application locally
mvn spring-boot:run

# Run a single test class
mvn -Dtest=OcrServiceTest test

# Run a single test method
mvn -Dtest=OcrServiceTest#routesPdfInputToPdfRecognition test
```

The application listens on port `8080` by default. Swagger UI is configured at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs`, and actuator exposes `health`, `info`, and `metrics`.

OCR runtime configuration is under the `ocr` prefix in `src/main/resources/application.yml`. Important environment overrides include:

```bash
OCR_TESSDATA_PATH=./tessdata
OCR_LANGUAGE=chi_sim+eng
OCR_PSM=3
OCR_TIMEOUT_SECONDS=30
OCR_POOL_SIZE=4
```

A local Tesseract tessdata directory is required for real OCR execution; the default language expects `chi_sim.traineddata` and `eng.traineddata` under `./tessdata` unless overridden.

## Architecture

- `com.example.ocr.OcrApplication` is the Spring Boot entry point and enables async execution plus `OcrProperties` binding.
- `controller/OcrController` is the HTTP API layer under `/api/ocr`. It accepts multipart and Base64 inputs, delegates OCR work, exposes async task status, and reports engine/tessdata metadata.
- `service/OcrService` and `service/impl/OcrServiceImpl` contain the main routing logic: validate input size, detect PDF vs image via `FileTypeDetector`, then call the OCR engine. `OcrAsyncService` wraps the same service with an executor and task store.
- `service/OcrTaskStore` is implemented by `InMemoryOcrTaskStore`; async OCR task state is process-local and not durable.
- `core/engine/OcrEngine` is the engine abstraction. `Tess4jOcrEngine` implements it using PDFBox for PDF page rendering, the preprocess pipeline, a pooled Tess4J `Tesseract` instance, and the postprocess pipeline.
- `core/engine/TesseractInstancePool` eagerly creates a bounded pool sized by `ocr.pool-size`; request processing borrows an instance and waits up to `ocr.timeout-seconds`.
- `core/preprocess` and `core/postprocess` are ordered Spring component pipelines. Add new stages by implementing `ImagePreprocessor` or `OcrPostprocessor` and returning the desired `order()`.
- `config/OcrProperties` is the source of OCR limits and defaults: tessdata path, language, PSM, timeouts, pool size, file size, PDF page/render limits, image dimensions, and preprocessing enablement.
- `exception/GlobalExceptionHandler` converts validation/input failures to 400 responses and OCR/unexpected failures to 500 responses using the common `ApiResponse` wrapper.

## Testing notes

Tests are JUnit 5/Spring Boot tests. Controller tests use `@WebMvcTest` with mocked services; service tests use stubbed `OcrEngine` implementations rather than invoking real Tesseract. Prefer this style for unit-level behavior that does not need native OCR data.
