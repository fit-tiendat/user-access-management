package com.r2s.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // map 1-1 theo username của auth-service
    @Column(nullable = false, unique = true)
    private String username;

    private String fullName;

    //  đồng bộ với DTO: không null, unique
    @Column(nullable = false, unique = true)
    private String email;
}
