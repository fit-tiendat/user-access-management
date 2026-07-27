package com.r2s.user;

import com.r2s.core.config.OpenApiConfig;
import com.r2s.core.exception.GlobalExceptionHandler;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtService;
import com.r2s.core.utils.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.r2s.user.entity")
@EnableJpaRepositories(basePackages = "com.r2s.user.repository")
@Import({
		JwtService.class,
		JwtFilter.class,
		GlobalExceptionHandler.class,
		ResponseBuilder.class,
		OpenApiConfig.class
})
public class UserServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}
}
