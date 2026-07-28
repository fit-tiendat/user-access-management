package com.r2s.user.service;

import com.r2s.user.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProfileQueryService {
    Profile getByUsername(String username);
    Page<Profile> getAll(Pageable pageable);
}
