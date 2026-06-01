package com.example.ocr.core.engine;

import com.example.ocr.config.OcrProperties;
import com.example.ocr.exception.OcrException;
import net.sourceforge.tess4j.Tesseract;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TesseractInstancePool implements AutoCloseable {

    private final OcrProperties properties;
    private final ArrayBlockingQueue<Tesseract> pool;

    public TesseractInstancePool(OcrProperties properties) {
        this.properties = properties;
        this.pool = new ArrayBlockingQueue<>(properties.getPoolSize());
        for (int i = 0; i < properties.getPoolSize(); i++) {
            pool.add(createInstance());
        }
    }

    public Tesseract borrowInstance() {
        try {
            Tesseract instance = pool.poll(Duration.ofSeconds(properties.getTimeoutSeconds()).toMillis(), TimeUnit.MILLISECONDS);
            if (instance == null) {
                throw new OcrException("No OCR engine instance available");
            }
            return instance;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrException("Interrupted while waiting for OCR engine instance", e);
        }
    }

    public void returnInstance(Tesseract instance) {
        if (instance != null) {
            pool.offer(instance);
        }
    }

    private Tesseract createInstance() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(properties.getTessdataPath());
        tesseract.setLanguage(properties.getLanguage());
        tesseract.setPageSegMode(properties.getPsm());
        tesseract.setVariable("user_defined_dpi", String.valueOf(properties.getTesseractDpi()));
        if (properties.isPreserveInterwordSpaces()) {
            tesseract.setVariable("preserve_interword_spaces", "1");
        }
        return tesseract;
    }

    @Override
    public void close() {
        pool.clear();
    }
}
