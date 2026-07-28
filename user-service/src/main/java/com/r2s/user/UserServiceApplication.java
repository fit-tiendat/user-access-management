package com.r2s.user;

import com.r2s.core.config.OpenApiConfig;
import com.r2s.core.exception.GlobalExceptionHandler;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtService;
import com.r2s.core.security.audit.SecurityAuditLogger;
import com.r2s.core.security.cors.CorsPolicyConfiguration;
import com.r2s.core.security.cors.CorsPolicyProperties;
import com.r2s.core.utils.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
		JwtService.class,
		JwtFilter.class,
		GlobalExceptionHandler.class,
		ResponseBuilder.class,
		OpenApiConfig.class,
		SecurityAuditLogger.class,
		CorsPolicyConfiguration.class,
		CorsPolicyProperties.class
})
public class UserServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}
}
