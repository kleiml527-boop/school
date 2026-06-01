package com.example.ocr.exception;

public class InvalidOcrInputException extends OcrException {

    public InvalidOcrInputException(String message) {
        super(message);
    }

    public InvalidOcrInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
