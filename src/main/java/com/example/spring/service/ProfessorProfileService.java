package com.example.spring.service;

import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.dto.ProfessorProfileResponseDTO;
import com.example.spring.dto.ProfessorProfileUpdateRequestDTO;
import com.example.spring.entity.ProfessorProfile;
import com.example.spring.entity.User;
import com.example.spring.repository.ProfessorProfileRepository;
import com.example.spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessorProfileService {

    private final ProfessorProfileRepository professorProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfessorProfileResponseDTO getProfessorProfile(Long professorId) {
        ProfessorProfile profile = professorProfileRepository.findByUser_UserId(professorId)
                .orElseThrow(() -> new NotFoundException("교수 소개 정보를 찾을 수 없습니다."));
        return new ProfessorProfileResponseDTO(profile);
    }

    @Transactional
    public ProfessorProfileResponseDTO updateMyProfile(Long currentUserId, ProfessorProfileUpdateRequestDTO req) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getUserRole() != 1) {
            throw new ForbiddenException("교수만 소개 정보를 수정할 수 있습니다.");
        }

        ProfessorProfile profile = professorProfileRepository.findByUser_UserId(currentUserId)
                .orElseGet(() -> professorProfileRepository.save(
                        ProfessorProfile.builder()
                                .user(user)
                                .bio(null)
                                .specialty(null)
                                .career(null)
                                .profileImageUrl(null)
                                .office(null)
                                .contactEmail(null)
                                .build()
                ));

        profile.updateProfile(
                trimToNull(req.getBio()),
                trimToNull(req.getSpecialty()),
                trimToNull(req.getCareer()),
                trimToNull(req.getProfileImageUrl()),
                trimToNull(req.getOffice()),
                trimToNull(req.getContactEmail())
        );

        return new ProfessorProfileResponseDTO(profile);
    }

    @Transactional
    public void deleteProfessorProfileByAdmin(Long professorId) {
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (professor.getUserRole() != 1) {
            throw new BadRequestException("교수 계정의 프로필만 삭제할 수 있습니다.");
        }

        ProfessorProfile profile = professorProfileRepository.findByUser_UserId(professorId)
                .orElseThrow(() -> new NotFoundException("교수 소개 정보를 찾을 수 없습니다."));

        professorProfileRepository.delete(profile);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}