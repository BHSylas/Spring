package com.example.spring.repository;

import com.example.spring.entity.ProfessorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorProfileRepository extends JpaRepository<ProfessorProfile, Long> {
    Optional<ProfessorProfile> findByUser_UserId(Long userId);
}