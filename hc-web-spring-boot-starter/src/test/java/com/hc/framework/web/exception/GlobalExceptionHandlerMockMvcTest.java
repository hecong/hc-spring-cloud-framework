package com.hc.framework.web.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 新增异常映射：405/415/413/缺少路径变量(400)
 * <p>通过 standalone MockMvc 验证 HTTP 状态码 + 统一 Result 结构。</p>
 */
class GlobalExceptionHandlerMockMvcTest {

    private MockMvc mockMvc;

    @RestController
    static class ErrorProbeController {

        @GetMapping("/probe/method/{id}")
        public String get(@PathVariable("id") long id) {
            return "ok";
        }

        @PostMapping(value = "/probe/consume", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String consume(@RequestBody String body) {
            return body;
        }

        @PostMapping("/probe/upload")
        public String upload() {
            throw new MaxUploadSizeExceededException(2L * 1024 * 1024);
        }

        @GetMapping("/probe/pathvar/{id}")
        public String pathvar(@PathVariable("name") String name) {
            return name;
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("请求方法不支持 → 405 + 统一结构")
    void methodNotSupportedReturns405() throws Exception {
        mockMvc.perform(delete("/probe/method/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.message").value("请求方法不支持"))
                .andExpect(jsonPath("$.path").value("/probe/method/1"));
    }

    @Test
    @DisplayName("Content-Type 不支持 → 415 + 统一结构")
    void mediaTypeNotSupportedReturns415() throws Exception {
        mockMvc.perform(post("/probe/consume")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415))
                .andExpect(jsonPath("$.message").value("不支持的请求内容类型"))
                .andExpect(jsonPath("$.path").value("/probe/consume"));
    }

    @Test
    @DisplayName("上传大小超限 → 413 + 固定提示（不泄漏阈值）")
    void uploadSizeExceededReturns413() throws Exception {
        mockMvc.perform(post("/probe/upload"))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value(413))
                .andExpect(jsonPath("$.message").value("上传文件大小超过限制"))
                .andExpect(jsonPath("$.path").value("/probe/upload"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("2097152"))));
    }

    @Test
    @DisplayName("缺少路径变量 → 400 + 提示含变量名")
    void missingPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/probe/pathvar/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少路径变量: name"))
                .andExpect(jsonPath("$.path").value("/probe/pathvar/1"));
    }
}
