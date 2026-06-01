package com.example.ocr.service;

import com.example.ocr.dto.OcrOptions;
import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import com.example.ocr.dto.QuestionRegion;
import com.example.ocr.dto.WrongQuestionItem;
import com.example.ocr.dto.WrongQuestionResult;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WrongQuestionExtractor {

    private static final String QUESTION_NUMBER = "[0-9０-９一二三四五六七八九十百千万两]+";
    private static final Pattern QUESTION_START = Pattern.compile(
            "^\\s*(?:[/／]\\s*)?(?:"
                    + "第\\s*(" + QUESTION_NUMBER + ")\\s*[题題]"
                    + "|[（(]\\s*(" + QUESTION_NUMBER + ")\\s*[）)]"
                    + "|(" + QUESTION_NUMBER + ")\\s*(?:[\\.．、,，:：)]|(?=\\s+[^0-9０-９+\\-*/／÷=])|$)"
                    + ")\\s*(.*)$");
    private static final Pattern QUESTION_MARKER = Pattern.compile(
            "(?m)(^|[\\s；;。])(?:[/／]\\s*)?(?:"
                    + "第\\s*" + QUESTION_NUMBER + "\\s*[题題]"
                    + "|[（(]\\s*" + QUESTION_NUMBER + "\\s*[）)]"
                    + "|" + QUESTION_NUMBER + "\\s*(?:[\\.．、,，:：)]|(?=\\s+[^0-9０-９+\\-*/／÷=]))"
                    + ")");
    private static final String METADATA_LABEL = "(?:正确答案|参考答案|标准答案|学生答案|我的答案|作答|答案|错误类型|错因|原因|知识点|考点)";
    private static final int ANSWER_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern STUDENT_ANSWER = Pattern.compile(
            "(?<!正确)(?<!参考)(?<!标准)(?:学生答案|我的答案|作答|答案)\\s*[:：]?\\s*(.+?)(?=\\s*"
                    + METADATA_LABEL + "\\s*[:：]?|[；;\\r\\n]|$)", ANSWER_FLAGS);
    private static final Pattern CORRECT_ANSWER = Pattern.compile(
            "(?:正确答案|参考答案|标准答案)\\s*[:：]?\\s*(.+?)(?=\\s*"
                    + METADATA_LABEL + "\\s*[:：]?|[；;\\r\\n]|$)", ANSWER_FLAGS);
    private static final Pattern TYPE = Pattern.compile("(选择题|填空题|判断题|计算题|应用题|解答题)");
    private static final Pattern ERROR_TYPE = Pattern.compile("(?:错误类型|错因|原因)\\s*[:：]?\\s*([^\\n；;]+)");
    private static final Pattern KNOWLEDGE_POINT = Pattern.compile("(?:知识点|考点)\\s*[:：]?\\s*([^\\n；;]+)");
    private static final Pattern WRONG_MARK_LABEL = Pattern.compile("(?:批改|判定|标记|结果|状态|marking(?:Status)?)\\s*[:：]?\\s*(?:wrong|错|错误)", ANSWER_FLAGS);
    private static final Pattern CORRECT_MARK_LABEL = Pattern.compile("(?:批改|判定|标记|结果|状态|marking(?:Status)?)\\s*[:：]?\\s*(?:correct|对|正确)", ANSWER_FLAGS);
    private static final Pattern STANDALONE_WRONG_MARK = Pattern.compile("(^|[\\s，,；;。])(?:错|错误)(?=$|[\\s，,；;。])");
    private static final Pattern STANDALONE_CORRECT_MARK = Pattern.compile("(^|[\\s，,；;。])(?:对|正确)(?=$|[\\s，,；;。])");
    private static final double LOW_CONFIDENCE_THRESHOLD = 60.0;
    private static final int SHORT_TEXT_THRESHOLD = 6;

    private final QuestionLayoutAnalyzer questionLayoutAnalyzer;

    public WrongQuestionExtractor() {
        this(new QuestionLayoutAnalyzer());
    }

    public WrongQuestionExtractor(QuestionLayoutAnalyzer questionLayoutAnalyzer) {
        this.questionLayoutAnalyzer = questionLayoutAnalyzer;
    }

    public WrongQuestionResult extract(List<OcrResult> ocrResults) {
        return extract(ocrResults, null);
    }

    public WrongQuestionResult extract(List<OcrResult> ocrResults, OcrOptions options) {
        WrongQuestionResult result = new WrongQuestionResult();
        if (ocrResults == null || ocrResults.isEmpty()) {
            result.setPageCount(0);
            result.setRawText("");
            return result;
        }

        List<WrongQuestionItem> questions = new ArrayList<>();
        List<QuestionRegion> questionRegions = new ArrayList<>();
        StringBuilder rawText = new StringBuilder();
        for (OcrResult ocrResult : ocrResults) {
            if (ocrResult == null) {
                continue;
            }
            applyProfile(ocrResult, options);
            String pageText = pageText(ocrResult);
            if (!pageText.isBlank()) {
                appendLine(rawText, pageText);
            }
            List<QuestionRegion> explicitRegions = safeRegions(ocrResult.getQuestionRegions());
            if (!explicitRegions.isEmpty()) {
                prepareRegions(explicitRegions, ocrResult);
                questionRegions.addAll(explicitRegions);
                questions.addAll(extractRegions(ocrResult, explicitRegions));
                continue;
            }
            List<QuestionRegion> blockRegions = questionLayoutAnalyzer.analyze(ocrResult);
            if (!blockRegions.isEmpty() && shouldUseBlockRegions(ocrResult, blockRegions)) {
                prepareRegions(blockRegions, ocrResult);
                ocrResult.setQuestionRegions(blockRegions);
                questionRegions.addAll(blockRegions);
                questions.addAll(extractRegions(ocrResult, blockRegions));
                continue;
            }
            if (ocrResult.getText() != null && !ocrResult.getText().isBlank()) {
                questions.addAll(extractPageFallback(ocrResult));
            }
        }
        result.setQuestions(questions);
        result.setQuestionRegions(questionRegions);
        result.setRawText(rawText.toString());
        result.setPageCount(ocrResults.size());
        result.setOcrEngine(firstResultValue(ocrResults, OcrResult::getEngine));
        result.setPreprocessProfile(firstResultValue(ocrResults, OcrResult::getPreprocessProfile));
        applyResultReviewSignals(result, questions);
        return result;
    }

    private void applyProfile(OcrResult ocrResult, OcrOptions options) {
        if (ocrResult.getPreprocessProfile() == null && options != null && options.getPreprocessProfile() != null) {
            ocrResult.setPreprocessProfile(options.getPreprocessProfile());
        }
    }

    private void prepareRegions(List<QuestionRegion> regions, OcrResult result) {
        for (QuestionRegion region : regions) {
            if (region == null) {
                continue;
            }
            if (region.getOcrEngine() == null) {
                region.setOcrEngine(result.getEngine());
            }
            if (region.getPreprocessProfile() == null) {
                region.setPreprocessProfile(result.getPreprocessProfile());
            }
        }
    }

    private boolean shouldUseBlockRegions(OcrResult ocrResult, List<QuestionRegion> blockRegions) {
        if (ocrResult.getText() == null || ocrResult.getText().isBlank()) {
            return true;
        }
        String blockText = blockRegions.stream()
                .map(region -> firstNonBlank(region.getText(), textFromBlocks(region.getBlocks())))
                .filter(text -> text != null && !text.isBlank())
                .reduce("", (left, right) -> left + "\n" + right);
        int blockLength = blockText.replaceAll("\\s+", "").length();
        int fullTextLength = ocrResult.getText().replaceAll("\\s+", "").length();
        return blockLength >= Math.max(6, fullTextLength / 2);
    }

    private List<WrongQuestionItem> extractRegions(OcrResult ocrResult, List<QuestionRegion> regions) {
        List<WrongQuestionItem> questions = new ArrayList<>();
        for (QuestionRegion region : regions) {
            if (region == null) {
                continue;
            }
            WrongQuestionItem item = extractRegion(ocrResult, region);
            if (item != null) {
                questions.add(item);
            }
        }
        return questions;
    }

    private WrongQuestionItem extractRegion(OcrResult ocrResult, QuestionRegion region) {
        String text = firstNonBlank(region.getText(), textFromBlocks(region.getBlocks()));
        if (text == null || text.isBlank()) {
            return null;
        }

        QuestionStart questionStart = parseQuestionStart(firstContentLine(text));
        WrongQuestionItem item = new WrongQuestionItem();
        item.setRegion(region);
        item.setQuestionId(firstNonBlank(region.getQuestionId(), questionStart == null ? null : questionStart.questionId()));
        item.setPage(region.getPage() > 0 ? region.getPage() : ocrResult.getPage());
        item.setConfidence(regionConfidence(region, ocrResult));
        item.setRawText(text.trim());
        item.setOcrEngine(firstNonBlank(region.getOcrEngine(), ocrResult.getEngine()));
        item.setPreprocessProfile(firstNonBlank(region.getPreprocessProfile(), ocrResult.getPreprocessProfile()));
        item.setMarkingStatus(canonicalMarkingStatus(firstNonBlank(region.getMarkingStatus(), detectMarkingStatus(text))));
        applyRegionBounds(item, region);

        applyMetadata(item, text);
        String contentSource = questionStart == null ? text : replaceFirstContentLine(text, questionStart.remainder());
        item.setContent(cleanContent(contentSource));
        if (item.getType() == null) {
            item.setType(inferType(item.getContent()));
        }
        resolveWrongStatus(item);
        applyReviewSignals(item, region.isNeedsReview(), region.getReviewReasons());
        return isEmptyQuestion(item) ? null : item;
    }

    private List<WrongQuestionItem> extractPageFallback(OcrResult ocrResult) {
        List<WrongQuestionItem> questions = new ArrayList<>();
        WrongQuestionItem current = null;
        StringBuilder content = new StringBuilder();
        StringBuilder raw = new StringBuilder();
        for (String line : ocrResult.getText().split("\\R")) {
            String normalized = line.trim();
            if (normalized.isBlank()) {
                continue;
            }
            QuestionStart questionStart = parseQuestionStart(normalized);
            if (questionStart != null) {
                current = finish(current, content, raw);
                if (current != null) {
                    questions.add(current);
                }
                current = new WrongQuestionItem();
                current.setQuestionId(questionStart.questionId());
                current.setPage(ocrResult.getPage());
                current.setConfidence(averageConfidence(ocrResult.getBlocks()));
                current.setOcrEngine(ocrResult.getEngine());
                current.setPreprocessProfile(ocrResult.getPreprocessProfile());
                current.setMarkingStatus("unknown");
                content = new StringBuilder();
                raw = new StringBuilder();
                if (questionStart.remainder() != null && !questionStart.remainder().isBlank()) {
                    appendLine(content, questionStart.remainder().trim());
                }
            } else if (current == null) {
                current = new WrongQuestionItem();
                current.setPage(ocrResult.getPage());
                current.setConfidence(averageConfidence(ocrResult.getBlocks()));
                current.setOcrEngine(ocrResult.getEngine());
                current.setPreprocessProfile(ocrResult.getPreprocessProfile());
                current.setMarkingStatus("unknown");
                appendLine(content, normalized);
            } else {
                appendLine(content, normalized);
            }
            appendLine(raw, normalized);
            applyMetadata(current, normalized);
        }
        current = finish(current, content, raw);
        if (current != null) {
            questions.add(current);
        }
        return questions;
    }

    private WrongQuestionItem finish(WrongQuestionItem item, StringBuilder content, StringBuilder raw) {
        if (item == null) {
            return null;
        }
        item.setContent(cleanContent(content.toString()));
        item.setRawText(raw.toString());
        if (item.getType() == null) {
            item.setType(inferType(item.getContent()));
        }
        if (item.getMarkingStatus() == null || "unknown".equals(item.getMarkingStatus())) {
            item.setMarkingStatus(detectMarkingStatus(item.getRawText()));
        }
        resolveWrongStatus(item);
        applyReviewSignals(item, false, List.of());
        return isEmptyQuestion(item) ? null : item;
    }

    private void applyMetadata(WrongQuestionItem item, String text) {
        if (item == null || text == null || text.isBlank()) {
            return;
        }
        if (isBlank(item.getStudentAnswer())) {
            applyFirstMatch(text, STUDENT_ANSWER, item::setStudentAnswer);
        }
        if (isBlank(item.getCorrectAnswer())) {
            applyFirstMatch(text, CORRECT_ANSWER, item::setCorrectAnswer);
        }
        if (isBlank(item.getErrorType())) {
            applyFirstMatch(text, ERROR_TYPE, item::setErrorType);
        }
        if (isBlank(item.getKnowledgePoint())) {
            applyFirstMatch(text, KNOWLEDGE_POINT, item::setKnowledgePoint);
        }
        if (item.getType() == null) {
            applyFirstMatch(text, TYPE, item::setType);
        }
    }

    private void applyFirstMatch(String text, Pattern pattern, java.util.function.Consumer<String> setter) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = cleanMetadataValue(matcher.group(1));
            if (!value.isBlank()) {
                setter.accept(value);
            }
        }
    }

    private void resolveWrongStatus(WrongQuestionItem item) {
        if (!isBlank(item.getStudentAnswer()) && !isBlank(item.getCorrectAnswer())) {
            item.setWrong(!normalizeAnswer(item.getStudentAnswer()).equals(normalizeAnswer(item.getCorrectAnswer())));
            return;
        }
        String markingStatus = canonicalMarkingStatus(item.getMarkingStatus());
        item.setMarkingStatus(markingStatus);
        if ("wrong".equals(markingStatus)) {
            item.setWrong(true);
        } else if ("correct".equals(markingStatus)) {
            item.setWrong(false);
        } else {
            item.setWrong(null);
        }
    }

    private void applyReviewSignals(WrongQuestionItem item, boolean inheritedNeedsReview, List<String> inheritedReasons) {
        Set<String> reasons = new LinkedHashSet<>();
        if (inheritedReasons != null) {
            inheritedReasons.stream()
                    .filter(reason -> reason != null && !reason.isBlank())
                    .forEach(reasons::add);
        }
        if (inheritedNeedsReview && reasons.isEmpty()) {
            reasons.add("region-needs-review");
        }
        if (isBlank(item.getQuestionId())) {
            reasons.add("missing-question-id");
        }
        String compactContent = firstNonBlank(item.getContent(), item.getRawText());
        if (compactContent == null || compactContent.replaceAll("\\s+", "").length() < SHORT_TEXT_THRESHOLD) {
            reasons.add("text-too-short");
        }
        if (item.getConfidence() > 0 && item.getConfidence() < LOW_CONFIDENCE_THRESHOLD) {
            reasons.add("low-confidence");
        }
        if (countQuestionMarkers(item.getRawText()) > 1) {
            reasons.add("multiple-question-markers");
        }
        item.setReviewReasons(new ArrayList<>(reasons));
        item.setNeedsReview(!reasons.isEmpty());
    }

    private void applyResultReviewSignals(WrongQuestionResult result, List<WrongQuestionItem> questions) {
        Set<String> reasons = new LinkedHashSet<>();
        for (WrongQuestionItem question : questions) {
            if (question.isNeedsReview()) {
                reasons.addAll(question.getReviewReasons());
            }
        }
        result.setReviewReasons(new ArrayList<>(reasons));
        result.setNeedsReview(!reasons.isEmpty());
    }

    private void applyRegionBounds(WrongQuestionItem item, QuestionRegion region) {
        Bounds blockBounds = bounds(region.getBlocks());
        boolean hasRegionBounds = region.getWidth() > 0 || region.getHeight() > 0;
        item.setX(hasRegionBounds ? region.getX() : blockBounds == null ? region.getX() : blockBounds.x());
        item.setY(hasRegionBounds ? region.getY() : blockBounds == null ? region.getY() : blockBounds.y());
        item.setWidth(region.getWidth() > 0 ? region.getWidth() : blockBounds == null ? 0 : blockBounds.width());
        item.setHeight(region.getHeight() > 0 ? region.getHeight() : blockBounds == null ? 0 : blockBounds.height());
    }

    private Bounds bounds(List<OcrTextBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean hasBounds = false;
        for (OcrTextBlock block : blocks) {
            if (block == null) {
                continue;
            }
            minX = Math.min(minX, block.getX());
            minY = Math.min(minY, block.getY());
            maxX = Math.max(maxX, block.getX() + block.getWidth());
            maxY = Math.max(maxY, block.getY() + block.getHeight());
            hasBounds = true;
        }
        return hasBounds ? new Bounds(minX, minY, Math.max(0, maxX - minX), Math.max(0, maxY - minY)) : null;
    }

    private String pageText(OcrResult ocrResult) {
        if (ocrResult.getText() != null && !ocrResult.getText().isBlank()) {
            return ocrResult.getText();
        }
        List<QuestionRegion> regions = safeRegions(ocrResult.getQuestionRegions());
        if (!regions.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (QuestionRegion region : regions) {
                if (region != null) {
                    String text = firstNonBlank(region.getText(), textFromBlocks(region.getBlocks()));
                    if (!isBlank(text)) {
                        appendLine(builder, text.trim());
                    }
                }
            }
            return builder.toString();
        }
        if (ocrResult.getBlocks() != null && !ocrResult.getBlocks().isEmpty()) {
            return textFromBlocks(ocrResult.getBlocks());
        }
        return "";
    }

    private String textFromBlocks(List<OcrTextBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (BlockLine line : linesFromBlocks(blocks)) {
            appendLine(builder, line.text());
        }
        return builder.toString();
    }

    private List<BlockLine> linesFromBlocks(List<OcrTextBlock> blocks) {
        List<OcrTextBlock> sortedBlocks = blocks.stream()
                .filter(block -> block != null && block.getText() != null && !block.getText().isBlank())
                .sorted(Comparator.comparingInt(OcrTextBlock::getPage)
                        .thenComparingInt(OcrTextBlock::getY)
                        .thenComparingInt(OcrTextBlock::getX))
                .toList();
        List<List<OcrTextBlock>> grouped = new ArrayList<>();
        for (OcrTextBlock block : sortedBlocks) {
            if (grouped.isEmpty() || !sameLine(grouped.get(grouped.size() - 1), block)) {
                List<OcrTextBlock> line = new ArrayList<>();
                line.add(block);
                grouped.add(line);
            } else {
                grouped.get(grouped.size() - 1).add(block);
            }
        }
        List<BlockLine> lines = new ArrayList<>();
        for (List<OcrTextBlock> lineBlocks : grouped) {
            lineBlocks.sort(Comparator.comparingInt(OcrTextBlock::getX));
            lines.add(new BlockLine(lineBlocks, lineText(lineBlocks)));
        }
        return lines;
    }

    private boolean sameLine(List<OcrTextBlock> line, OcrTextBlock block) {
        OcrTextBlock first = line.get(0);
        int lineCenter = first.getY() + first.getHeight() / 2;
        int blockCenter = block.getY() + block.getHeight() / 2;
        int threshold = Math.max(8, Math.max(first.getHeight(), block.getHeight()) / 2);
        return first.getPage() == block.getPage() && Math.abs(lineCenter - blockCenter) <= threshold;
    }

    private String lineText(List<OcrTextBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        OcrTextBlock previous = null;
        for (OcrTextBlock block : blocks) {
            String token = block.getText().trim();
            if (token.isBlank()) {
                continue;
            }
            if (previous != null) {
                int gap = block.getX() - (previous.getX() + previous.getWidth());
                if (gap > 3) {
                    builder.append(' ');
                }
            }
            builder.append(token);
            previous = block;
        }
        return builder.toString().trim();
    }

    private QuestionStart parseQuestionStart(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        Matcher matcher = QUESTION_START.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }
        String questionId = firstNonBlank(matcher.group(1), matcher.group(2), matcher.group(3));
        if (isBlank(questionId)) {
            return null;
        }
        return new QuestionStart(questionId, matcher.group(4) == null ? "" : matcher.group(4).trim());
    }

    private String firstContentLine(String text) {
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }
        return text.trim();
    }

    private String replaceFirstContentLine(String text, String replacement) {
        String[] lines = text.split("\\R", -1);
        StringBuilder builder = new StringBuilder();
        boolean replaced = false;
        for (String line : lines) {
            if (!replaced && !line.isBlank()) {
                if (replacement != null && !replacement.isBlank()) {
                    appendLine(builder, replacement.trim());
                }
                replaced = true;
            } else if (!line.isBlank()) {
                appendLine(builder, line.trim());
            }
        }
        return builder.toString();
    }

    private String inferType(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = TYPE.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (content.matches("(?s).*[A-DＡ-Ｄ][\\.．、].*[B-DＢ-Ｄ][\\.．、].*")) {
            return "选择题";
        }
        return null;
    }

    private double regionConfidence(QuestionRegion region, OcrResult result) {
        if (region.getConfidence() > 0) {
            return region.getConfidence();
        }
        double blockConfidence = averageConfidence(region.getBlocks());
        return blockConfidence > 0 ? blockConfidence : averageConfidence(result.getBlocks());
    }

    private double averageConfidence(List<OcrTextBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return 0;
        }
        return blocks.stream().filter(block -> block != null).mapToDouble(OcrTextBlock::getConfidence).average().orElse(0);
    }

    private String cleanContent(String content) {
        if (content == null) {
            return "";
        }
        String cleaned = STUDENT_ANSWER.matcher(content).replaceAll("");
        cleaned = CORRECT_ANSWER.matcher(cleaned).replaceAll("");
        cleaned = ERROR_TYPE.matcher(cleaned).replaceAll("");
        cleaned = KNOWLEDGE_POINT.matcher(cleaned).replaceAll("");
        QuestionStart questionStart = parseQuestionStart(firstContentLine(cleaned));
        if (questionStart != null) {
            cleaned = replaceFirstContentLine(cleaned, questionStart.remainder());
        }
        return cleaned.replaceAll("[ \\t]+\\n", "\\n")
                .replaceAll("\\n[ \\t]+", "\\n")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private String cleanMetadataValue(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("^[：:，,；;\\s]+", "")
                .replaceAll("[。；;\\s]+$", "")
                .trim();
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        String normalized = Normalizer.normalize(answer.trim(), Normalizer.Form.NFKC)
                .replace('，', ',')
                .replace('、', ',')
                .replace('；', ';')
                .replace('（', '(')
                .replace('）', ')')
                .replace("√", "对")
                .replace("✓", "对")
                .replace("✔", "对")
                .replace("×", "错")
                .replace("✗", "错")
                .replace("✘", "错")
                .replaceAll("\\s+", "")
                .replaceAll("[。；;]+$", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.matches("[A-Z](,[A-Z])+")) {
            String[] parts = normalized.split(",");
            java.util.Arrays.sort(parts);
            return String.join(",", parts);
        }
        return normalized;
    }

    private String detectMarkingStatus(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        String contentOnly = cleanContent(text);
        if (WRONG_MARK_LABEL.matcher(text).find()
                || contentOnly.matches("(?s).*[×✗✘].*")
                || STANDALONE_WRONG_MARK.matcher(contentOnly).find()) {
            return "wrong";
        }
        if (CORRECT_MARK_LABEL.matcher(text).find()
                || contentOnly.matches("(?s).*[√✓✔].*")
                || STANDALONE_CORRECT_MARK.matcher(contentOnly).find()) {
            return "correct";
        }
        return "unknown";
    }

    private String canonicalMarkingStatus(String markingStatus) {
        if (markingStatus == null || markingStatus.isBlank()) {
            return "unknown";
        }
        String normalized = Normalizer.normalize(markingStatus.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if (normalized.contains("wrong") || normalized.contains("×") || normalized.contains("✗") || normalized.contains("✘")
                || normalized.contains("错") || normalized.contains("错误")) {
            return "wrong";
        }
        if (normalized.contains("correct") || normalized.contains("√") || normalized.contains("✓") || normalized.contains("✔")
                || normalized.contains("对") || normalized.contains("正确")) {
            return "correct";
        }
        return "unknown";
    }

    private int countQuestionMarkers(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Matcher matcher = QUESTION_MARKER.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private boolean isEmptyQuestion(WrongQuestionItem item) {
        return isBlank(item.getContent()) && isBlank(item.getQuestionId()) && isBlank(item.getRawText());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<QuestionRegion> safeRegions(List<QuestionRegion> regions) {
        return regions == null ? List.of() : regions;
    }

    private String firstResultValue(List<OcrResult> results, java.util.function.Function<OcrResult, String> getter) {
        for (OcrResult result : results) {
            if (result != null) {
                String value = getter.apply(result);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private void appendLine(StringBuilder builder, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private record QuestionStart(String questionId, String remainder) {
    }

    private record BlockLine(List<OcrTextBlock> blocks, String text) {
    }

    private record Bounds(int x, int y, int width, int height) {
    }
}
