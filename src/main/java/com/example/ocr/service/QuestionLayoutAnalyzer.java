package com.example.ocr.service;

import com.example.ocr.dto.OcrResult;
import com.example.ocr.dto.OcrTextBlock;
import com.example.ocr.dto.QuestionRegion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuestionLayoutAnalyzer {

    public static final String REVIEW_REASON_LOW_CONFIDENCE = "LOW_CONFIDENCE";
    public static final String REVIEW_REASON_MISSING_QUESTION_NUMBER = "MISSING_QUESTION_NUMBER";

    private static final double LOW_CONFIDENCE_THRESHOLD = 60.0;
    private static final String QUESTION_NUMBER = "0-9０-９一二三四五六七八九十百千零〇两兩壹贰貳叁參肆伍陆陸柒捌玖拾佰仟";
    private static final Pattern FULLWIDTH_DIGIT = Pattern.compile("[０-９]");
    private static final Pattern STANDALONE_MARKER = Pattern.compile("^\\s*(?:[/／]\\s*)?(?:第\\s*)?(?:[" + QUESTION_NUMBER + "]+)\\s*(?:题|題|[.．、,，:：)）])?\\s*$");
    private static final List<Pattern> QUESTION_START_PATTERNS = List.of(
            Pattern.compile("^\\s*第\\s*([" + QUESTION_NUMBER + "]+)\\s*[题題].*"),
            Pattern.compile("^\\s*[（(]\\s*([" + QUESTION_NUMBER + "]+)\\s*[）)].*"),
            Pattern.compile("^\\s*(?:[/／]\\s*)?([" + QUESTION_NUMBER + "]+)\\s*[.．、,，:：)]\\s*.*"),
            Pattern.compile("^\\s*([0-9０-９]{1,3})\\s+(?=\\S).*"),
            Pattern.compile("^\\s*([一二三四五六七八九十百千零〇两兩壹贰貳叁參肆伍陆陸柒捌玖拾佰仟]{1,6})\\s+(?=\\S).*"),
            Pattern.compile("^\\s*([0-9０-９]{1,3}|[一二三四五六七八九十百千零〇两兩壹贰貳叁參肆伍陆陸柒捌玖拾佰仟]{1,6})\\s*$")
    );

    public List<QuestionRegion> analyze(OcrResult ocrResult) {
        if (ocrResult == null || ocrResult.getBlocks() == null || ocrResult.getBlocks().isEmpty()) {
            return List.of();
        }

        List<LayoutBlock> blocks = ocrResult.getBlocks().stream()
                .filter(block -> block != null && block.getText() != null && !block.getText().isBlank())
                .map(block -> new LayoutBlock(block, page(block, ocrResult)))
                .toList();
        if (blocks.isEmpty()) {
            return List.of();
        }

        List<TextUnit> units = new ArrayList<>();
        for (Map.Entry<Integer, List<LayoutBlock>> pageEntry : groupByPage(blocks).entrySet()) {
            units.addAll(pageUnits(pageEntry.getValue(), pageEntry.getKey()));
        }
        units.sort(Comparator.comparingInt(TextUnit::page)
                .thenComparingInt(TextUnit::column)
                .thenComparingInt(TextUnit::y)
                .thenComparingInt(TextUnit::x));

        return collectRegions(units);
    }

    public List<QuestionRegion> analyze(List<OcrResult> ocrResults) {
        if (ocrResults == null || ocrResults.isEmpty()) {
            return List.of();
        }
        return ocrResults.stream()
                .flatMap(result -> analyze(result).stream())
                .sorted(Comparator.comparingInt(QuestionRegion::getPage)
                        .thenComparingInt(QuestionRegion::getX)
                        .thenComparingInt(QuestionRegion::getY))
                .toList();
    }

    private int page(OcrTextBlock block, OcrResult result) {
        if (block.getPage() > 0) {
            return block.getPage();
        }
        return result.getPage();
    }

    private Map<Integer, List<LayoutBlock>> groupByPage(List<LayoutBlock> blocks) {
        Map<Integer, List<LayoutBlock>> byPage = new LinkedHashMap<>();
        blocks.stream()
                .sorted(Comparator.comparingInt(LayoutBlock::page)
                        .thenComparingInt(LayoutBlock::y)
                        .thenComparingInt(LayoutBlock::x))
                .forEach(block -> byPage.computeIfAbsent(block.page(), ignored -> new ArrayList<>()).add(block));
        return byPage;
    }

    private List<TextUnit> pageUnits(List<LayoutBlock> pageBlocks, int page) {
        List<LineBand> bands = lineBands(pageBlocks);
        int pageWidth = pageWidth(pageBlocks);
        int splitGap = Math.max(80, (int) Math.round(pageWidth * 0.12));
        int markerSplitGap = Math.max(12, medianHeight(pageBlocks));

        List<TextUnit> units = new ArrayList<>();
        for (LineBand band : bands) {
            units.addAll(splitBand(band, page, splitGap, markerSplitGap));
        }
        assignColumns(units, pageWidth);
        return units;
    }

    private List<LineBand> lineBands(List<LayoutBlock> pageBlocks) {
        List<LayoutBlock> sorted = pageBlocks.stream()
                .sorted(Comparator.comparingInt(LayoutBlock::centerY)
                        .thenComparingInt(LayoutBlock::x))
                .toList();
        List<LineBand> bands = new ArrayList<>();
        for (LayoutBlock block : sorted) {
            LineBand target = null;
            for (LineBand band : bands) {
                if (band.accepts(block)) {
                    target = band;
                    break;
                }
            }
            if (target == null) {
                target = new LineBand();
                bands.add(target);
            }
            target.add(block);
        }
        bands.sort(Comparator.comparingInt(LineBand::y).thenComparingInt(LineBand::x));
        return bands;
    }

    private List<TextUnit> splitBand(LineBand band, int page, int splitGap, int markerSplitGap) {
        List<LayoutBlock> sorted = band.blocks().stream()
                .sorted(Comparator.comparingInt(LayoutBlock::x))
                .toList();
        List<TextUnit> units = new ArrayList<>();
        List<LayoutBlock> current = new ArrayList<>();
        LayoutBlock previous = null;
        for (LayoutBlock block : sorted) {
            if (previous != null && shouldSplit(previous, block, current, splitGap, markerSplitGap)) {
                units.add(new TextUnit(page, current));
                current = new ArrayList<>();
            }
            current.add(block);
            previous = block;
        }
        if (!current.isEmpty()) {
            units.add(new TextUnit(page, current));
        }
        return units;
    }

    private boolean shouldSplit(LayoutBlock previous, LayoutBlock block, List<LayoutBlock> current, int splitGap, int markerSplitGap) {
        int gap = block.x() - previous.right();
        if (gap > splitGap) {
            return true;
        }
        return gap > markerSplitGap && !current.isEmpty() && isStandaloneQuestionMarker(block.text());
    }

    private boolean isStandaloneQuestionMarker(String text) {
        return text != null && STANDALONE_MARKER.matcher(text.trim()).matches();
    }

    private void assignColumns(List<TextUnit> units, int pageWidth) {
        List<TextUnit> sorted = units.stream()
                .sorted(Comparator.comparingInt(TextUnit::x)
                        .thenComparingInt(TextUnit::y))
                .toList();
        List<Column> columns = new ArrayList<>();
        int mergeGap = Math.max(40, (int) Math.round(pageWidth * 0.06));
        for (TextUnit unit : sorted) {
            Column column = matchingColumn(columns, unit, mergeGap);
            if (column == null) {
                column = new Column();
                columns.add(column);
            }
            column.add(unit);
        }
        columns.sort(Comparator.comparingInt(Column::minX));
        for (int i = 0; i < columns.size(); i++) {
            for (TextUnit unit : columns.get(i).units()) {
                unit.setColumn(i);
            }
        }
    }

    private Column matchingColumn(List<Column> columns, TextUnit unit, int mergeGap) {
        return columns.stream()
                .filter(column -> unit.x() <= column.maxRight() + mergeGap && unit.right() >= column.minX() - mergeGap)
                .min(Comparator.comparingInt(column -> Math.abs(column.minX() - unit.x())))
                .orElse(null);
    }

    private List<QuestionRegion> collectRegions(List<TextUnit> units) {
        List<QuestionRegion> regions = new ArrayList<>();
        RegionBuilder current = null;
        for (TextUnit unit : units) {
            QuestionStart questionStart = questionStart(unit);
            if (questionStart.matched()) {
                if (current != null) {
                    regions.add(current.build());
                }
                current = new RegionBuilder(questionStart.questionId());
                current.add(unit);
                continue;
            }

            if (current != null && current.canAppend(unit)) {
                current.add(unit);
            } else {
                if (current != null) {
                    regions.add(current.build());
                }
                current = new RegionBuilder(null);
                current.add(unit);
            }
        }
        if (current != null) {
            regions.add(current.build());
        }
        return regions;
    }

    private QuestionStart questionStart(TextUnit unit) {
        QuestionStart fromText = questionStart(unit.text());
        if (fromText.matched()) {
            return fromText;
        }
        if (unit.blocks.isEmpty()) {
            return QuestionStart.notMatched();
        }
        return questionStart(unit.blocks.get(0).text());
    }

    private QuestionStart questionStart(String text) {
        if (text == null || text.isBlank()) {
            return QuestionStart.notMatched();
        }
        String normalized = text.trim();
        for (Pattern pattern : QUESTION_START_PATTERNS) {
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.matches()) {
                return new QuestionStart(true, normalizeQuestionId(matcher.group(1)));
            }
        }
        return QuestionStart.notMatched();
    }

    private String normalizeQuestionId(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (char ch : value.trim().toCharArray()) {
            Matcher matcher = FULLWIDTH_DIGIT.matcher(String.valueOf(ch));
            if (matcher.matches()) {
                builder.append((char) ('0' + ch - '０'));
            } else if (!Character.isWhitespace(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private int pageWidth(List<LayoutBlock> blocks) {
        int minX = blocks.stream().mapToInt(LayoutBlock::x).min().orElse(0);
        int maxRight = blocks.stream().mapToInt(LayoutBlock::right).max().orElse(minX);
        return Math.max(1, maxRight - minX);
    }

    private int medianHeight(List<LayoutBlock> blocks) {
        List<Integer> heights = blocks.stream()
                .map(LayoutBlock::height)
                .filter(height -> height > 0)
                .sorted()
                .toList();
        if (heights.isEmpty()) {
            return 12;
        }
        return heights.get(heights.size() / 2);
    }

    private final class RegionBuilder {
        private final String questionId;
        private final List<TextUnit> units = new ArrayList<>();
        private TextUnit lastUnit;

        private RegionBuilder(String questionId) {
            this.questionId = questionId;
        }

        private void add(TextUnit unit) {
            units.add(unit);
            lastUnit = unit;
        }

        private boolean canAppend(TextUnit unit) {
            if (lastUnit == null) {
                return false;
            }
            int verticalGap = unit.y() - lastUnit.bottom();
            int threshold = Math.max(60, Math.max(lastUnit.height(), unit.height()) * 3);
            if (verticalGap > threshold) {
                return false;
            }
            if (questionId != null && units.size() == 1 && isStandaloneQuestionMarker(lastUnit.text()) && lastUnit.page() == unit.page()) {
                return true;
            }
            return lastUnit.column() == unit.column();
        }

        private QuestionRegion build() {
            QuestionRegion region = new QuestionRegion();
            region.setQuestionId(questionId);
            region.setPage(units.get(0).page());
            region.setBlocks(blocks());
            region.setText(text());
            region.setConfidence(confidence(region.getBlocks()));
            applyBounds(region, region.getBlocks());
            applyReview(region);
            return region;
        }

        private List<OcrTextBlock> blocks() {
            return units.stream()
                    .flatMap(unit -> unit.originalBlocks().stream())
                    .toList();
        }

        private String text() {
            return String.join("\n", units.stream().map(TextUnit::text).toList()).trim();
        }

        private double confidence(List<OcrTextBlock> blocks) {
            if (blocks.isEmpty()) {
                return 0;
            }
            return blocks.stream().mapToDouble(OcrTextBlock::getConfidence).average().orElse(0);
        }

        private void applyBounds(QuestionRegion region, List<OcrTextBlock> blocks) {
            int minX = blocks.stream().mapToInt(OcrTextBlock::getX).min().orElse(0);
            int minY = blocks.stream().mapToInt(OcrTextBlock::getY).min().orElse(0);
            int maxRight = blocks.stream().mapToInt(block -> block.getX() + Math.max(0, block.getWidth())).max().orElse(minX);
            int maxBottom = blocks.stream().mapToInt(block -> block.getY() + Math.max(0, block.getHeight())).max().orElse(minY);
            region.setX(minX);
            region.setY(minY);
            region.setWidth(Math.max(0, maxRight - minX));
            region.setHeight(Math.max(0, maxBottom - minY));
        }

        private void applyReview(QuestionRegion region) {
            List<String> reasons = new ArrayList<>();
            if (region.getQuestionId() == null || region.getQuestionId().isBlank()) {
                reasons.add(REVIEW_REASON_MISSING_QUESTION_NUMBER);
            }
            if (region.getConfidence() < LOW_CONFIDENCE_THRESHOLD) {
                reasons.add(REVIEW_REASON_LOW_CONFIDENCE);
            }
            region.setReviewReasons(reasons);
            region.setNeedsReview(!reasons.isEmpty());
        }
    }

    private static final class LayoutBlock {
        private final OcrTextBlock original;
        private final int page;

        private LayoutBlock(OcrTextBlock original, int page) {
            this.original = original;
            this.page = page;
        }

        private OcrTextBlock original() {
            return original;
        }

        private int page() {
            return page;
        }

        private String text() {
            return original.getText().trim();
        }

        private int x() {
            return original.getX();
        }

        private int y() {
            return original.getY();
        }

        private int width() {
            return Math.max(0, original.getWidth());
        }

        private int height() {
            return Math.max(0, original.getHeight());
        }

        private int right() {
            return x() + width();
        }

        private int bottom() {
            return y() + height();
        }

        private int centerY() {
            return y() + height() / 2;
        }
    }

    private static final class LineBand {
        private final List<LayoutBlock> blocks = new ArrayList<>();
        private int centerY;
        private int maxHeight;

        private boolean accepts(LayoutBlock block) {
            if (blocks.isEmpty()) {
                return true;
            }
            int threshold = Math.max(8, Math.max(maxHeight, block.height()) / 2);
            return Math.abs(block.centerY() - centerY) <= threshold || verticalOverlap(block) > 0;
        }

        private int verticalOverlap(LayoutBlock block) {
            int top = blocks.stream().mapToInt(LayoutBlock::y).min().orElse(block.y());
            int bottom = blocks.stream().mapToInt(LayoutBlock::bottom).max().orElse(block.bottom());
            return Math.min(bottom, block.bottom()) - Math.max(top, block.y());
        }

        private void add(LayoutBlock block) {
            blocks.add(block);
            centerY = (int) Math.round(blocks.stream().mapToInt(LayoutBlock::centerY).average().orElse(block.centerY()));
            maxHeight = blocks.stream().mapToInt(LayoutBlock::height).max().orElse(block.height());
        }

        private List<LayoutBlock> blocks() {
            return blocks;
        }

        private int x() {
            return blocks.stream().mapToInt(LayoutBlock::x).min().orElse(0);
        }

        private int y() {
            return blocks.stream().mapToInt(LayoutBlock::y).min().orElse(0);
        }
    }

    private static final class TextUnit {
        private final int page;
        private final List<LayoutBlock> blocks;
        private int column;

        private TextUnit(int page, List<LayoutBlock> blocks) {
            this.page = page;
            this.blocks = blocks.stream()
                    .sorted(Comparator.comparingInt(LayoutBlock::x))
                    .toList();
        }

        private int page() {
            return page;
        }

        private int column() {
            return column;
        }

        private void setColumn(int column) {
            this.column = column;
        }

        private int x() {
            return blocks.stream().mapToInt(LayoutBlock::x).min().orElse(0);
        }

        private int y() {
            return blocks.stream().mapToInt(LayoutBlock::y).min().orElse(0);
        }

        private int right() {
            return blocks.stream().mapToInt(LayoutBlock::right).max().orElse(x());
        }

        private int bottom() {
            return blocks.stream().mapToInt(LayoutBlock::bottom).max().orElse(y());
        }

        private int height() {
            return Math.max(0, bottom() - y());
        }

        private String text() {
            String joined = String.join(" ", blocks.stream().map(LayoutBlock::text).toList());
            return normalizeSpacing(joined);
        }

        private List<OcrTextBlock> originalBlocks() {
            return blocks.stream().map(LayoutBlock::original).toList();
        }

        private static String normalizeSpacing(String text) {
            return text.replaceAll("\\s+([.．、,，:：;；?!？）)])", "$1")
                    .replaceAll("(?<=[\\p{IsHan}、，：；？！])\\s+(?=\\p{IsHan})", "")
                    .replaceAll("([（(])\\s+", "$1")
                    .replaceAll("第\\s+([" + QUESTION_NUMBER + "]+)\\s+([题題])", "第$1$2")
                    .replaceAll("\\s{2,}", " ")
                    .trim();
        }
    }

    private static final class Column {
        private final List<TextUnit> units = new ArrayList<>();
        private int minX = Integer.MAX_VALUE;
        private int maxRight = Integer.MIN_VALUE;

        private void add(TextUnit unit) {
            units.add(unit);
            minX = Math.min(minX, unit.x());
            maxRight = Math.max(maxRight, unit.right());
        }

        private List<TextUnit> units() {
            return units;
        }

        private int minX() {
            return minX;
        }

        private int maxRight() {
            return maxRight;
        }
    }

    private static final class QuestionStart {
        private final boolean matched;
        private final String questionId;

        private QuestionStart(boolean matched, String questionId) {
            this.matched = matched;
            this.questionId = questionId;
        }

        private boolean matched() {
            return matched;
        }

        private String questionId() {
            return questionId;
        }

        private static QuestionStart notMatched() {
            return new QuestionStart(false, null);
        }
    }
}
