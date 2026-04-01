package com.hnhegui.framework.excel.service.impl;

import com.hnhegui.framework.excel.service.ExcelOperatorResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 匿名操作人解析器
 *
 * <p>默认实现，返回匿名用户信息。</p>
 * <p>引用方可以实现 {@link ExcelOperatorResolver} 接口并注册为Spring Bean来覆盖此实现，</p>
 * <p>从项目的安全上下文中获取当前用户信息。</p>
 *
 * <p>此实现由 {@code ExcelAutoConfiguration} 通过 @Bean 显式注册，
 * 确保在引用项目不能扫描到框架包时仍能正常工作。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class AnonymousOperatorResolver implements ExcelOperatorResolver {

    private static final Logger log = LoggerFactory.getLogger(AnonymousOperatorResolver.class);

    @Override
    public String getOperatorId() {
        if (log.isDebugEnabled()) {
            log.debug("[操作人解析]使用默认匿名解析器，请实现 ExcelOperatorResolver 以获取真实用户信息");
        }
        return "anonymous";
    }

    @Override
    public String getOperatorName() {
        return "匿名用户";
    }
}
