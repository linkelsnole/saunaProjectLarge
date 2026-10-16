package com.example.modbus_api.config;

import com.example.modbus_api.service.PlatformStateService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class PlatformUpdateGuard implements WebMvcConfigurer, HandlerInterceptor {
    private final PlatformStateService platform;
    public PlatformUpdateGuard(PlatformStateService platform) { this.platform = platform; }

    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**").excludePathPatterns("/api/v1/auth/**", "/api/v1/panel-updates/**");
    }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) return true;
        String client = request.getHeader("X-Panel-Build");
        boolean stale = client != null && !platform.buildId().isEmpty() && !platform.buildId().equals(client);
        if (!stale && !platform.updating()) return true;
        response.setStatus(503);
        response.setHeader("X-Panel-Update", "required");
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Платформа обновляется. Команда не выполнена.\"}");
        return false;
    }

}
