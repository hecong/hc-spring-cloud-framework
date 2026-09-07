package com.hc.framework.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IpUtils 客户端 IP 解析：可信代理白名单（CIDR）、伪造头防护、回环/代理链场景
 */
class IpUtilsTest {

    private static final String HEADER_REAL_IP = "X-Real-IP";
    private static final String HEADER_XFF = "X-Forwarded-For";

    @AfterEach
    void tearDown() {
        IpUtils.setTrustedProxies(Collections.emptyList());
    }

    private MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (remoteAddr != null) {
            request.setRemoteAddr(remoteAddr);
        }
        return request;
    }

    // ---------- 4.1 isTrusted：精确/CIDR/非法项 ----------

    @Test
    @DisplayName("isTrusted：CIDR 与精确 IP 混合配置均识别")
    void trustedMixedCidrAndExact() {
        List<String> trusted = List.of("10.0.0.0/8", "192.168.1.1");
        assertTrue(IpUtils.isTrusted("10.1.2.3", trusted));
        assertTrue(IpUtils.isTrusted("192.168.1.1", trusted));
        assertFalse(IpUtils.isTrusted("8.8.8.8", trusted));
        assertFalse(IpUtils.isTrusted("192.168.1.2", trusted));
    }

    @Test
    @DisplayName("isTrusted：0.0.0.0/0 全匹配、/32 精确匹配")
    void trustedFullAndHostCidr() {
        assertTrue(IpUtils.isTrusted("203.0.113.9", List.of("0.0.0.0/0")));
        assertTrue(IpUtils.isTrusted("10.1.1.1", List.of("10.1.1.1/32")));
        assertFalse(IpUtils.isTrusted("10.1.1.2", List.of("10.1.1.1/32")));
    }

    @Test
    @DisplayName("isTrusted：非法配置项被忽略，不影响其余项生效")
    void trustedIllegalRulesIgnored() {
        List<String> trusted = List.of("10.1.0.0/24", "999.1.1.1", "bad", "10.1.0.0/33", "10.0.0.256", "192.168.1.1/40");
        assertTrue(IpUtils.isTrusted("10.1.0.5", trusted), "合法 /24 应生效");
        assertFalse(IpUtils.isTrusted("10.2.0.5", trusted), "非法 /33 不得被当作 10.0.0.0/8 生效");
        assertTrue(IpUtils.isTrusted("2001:db8::1", List.of("2001:db8::1")), "IPv6 精确匹配");
        assertFalse(IpUtils.isTrusted("2001:db8::2", List.of("2001:db8::1")), "IPv6 精确不匹配");
    }

    // ---------- 4.2 解析算法 ----------

    @Test
    @DisplayName("缺省无可信代理：伪造 XFF 无效，直接返回 remoteAddr")
    void defaultEmptyIgnoresForgedXff() {
        MockHttpServletRequest request = request("8.8.8.8");
        request.addHeader(HEADER_XFF, "1.2.3.4");
        request.addHeader(HEADER_REAL_IP, "6.6.6.6");
        assertEquals("8.8.8.8", IpUtils.getClientIp(request), "缺省应忽略全部转发头");
        assertEquals("8.8.8.8", IpUtils.getClientIp(request, Collections.emptyList()));
    }

    @Test
    @DisplayName("回环地址：优先 X-Real-IP，其次 XFF 首 IP（本机单跳代理/调试）")
    void loopbackPrefersRealIpThenXffFirst() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader(HEADER_REAL_IP, "10.20.30.40");
        request.addHeader(HEADER_XFF, "1.2.3.4");
        assertEquals("10.20.30.40", IpUtils.getClientIp(request));

        MockHttpServletRequest request2 = request("127.0.0.1");
        request2.addHeader(HEADER_XFF, "5.6.7.8, 9.9.9.9");
        assertEquals("5.6.7.8", IpUtils.getClientIp(request2), "无 X-Real-IP 时取 XFF 首 IP");
    }

    @Test
    @DisplayName("::1 回环归一为 127.0.0.1；无转发头时兜底回环地址")
    void loopbackV6NormalizedAndFallback() {
        MockHttpServletRequest request = request("0:0:0:0:0:0:0:1");
        assertEquals("127.0.0.1", IpUtils.getClientIp(request), "::1 归一为 127.0.0.1");

        MockHttpServletRequest request2 = request("0:0:0:0:0:0:0:1");
        request2.addHeader(HEADER_REAL_IP, "10.20.30.40");
        request2.addHeader(HEADER_XFF, "1.2.3.4");
        assertEquals("10.20.30.40", IpUtils.getClientIp(request2), "::1 同样走回环 X-Real-IP 优先");
    }

    @Test
    @DisplayName("可信链：XFF 右→左首个不可信 IP")
    void trustedChainRightToLeftFirstUntrusted() {
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader(HEADER_XFF, "8.8.8.8, 10.0.0.4, 10.0.0.5");
        assertEquals("8.8.8.8", IpUtils.getClientIp(request, List.of("10.0.0.0/8")));
    }

    @Test
    @DisplayName("可信链全可信：回退 X-Real-IP，再回退 remoteAddr")
    void trustedChainAllTrustedFallsBackToRealThenRemote() {
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader(HEADER_XFF, "10.0.0.1, 10.0.0.2");
        request.addHeader(HEADER_REAL_IP, "7.7.7.7");
        assertEquals("7.7.7.7", IpUtils.getClientIp(request, List.of("10.0.0.0/8")), "全可信时优先 X-Real-IP");

        MockHttpServletRequest request2 = request("10.0.0.5");
        request2.addHeader(HEADER_XFF, "10.0.0.1, 10.0.0.2");
        assertEquals("10.0.0.5", IpUtils.getClientIp(request2, List.of("10.0.0.0/8")), "无 X-Real-IP 时回退 remoteAddr");
    }

    @Test
    @DisplayName("不可信直连：忽略全部转发头，返回 remoteAddr")
    void untrustedDirectIgnoresHeaders() {
        MockHttpServletRequest request = request("203.0.113.9");
        request.addHeader(HEADER_XFF, "1.2.3.4");
        request.addHeader(HEADER_REAL_IP, "6.6.6.6");
        assertEquals("203.0.113.9", IpUtils.getClientIp(request, List.of("10.0.0.0/8")));
    }

    @Test
    @DisplayName("XFF 链含 unknown/畸形项：跳过继续向左，返回首个不可信合法 IP")
    void trustedChainSkipsUnknownAndMalformed() {
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader(HEADER_XFF, "9.9.9.9, unknown, 10.0.0.4");
        assertEquals("9.9.9.9", IpUtils.getClientIp(request, List.of("10.0.0.0/8")));
    }

    @Test
    @DisplayName("setTrustedProxies 注入持有者后 getClientIp(request) 生效，重置后恢复安全默认")
    void holderInjectionDrivesGetClientIp() {
        IpUtils.setTrustedProxies(List.of("10.0.0.0/8"));
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader(HEADER_XFF, "8.8.8.8, 10.0.0.4, 10.0.0.5");
        assertEquals("8.8.8.8", IpUtils.getClientIp(request), "持有者配置生效");

        IpUtils.setTrustedProxies(Collections.emptyList());
        MockHttpServletRequest request2 = request("8.8.8.8");
        request2.addHeader(HEADER_XFF, "1.2.3.4");
        assertEquals("8.8.8.8", IpUtils.getClientIp(request2), "重置后伪造 XFF 无效");
    }
}
