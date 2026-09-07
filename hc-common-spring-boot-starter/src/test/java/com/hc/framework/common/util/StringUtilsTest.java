package com.hc.framework.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StringUtils 行为契约测试（标准库/build#1 产物）。
 *
 * <p>委托结论（行为对比探针已逐项与 hutool6 比对，差异点见各方法 Javadoc）：</p>
 * <ul>
 *     <li><b>已委托</b>：isBlank/isNotBlank（StrUtil）、isEmpty(Collection/Map)（CollUtil/MapUtil）、
 *         nullToEmpty（StrUtil.emptyIfNull）、defaultIfBlank（StrUtil.defaultIfBlank）、
 *         capitalize（StrUtil.upperFirst）——本测试断言委托前后对外输出不变；</li>
 *     <li><b>保留原实现</b>：camelToUnderscore（缩写保留大写语义不同）、truncate（hutool6 无同契约方法）、
 *         format（反斜杠转义与 null 模板语义不同）、mask 系列脱敏、isMobile、isEmail、splitAndTrim——本测试锁定其既有契约。</li>
 * </ul>
 *
 * <p>空白语义说明：委托后 isBlank 系列以 hutool6 为准——NBSP（U+00A0）、BOM（U+FEFF）判为 blank 为期望的
 * 超集对齐；零宽空格 U+200B 与常用空白判定同旧 String.isBlank。下述用例按该契约断言。</p>
 */
class StringUtilsTest {

    // ==================== 判空（委托 StrUtil） ====================

    @Test
    @DisplayName("isBlank：null/空/纯空白/NBSP与BOM均为 true")
    void isBlankTrueCases() {
        // NBSP U+00A0、BOM U+FEFF：hutool6 判为 blank（委托后为期望的超集对齐）
        for (String s : new String[]{null, "", " ", "\t", "\n", "\r\n", "\u3000", "\u00A0", "\uFEFF"}) {
            assertTrue(StringUtils.isBlank(s), () -> "isBlank should be true for <" + escape(s) + ">");
        }
    }

    @Test
    @DisplayName("isBlank：含非空白字符及 ZWSP 均为 false")
    void isBlankFalseCases() {
        // 零宽空格 U+200B：hutool6 判为非空白（与旧 String.isBlank 一致）
        for (String s : new String[]{"a", " a", "a ", "1", "\u200B", "\u00A0a", "\r\na"}) {
            assertFalse(StringUtils.isBlank(s), () -> "isBlank should be false for <" + escape(s) + ">");
        }
    }

    @Test
    @DisplayName("isNotBlank 与 isBlank 恒互斥")
    void isNotBlankInverse() {
        assertFalse(StringUtils.isNotBlank(null));
        assertFalse(StringUtils.isNotBlank(" "));
        assertTrue(StringUtils.isNotBlank(" a"));
    }

    @Test
    @DisplayName("isEmpty(Collection/Map) 委托后 null 与空容器判定不变")
    void isEmptyCollectionAndMap() {
        assertTrue(StringUtils.isEmpty((List<String>) null));
        assertTrue(StringUtils.isEmpty(List.of()));
        assertFalse(StringUtils.isEmpty(List.of("a")));

        assertTrue(StringUtils.isEmpty((Map<String, String>) null));
        assertTrue(StringUtils.isEmpty(Map.of()));
        assertFalse(StringUtils.isEmpty(Map.of("k", "v")));
    }

    // ==================== 默认值（委托 StrUtil） ====================

    @Test
    @DisplayName("defaultIfBlank 委托后 blank 取默认、非 blank 原样返回")
    void defaultIfBlankContract() {
        assertEquals("D", StringUtils.defaultIfBlank(null, "D"));
        assertEquals("D", StringUtils.defaultIfBlank("", "D"));
        assertEquals("D", StringUtils.defaultIfBlank(" \t", "D"));
        assertEquals(" a", StringUtils.defaultIfBlank(" a", "D")); // 非 blank 原样返回（含前置空格）
        assertEquals("x", StringUtils.defaultIfBlank("x", null));
        assertEquals(null, StringUtils.defaultIfBlank(null, null));
    }

    @Test
    @DisplayName("nullToEmpty 委托后 null 转空串、非 null 原样返回")
    void nullToEmptyContract() {
        assertEquals("", StringUtils.nullToEmpty(null));
        assertEquals("", StringUtils.nullToEmpty(""));
        assertEquals("a", StringUtils.nullToEmpty("a"));
    }

    // ==================== 大小写（capitalize 委托 / camel 保留） ====================

    @Test
    @DisplayName("capitalize 委托后首字符小写转大写，空值/非字母首字符原样")
    void capitalizeContract() {
        assertEquals("Hello", StringUtils.capitalize("hello"));
        assertEquals("Hello", StringUtils.capitalize("Hello"));
        assertEquals("Hello World", StringUtils.capitalize("hello World"));
        assertEquals("1abc", StringUtils.capitalize("1abc"));
        assertEquals("", StringUtils.capitalize(""));
        assertEquals(null, StringUtils.capitalize(null));
    }

    @Test
    @DisplayName("camelToUnderscore 保留原契约：全小写、每个大写字母前插下划线")
    void camelToUnderscoreContract() {
        assertEquals("user_id", StringUtils.camelToUnderscore("userId"));
        assertEquals("user_name", StringUtils.camelToUnderscore("userName"));
        assertEquals("user_i_d", StringUtils.camelToUnderscore("UserID"));
        assertEquals("u_r_l_value", StringUtils.camelToUnderscore("URLValue"));
        assertEquals("get_u_r_l", StringUtils.camelToUnderscore("getURL"));
        assertEquals("user", StringUtils.camelToUnderscore("user"));
        assertEquals("", StringUtils.camelToUnderscore(""));
        assertEquals(null, StringUtils.camelToUnderscore(null));
    }

    // ==================== 格式化（保留原实现） ====================

    @Test
    @DisplayName("format：占位符替换/参数不足/参数富余/花括号/反斜杠均按既有契约")
    void formatContract() {
        assertEquals("Hello, World!", StringUtils.format("Hello, {}!", "World"));
        assertEquals("1 + 2 = 3", StringUtils.format("{} + {} = {}", 1, 2, 3));
        assertEquals("a x b {}", StringUtils.format("a {} b {}", "x"));
        assertEquals("a x", StringUtils.format("a {}", "x", "y", "z"));
        assertEquals("{x}", StringUtils.format("{{}}", "x"));
        assertEquals("x{}", StringUtils.format("{}{}", "x")); // 参数耗尽后剩余占位符原样输出
        assertEquals("a \\x b", StringUtils.format("a \\{} b", "x"));
        assertEquals(null, StringUtils.format(null, "x"));
        assertEquals(null, StringUtils.format(null, (Object[]) null));
        assertEquals("a {}", StringUtils.format("a {}", (Object[]) null));
        assertEquals("", StringUtils.format("", "x"));
    }

    // ==================== 截断（保留原实现） ====================

    @Test
    @DisplayName("truncate：超出截断补省略号，未超出/空值原样，maxLength=0 取空前缀+省略号")
    void truncateContract() {
        assertEquals("Hello Wo...", StringUtils.truncate("Hello World", 8));
        assertEquals("Hello...", StringUtils.truncate("Hello World", 5));
        assertEquals("Hello World", StringUtils.truncate("Hello World", 11));
        assertEquals("Hello World", StringUtils.truncate("Hello World", 12));
        assertEquals("Hi", StringUtils.truncate("Hi", 8));
        assertEquals("...", StringUtils.truncate("abc", 0));
        assertEquals("", StringUtils.truncate("", 8));
        assertEquals(null, StringUtils.truncate(null, 8));
    }

    // ==================== 脱敏（保留原实现） ====================

    @Test
    @DisplayName("maskMobile：11 位脱敏、非法/空原样返回")
    void maskMobileContract() {
        assertEquals("138****5678", StringUtils.maskMobile("13812345678"));
        assertEquals("12345", StringUtils.maskMobile("12345"));
        assertEquals(null, StringUtils.maskMobile(null));
    }

    @Test
    @DisplayName("maskEmail：无 @ 原样返回，前缀 <=2 位补 **，否则留前 2 位")
    void maskEmailContract() {
        assertEquals("te**@abc.com", StringUtils.maskEmail("testuser@abc.com"));
        assertEquals("ab**@c.com", StringUtils.maskEmail("ab@c.com"));
        assertEquals("a**@b.com", StringUtils.maskEmail("a@b.com"));
        assertEquals("no-at", StringUtils.maskEmail("no-at"));
        assertEquals(null, StringUtils.maskEmail(null));
    }

    @Test
    @DisplayName("maskIdCard：长度 >=8 脱敏保留前4后4，否则原样")
    void maskIdCardContract() {
        assertEquals("1101**********1234", StringUtils.maskIdCard("110101199001011234"));
        assertEquals("1234567", StringUtils.maskIdCard("1234567"));
        assertEquals(null, StringUtils.maskIdCard(null));
    }

    // ==================== 校验与分割（保留原实现） ====================

    @Test
    @DisplayName("isMobile / isEmail 校验契约")
    void validateContract() {
        assertTrue(StringUtils.isMobile("13812345678"));
        assertFalse(StringUtils.isMobile("12345678901"));
        assertFalse(StringUtils.isMobile(null));

        assertTrue(StringUtils.isEmail("test@abc.com"));
        assertFalse(StringUtils.isEmail("abc"));
        assertFalse(StringUtils.isEmail(null));
    }

    @Test
    @DisplayName("splitAndTrim：去空白项、trim 后返回")
    void splitAndTrimContract() {
        assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.splitAndTrim("a,b, c ,,", ","));
        assertArrayEquals(new String[]{"hello", "world"}, StringUtils.splitAndTrim(" hello ; world ", ";"));
        assertArrayEquals(new String[0], StringUtils.splitAndTrim(null, ","));
        assertArrayEquals(new String[0], StringUtils.splitAndTrim("  ", ","));
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                sb.append(String.format("\\u%04X", (int) c));
            } else if (c == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
