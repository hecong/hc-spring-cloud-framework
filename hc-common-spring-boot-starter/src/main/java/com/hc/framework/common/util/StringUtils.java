package com.hc.framework.common.util;

import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * <p>提供项目中最常用的字符串操作方法。判空、默认值等与 Hutool 6 语义等价的通用能力
 * 直接委托 {@link StrUtil} / {@link CollUtil} / {@link MapUtil} 实现（委托点见各方法 Javadoc），
 * 格式化、截断、脱敏、校验等框架特色方法保留原实现。所有方法均为静态工具方法，不依赖 Spring 容器。</p>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * // 判空
 * StringUtils.isBlank(str);        // null / 空 / 纯空白 → true
 * StringUtils.isNotBlank(str);
 *
 * // 默认值
 * String val = StringUtils.defaultIfBlank(str, "默认值");
 *
 * // 格式化
 * String msg = StringUtils.format("用户{}登录失败，原因：{}", username, reason);
 *
 * // 敏感信息脱敏
 * String mobile = StringUtils.maskMobile("13812345678");  // "138****5678"
 * String email  = StringUtils.maskEmail("test@abc.com");  // "te**@abc.com"
 *
 * // 集合判空
 * StringUtils.isEmpty(list);
 * }</pre>
 *
 * @author hc-framework
 */
public class StringUtils {

    /** 手机号正则 */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 邮箱正则 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private StringUtils() {
    }

    // ==================== 判空 ====================

    /**
     * 判断字符串是否为空（null、空字符串、纯空白均返回 true）
     *
     * <p>实现：委托 {@link StrUtil#isBlank(CharSequence)}。hutool6 将 NBSP（U+00A0）、BOM（U+FEFF）等
     * 特殊字符同样判为 blank，相较旧实现（{@code String.isBlank}）为期望的超集对齐；
     * 零宽空格 U+200B 与常用空白判定与旧实现一致。空白判定与框架内既有的
     * {@code StrUtil.isBlank/isNotBlank} 用法保持一致。</p>
     *
     * @param str 待检测字符串
     * @return true 表示为空
     */
    public static boolean isBlank(String str) {
        return StrUtil.isBlank(str);
    }

    /**
     * 判断字符串是否不为空
     *
     * <p>实现：委托 {@link StrUtil#isNotBlank(CharSequence)}，空白定义同 {@link #isBlank(String)}。</p>
     *
     * @param str 待检测字符串
     * @return true 表示不为空
     */
    public static boolean isNotBlank(String str) {
        return StrUtil.isNotBlank(str);
    }

    /**
     * 判断集合是否为空（null 或 size == 0）
     *
     * <p>实现：委托 {@link CollUtil#isEmpty(Collection)}。</p>
     *
     * @param collection 集合
     * @return true 表示为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return CollUtil.isEmpty(collection);
    }

    /**
     * 判断集合是否不为空
     *
     * @param collection 集合
     * @return true 表示不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为空（null 或 size == 0）
     *
     * <p>实现：委托 {@link MapUtil#isEmpty(Map)}。</p>
     *
     * @param map Map
     * @return true 表示为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return MapUtil.isEmpty(map);
    }

    // ==================== 默认值 ====================

    /**
     * 若字符串为空则返回默认值
     *
     * <p>实现：委托 {@link StrUtil#defaultIfBlank(CharSequence, CharSequence)}（入参/返回按 String 收窄），
     * 空白定义同 {@link #isBlank(String)}。</p>
     *
     * @param str          原字符串
     * @param defaultValue 默认值
     * @return 原字符串不为空时返回原字符串，否则返回默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return StrUtil.defaultIfBlank(str, defaultValue);
    }

    /**
     * null 转换为空字符串
     *
     * <p>实现：委托 {@link StrUtil#emptyIfNull(CharSequence)}。</p>
     *
     * @param str 原字符串
     * @return 不为 null 时返回原值，否则返回 ""
     */
    public static String nullToEmpty(String str) {
        return StrUtil.emptyIfNull(str);
    }

    // ==================== 格式化 ====================

    /**
     * 使用 {} 占位符格式化字符串（类似 SLF4J 风格）
     *
     * <pre>{@code
     * StringUtils.format("Hello, {}!", "World");   // "Hello, World!"
     * StringUtils.format("{} + {} = {}", 1, 2, 3); // "1 + 2 = 3"
     * }</pre>
     *
     * <p>保留原实现：行为对比发现 hutool6 {@code StrUtil.format} 对反斜杠转义
     * （{@code \{} 输出字面量）与 null 模板的语义不同，委托会导致既有输出漂移，故不委托。</p>
     *
     * @param template 模板字符串，使用 {} 作为占位符
     * @param args     替换参数
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (isBlank(template) || args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + args.length * 8);
        int argIdx = 0;
        int i = 0;
        while (i < template.length()) {
            if (template.charAt(i) == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}') {
                sb.append(argIdx < args.length ? args[argIdx++] : "{}");
                i += 2;
            } else {
                sb.append(template.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    // ==================== 大小写转换 ====================

    /**
     * 驼峰命名转下划线命名
     * <p>示例：{@code camelToUnderscore("userId")} → {@code "user_id"}</p>
     *
     * <p>保留原实现：行为对比发现 hutool6 {@code StrUtil.toUnderlineCase} 对连续大写缩写
     * （如 {@code "UserID"/"URLValue"/"getURL"}）会保留缩写大小写并输出大写片段，
     * 与本方法「全小写 + 每个大写字母前插下划线」的既有契约不一致，委托会导致既有输出漂移，故不委托。</p>
     *
     * @param camel 驼峰字符串
     * @return 下划线字符串（全小写）
     */
    public static String camelToUnderscore(String camel) {
        if (isBlank(camel)) {
            return camel;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * 首字母大写
     *
     * <p>实现：委托 {@link StrUtil#upperFirst(CharSequence)}（入参/返回按 String 收窄），
     * 行为对比覆盖 null/空串/空白/首字符非字母等边界与 hutool6 等价。</p>
     *
     * @param str 原字符串
     * @return 首字母大写的字符串，str 为空时返回原值
     */
    public static String capitalize(String str) {
        return StrUtil.upperFirst(str);
    }

    // ==================== 脱敏 ====================

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，中间替换为 ****
     * <p>示例：{@code maskMobile("13812345678")} → {@code "138****5678"}</p>
     *
     * @param mobile 手机号，格式不正确时直接返回原值
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile) {
        if (isBlank(mobile) || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 邮箱脱敏：保留 @ 前两位，其余替换为 **
     * <p>示例：{@code maskEmail("testuser@abc.com")} → {@code "te**@abc.com"}</p>
     *
     * @param email 邮箱，格式不正确时直接返回原值
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return email;
        }
        int atIdx = email.indexOf('@');
        String prefix = email.substring(0, atIdx);
        String suffix = email.substring(atIdx);
        if (prefix.length() <= 2) {
            return prefix + "**" + suffix;
        }
        return prefix.substring(0, 2) + "**" + suffix;
    }

    /**
     * 身份证脱敏：保留前 4 位和后 4 位，中间替换为 ********
     * <p>示例：{@code maskIdCard("110101199001011234")} → {@code "1101**********1234"}</p>
     *
     * @param idCard 身份证号，长度不足时直接返回原值
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (isBlank(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    // ==================== 校验 ====================

    /**
     * 判断是否是合法手机号（1开头的11位数字，第2位为3-9）
     *
     * @param mobile 手机号
     * @return true 表示合法
     */
    public static boolean isMobile(String mobile) {
        if (isBlank(mobile)) {
            return false;
        }
        return MOBILE_PATTERN.matcher(mobile).matches();
    }

    /**
     * 判断是否是合法邮箱格式
     *
     * @param email 邮箱
     * @return true 表示合法
     */
    public static boolean isEmail(String email) {
        if (isBlank(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    // ==================== 截取与补全 ====================

    /**
     * 若字符串超过指定长度，截断并追加省略号
     *
     * <pre>{@code
     * StringUtils.truncate("Hello World", 8);  // "Hello Wo..."
     * StringUtils.truncate("Hi", 8);           // "Hi"
     * }</pre>
     *
     * <p>保留原实现：hutool6 无同契约方法（{@code StrUtil.maxLength} 已不存在），相近的
     * {@code StrUtil.limitLength} 要求 maxLength &gt; 0 且不处理 maxLength = 0 场景，
     * 与本方法「最大长度不含省略号、超出补 …」的契约不一致，故不委托。</p>
     *
     * @param str       原字符串
     * @param maxLength 最大长度（不含省略号）
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (isBlank(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 将字符串按指定分隔符分割，去除空白项
     *
     * @param str       原字符串
     * @param delimiter 分隔符
     * @return 分割后的数组，str 为空时返回空数组
     */
    public static String[] splitAndTrim(String str, String delimiter) {
        if (isBlank(str)) {
            return new String[0];
        }
        return java.util.Arrays.stream(str.split(delimiter))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
