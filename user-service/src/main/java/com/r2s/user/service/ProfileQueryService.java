package com.r2s.user.service;

import com.r2s.user.entity.Profile;

import java.util.List;

public interface ProfileQueryService {
    Profile getByUsername(String username);
    List<Profile> getAll();
}
