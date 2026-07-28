package com.r2s.core.security.cors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.cors")
public class CorsPolicyProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
