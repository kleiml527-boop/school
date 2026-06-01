package com.example.ocr.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {

    @NotBlank
    private String engine = "tess4j";

    @NotBlank
    private String tessdataPath = "./tessdata";

    @NotBlank
    private String language = "chi_sim+eng";

    @Min(0)
    @Max(13)
    private int psm = 3;

    @Min(1)
    private int timeoutSeconds = 30;

    @Min(1)
    private int poolSize = 4;

    @DataSizeUnit(DataUnit.BYTES)
    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    @Min(1)
    private int maxPdfPages = 20;

    @Min(72)
    private int pdfRenderDpi = 200;

    @Min(100)
    private int maxImageWidth = 4000;

    @Min(100)
    private int maxImageHeight = 4000;

    @Min(70)
    private int tesseractDpi = 300;

    private boolean preserveInterwordSpaces = true;

    private Preprocessing preprocessing = new Preprocessing();

    private Paddle paddle = new Paddle();

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getTessdataPath() {
        return tessdataPath;
    }

    public void setTessdataPath(String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getPsm() {
        return psm;
    }

    public void setPsm(int psm) {
        this.psm = psm;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxPdfPages() {
        return maxPdfPages;
    }

    public void setMaxPdfPages(int maxPdfPages) {
        this.maxPdfPages = maxPdfPages;
    }

    public int getPdfRenderDpi() {
        return pdfRenderDpi;
    }

    public void setPdfRenderDpi(int pdfRenderDpi) {
        this.pdfRenderDpi = pdfRenderDpi;
    }

    public int getMaxImageWidth() {
        return maxImageWidth;
    }

    public void setMaxImageWidth(int maxImageWidth) {
        this.maxImageWidth = maxImageWidth;
    }

    public int getMaxImageHeight() {
        return maxImageHeight;
    }

    public void setMaxImageHeight(int maxImageHeight) {
        this.maxImageHeight = maxImageHeight;
    }

    public int getTesseractDpi() {
        return tesseractDpi;
    }

    public void setTesseractDpi(int tesseractDpi) {
        this.tesseractDpi = tesseractDpi;
    }

    public boolean isPreserveInterwordSpaces() {
        return preserveInterwordSpaces;
    }

    public void setPreserveInterwordSpaces(boolean preserveInterwordSpaces) {
        this.preserveInterwordSpaces = preserveInterwordSpaces;
    }

    public Preprocessing getPreprocessing() {
        return preprocessing;
    }

    public void setPreprocessing(Preprocessing preprocessing) {
        this.preprocessing = preprocessing;
    }

    public Paddle getPaddle() {
        return paddle;
    }

    public void setPaddle(Paddle paddle) {
        this.paddle = paddle;
    }

    public static class Preprocessing {
        private boolean enabled = true;

        private String profile = "chinese-math";

        private boolean binarizationEnabled = false;

        private boolean denoise = false;

        private boolean deskew = false;

        @Min(0)
        @Max(255)
        private int denoiseThreshold = 18;

        @Min(0)
        @Max(15)
        private double deskewMaxAngle = 5.0;

        @Min(3)
        private int adaptiveThresholdWindowSize = 31;

        @Min(0)
        private int adaptiveThresholdBias = 10;

        private boolean upscaleSmallImages = true;

        private boolean enhanceContrast = true;

        @Min(0)
        private double contrastFactor = 1.2;

        @Min(-255)
        @Max(255)
        private int brightnessOffset = 5;

        private boolean sharpen = false;

        @Min(100)
        private int minImageWidth = 1200;

        @Min(100)
        private int minImageHeight = 1200;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public boolean isBinarizationEnabled() {
            return binarizationEnabled;
        }

        public void setBinarizationEnabled(boolean binarizationEnabled) {
            this.binarizationEnabled = binarizationEnabled;
        }

        public boolean isDenoise() {
            return denoise;
        }

        public void setDenoise(boolean denoise) {
            this.denoise = denoise;
        }

        public boolean isDeskew() {
            return deskew;
        }

        public void setDeskew(boolean deskew) {
            this.deskew = deskew;
        }

        public int getDenoiseThreshold() {
            return denoiseThreshold;
        }

        public void setDenoiseThreshold(int denoiseThreshold) {
            this.denoiseThreshold = denoiseThreshold;
        }

        public double getDeskewMaxAngle() {
            return deskewMaxAngle;
        }

        public void setDeskewMaxAngle(double deskewMaxAngle) {
            this.deskewMaxAngle = deskewMaxAngle;
        }

        public int getAdaptiveThresholdWindowSize() {
            return adaptiveThresholdWindowSize;
        }

        public void setAdaptiveThresholdWindowSize(int adaptiveThresholdWindowSize) {
            this.adaptiveThresholdWindowSize = adaptiveThresholdWindowSize;
        }

        public int getAdaptiveThresholdBias() {
            return adaptiveThresholdBias;
        }

        public void setAdaptiveThresholdBias(int adaptiveThresholdBias) {
            this.adaptiveThresholdBias = adaptiveThresholdBias;
        }

        public boolean isUpscaleSmallImages() {
            return upscaleSmallImages;
        }

        public void setUpscaleSmallImages(boolean upscaleSmallImages) {
            this.upscaleSmallImages = upscaleSmallImages;
        }

        public boolean isEnhanceContrast() {
            return enhanceContrast;
        }

        public void setEnhanceContrast(boolean enhanceContrast) {
            this.enhanceContrast = enhanceContrast;
        }

        public double getContrastFactor() {
            return contrastFactor;
        }

        public void setContrastFactor(double contrastFactor) {
            this.contrastFactor = contrastFactor;
        }

        public int getBrightnessOffset() {
            return brightnessOffset;
        }

        public void setBrightnessOffset(int brightnessOffset) {
            this.brightnessOffset = brightnessOffset;
        }

        public boolean isSharpen() {
            return sharpen;
        }

        public void setSharpen(boolean sharpen) {
            this.sharpen = sharpen;
        }

        public int getMinImageWidth() {
            return minImageWidth;
        }

        public void setMinImageWidth(int minImageWidth) {
            this.minImageWidth = minImageWidth;
        }

        public int getMinImageHeight() {
            return minImageHeight;
        }

        public void setMinImageHeight(int minImageHeight) {
            this.minImageHeight = minImageHeight;
        }
    }

    public static class Paddle {
        private boolean enabled = false;

        private String command = "";

        private String workingDirectory = "";

        @NotBlank
        private String language = "ch";

        private boolean useAngleCls = true;

        @Min(1)
        private int timeoutSeconds = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public boolean isUseAngleCls() {
            return useAngleCls;
        }

        public void setUseAngleCls(boolean useAngleCls) {
            this.useAngleCls = useAngleCls;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
