package com.r2s.user.service;

import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;

public interface ProfileCommandService {
    Profile upsert(ProfileDto dto);
    void deleteByUsername(String username);
}
