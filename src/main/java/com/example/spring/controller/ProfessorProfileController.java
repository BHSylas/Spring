package com.example.spring.controller;

import com.example.spring.dto.ProfessorProfileResponseDTO;
import com.example.spring.dto.ProfessorProfileUpdateRequestDTO;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.ProfessorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/professor")
public class ProfessorProfileController {

    private final ProfessorProfileService professorProfileService;

    @GetMapping("/{professorId}/profile")
    public ProfessorProfileResponseDTO getProfile(@PathVariable Long professorId) {
        return professorProfileService.getProfessorProfile(professorId);
    }

    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN')")
    @PatchMapping("/me/profile")
    public ProfessorProfileResponseDTO updateMyProfile(
            Authentication authentication,
            @RequestBody @Valid ProfessorProfileUpdateRequestDTO req
    ) {
        Long currentUserId = CurrentUser.getUserId(authentication);
        return professorProfileService.updateMyProfile(currentUserId, req);
    }
}