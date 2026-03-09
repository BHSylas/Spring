package com.example.spring.controller;

import com.example.spring.service.ProfessorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/professors")
public class AdminProfessorProfileController {

    private final ProfessorProfileService professorProfileService;

    @DeleteMapping("/{professorId}/profile")
    public void deleteProfessorProfile(@PathVariable Long professorId) {
        professorProfileService.deleteProfessorProfileByAdmin(professorId);
    }
}