package com.hc.framework.common.constant;

/**
 * 日期时间格式常量
 *
 * <p>统一定义项目中使用的日期、时间格式字符串，
 * 配合 {@link com.hc.framework.common.util.DateUtils} 使用。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 格式化当前时间
 * String now = DateUtils.format(LocalDateTime.now(), DateConstants.DATETIME_PATTERN);
 *
 * // Jackson 注解中使用
 * @JsonFormat(pattern = DateConstants.DATETIME_PATTERN)
 * private LocalDateTime createTime;
 * }</pre>
 *
 * @author hc-framework
 */
public interface DateConstants {

    // ==================== 日期时间格式 ====================

    /**
     * 标准日期时间格式：yyyy-MM-dd HH:mm:ss
     * <p>示例：2024-01-15 09:30:00</p>
     */
    String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期时间格式（含毫秒）：yyyy-MM-dd HH:mm:ss.SSS
     * <p>示例：2024-01-15 09:30:00.123</p>
     */
    String DATETIME_MS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * ISO 8601 日期时间格式：yyyy-MM-dd'T'HH:mm:ss
     * <p>示例：2024-01-15T09:30:00</p>
     */
    String DATETIME_ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    // ==================== 日期格式 ====================

    /**
     * 标准日期格式：yyyy-MM-dd
     * <p>示例：2024-01-15</p>
     */
    String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 紧凑日期格式（无分隔符）：yyyyMMdd
     * <p>示例：20240115</p>
     */
    String DATE_COMPACT_PATTERN = "yyyyMMdd";

    /**
     * 年月格式：yyyy-MM
     * <p>示例：2024-01</p>
     */
    String YEAR_MONTH_PATTERN = "yyyy-MM";

    /**
     * 中文日期格式：yyyy年MM月dd日
     * <p>示例：2024年01月15日</p>
     */
    String DATE_CHINESE_PATTERN = "yyyy年MM月dd日";

    // ==================== 时间格式 ====================

    /**
     * 标准时间格式：HH:mm:ss
     * <p>示例：09:30:00</p>
     */
    String TIME_PATTERN = "HH:mm:ss";

    /**
     * 时分格式：HH:mm
     * <p>示例：09:30</p>
     */
    String HOUR_MINUTE_PATTERN = "HH:mm";

    // ==================== 时区 ====================

    /**
     * 上海时区（中国标准时间 CST，UTC+8）
     */
    String TIMEZONE_SHANGHAI = "Asia/Shanghai";

    /**
     * UTC 时区
     */
    String TIMEZONE_UTC = "UTC";
}
