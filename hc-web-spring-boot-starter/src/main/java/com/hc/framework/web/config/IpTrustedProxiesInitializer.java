package com.hc.framework.web.config;

import com.hc.framework.common.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.List;

/**
 * 可信代理配置启动注入器
 *
 * <p>将 {@code hc.web.trusted-proxies} 注入 {@link IpUtils} 静态持有者；
 * 未引入 hc-web 或未配置时保持空列表（安全默认：不采信代理头）。
 * 业务可通过自定义同名 Bean（{@code @ConditionalOnMissingBean}）覆盖默认注入逻辑。</p>
 *
 * @author hc-framework
 * @since 1.1.0
 */
@Slf4j
public class IpTrustedProxiesInitializer implements InitializingBean {

    private final List<String> trustedProxies;

    public IpTrustedProxiesInitializer(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> proxies = trustedProxies == null ? Collections.emptyList() : trustedProxies;
        IpUtils.setTrustedProxies(proxies);
        log.info("IpUtils trusted proxies configured: {}", proxies);
    }
}
