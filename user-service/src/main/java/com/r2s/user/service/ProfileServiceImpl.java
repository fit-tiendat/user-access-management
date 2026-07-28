package com.r2s.user.service;

import com.r2s.core.exception.NotFoundException;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.r2s.user.config.ProfileCacheConfig.PROFILES_BY_USERNAME;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository repo;

    @Override
    @Transactional
    @CacheEvict(cacheNames = PROFILES_BY_USERNAME, key = "#p0")
    public Profile upsert(String username, ProfileDto dto) {
        Profile p = repo.findByUsername(username)
                .orElseGet(() -> Profile.builder().username(username).build());

        p.setFullName(dto.fullName().trim().replaceAll(" +", " "));
        p.setEmail(dto.email().trim());
        return repo.save(p);
    }

    @Override
    @Cacheable(cacheNames = PROFILES_BY_USERNAME, key = "#p0")
    public Profile getByUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    @Override
    public Page<Profile> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = PROFILES_BY_USERNAME, key = "#p0")
    public void deleteByUsername(String username) {
        long affected = repo.deleteByUsername(username);
        if (affected == 0) {
            throw new NotFoundException("Profile not found");
        }
    }
}
