package com.hc.framework.common.util;

import com.hc.framework.common.constant.DateConstants;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 日期时间工具类
 *
 * <p>基于 Java 8+ {@link LocalDateTime} / {@link LocalDate} 实现，线程安全。
 * 所有格式化常量定义在 {@link DateConstants} 中。</p>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * // 获取当前日期时间字符串
 * String now = DateUtils.nowStr();                        // "2024-01-15 09:30:00"
 *
 * // 格式化指定时间
 * String s = DateUtils.format(LocalDateTime.now(), DateConstants.DATE_PATTERN); // "2024-01-15"
 *
 * // 字符串解析
 * LocalDateTime ldt = DateUtils.parseDateTime("2024-01-15 09:30:00");
 * LocalDate     ld  = DateUtils.parseDate("2024-01-15");
 *
 * // 时间计算
 * LocalDateTime next = DateUtils.addDays(LocalDateTime.now(), 7);
 * long diff = DateUtils.betweenDays(LocalDate.of(2024,1,1), LocalDate.now());
 *
 * // Date 互转
 * Date       date = DateUtils.toDate(LocalDateTime.now());
 * LocalDateTime ldt2 = DateUtils.toLocalDateTime(new Date());
 * }</pre>
 *
 * @author hc-framework
 * @see DateConstants
 */
public class DateUtils {

    /** 默认时区：上海 CST UTC+8 */
    private static final ZoneId DEFAULT_ZONE = ZoneId.of(DateConstants.TIMEZONE_SHANGHAI);

    private DateUtils() {
    }

    // ==================== 当前时间 ====================

    /**
     * 获取当前日期时间（LocalDateTime）
     *
     * @return 当前 LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * 获取当前日期（LocalDate）
     *
     * @return 当前 LocalDate
     */
    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    /**
     * 获取当前日期时间字符串，格式：yyyy-MM-dd HH:mm:ss
     *
     * @return 当前时间字符串，例如 "2024-01-15 09:30:00"
     */
    public static String nowStr() {
        return format(now(), DateConstants.DATETIME_PATTERN);
    }

    /**
     * 获取当前日期字符串，格式：yyyy-MM-dd
     *
     * @return 当前日期字符串，例如 "2024-01-15"
     */
    public static String todayStr() {
        return format(today(), DateConstants.DATE_PATTERN);
    }

    // ==================== 格式化 ====================

    /**
     * 将 LocalDateTime 格式化为指定格式字符串
     *
     * @param dateTime 日期时间，为 null 时返回空字符串
     * @param pattern  格式，参考 {@link DateConstants}
     * @return 格式化结果
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将 LocalDate 格式化为指定格式字符串
     *
     * @param date    日期，为 null 时返回空字符串
     * @param pattern 格式，参考 {@link DateConstants}
     * @return 格式化结果
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    // ==================== 解析 ====================

    /**
     * 将字符串解析为 LocalDateTime（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTimeStr 日期时间字符串，例如 "2024-01-15 09:30:00"
     * @return LocalDateTime，字符串为空时返回 null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(DateConstants.DATETIME_PATTERN));
    }

    /**
     * 将字符串按指定格式解析为 LocalDateTime
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式，参考 {@link DateConstants}
     * @return LocalDateTime，字符串为空时返回 null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将字符串解析为 LocalDate（格式：yyyy-MM-dd）
     *
     * @param dateStr 日期字符串，例如 "2024-01-15"
     * @return LocalDate，字符串为空时返回 null
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(DateConstants.DATE_PATTERN));
    }

    // ==================== 时间计算 ====================

    /**
     * 在给定时间上增加指定天数
     *
     * @param dateTime 基准时间
     * @param days     增加的天数（可为负数，表示减少）
     * @return 计算结果
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        return dateTime != null ? dateTime.plusDays(days) : null;
    }

    /**
     * 在给定时间上增加指定小时数
     *
     * @param dateTime 基准时间
     * @param hours    增加的小时数（可为负数）
     * @return 计算结果
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        return dateTime != null ? dateTime.plusHours(hours) : null;
    }

    /**
     * 在给定时间上增加指定分钟数
     *
     * @param dateTime 基准时间
     * @param minutes  增加的分钟数（可为负数）
     * @return 计算结果
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime != null ? dateTime.plusMinutes(minutes) : null;
    }

    /**
     * 在给定时间上增加指定月数
     *
     * @param dateTime 基准时间
     * @param months   增加的月数（可为负数）
     * @return 计算结果
     */
    public static LocalDateTime addMonths(LocalDateTime dateTime, long months) {
        return dateTime != null ? dateTime.plusMonths(months) : null;
    }

    /**
     * 计算两个日期相差的天数（end - start）
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 相差天数，end 早于 start 时为负数
     */
    public static long betweenDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间相差的秒数（end - start）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 相差秒数
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    // ==================== 月份边界 ====================

    /**
     * 获取指定日期所在月的第一天（00:00:00）
     *
     * @param date 任意日期
     * @return 当月第一天零时
     */
    public static LocalDateTime beginOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }

    /**
     * 获取指定日期所在月的最后一天（23:59:59）
     *
     * @param date 任意日期
     * @return 当月最后一天末时
     */
    public static LocalDateTime endOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
    }

    // ==================== Date 互转 ====================

    /**
     * 将 LocalDateTime 转换为 java.util.Date
     *
     * @param dateTime LocalDateTime，为 null 时返回 null
     * @return java.util.Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
    }

    /**
     * 将 java.util.Date 转换为 LocalDateTime
     *
     * @param date java.util.Date，为 null 时返回 null
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * 将时间戳（毫秒）转换为 LocalDateTime
     *
     * @param timestamp 毫秒时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime ofTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE);
    }

    /**
     * 将 LocalDateTime 转换为毫秒时间戳
     *
     * @param dateTime LocalDateTime
     * @return 毫秒时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }
}
