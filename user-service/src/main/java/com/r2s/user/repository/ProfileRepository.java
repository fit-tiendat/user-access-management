package com.r2s.user.repository;

import com.r2s.user.entity.Profile;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUsername(String username);
    boolean existsByUsername(String username);

    // ĐỂ Ý: dùng kiểu trả về long để biết có xóa được hay không
//    long deleteByUsername(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from Profile p where p.username = :username")
    int deleteByUsername(@Param("username") String username);
}














