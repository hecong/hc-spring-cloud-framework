package com.hc.framework.web.xss;

import java.util.regex.Pattern;

/**
 * XSS 防护工具类
 *
 * <p>精准过滤危险脚本，保留正常 HTML 字符（如 &lt; &gt; &amp; 等）。</p>
 * <p>适用于富文本场景：正常内容如 "a < b" 不会被破坏，但 "&lt;script&gt;" 会被移除。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class XssKit {

    // 危险事件处理器模式 (onclick=, onload=, javascript: 等)
    private static final Pattern EVENT_PATTERN = Pattern.compile(
        "(?i)(javascript:|data:|vbscript:|on\\w+\\s*=)",
        Pattern.CASE_INSENSITIVE
    );

    // 危险标签模式 - 只拦截真正危险的标签
    private static final Pattern[] DANGEROUS_PATTERNS = {
        // <script> 标签及其内容
        Pattern.compile("(?i)<script[^>]*>.*?</script>", Pattern.DOTALL),
        // <iframe> 标签
        Pattern.compile("(?i)<iframe[^>]*>.*?</iframe>", Pattern.DOTALL),
        // <object> 标签
        Pattern.compile("(?i)<object[^>]*>.*?</object>", Pattern.DOTALL),
        // <embed> 标签
        Pattern.compile("(?i)<embed[^>]*>.*?</embed>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE),
        // <form> 标签（防止表单劫持）
        Pattern.compile("(?i)<form[^>]*>.*?</form>", Pattern.DOTALL),
        // 表达式和 CSS 注入
        Pattern.compile("(?i)expression\\s*\\([^)]*\\)"),
        // 危险 URL 协议
        Pattern.compile("(?i)url\\s*\\(\\s*['\"]*javascript:")
    };

    /**
     * 智能 XSS 过滤
     *
     * <p>只移除危险的脚本代码，保留正常的 HTML 字符：</p>
     * <ul>
     *   <li>✅ 保留：a &lt; b, 1 &gt; 0, A &amp; B, 价格 &lt; 100</li>
     *   <li>❌ 移除：&lt;script&gt;, &lt;iframe&gt;, onclick=, javascript:</li>
     * </ul>
     *
     * @param value 原始字符串
     * @return 过滤后的安全字符串
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String result = value;

        // 1. 移除危险事件处理器和协议
        result = EVENT_PATTERN.matcher(result).replaceAll("");

        // 2. 移除危险标签
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }

        return result;
    }

    /**
     * 严格模式 XSS 过滤
     *
     * <p>除了危险脚本，还转义 HTML 特殊字符（&lt; &gt; &amp; &quot;）。</p>
     * <p>适用于纯文本字段，不需要保留 HTML 格式的场景。</p>
     *
     * @param value 原始字符串
     * @return 完全转义后的字符串
     */
    public static String escapeStrict(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 先进行智能过滤
        String result = escape(value);

        // 再转义 HTML 特殊字符
        result = result.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;")
                       .replace("\"", "&quot;")
                       .replace("'", "&#x27;");

        return result;
    }
}