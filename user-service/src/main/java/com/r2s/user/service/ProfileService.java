package com.r2s.user.service;

import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;

import java.util.List;

public interface ProfileService {
    Profile upsert(ProfileDto dto);            // tạo mới nếu chưa có, có rồi thì cập nhật
    Profile getByUsername(String username);
    List<Profile> getAll();                    // ADMIN
    void deleteByUsername(String username);    // ADMIN
}
