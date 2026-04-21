package com.hc.framework.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 配置属性
 *
 * <p>配置前缀：hc.satoken，覆盖 Sa-Token 所有核心配置。</p>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     token:
 *       name: Authorization
 *       timeout: 86400
 *       style: uuid
 *     jwt:
 *       enabled: true
 *       secret: your-secret-key
 *     token-clean:
 *       enabled: true
 *       cron: "0 0 3 * * ?"
 *     sso:
 *       enabled: true
 *       server-url: http://sso.example.com
 *     permission:
 *       enabled: true
 *       url-permissions:
 *         - path: /admin/**
 *           role: admin
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "hc.satoken")
public class SaTokenProperties {

    /**
     * 是否启用 Sa-Token 功能
     */
    private Boolean enabled = true;

    // ==================== Token 基础配置 ====================

    /**
     * Token 配置
     */
    private TokenConfig token = new TokenConfig();

    /**
     * Token 配置类
     */
    @Data
    public static class TokenConfig {

        /**
         * Token 名称（同时也是 Cookie 名称）
         */
        private String name = "Authorization";

        /**
         * Token 有效期（单位：秒），默认 30 天
         * <p>-1 表示永不过期</p>
         */
        private Long timeout = 2592000L;

        /**
         * Token 临时有效期（指定时间内无操作就过期）
         * <p>单位：秒，-1 表示不启用</p>
         */
        private Long activityTimeout = -1L;

        /**
         * Token 风格
         * <p>可选值：uuid / simple-uuid / random-32 / random-64 / random-128 / snowflake-32 / snowflake-64 / jwt</p>
         */
        private String style = "uuid";

        /**
         * Token 前缀
         * <p>例如：Bearer，前端需拼接为 Bearer xxx</p>
         */
        private String prefix = "";

        /**
         * 是否允许同一账号并发登录
         */
        private Boolean isConcurrent = true;

        /**
         * 多人登录同一账号时，是否共享 Token
         */
        private Boolean isShare = false;

        /**
         * 是否从 Header 中读取 Token
         */
        private Boolean isReadHeader = true;

        /**
         * 是否从 Cookie 中读取 Token
         */
        private Boolean isReadCookie = false;

        /**
         * 是否从请求参数中读取 Token
         */
        private Boolean isReadBody = false;

        /**
         * 是否在登录后的每次请求都会刷新 Token 有效期
         */
        private Boolean isAutoRenew = true;
    }

    // ==================== Cookie 配置 ====================

    /**
     * Cookie 配置
     */
    private CookieConfig cookie = new CookieConfig();

    /**
     * Cookie 配置类
     */
    @Data
    public static class CookieConfig {

        /**
         * 是否在 Cookie 中存储 Token
         */
        private Boolean isEnabled = false;

        /**
         * Cookie 作用域
         */
        private String domain = "";

        /**
         * Cookie 路径
         */
        private String path = "/";

        /**
         * 是否仅 HTTPS 传输
         */
        private Boolean isSecure = false;

        /**
         * 是否禁止 JS 读取（防 XSS）
         */
        private Boolean isHttpOnly = true;

        /**
         * SameSite 策略（Strict / Lax / None）
         */
        private String sameSite = "Lax";
    }

    // ==================== Redis 存储配置 ====================

    /**
     * Redis 存储配置
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * Redis 配置类
     */
    @Data
    public static class RedisConfig {

        /**
         * 是否启用 Redis 存储（默认自动检测，有 Redis 依赖时自动启用）
         */
        private Boolean enabled = null;

        /**
         * Redis Key 前缀
         */
        private String keyPrefix = "hc:satoken:";
    }

    // ==================== Token 清理配置 ====================

    /**
     * Token 清理配置
     */
    private TokenCleanConfig tokenClean = new TokenCleanConfig();

    /**
     * Token 清理配置类
     */
    @Data
    public static class TokenCleanConfig {

        /**
         * 是否启用 Token 过期清理
         */
        private Boolean enabled = true;

        /**
         * 清理任务 Cron 表达式
         * <p>默认每天凌晨 3 点执行</p>
         */
        private String cron = "0 0 3 * * ?";

        /**
         * 每次清理的批次大小
         */
        private Integer batchSize = 1000;
    }

    // ==================== JWT 配置 ====================

    /**
     * JWT 配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * JWT 配置类
     */
    @Data
    public static class JwtConfig {

        /**
         * 是否启用 JWT 模式
         */
        private Boolean enabled = false;

        /**
         * JWT 密钥
         * <p>必须配置，否则启动时校验失败。建议使用 32 位以上的随机字符串。</p>
         * <p>禁止使用默认值或空值，否则应用将无法启动。</p>
         */
        private String secret = null;

        /**
         * JWT 有效期（秒）
         * <p>默认与 Token 有效期一致，-1 表示永不过期</p>
         */
        private Long timeout = null;

        /**
         * JWT 签名算法
         * <p>可选值：HS256 / HS384 / HS512 / RS256 / RS384 / RS512</p>
         */
        private String algorithm = "HS256";

        /**
         * JWT Issuer（签发者）
         */
        private String issuer = "";

        /**
         * JWT Audience（接收者）
         */
        private String audience = "";
    }

    // ==================== SSO 配置 ====================

    /**
     * SSO 配置
     */
    private SsoConfig sso = new SsoConfig();

    /**
     * SSO 配置类
     */
    @Data
    public static class SsoConfig {

        /**
         * 是否启用 SSO 单点登录
         */
        private Boolean enabled = false;

        /**
         * SSO 认证中心地址
         */
        private String serverUrl = "";

        /**
         * 当前客户端地址
         */
        private String clientUrl = "";

        /**
         * SSO 登录接口路径
         */
        private String loginPath = "/sso/login";

        /**
         * SSO 注销接口路径
         */
        private String logoutPath = "/sso/logout";

        /**
         * SSO Ticket 有效期（秒）
         */
        private Long ticketTimeout = 300L;

        /**
         * 是否允许携带参数跳转
         */
        private Boolean allowUrlParams = true;

        /**
         * SSO 接口调用密钥
         */
        private String secret = "";

        /**
         * 允许的重定向域名白名单
         * <p>为空时仅允许相对路径跳转（以 / 开头），禁止跳转到外部域名。</p>
         * <p>配置示例：app.example.com,admin.example.com</p>
         */
        private List<String> allowedRedirectDomains = new ArrayList<>();
    }

    // ==================== 权限配置 ====================

    /**
     * 权限配置
     */
    private PermissionConfig permission = new PermissionConfig();

    /**
     * 权限配置类
     */
    @Data
    public static class PermissionConfig {

        /**
         * 是否启用权限校验
         */
        private Boolean enabled = true;

        /**
         * URL 权限规则列表
         */
        private List<UrlPermission> urlPermissions = new ArrayList<>();

        /**
         * 是否开启权限缓存
         */
        private Boolean cacheEnabled = true;

        /**
         * 权限缓存过期时间（秒）
         */
        private Long cacheTimeout = 300L;
    }

    /**
     * URL 权限规则
     */
    @Data
    public static class UrlPermission {

        /**
         * 路径（支持 Ant 风格，如 /admin/**）
         */
        private String path;

        /**
         * 所需角色（多个用逗号分隔）
         */
        private String role;

        /**
         * 所需权限（多个用逗号分隔）
         */
        private String permission;

        /**
         * 是否需要登录
         */
        private Boolean requireLogin = true;

        /**
         * 多角色/权限的匹配模式
         * <p>ANY：拥有任一角色/权限即可（默认，向后兼容）</p>
         * <p>ALL：必须拥有全部角色/权限</p>
         */
        private MatchMode matchMode = MatchMode.ANY;
    }

    /**
     * 角色/权限匹配模式
     */
    public enum MatchMode {
        /**
         * 拥有任一即可
         */
        ANY,
        /**
         * 必须全部拥有
         */
        ALL
    }

    // ==================== 登录配置 ====================

    /**
     * 登录配置
     */
    private LoginConfig login = new LoginConfig();

    /**
     * 登录配置类
     */
    @Data
    public static class LoginConfig {

        /**
         * 登录接口路径（排除鉴权）
         */
        private List<String> loginPaths = new ArrayList<>();

        /**
         * 记住我默认时长（秒），默认 7 天
         */
        private Long rememberMeTimeout = 604800L;
    }

    // ==================== 排除路径配置 ====================

    /**
     * 排除路径（不需要鉴权的路径）
     */
    private List<String> excludePaths = new ArrayList<>();

    // ==================== 密码加密配置 ====================

    /**
     * 密码加密配置
     */
    private PasswordConfig password = new PasswordConfig();

    /**
     * 密码加密配置类
     */
    @Data
    public static class PasswordConfig {

        /**
         * 默认密码加密算法
         * <p>可选值：BCRYPT（推荐）、MD5（不推荐）、SM3（国密）</p>
         */
        private String algorithm = "BCRYPT";

        /**
         * 是否启用加密
         */
        private Boolean enabled = true;
    }

    // ==================== 前后端分离配置 ====================

    /**
     * 前后端分离配置
     */
    private FrontendConfig frontend = new FrontendConfig();

    /**
     * 前后端分离配置类
     */
    @Data
    public static class FrontendConfig {

        /**
         * 是否启用跨域自动配置
         */
        private Boolean corsEnabled = true;

        /**
         * Token 读取优先级
         * <p>可选值：HEADER、PARAMETER、COOKIE，多个用逗号分隔，按优先级排序</p>
         * <p>默认：HEADER,COOKIE（不包含 PARAMETER，因 URL 参数中的 Token 会泄露到日志/Referer/浏览器历史）</p>
         * <p>如需从 URL 参数读取（如 WebSocket 场景），可配置为 HEADER,PARAMETER,COOKIE，但建议使用短时效 Token</p>
         */
        private String tokenReadOrder = "HEADER,COOKIE";

        /**
         * 是否自动将 Token 加入跨域允许头
         */
        private Boolean corsTokenHeader = true;

        /**
         * 跨域允许的来源（默认 * 表示所有来源）
         */
        private String corsAllowedOrigins = "*";

        /**
         * 跨域允许的方法
         */
        private String corsAllowedMethods = "GET,POST,PUT,DELETE,OPTIONS";

        /**
         * 跨域允许的头
         */
        private String corsAllowedHeaders = "*";

        /**
         * 跨域预检请求缓存时间（秒）
         */
        private Long corsMaxAge = 3600L;

        /**
         * 是否允许携带凭证
         */
        private Boolean corsAllowCredentials = true;
    }

    // ==================== 日志配置 ====================

    /**
     * 日志配置
     */
    private AuthLogConfig authLog = new AuthLogConfig();

    /**
     * 认证日志配置类
     */
    @Data
    public static class AuthLogConfig {

        /**
         * 是否启用登录日志
         */
        private Boolean loginLogEnabled = true;

        /**
         * 是否启用鉴权日志
         */
        private Boolean authLogEnabled = true;

        /**
         * 是否记录登录成功日志
         */
        private Boolean logLoginSuccess = true;

        /**
         * 是否记录登录失败日志
         */
        private Boolean logLoginFailure = true;

        /**
         * 是否记录权限校验失败日志
         */
        private Boolean logPermissionDenied = true;

        /**
         * 日志格式：SIMPLE（简单）、DETAIL（详细）
         */
        private String logFormat = "SIMPLE";
    }

    /**
     * 是否输出 Sa-Token 操作日志（兼容旧配置）
     */
    private Boolean isLog = false;

    // ==================== 配置刷新支持 ====================

    /**
     * 是否支持配置动态刷新（Nacos/Apollo）
     */
    private Boolean refreshEnabled = true;

    // ==================== 工具方法 ====================

    /**
     * 获取完整的排除路径列表（包含默认路径 + 配置路径）
     */
    public List<String> getAllExcludePaths() {
        List<String> paths = new ArrayList<>();
        // 默认排除路径
        paths.add("/error");
        paths.add("/swagger-ui/**");
        paths.add("/swagger-resources/**");
        paths.add("/v3/api-docs/**");
        paths.add("/webjars/**");
        paths.add("/doc.html");
        paths.add("/favicon.ico");
        // SSO 排除路径
        if (Boolean.TRUE.equals(sso.getEnabled())) {
            paths.add(sso.getLoginPath() + "/**");
            paths.add(sso.getLogoutPath() + "/**");
        }
        // 登录路径
        paths.addAll(login.getLoginPaths());
        // 配置的排除路径
        paths.addAll(excludePaths);
        return paths;
    }

    /**
     * 判断是否使用 JWT Token
     */
    public boolean isJwtEnabled() {
        return Boolean.TRUE.equals(jwt.getEnabled()) || "jwt".equalsIgnoreCase(token.getStyle());
    }
}
