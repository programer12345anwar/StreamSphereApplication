package com.streamsphere.central.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//or use @Data
@Table(name = "Users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank
    private String name;
    @Column(unique = true, nullable = false)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Email
    private String email;
    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 8, max = 72)
    private String password;
    @Column(unique = true)
    private Long phoneNumber;
    private LocalDate dob;
    private String gender;
    private String country;
    @Column(nullable = false)
    private String role = "USER";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

