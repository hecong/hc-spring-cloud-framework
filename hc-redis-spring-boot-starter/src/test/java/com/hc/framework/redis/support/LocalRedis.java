package com.hc.framework.redis.support;

import com.hc.framework.redis.core.CustomGenericJackson2JsonRedisSerializer;
import org.junit.jupiter.api.Assumptions;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 本地 Redis 集成测试支撑（无 Docker 依赖）。
 *
 * <p>通过 {@code -Dredis.host} / {@code -Dredis.port} 指定目标实例，默认 127.0.0.1:6379。
 * 不可达时测试以 assumption 跳过，保证在无 Redis 的 CI 环境构建仍为绿色。</p>
 *
 * <p>所有用例均使用随机前缀 key，并在用例结束后清理，不影响库中既有数据。</p>
 */
public final class LocalRedis {

    public static final String HOST = System.getProperty("redis.host", "127.0.0.1");
    public static final int PORT = Integer.parseInt(System.getProperty("redis.port", "6379"));

    private LocalRedis() {
    }

    /**
     * 断言本地 Redis 可用；不可用则跳过当前集成测试。
     */
    public static void requireAvailable() {
        Assumptions.assumeTrue(isReachable(),
            "本地 Redis 不可达 " + HOST + ":" + PORT + "，集成测试跳过。"
                + "可用 -Dredis.host / -Dredis.port 指向可用实例。");
    }

    /**
     * 构建一个与框架配置一致的 RedisTemplate（String Key + 白名单 JSON Value）。
     */
    public static RedisTemplate<String, Object> newRedisTemplate() {
        LettuceConnectionFactory factory =
            new LettuceConnectionFactory(new RedisStandaloneConfiguration(HOST, PORT));
        factory.afterPropertiesSet();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(new CustomGenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new CustomGenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 关闭由 {@link #newRedisTemplate()} 创建的基础连接。
     */
    public static void closeRedisTemplate(RedisTemplate<String, Object> template) {
        if (template != null && template.getConnectionFactory() instanceof LettuceConnectionFactory factory) {
            factory.destroy();
        }
    }

    private static boolean isReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LocalRedis.HOST, LocalRedis.PORT), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
