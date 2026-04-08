package com.hc.framework.common.util;

import java.util.Collection;

/**
 * 业务断言工具类
 *
 * <p>提供统一的业务条件校验方法，校验失败时抛出 {@link IllegalArgumentException}（运行时异常），
 * 便于与全局异常处理器配合使用。</p>
 *
 * <p>设计说明：此工具类不依赖 hc-web-spring-boot-starter 中的 BusinessException，
 * 避免循环依赖。调用方可在全局异常处理器中统一捕获 {@link IllegalArgumentException} 并转换为标准响应。
 * 如需抛出带错误码的自定义异常，请使用带 {@code exceptionSupplier} 参数的重载方法。</p>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * // 断言条件为真（条件不满足则抛出异常）
 * AssertUtils.isTrue(user.getAge() >= 18, "用户年龄不满18岁");
 *
 * // 断言对象非空
 * AssertUtils.notNull(userId, "用户ID不能为空");
 *
 * // 断言字符串非空
 * AssertUtils.notBlank(username, "用户名不能为空");
 *
 * // 断言集合非空
 * AssertUtils.notEmpty(userList, "用户列表不能为空");
 *
 * // 断言两个对象相等
 * AssertUtils.equals(inputPwd, dbPwd, "密码错误");
 *
 * // 使用自定义异常（适配 BusinessException）
 * AssertUtils.isTrue(stock > 0, () -> new BusinessException(400, "库存不足"));
 * }</pre>
 *
 * @author hc-framework
 */
public class AssertUtils {

    private AssertUtils() {
    }

    // ==================== 条件断言 ====================

    /**
     * 断言条件为 true，否则抛出 {@link IllegalArgumentException}
     *
     * @param condition 断言条件
     * @param message   条件不满足时的错误信息
     * @throws IllegalArgumentException 条件为 false 时抛出
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言条件为 true，否则通过 Supplier 抛出自定义异常
     *
     * <pre>{@code
     * AssertUtils.isTrue(price > 0, () -> new BusinessException(400, "价格必须大于0"));
     * }</pre>
     *
     * @param condition         断言条件
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     * @throws X 条件为 false 时抛出 Supplier 提供的异常
     */
    public static <X extends RuntimeException> void isTrue(boolean condition,
            java.util.function.Supplier<X> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言条件为 false，否则抛出 {@link IllegalArgumentException}
     *
     * @param condition 断言条件（为 true 时抛出异常）
     * @param message   错误信息
     * @throws IllegalArgumentException 条件为 true 时抛出
     */
    public static void isFalse(boolean condition, String message) {
        isTrue(!condition, message);
    }

    // ==================== 非空断言 ====================

    /**
     * 断言对象不为 null，否则抛出 {@link IllegalArgumentException}
     *
     * @param object  待断言对象
     * @param message 错误信息
     * @throws IllegalArgumentException 对象为 null 时抛出
     */
    public static void notNull(Object object, String message) {
        isTrue(object != null, message);
    }

    /**
     * 断言对象不为 null，否则通过 Supplier 抛出自定义异常
     *
     * @param object            待断言对象
     * @param exceptionSupplier 异常提供者
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void notNull(Object object,
            java.util.function.Supplier<X> exceptionSupplier) {
        isTrue(object != null, exceptionSupplier);
    }

    /**
     * 断言字符串不为空（null、空字符串、纯空白均视为空），否则抛出 {@link IllegalArgumentException}
     *
     * @param str     待断言字符串
     * @param message 错误信息
     * @throws IllegalArgumentException 字符串为空时抛出
     */
    public static void notBlank(String str, String message) {
        isTrue(str != null && !str.isBlank(), message);
    }

    /**
     * 断言集合不为空（null 或 size == 0 均视为空），否则抛出 {@link IllegalArgumentException}
     *
     * @param collection 待断言集合
     * @param message    错误信息
     * @throws IllegalArgumentException 集合为空时抛出
     */
    public static void notEmpty(Collection<?> collection, String message) {
        isTrue(collection != null && !collection.isEmpty(), message);
    }

    // ==================== 相等断言 ====================

    /**
     * 断言两个对象相等（使用 equals 比较），否则抛出 {@link IllegalArgumentException}
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param message  错误信息
     * @throws IllegalArgumentException 两者不相等时抛出
     */
    public static void equals(Object expected, Object actual, String message) {
        if (expected == null) {
            isTrue(actual == null, message);
        } else {
            isTrue(expected.equals(actual), message);
        }
    }

    /**
     * 断言两个对象不相等，否则抛出 {@link IllegalArgumentException}
     *
     * @param unexpected 不期望的值
     * @param actual     实际值
     * @param message    错误信息
     * @throws IllegalArgumentException 两者相等时抛出
     */
    public static void notEquals(Object unexpected, Object actual, String message) {
        if (unexpected == null) {
            isTrue(actual != null, message);
        } else {
            isTrue(!unexpected.equals(actual), message);
        }
    }

    // ==================== 范围断言 ====================

    /**
     * 断言数值大于 0，否则抛出 {@link IllegalArgumentException}
     *
     * @param value   待断言数值
     * @param message 错误信息
     * @throws IllegalArgumentException 数值不大于 0 时抛出
     */
    public static void positive(long value, String message) {
        isTrue(value > 0, message);
    }

    /**
     * 断言数值不小于 0（即 >= 0），否则抛出 {@link IllegalArgumentException}
     *
     * @param value   待断言数值
     * @param message 错误信息
     * @throws IllegalArgumentException 数值为负时抛出
     */
    public static void nonNegative(long value, String message) {
        isTrue(value >= 0, message);
    }

    /**
     * 断言数值在指定范围内 [min, max]，否则抛出 {@link IllegalArgumentException}
     *
     * @param value   待断言数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误信息
     * @throws IllegalArgumentException 数值超出范围时抛出
     */
    public static void inRange(long value, long min, long max, String message) {
        isTrue(value >= min && value <= max, message);
    }
}
