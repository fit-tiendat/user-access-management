package com.r2s.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)   // map 1-1 theo username của auth-service
    private String username;

    private String fullName;
    private String email;      // có thể null (Task 2 bạn đã cho phép nullable ở auth)
}
