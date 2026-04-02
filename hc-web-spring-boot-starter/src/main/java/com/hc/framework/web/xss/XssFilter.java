package com.hc.framework.web.xss;

import com.hc.framework.web.config.WebProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import java.io.IOException;

@RequiredArgsConstructor
public class XssFilter implements Filter {

    private final WebProperties properties;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        // 关闭XSS or 放行路径
        if (!properties.isXssEnabled() || isExclude(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 包装过滤
        chain.doFilter(new XssHttpServletRequestWrapper(req), response);
    }

    private boolean isExclude(String uri) {
        return properties.getXssExcludeUrls().stream()
            .anyMatch(p -> matcher.match(p, uri));
    }
}