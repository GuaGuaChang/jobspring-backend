package com.jobspring.jobspringbackend.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity // 开启方法级别安全：@PreAuthorize @PostAuthorize 等
public class MethodSecurityConfig {
    // 一般不用写任何内容，Spring 会自动处理
}
