package com.example.ocr.util;

import com.example.ocr.exception.InvalidOcrInputException;

public final class FileTypeDetector {

    private FileTypeDetector() {
    }

    public static FileType detect(byte[] data) {
        if (data == null || data.length < 4) {
            throw new InvalidOcrInputException("Input file is empty or too small");
        }
        if (startsWith(data, 0x25, 0x50, 0x44, 0x46)) {
            return FileType.PDF;
        }
        if (startsWith(data, 0x89, 0x50, 0x4E, 0x47)) {
            return FileType.IMAGE;
        }
        if (startsWith(data, 0xFF, 0xD8, 0xFF)) {
            return FileType.IMAGE;
        }
        if (startsWith(data, 0x42, 0x4D)) {
            return FileType.IMAGE;
        }
        throw new InvalidOcrInputException("Only JPG, PNG, BMP and PDF inputs are supported");
    }

    private static boolean startsWith(byte[] data, int... bytes) {
        if (data.length < bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if ((data[i] & 0xFF) != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    public enum FileType {
        IMAGE,
        PDF
    }
}
