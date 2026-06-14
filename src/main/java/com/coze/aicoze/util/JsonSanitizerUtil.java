package com.coze.aicoze.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 模型输出 JSON 的字符级清洗与局部修复（标点、尾随逗号、串内引号、数组末对象缺 {@code }} 或 {@code ]} {@code }} 反序、{@code "回复"} 字段等）。
 *
 * <p>与 {@link JsonInputNormalizer} 分工：后者负责去掉 Markdown 围栏、粗提最外层 {@code {...}}；
 * 若需从混排文本中取<strong>最后一个根级完整对象</strong>，应用 {@link CozeJsonExtractor#extractLastJsonObject(String)}。</p>
 */
public final class JsonSanitizerUtil {

    private static final JsonMapper MODEL_JSON_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    private JsonSanitizerUtil() {
    }

    /**
     * 将模型常见「伪 JSON」标点转为合法 JSON 字符：全角冒号/逗号、弯引号等。
     * 仅在 JSON 结构层（双引号字符串外）替换，避免把题干/回复值内的弯引号变成未转义 ASCII {@code "}。
     */
    public static String normalizePunctuation(String s) {
        return normalizePunctuationOutsideStrings(s);
    }

    /**
     * 与 {@link #normalizePunctuation(String)} 相同；显式命名供调用方区分语义。
     */
    public static String normalizePunctuationOutsideStrings(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escape) {
                    sb.append(c);
                    escape = false;
                } else if (c == '\\') {
                    sb.append(c);
                    escape = true;
                } else if (c == '"') {
                    sb.append(c);
                    inString = false;
                } else {
                    sb.append(c);
                }
                continue;
            }
            if (c == '"') {
                sb.append(c);
                inString = true;
                continue;
            }
            sb.append(mapPunctuationChar(c));
        }
        return sb.toString();
    }

    private static char mapPunctuationChar(char c) {
        if (c == '\uFF1A') {
            return ':';
        }
        if (c == '\uFF0C') {
            return ',';
        }
        if (c == '\u201C' || c == '\u201D' || c == '\u201F' || c == '\uFF02') {
            return '"';
        }
        if (c == '\u2018' || c == '\u2019') {
            return '\'';
        }
        return c;
    }

    /**
     * 修复 LLM 在 JSON 双引号字符串值内插入的<strong>未转义 ASCII 双引号</strong>（例如 {@code 优秀的"执行者"}）。
     *
     * <p>策略（与常见 json-repair 实现同类）：扫描整段文本；处于双引号字符串内时，若遇到 {@code "} 且其后（跳过空白）不是
     * 逗号、对象/数组结束符或文本结束，则视为串内字符并在该引号前插入反斜杠。已合法的转义引号由反斜杠规则跳过。</p>
     *
     * <p>局限：依赖「真正的结束引号」后紧跟结构符号（含键名后的冒号）；反引号 {@code `...`} 代码块内一律转义双引号，不按结构符截断。</p>
     */
    public static String repairUnescapedAsciiDoubleQuotesInJsonStrings(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        StringBuilder out = new StringBuilder(json.length() + 16);
        boolean inString = false;
        boolean inBacktick = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (!inString) {
                out.append(c);
                if (c == '"') {
                    inString = true;
                }
                continue;
            }
            if (c == '\\') {
                out.append('\\');
                if (i + 1 < json.length()) {
                    i++;
                    out.append(json.charAt(i));
                }
                continue;
            }
            if (c == '`') {
                inBacktick = !inBacktick;
                out.append(c);
                continue;
            }
            if (c == '"') {
                if (inBacktick || !isStructuralEndOfJsonString(json, i + 1)) {
                    out.append('\\').append('"');
                } else {
                    out.append('"');
                    inString = false;
                    inBacktick = false;
                }
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isStructuralEndOfJsonString(String s, int afterQuoteIndex) {
        int j = afterQuoteIndex;
        while (j < s.length() && Character.isWhitespace(s.charAt(j))) {
            j++;
        }
        if (j >= s.length()) {
            return true;
        }
        char d = s.charAt(j);
        // 值结束：, } ]；键结束后面是冒号（若把键内引号误判为「串内」会破坏整段 JSON）
        return d == ',' || d == '}' || d == ']' || d == ':';
    }

    /**
     * 去掉数组 / 对象末尾多余逗号（可重复多轮以处理嵌套 {@code ,] }、{@code ,} }）。
     */
    public static String stripTrailingCommas(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String s = json;
        String prev;
        do {
            prev = s;
            s = s.replaceAll(",(\\s*\\])", "$1");
            s = s.replaceAll(",(\\s*\\})", "$1");
        } while (!s.equals(prev));
        return s;
    }

    /**
     * 本地 LLM 修复三连：全角标点 → 去尾随逗号 → 串内未转义 ASCII 双引号前补反斜杠。
     * 供 {@link JsonParseUtil#toBeanQuiet(String, Class)} 等构建解析候选串时使用。
     */
    public static String applyLocalLlmRepairs(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String s = normalizePunctuation(json.trim());
        s = stripTrailingCommas(s);
        return repairUnescapedAsciiDoubleQuotesInJsonStrings(s);
    }

    /**
     * 修复 LLM 常见结构错误：数组尚未结束时，最后一个元素为对象但少写了闭合 {@code }，直接出现 {@code ]}。
     *
     * <p>策略：在 JSON 结构层（尊重双引号字符串与反斜杠转义）维护花括号/方括号栈；遇到 {@code ]} 时若栈顶仍为
     * 未闭合的对象（{@code '{'}），则在输出中先补闭合花括号再输出该 {@code ]}，直至栈顶为数组 {@code '['} 或栈空。</p>
     *
     * <p><strong>B3</strong>：若本处因补 {@code }} 才闭合了对象（{@code ]} 写在 {@code }} 之前），且紧随空白后的 {@code }} 之后（再跳过空白）为逗号
     * {@code ,}，则该 {@code }} 为与 {@code ]} 写反时多出的闭合，予以跳过；若其后为 {@code ]} 或 {@code }} 等，则该 {@code }} 用于闭合外层，必须保留。</p>
     *
     * <p>合法 JSON 上为幂等：正常 {@code ]} 前栈顶已是 {@code [}，不会插入字符，也不会跳过任何字符。</p>
     */
    public static String repairMissingObjectCloseBeforeArrayEnd(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        StringBuilder out = new StringBuilder(json.length() + 8);
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                out.append(c);
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
                continue;
            }
            if (c == '{') {
                stack.push('{');
                out.append(c);
                continue;
            }
            if (c == '[') {
                stack.push('[');
                out.append(c);
                continue;
            }
            if (c == '}') {
                if (!stack.isEmpty() && stack.peek() == '{') {
                    stack.pop();
                }
                out.append(c);
                continue;
            }
            if (c == ']') {
                boolean insertedClose = false;
                while (!stack.isEmpty() && stack.peek() == '{') {
                    out.append('}');
                    stack.pop();
                    insertedClose = true;
                }
                // 误把 ] 当作对象结束且数组仍有下一元素：..."] }, {" → ..."}, {"
                if (insertedClose && !stack.isEmpty() && stack.peek() == '['
                        && isMisplacedBracketBeforeNextArrayElement(json, i + 1)) {
                    i = skipThroughClosingBraceBeforeComma(json, i + 1) - 1;
                    continue;
                }
                if (!stack.isEmpty() && stack.peek() == '[') {
                    stack.pop();
                }
                out.append(']');
                if (insertedClose) {
                    int j = i + 1;
                    while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                        j++;
                    }
                    if (j < json.length() && json.charAt(j) == '}') {
                        int k = j + 1;
                        while (k < json.length() && Character.isWhitespace(json.charAt(k))) {
                            k++;
                        }
                        if (k < json.length() && json.charAt(k) == ',') {
                            i = j;
                        }
                    }
                }
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Bot 题目 JSON：{@link JsonInputNormalizer#normalizeJsonInput(String)} 后再 {@link #prepareModelJson(String)}。
     */
    public static String normalizeAndPrepareQuestionJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String n = JsonInputNormalizer.normalizeJsonInput(raw);
        if (n == null || n.isEmpty()) {
            return null;
        }
        return prepareModelJson(n);
    }

    /**
     * 判断是否为「JSON 文本语法类」解析失败，便于仅在此时做一次 {@link #prepareModelJsonWithStructuralRepair} 重试。
     *
     * <p>不依赖日志整句文案，沿 {@link Throwable#getCause()} 链检查 Jackson 异常与常见英文 message 片段。</p>
     */
    public static boolean isLikelyJsonSyntaxParseError(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof JsonParseException) {
                return true;
            }
            String msg = c.getMessage();
            if (msg == null) {
                continue;
            }
            if (msg.contains("Unexpected character")) {
                return true;
            }
            if (msg.contains("Unrecognized token")) {
                return true;
            }
            if (msg.contains("Expected") && (msg.contains("','") || msg.contains("'}'") || msg.contains("':'")
                    || msg.contains("EOF") || msg.contains("token"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 与 {@link #prepareModelJson(String)} 相同流水线，并额外执行
     * {@link #repairMissingObjectCloseBeforeArrayEnd}（B2+B3），供题目 JSON 首次解析失败时最多重试一次。
     */
    public static String prepareModelJsonWithStructuralRepair(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        String base = stripTrailingCommas(normalizePunctuation(json.trim()));
        String canonical = tryJacksonCanonicalize(base);
        if (canonical != null) {
            return canonical;
        }
        String repaired = repairUnescapedAsciiDoubleQuotesInJsonStrings(base);
        canonical = tryJacksonCanonicalize(repaired);
        if (canonical != null) {
            return canonical;
        }
        String structural = repairMissingObjectCloseBeforeArrayEnd(repaired);
        canonical = tryJacksonCanonicalize(structural);
        return canonical != null ? canonical : structural;
    }

    /**
     * 供 Hutool 等解析器使用：先做 {@link #normalizePunctuation}、{@link #stripTrailingCommas}，
     * 再经 Jackson 读入（允许尾随逗号、同一 object 重复 key 保留后者）后写出规范 JSON。
     * <p>
     * 与 {@link #sanitize(String)} 职责不同：{@code sanitize} 针对「回复」字段内嵌引号；
     * 本方法针对 Bot 整段题目 JSON 的常见语法问题。
     * <p>
     * 不在此路径默认执行 {@link #repairMissingObjectCloseBeforeArrayEnd}；题目解析失败且
     * {@link #isLikelyJsonSyntaxParseError(Throwable)} 为真时，由调用方改用 {@link #prepareModelJsonWithStructuralRepair(String)} 重试一次。
     */
    public static String prepareModelJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        String base = stripTrailingCommas(normalizePunctuation(json.trim()));
        String canonical = tryJacksonCanonicalize(base);
        if (canonical != null) {
            return canonical;
        }
        String repaired = repairUnescapedAsciiDoubleQuotesInJsonStrings(base);
        canonical = tryJacksonCanonicalize(repaired);
        return canonical != null ? canonical : repaired;
    }

    private static String tryJacksonCanonicalize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MODEL_JSON_MAPPER.readTree(json);
            return MODEL_JSON_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static String sanitize(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String s = normalizePunctuation(json.trim());
        s = stripTrailingCommas(s);
        final String key = "\"回复\":";
        int keyIndex = s.indexOf(key);
        // 无「回复」键时（例如 Coze 英文键 reply）：normalizePunctuation 会把串内弯引号变成 ASCII "，
        // 需再修复串内未转义双引号，否则整段无法 JSON.parse。
        if (keyIndex == -1) {
            return repairUnescapedAsciiDoubleQuotesInJsonStrings(s);
        }

        // 定位值的起始位置（跳过空白和第一个引号）
        int valueStart = keyIndex + key.length();
        while (valueStart < s.length() && Character.isWhitespace(s.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= s.length() || s.charAt(valueStart) != '"') {
            return repairUnescapedAsciiDoubleQuotesInJsonStrings(s);
        }
        int firstQuote = valueStart;
        valueStart++; // 跳过起始引号，此时 valueStart 指向值内容的第一个字符

        // 查找真正的结束引号（从后向前找最后一个符合条件的引号）
        int lastQuote = findRealEndQuote(s, firstQuote);
        if (lastQuote == -1) {
            return repairUnescapedAsciiDoubleQuotesInJsonStrings(s);
        }

        // 提取原始回复内容（不包含外层引号）
        String originalReply = s.substring(valueStart, lastQuote);

        // 将原始回复中的英文双引号替换为交替的方括号
        StringBuilder replaced = new StringBuilder(originalReply.length());
        boolean odd = true; // 第一个替换为 “
        for (int i = 0; i < originalReply.length(); i++) {
            char c = originalReply.charAt(i);
            if (c == '"') {
                replaced.append(odd ? '“' : '”');
                odd = !odd;
            } else {
                replaced.append(c);
            }
        }

        // 重新组装 JSON：保留开始部分 + 替换后的值 + 结束部分
        return s.substring(0, valueStart) + replaced + s.substring(lastQuote);
    }
    /**
     * 从起始引号位置向后查找真正的结束引号（从后向前扫描）
     */
    /** {@code ]} 后是否为 {@code }, {}} 模式（对象误用 ] 结束且数组未完结）。 */
    private static boolean isMisplacedBracketBeforeNextArrayElement(String s, int afterBracket) {
        int j = skipWhitespace(s, afterBracket);
        if (j >= s.length() || s.charAt(j) != '}') {
            return false;
        }
        j = skipWhitespace(s, j + 1);
        if (j >= s.length() || s.charAt(j) != ',') {
            return false;
        }
        j = skipWhitespace(s, j + 1);
        return j < s.length() && s.charAt(j) == '{';
    }

    /** 跳过 {@code ]} 之后到逗号（含）前的 {@code }}（用于 B3 变体）。 */
    private static int skipThroughClosingBraceBeforeComma(String s, int afterBracket) {
        int j = skipWhitespace(s, afterBracket);
        if (j < s.length() && s.charAt(j) == '}') {
            j = skipWhitespace(s, j + 1);
        }
        return j;
    }

    private static int skipWhitespace(String s, int from) {
        int j = from;
        while (j < s.length() && Character.isWhitespace(s.charAt(j))) {
            j++;
        }
        return j;
    }

    private static int findRealEndQuote(String json, int startQuotePos) {
        int pos = json.length() - 1;
        while (pos > startQuotePos) {
            if (json.charAt(pos) == '"') {
                // 检查该引号是否被转义（前面是反斜杠）
                if (pos > 0 && json.charAt(pos - 1) == '\\') {
                    pos--;
                    continue;
                }
                // 检查该引号之后（忽略空白）是否为 ',' 或 '}'
                int next = pos + 1;
                while (next < json.length() && Character.isWhitespace(json.charAt(next))) {
                    next++;
                }
                if (next < json.length()) {
                    char ch = json.charAt(next);
                    if (ch == ',' || ch == '}') {
                        return pos;
                    }
                }
            }
            pos--;
        }
        return -1;
    }
}
