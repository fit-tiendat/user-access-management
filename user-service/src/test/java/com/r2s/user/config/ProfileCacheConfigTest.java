package com.r2s.user.config;

import com.r2s.user.entity.Profile;
import com.r2s.user.repository.ProfileRepository;
import com.r2s.user.service.ProfileService;
import com.r2s.user.service.ProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileCacheConfigTest {

    @Test
    void cachesProfileReadsAndEvictsAfterDelete() {
        try (var context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            ProfileRepository repository = context.getBean(ProfileRepository.class);
            ProfileService service = context.getBean(ProfileService.class);
            Profile profile = Profile.builder().username("john").build();

            when(repository.findByUsername("john")).thenReturn(Optional.of(profile));
            when(repository.deleteByUsername("john")).thenReturn(1);

            service.getByUsername("john");
            service.getByUsername("john");
            verify(repository, times(1)).findByUsername("john");

            service.deleteByUsername("john");
            service.getByUsername("john");
            verify(repository, times(2)).findByUsername("john");
        }
    }

    @Configuration
    @EnableCaching
    static class TestConfiguration {

        @Bean
        ProfileRepository profileRepository() {
            return mock(ProfileRepository.class);
        }

        @Bean
        ProfileService profileService(ProfileRepository repository) {
            return new ProfileServiceImpl(repository);
        }

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager(ProfileCacheConfig.PROFILES_BY_USERNAME);
        }
    }
}
