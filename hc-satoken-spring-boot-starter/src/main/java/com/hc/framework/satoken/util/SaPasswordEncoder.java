package com.hc.framework.satoken.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Sa-Token 密码加密工具类
 *
 * <p>支持多种加密算法，可通过配置切换默认算法。</p>
 *
 * <p>支持的算法：</p>
 * <ul>
 *   <li>BCrypt - 推荐使用，自带盐值，安全性高</li>
 *   <li>MD5 - 不推荐，仅用于兼容旧系统</li>
 *   <li>SM3 - 国密算法，适用于需要国密标准的场景</li>
 * </ul>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     password:
 *       algorithm: BCRYPT  # 可选值：BCRYPT, MD5, SM3
 * }</pre>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 加密密码
 * String encoded = SaPasswordEncoder.encode("password123");
 *
 * // 验证密码
 * boolean matches = SaPasswordEncoder.matches("password123", encoded);
 *
 * // 使用指定算法
 * String md5Encoded = SaPasswordEncoder.encode("password123", PasswordAlgorithm.MD5);
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaPasswordEncoder {

    /**
     * 默认算法
     */
    private static volatile PasswordAlgorithm defaultAlgorithm = PasswordAlgorithm.BCRYPT;

    /**
     * BCrypt 编码器（延迟初始化）
     */
    private static volatile BCryptPasswordEncoder bCryptPasswordEncoder;

    private SaPasswordEncoder() {
        // 工具类，禁止实例化
    }

    /**
     * 设置默认加密算法
     *
     * @param algorithm 加密算法
     */
    public static void setDefaultAlgorithm(PasswordAlgorithm algorithm) {
        if (algorithm != null) {
            defaultAlgorithm = algorithm;
            log.info("SaPasswordEncoder 默认算法已切换为: {}", algorithm);
        }
    }

    /**
     * 获取默认加密算法
     *
     * @return 默认加密算法
     */
    public static PasswordAlgorithm getDefaultAlgorithm() {
        return defaultAlgorithm;
    }

    /**
     * 使用默认算法加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(CharSequence rawPassword) {
        return encode(rawPassword, defaultAlgorithm);
    }

    /**
     * 使用指定算法加密密码
     *
     * @param rawPassword 原始密码
     * @param algorithm   加密算法
     * @return 加密后的密码
     */
    public static String encode(CharSequence rawPassword, PasswordAlgorithm algorithm) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return switch (algorithm) {
            case BCRYPT -> encodeWithBCrypt(rawPassword);
            case MD5 -> encodeWithMD5(rawPassword);
            case SM3 -> encodeWithSM3(rawPassword);
        };
    }

    /**
     * 验证密码是否匹配
     *
     * <p>自动识别密码的加密算法：</p>
     * <ul>
     *   <li>BCrypt 密码以 $2a$ 或 $2b$ 或 $2y$ 开头</li>
     *   <li>MD5 密码为 32 位十六进制</li>
     *   <li>SM3 密码为 64 位十六进制</li>
     * </ul>
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        // 自动识别算法
        PasswordAlgorithm algorithm = detectAlgorithm(encodedPassword);
        return matches(rawPassword, encodedPassword, algorithm);
    }

    /**
     * 使用指定算法验证密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @param algorithm       加密算法
     * @return 是否匹配
     */
    public static boolean matches(CharSequence rawPassword, String encodedPassword, PasswordAlgorithm algorithm) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        return switch (algorithm) {
            case BCRYPT -> matchesWithBCrypt(rawPassword, encodedPassword);
            case MD5 -> matchesWithMD5(rawPassword, encodedPassword);
            case SM3 -> matchesWithSM3(rawPassword, encodedPassword);
        };
    }

    // ==================== BCrypt 算法 ====================

    /**
     * 使用 BCrypt 加密
     */
    private static String encodeWithBCrypt(CharSequence rawPassword) {
        return getBCryptPasswordEncoder().encode(rawPassword);
    }

    /**
     * 使用 BCrypt 验证
     */
    private static boolean matchesWithBCrypt(CharSequence rawPassword, String encodedPassword) {
        return getBCryptPasswordEncoder().matches(rawPassword, encodedPassword);
    }

    /**
     * 获取 BCrypt 编码器（双重检查锁定）
     */
    private static BCryptPasswordEncoder getBCryptPasswordEncoder() {
        if (bCryptPasswordEncoder == null) {
            synchronized (SaPasswordEncoder.class) {
                if (bCryptPasswordEncoder == null) {
                    bCryptPasswordEncoder = new BCryptPasswordEncoder();
                }
            }
        }
        return bCryptPasswordEncoder;
    }

    // ==================== MD5 算法 ====================

    /**
     * 使用 MD5 加密
     *
     * <p>注意：MD5 已不再推荐用于密码加密，仅用于兼容旧系统。</p>
     */
    private static String encodeWithMD5(CharSequence rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    /**
     * 使用 MD5 验证
     */
    private static boolean matchesWithMD5(CharSequence rawPassword, String encodedPassword) {
        String encrypted = encodeWithMD5(rawPassword);
        return encrypted.equalsIgnoreCase(encodedPassword);
    }

    // ==================== SM3 算法 ====================

    /**
     * 使用 SM3 加密
     *
     * <p>SM3 是中国国家密码管理局发布的密码哈希算法，输出长度为 256 位（64 个十六进制字符）。</p>
     * <p>适用于需要符合国密标准的应用场景。</p>
     */
    private static String encodeWithSM3(CharSequence rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SM3");
            byte[] digest = md.digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // 如果 JDK 不支持 SM3，使用 Bouncy Castle 实现
            return encodeWithSM3Fallback(rawPassword);
        }
    }

    /**
     * SM3 备用实现（当 JDK 不支持时）
     */
    private static String encodeWithSM3Fallback(CharSequence rawPassword) {
        // 简化的 SM3 实现
        byte[] data = rawPassword.toString().getBytes(StandardCharsets.UTF_8);
        byte[] hash = sm3Hash(data);
        return HexFormat.of().formatHex(hash);
    }

    /**
     * SM3 哈希计算
     */
    private static byte[] sm3Hash(byte[] data) {
        // SM3 常量
        int[] IV = {
                0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
                0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e
        };

        // 消息填充
        int len = data.length;
        int padLen = ((len + 9) / 64 + 1) * 64;
        byte[] padded = new byte[padLen];
        System.arraycopy(data, 0, padded, 0, len);
        padded[len] = (byte) 0x80;

        // 长度填充
        long bitLen = (long) len * 8;
        for (int i = 0; i < 8; i++) {
            padded[padLen - 1 - i] = (byte) (bitLen >>> (i * 8));
        }

        // 压缩函数
        int[] v = IV.clone();
        for (int i = 0; i < padLen / 64; i++) {
            int[] w = new int[68];
            int[] w1 = new int[64];

            // 消息扩展
            for (int j = 0; j < 16; j++) {
                w[j] = bytesToInt(padded, i * 64 + j * 4);
            }
            for (int j = 16; j < 68; j++) {
                w[j] = p1(w[j - 16] ^ w[j - 9] ^ rotateLeft(w[j - 3], 15))
                        ^ rotateLeft(w[j - 13], 7) ^ w[j - 6];
            }
            for (int j = 0; j < 64; j++) {
                w1[j] = w[j] ^ w[j + 4];
            }

            // 压缩
            int a = v[0], b = v[1], c = v[2], d = v[3];
            int e = v[4], f = v[5], g = v[6], h = v[7];

            for (int j = 0; j < 64; j++) {
                int ss1 = rotateLeft(rotateLeft(a, 12) + e + rotateLeft(t(j), j), 7);
                int ss2 = ss1 ^ rotateLeft(a, 12);
                int tt1 = ff(a, b, c, j) + d + ss2 + w1[j];
                int tt2 = gg(e, f, g, j) + h + ss1 + w[j];
                d = c;
                c = rotateLeft(b, 9);
                b = a;
                a = tt1;
                h = g;
                g = rotateLeft(f, 19);
                f = e;
                e = p0(tt2);
            }

            v[0] ^= a;
            v[1] ^= b;
            v[2] ^= c;
            v[3] ^= d;
            v[4] ^= e;
            v[5] ^= f;
            v[6] ^= g;
            v[7] ^= h;
        }

        // 输出
        byte[] result = new byte[32];
        for (int i = 0; i < 8; i++) {
            intToBytes(v[i], result, i * 4);
        }
        return result;
    }

    private static int bytesToInt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24) |
                ((data[offset + 1] & 0xff) << 16) |
                ((data[offset + 2] & 0xff) << 8) |
                (data[offset + 3] & 0xff);
    }

    private static void intToBytes(int value, byte[] data, int offset) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }

    private static int rotateLeft(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    private static int p0(int x) {
        return x ^ rotateLeft(x, 9) ^ rotateLeft(x, 17);
    }

    private static int p1(int x) {
        return x ^ rotateLeft(x, 15) ^ rotateLeft(x, 23);
    }

    private static int t(int j) {
        return j < 16 ? 0x79cc4519 : 0x7a879d8a;
    }

    private static int ff(int x, int y, int z, int j) {
        if (j < 16) {
            return x ^ y ^ z;
        }
        return (x & y) | (x & z) | (y & z);
    }

    private static int gg(int x, int y, int z, int j) {
        if (j < 16) {
            return x ^ y ^ z;
        }
        return (x & y) | (~x & z);
    }

    /**
     * 使用 SM3 验证
     */
    private static boolean matchesWithSM3(CharSequence rawPassword, String encodedPassword) {
        String encrypted = encodeWithSM3(rawPassword);
        return encrypted.equalsIgnoreCase(encodedPassword);
    }

    // ==================== 算法识别 ====================

    /**
     * 根据加密后的密码自动识别算法
     *
     * @param encodedPassword 加密后的密码
     * @return 识别出的算法
     */
    private static PasswordAlgorithm detectAlgorithm(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return defaultAlgorithm;
        }

        // BCrypt 格式：$2a$、$2b$、$2y$ 开头
        if (encodedPassword.startsWith("$2a$") ||
                encodedPassword.startsWith("$2b$") ||
                encodedPassword.startsWith("$2y$")) {
            return PasswordAlgorithm.BCRYPT;
        }

        // SM3：64 位十六进制
        if (encodedPassword.length() == 64 && isHex(encodedPassword)) {
            return PasswordAlgorithm.SM3;
        }

        // MD5：32 位十六进制
        if (encodedPassword.length() == 32 && isHex(encodedPassword)) {
            return PasswordAlgorithm.MD5;
        }

        // 无法识别，使用默认算法
        log.warn("无法识别密码格式，使用默认算法: {}", defaultAlgorithm);
        return defaultAlgorithm;
    }

    /**
     * 检查字符串是否为有效的十六进制
     */
    private static boolean isHex(String str) {
        for (char c : str.toCharArray()) {
            if (!((c >= '0' && c <= '9') ||
                    (c >= 'a' && c <= 'f') ||
                    (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 密码加密算法枚举
     */
    public enum PasswordAlgorithm {
        /**
         * BCrypt 算法（推荐）
         * <p>安全性高，自带盐值，自动处理密码哈希</p>
         */
        BCRYPT,

        /**
         * MD5 算法（不推荐）
         * <p>仅用于兼容旧系统，不推荐用于新项目</p>
         */
        MD5,

        /**
         * SM3 国密算法
         * <p>中国国家密码管理局发布的密码哈希算法</p>
         */
        SM3
    }
}
