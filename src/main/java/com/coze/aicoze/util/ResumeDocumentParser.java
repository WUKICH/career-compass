package com.coze.aicoze.util;

import cn.hutool.core.util.StrUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 简历文件 → 纯文本：保留段落结构、DOCX 表格、PDF 阅读顺序；超长时按章节分配 LLM 预算。
 */
public final class ResumeDocumentParser {

    public static final int MAX_LLM_TEXT_CHARS = 6000;

    private static final String MIDDLE_OMITTED_MARK =
            "\n\n...[本节内容已截断]...\n\n";

    private static final Pattern SECTION_HEADER = Pattern.compile(
            "^(?:(?:[一二三四五六七八九十]|[0-9]+)\\s*[、.．)]\\s*)?"
                    + "(个人信息|基本信息|联系方式|求职意向|工作经历|工作经验|工作及项目经历|实习经历|其他经历|项目经验|项目经历|教育经历|教育背景|在校经历|"
                    + "技能[/／]?证书[^\\n]{0,12}|技能特长|专业技能|核心技能|技术栈|荣誉奖项|证书荣誉|获奖情况|自我评价|个人评价|培训经历)"
                    + "\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HORIZONTAL_SPACE = Pattern.compile("[ \\t\\f\\x0B]+");
    private static final Pattern EXCESS_BLANK_LINES = Pattern.compile("\\n{3,}");

    private ResumeDocumentParser() {
    }

    public record ResumeParseText(String fullText, String textForLlm) {
    }

    public static ResumeParseText parse(MultipartFile file, String fileName) throws Exception {
        String raw = extractRawText(file, fileName);
        return toParseText(raw);
    }

    /** 本地文件解析（测试或脚本用）。 */
    public static ResumeParseText parseFromPath(Path path) throws Exception {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("简历文件不存在: " + path);
        }
        byte[] bytes = Files.readAllBytes(path);
        String fileName = path.getFileName().toString();
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            return toParseText(extractRawTextFromStream(is, fileName, bytes));
        }
    }

    private static ResumeParseText toParseText(String raw) {
        String full = normalizeResumeText(raw);
        String forLlm = buildTextForLlm(full, MAX_LLM_TEXT_CHARS);
        return new ResumeParseText(full, forLlm);
    }

    static String normalizeResumeText(String raw) {
        if (raw == null) {
            return "";
        }
        String[] lines = raw.replace('\u0000', ' ').replace('\r', '\n').split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String cleaned = HORIZONTAL_SPACE.matcher(line).replaceAll(" ").trim();
            cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
            if (cleaned.isEmpty()) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append('\n');
                }
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(cleaned);
        }
        return EXCESS_BLANK_LINES.matcher(sb.toString().trim()).replaceAll("\n\n");
    }

    static String buildTextForLlm(String fullText, int maxChars) {
        if (StrUtil.isBlank(fullText) || fullText.length() <= maxChars) {
            return fullText == null ? "" : fullText;
        }
        List<Section> sections = splitSections(fullText);
        if (sections.size() <= 1) {
            return headTailTruncate(fullText, maxChars);
        }
        return fitSectionsToBudget(sections, maxChars);
    }

    private static String extractRawText(MultipartFile file, String fileName) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return extractRawTextFromStream(is, fileName, file.getBytes());
        }
    }

    private static String extractRawTextFromStream(InputStream is, String fileName, byte[] rawBytes) throws Exception {
        String n = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) {
            return extractPdfTextSorted(is);
        }
        if (n.endsWith(".docx")) {
            return extractDocxText(is);
        }
        if (n.endsWith(".doc")) {
            return extractDocText(is);
        }
        return new String(rawBytes, StandardCharsets.UTF_8);
    }

    /**
     * PDF 提取：使用 PDFBox 内置按坐标排序，避免手写 writeString 重组导致丢字、乱序。
     */
    private static String extractPdfTextSorted(InputStream is) throws Exception {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private static String extractDocxText(InputStream is) throws Exception {
        try (XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (StrUtil.isNotBlank(text)) {
                        appendLine(sb, text.trim());
                    }
                } else if (element instanceof XWPFTable table) {
                    appendTable(sb, table);
                }
            }
            return sb.toString();
        }
    }

    private static void appendTable(StringBuilder sb, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = cell == null ? "" : StrUtil.blankToDefault(cell.getText(), "").trim();
                cellText = HORIZONTAL_SPACE.matcher(cellText.replace('\n', ' ')).replaceAll(" ");
                if (!cellText.isEmpty()) {
                    cells.add(cellText);
                }
            }
            if (!cells.isEmpty()) {
                appendLine(sb, String.join(" | ", cells));
            }
        }
    }

    private static void appendLine(StringBuilder sb, String line) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(line);
    }

    private static String extractDocText(InputStream is) throws Exception {
        try (HWPFDocument document = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(document)) {
            return String.join("\n", extractor.getParagraphText());
        }
    }

    static List<Section> splitSections(String text) {
        List<Section> sections = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        String currentTitle = "";
        StringBuilder body = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (isSectionHeader(trimmed)) {
                flushSection(sections, currentTitle, body);
                currentTitle = trimmed;
                body.setLength(0);
            } else {
                if (body.length() > 0) {
                    body.append('\n');
                }
                body.append(line);
            }
        }
        flushSection(sections, currentTitle, body);

        if (sections.isEmpty()) {
            sections.add(new Section("", text.trim()));
        }
        return sections;
    }

    private static void flushSection(List<Section> sections, String title, StringBuilder body) {
        String bodyStr = body.toString().trim();
        if (title.isEmpty() && bodyStr.isEmpty()) {
            return;
        }
        sections.add(new Section(title, bodyStr));
    }

    static boolean isSectionHeader(String trimmedLine) {
        if (StrUtil.isBlank(trimmedLine) || trimmedLine.length() > 40) {
            return false;
        }
        return SECTION_HEADER.matcher(trimmedLine).matches();
    }

    static int sectionPriority(String title) {
        if (StrUtil.isBlank(title)) {
            return 85;
        }
        String t = title.toLowerCase(Locale.ROOT);
        if (containsAny(t, "技能", "技术栈", "核心技术", "证书")) {
            return 100;
        }
        if (containsAny(t, "荣誉", "获奖")) {
            return 68;
        }
        if (containsAny(t, "其他经历")) {
            return 82;
        }
        if (containsAny(t, "求职", "意向")) {
            return 98;
        }
        if (containsAny(t, "项目")) {
            return 92;
        }
        if (containsAny(t, "工作", "实习") && !containsAny(t, "教育", "在校")) {
            return 88;
        }
        if (containsAny(t, "个人", "基本", "联系")) {
            return 86;
        }
        if (containsAny(t, "自我", "评价")) {
            return 78;
        }
        if (containsAny(t, "教育", "在校")) {
            return 72;
        }
        if (containsAny(t, "证书", "获奖", "培训", "荣誉")) {
            return 65;
        }
        return 55;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String fitSectionsToBudget(List<Section> sections, int maxChars) {
        int n = sections.size();
        int[] limits = new int[n];
        for (int i = 0; i < n; i++) {
            limits[i] = sections.get(i).body.length();
        }

        int totalLen = joinedLength(sections, limits);
        int guard = 0;
        while (totalLen > maxChars && guard++ < 10_000) {
            int shrinkIdx = pickShrinkIndex(sections, limits);
            if (shrinkIdx < 0) {
                break;
            }
            int step = Math.max(80, limits[shrinkIdx] / 4);
            limits[shrinkIdx] = Math.max(0, limits[shrinkIdx] - step);
            totalLen = joinedLength(sections, limits);
        }

        if (totalLen > maxChars) {
            return headTailTruncate(joinSections(sections, limits), maxChars);
        }
        return joinSections(sections, limits);
    }

    private static int pickShrinkIndex(List<Section> sections, int[] limits) {
        int candidate = -1;
        int lowestPriority = Integer.MAX_VALUE;
        for (int i = 0; i < sections.size(); i++) {
            if (limits[i] <= 0) {
                continue;
            }
            int priority = sectionPriority(sections.get(i).title);
            if (priority < lowestPriority) {
                lowestPriority = priority;
                candidate = i;
            }
        }
        return candidate;
    }

    private static int joinedLength(List<Section> sections, int[] bodyLimits) {
        int len = 0;
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            if (!s.title.isEmpty()) {
                len += s.title.length() + 1;
            }
            len += Math.min(s.body.length(), bodyLimits[i]);
            if (bodyLimits[i] < s.body.length()) {
                len += MIDDLE_OMITTED_MARK.length();
            }
            len += 2;
        }
        return Math.max(0, len - 2);
    }

    private static String joinSections(List<Section> sections, int[] bodyLimits) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            if (out.length() > 0) {
                out.append("\n\n");
            }
            if (!s.title.isEmpty()) {
                out.append(s.title).append('\n');
            }
            int limit = bodyLimits[i];
            if (limit <= 0) {
                continue;
            }
            if (limit >= s.body.length()) {
                out.append(s.body);
            } else {
                out.append(s.body, 0, limit).append(MIDDLE_OMITTED_MARK);
            }
        }
        return out.toString().trim();
    }

    static String headTailTruncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        String mark = "\n\n...[中间内容已省略]...\n\n";
        int markLen = mark.length();
        int budget = maxChars - markLen;
        if (budget < 2) {
            return text.substring(0, maxChars);
        }
        int headLen = budget * 62 / 100;
        int tailLen = budget - headLen;
        return text.substring(0, headLen) + mark + text.substring(text.length() - tailLen);
    }

    record Section(String title, String body) {
    }
}
