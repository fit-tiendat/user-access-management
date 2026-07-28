package com.r2s.user.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class ProfileCacheConfig {

    public static final String PROFILES_BY_USERNAME = "profiles-by-username";
}
