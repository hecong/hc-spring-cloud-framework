package com.hc.framework.excel.service;

/**
 * 操作人解析器接口
 *
 * <p>用于获取当前操作人的ID和姓名，框架本身不提供具体实现，</p>
 * <p>由引用方根据项目的安全上下文（Session、JWT Token、ThreadLocal等）实现。</p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>记录Excel导入导出操作日志时需要记录操作人信息</li>
 *   <li>业务方可以从Spring Security、JWT Token、Session等获取当前用户</li>
 * </ul>
 *
 * <h3>自定义实现示例：</h3>
 * <pre>{@code
 * // 基于Spring Security的实现
 * @Component
 * public class SecurityOperatorResolver implements ExcelOperatorResolver {
 *     @Override
 *     public String getOperatorId() {
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         return auth != null ? auth.getName() : "anonymous";
 *     }
 *
 *     @Override
 *     public String getOperatorName() {
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         if (auth != null && auth.getPrincipal() instanceof UserDetails) {
 *             return ((UserDetails) auth.getPrincipal()).getUsername();
 *         }
 *         return "匿名用户";
 *     }
 * }
 *
 * // 基于JWT Token的实现
 * @Component
 * public class JwtOperatorResolver implements ExcelOperatorResolver {
 *     @Autowired
 *     private HttpServletRequest request;
 *
 *     @Override
 *     public String getOperatorId() {
 *         String token = request.getHeader("Authorization");
 *         return JwtUtil.parseUserId(token);
 *     }
 *
 *     @Override
 *     public String getOperatorName() {
 *         String token = request.getHeader("Authorization");
 *         return JwtUtil.parseUserName(token);
 *     }
 * }
 *
 * // 基于ThreadLocal的实现
 * @Component
 * public class ThreadLocalOperatorResolver implements ExcelOperatorResolver {
 *     @Override
 *     public String getOperatorId() {
 *         return UserContext.getCurrentUserId();
 *     }
 *
 *     @Override
 *     public String getOperatorName() {
 *         return UserContext.getCurrentUserName();
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface ExcelOperatorResolver {

    /**
     * 获取当前操作人ID
     *
     * @return 操作人ID，未登录时返回 null 或 "anonymous"
     */
    String getOperatorId();

    /**
     * 获取当前操作人姓名
     *
     * @return 操作人姓名，未登录时返回 null 或 "匿名用户"
     */
    String getOperatorName();
}
