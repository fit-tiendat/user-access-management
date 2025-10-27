package com.r2s.user.service;

import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.exception.NotFoundException;
import com.r2s.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository repo;

    @Override
    @Transactional
    public Profile upsert(ProfileDto dto) {
        Profile p = repo.findByUsername(dto.username())
                .orElseGet(() -> Profile.builder().username(dto.username()).build());
        p.setFullName(dto.fullName());
        p.setEmail(dto.email());
        return repo.save(p);
    }

    @Override
    public Profile getByUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    @Override
    public List<Profile> getAll() {
        return repo.findAll();
    }

    @Override
    @Transactional
    public void deleteByUsername(String username) {
        long affected = repo.deleteByUsername(username); // JPA sẽ trả số hàng xóa
        if (affected == 0) {
            throw new NotFoundException("Profile not found");
        }
    }
}
