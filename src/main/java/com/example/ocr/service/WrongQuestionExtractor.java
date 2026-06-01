package com.example.ocr.service;

import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.WrongQuestionItem;
import com.example.ocr.dto.WrongQuestionResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WrongQuestionExtractor {

    private static final Pattern QUESTION_START = Pattern.compile("^\\s*(?:第?([0-9０-９一二三四五六七八九十百]+)[题題]?[\\.．、)]?|[（(]([0-9０-９一二三四五六七八九十百]+)[）)])\\s*(.*)$");
    private static final Pattern STUDENT_ANSWER = Pattern.compile("(?:学生答案|我的答案|作答|答案)\\s*[:：]?\\s*([A-Za-zＡ-Ｚａ-ｚ0-9０-９一二三四五六七八九十百]+)");
    private static final Pattern CORRECT_ANSWER = Pattern.compile("(?:正确答案|参考答案|标准答案)\\s*[:：]?\\s*([A-Za-zＡ-Ｚａ-ｚ0-9０-９一二三四五六七八九十百]+)");
    private static final Pattern TYPE = Pattern.compile("(选择题|填空题|判断题|计算题|应用题|解答题)");
    private static final Pattern ERROR_TYPE = Pattern.compile("(?:错误类型|错因|原因)\\s*[:：]?\\s*([^\\n；;]+)");
    private static final Pattern KNOWLEDGE_POINT = Pattern.compile("(?:知识点|考点)\\s*[:：]?\\s*([^\\n；;]+)");

    public WrongQuestionResult extract(List<OcrResult> ocrResults) {
        WrongQuestionResult result = new WrongQuestionResult();
        if (ocrResults == null || ocrResults.isEmpty()) {
            result.setPageCount(0);
            result.setRawText("");
            return result;
        }

        List<WrongQuestionItem> questions = new ArrayList<>();
        StringBuilder rawText = new StringBuilder();
        for (OcrResult ocrResult : ocrResults) {
            if (ocrResult == null || ocrResult.getText() == null || ocrResult.getText().isBlank()) {
                continue;
            }
            if (!rawText.isEmpty()) {
                rawText.append('\n');
            }
            rawText.append(ocrResult.getText());
            questions.addAll(extractPage(ocrResult));
        }
        result.setQuestions(questions);
        result.setRawText(rawText.toString());
        result.setPageCount(ocrResults.size());
        return result;
    }

    private List<WrongQuestionItem> extractPage(OcrResult ocrResult) {
        List<WrongQuestionItem> questions = new ArrayList<>();
        WrongQuestionItem current = null;
        StringBuilder content = new StringBuilder();
        StringBuilder raw = new StringBuilder();
        for (String line : ocrResult.getText().split("\\R")) {
            String normalized = line.trim();
            if (normalized.isBlank()) {
                continue;
            }
            Matcher questionMatcher = QUESTION_START.matcher(normalized);
            if (questionMatcher.matches() && hasQuestionMarker(normalized)) {
                current = finish(current, content, raw);
                if (current != null) {
                    questions.add(current);
                }
                current = new WrongQuestionItem();
                current.setQuestionId(firstPresent(questionMatcher.group(1), questionMatcher.group(2)));
                current.setPage(ocrResult.getPage());
                current.setConfidence(averageConfidence(ocrResult));
                current.setMarkingStatus("unknown");
                content = new StringBuilder();
                raw = new StringBuilder();
                String remainder = questionMatcher.group(3);
                if (remainder != null && !remainder.isBlank()) {
                    appendLine(content, remainder.trim());
                }
            } else if (current == null) {
                current = new WrongQuestionItem();
                current.setPage(ocrResult.getPage());
                current.setConfidence(averageConfidence(ocrResult));
                current.setMarkingStatus("unknown");
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
        if (item.getWrong() == null && item.getStudentAnswer() != null && item.getCorrectAnswer() != null) {
            item.setWrong(!normalizeAnswer(item.getStudentAnswer()).equals(normalizeAnswer(item.getCorrectAnswer())));
        }
        return item.getContent().isBlank() && item.getQuestionId() == null ? null : item;
    }

    private void applyMetadata(WrongQuestionItem item, String line) {
        if (item == null) {
            return;
        }
        applyFirstMatch(line, STUDENT_ANSWER, item::setStudentAnswer);
        applyFirstMatch(line, CORRECT_ANSWER, item::setCorrectAnswer);
        applyFirstMatch(line, ERROR_TYPE, item::setErrorType);
        applyFirstMatch(line, KNOWLEDGE_POINT, item::setKnowledgePoint);
        if (item.getType() == null) {
            applyFirstMatch(line, TYPE, item::setType);
        }
    }

    private void applyFirstMatch(String line, Pattern pattern, java.util.function.Consumer<String> setter) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            setter.accept(matcher.group(1).trim());
        }
    }

    private boolean hasQuestionMarker(String line) {
        return line.matches("^\\s*(?:第?[0-9０-９一二三四五六七八九十百]+[题題\\.．、)]|[（(][0-9０-９一二三四五六七八九十百]+[）)]).*");
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

    private double averageConfidence(OcrResult result) {
        if (result.getBlocks() == null || result.getBlocks().isEmpty()) {
            return 0;
        }
        return result.getBlocks().stream().mapToDouble(block -> block.getConfidence()).average().orElse(0);
    }

    private String cleanContent(String content) {
        return content.replaceAll("(?:学生答案|我的答案|作答|答案|正确答案|参考答案|标准答案|错误类型|错因|原因|知识点|考点)\\s*[:：]?\\s*[^\\n；;]+", "")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().replace('（', '(').replace('）', ')').toUpperCase(Locale.ROOT);
    }

    private String firstPresent(String first, String second) {
        return first != null ? first : second;
    }

    private void appendLine(StringBuilder builder, String line) {
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
