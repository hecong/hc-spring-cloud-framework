package com.hc.framework.common.util;

import com.hc.framework.common.constant.SystemConstants;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * <p>提供项目中最常用的字符串操作方法，补充 Hutool StrUtil 不便直接引用的场景。
 * 所有方法均为静态工具方法，不依赖 Spring 容器。</p>
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
     * @param str 待检测字符串
     * @return true 表示为空
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str 待检测字符串
     * @return true 表示不为空
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 判断集合是否为空（null 或 size == 0）
     *
     * @param collection 集合
     * @return true 表示为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
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
     * @param map Map
     * @return true 表示为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    // ==================== 默认值 ====================

    /**
     * 若字符串为空则返回默认值
     *
     * @param str          原字符串
     * @param defaultValue 默认值
     * @return 原字符串不为空时返回原字符串，否则返回默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * null 转换为空字符串
     *
     * @param str 原字符串
     * @return 不为 null 时返回原值，否则返回 ""
     */
    public static String nullToEmpty(String str) {
        return str == null ? SystemConstants.EMPTY : str;
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
     * @param str 原字符串
     * @return 首字母大写的字符串，str 为空时返回原值
     */
    public static String capitalize(String str) {
        if (isBlank(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
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
