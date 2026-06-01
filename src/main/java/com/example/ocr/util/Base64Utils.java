package com.example.ocr.util;

import com.example.ocr.exception.InvalidOcrInputException;

import java.util.Base64;

public final class Base64Utils {

    private Base64Utils() {
    }

    public static byte[] decode(String data) {
        if (data == null || data.isBlank()) {
            throw new InvalidOcrInputException("Base64 data is required");
        }
        String payload = data.trim();
        int comma = payload.indexOf(',');
        if (payload.startsWith("data:") && comma >= 0) {
            payload = payload.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new InvalidOcrInputException("Invalid Base64 data", e);
        }
    }
}
