package com.example.spring.service;

import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.dto.*;
import com.example.spring.entity.*;
import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;

    // =========================================================
    // 수강 신청
    // =========================================================
    @Transactional
    public EnrollmentResponseDTO enroll(Long currentUserId, Long lectureId) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        requireUserOrAdmin(me);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        if (lecture.getStatus() != LectureStatus.APPROVED) {
            throw new BadRequestException("승인된 강의만 수강할 수 있습니다.");
        }

        Enrollment enrollment = enrollmentRepository
                .findByUser_UserIdAndLecture_LectureId(currentUserId, lectureId)
                .orElseGet(() -> enrollmentRepository.save(Enrollment.create(me, lecture)));

        // 기존에 CANCELED였다가 재수강 허용 정책
        if (enrollment.getStatus() == EnrollmentStatus.CANCELED || enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            enrollment.reactivateAndReset();
        }

        return toResponse(enrollment);
    }

    // =========================================================
    // 수강 취소
    // =========================================================
    @Transactional
    public EnrollmentResponseDTO cancelEnrollment(Long currentUserId, Long lectureId) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        requireUserOrAdmin(me);

        Enrollment enrollment = enrollmentRepository
                .findByUser_UserIdAndLecture_LectureId(currentUserId, lectureId)
                .orElseThrow(() -> new NotFoundException("수강 정보가 없습니다."));

        if (enrollment.getStatus() != EnrollmentStatus.CANCELED) {
            enrollment.cancel();
        }

        return toResponse(enrollment);
    }

    // =========================================================
    // 진도 업데이트
    // =========================================================
    @Transactional
    public EnrollmentResponseDTO updateProgress(Long currentUserId, Long lectureId, ProgressUpdateRequestDTO req) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        requireUserOrAdmin(me);

        Enrollment enrollment = enrollmentRepository
                .findByUser_UserIdAndLecture_LectureId(currentUserId, lectureId)
                .orElseThrow(() -> new NotFoundException("수강 정보가 없습니다. 먼저 수강 신청하세요."));

        // 취소 상태면 진도 갱신 막기
        if (enrollment.getStatus() == EnrollmentStatus.CANCELED) {
            throw new BadRequestException("취소된 수강은 진도를 업데이트할 수 없습니다.");
        }

        int lastWatched = req.getLastWatchedTime();
        int totalDuration = req.getTotalDuration();

        int progress = resolveProgress(req, lastWatched, totalDuration);
        enrollment.updateVideoProgress(progress, lastWatched, totalDuration);

        return toResponse(enrollment);
    }

    // =========================================================
    // 내 수강 목록(진도 상태 포함) - status/sort 최종
    // status = ALL|NOT_STARTED|IN_PROGRESS|COMPLETED|CANCELED
    // sort   = recent|progress|title
    // =========================================================
    @Transactional
    public Page<MyEnrollmentItemDTO> listMyEnrollments(
            Long currentUserId,
            String status,
            String sort,
            int page,
            int size
    ) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        requireUserOrAdmin(me);

        EnrollmentStatus parsedStatus = parseEnrollmentStatusOrNull(status);
        String s = (sort == null || sort.isBlank()) ? "recent" : sort.trim().toLowerCase();

        // title 정렬은 전용 Repository 메서드 사용
        if ("title".equals(s)) {
            Pageable pageable = PageRequest.of(page, size); // 정렬은 메서드명이 담당
            Page<Enrollment> result = (parsedStatus == null)
                    ? enrollmentRepository.findByUser_UserIdOrderByLecture_TitleAscEnrollmentIdDesc(currentUserId, pageable)
                    : enrollmentRepository.findByUser_UserIdAndStatusOrderByLecture_TitleAscEnrollmentIdDesc(currentUserId, parsedStatus, pageable);

            return result.map(this::toMyEnrollmentItemDTO);
        }

        // recent/progress는 기존 방식 + 안정 tie-breaker
        Pageable pageable = PageRequest.of(page, size, resolveMyEnrollSort(s));

        Page<Enrollment> result = (parsedStatus == null)
                ? enrollmentRepository.findAllByUser_UserId(currentUserId, pageable)
                : enrollmentRepository.findAllByUser_UserIdAndStatus(currentUserId, parsedStatus, pageable);

        return result.map(this::toMyEnrollmentItemDTO);
    }

    /**
     * 교수: 특정 강의 수강생 목록(+상태 필터)
     * - PROFESSOR: 본인 강의만
     * - ADMIN: 모든 강의 가능
     */
    @Transactional
    public Page<LectureStudentEnrollmentItemDTO> listLectureStudents(
            Long currentUserId,
            Long lectureId,
            EnrollmentStatus status,
            Pageable pageable
    ) {
        User caller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        UserRole role = UserRole.fromCode(caller.getUserRole());
        if (role != UserRole.PROFESSOR && role != UserRole.ADMIN) {
            throw new ForbiddenException("교수 또는 관리자만 조회할 수 있습니다.");
        }

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        if (role == UserRole.PROFESSOR && !lecture.getProfessor().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("본인 강의의 수강생만 조회할 수 있습니다.");
        }

        Page<Enrollment> page = (status == null)
                ? enrollmentRepository.findAllByLecture_LectureId(lectureId, pageable)
                : enrollmentRepository.findAllByLecture_LectureIdAndStatus(lectureId, status, pageable);

        return page.map(e -> new LectureStudentEnrollmentItemDTO(
                e.getEnrollmentId(),
                e.getUser().getUserId(),
                e.getUser().getUserNickname(),
                e.getUser().getUserEmail(),
                e.getStatus().name(),
                e.getProgressRate(),
                e.getLastWatchedTime(),
                e.getTotalDuration(),
                e.getLastAccessedAt(),
                e.getCompletedAt()
        ));
    }

    public EnrollmentResponseDTO getMyEnrollment(Long currentUserId, Long lectureId) {
        Enrollment e = enrollmentRepository
                .findByUser_UserIdAndLecture_LectureId(currentUserId, lectureId)
                .orElseThrow(() -> new NotFoundException("수강 정보가 없습니다."));
        return toResponse(e);
    }

    // =========================================================
    // 내부 유틸
    // =========================================================

    private void requireUserOrAdmin(User user) {
        UserRole role = UserRole.fromCode(user.getUserRole());
        if (role != UserRole.USER && role != UserRole.ADMIN) {
            throw new ForbiddenException("학생 또는 관리자만 접근할 수 있습니다.");
        }
    }

    private EnrollmentStatus parseEnrollmentStatusOrNull(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        try {
            return EnrollmentStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("status 값이 올바르지 않습니다. (ALL, NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELED)");
        }
    }

    private Sort resolveMyEnrollSort(String sortLower) {
        return switch (sortLower) {
            case "progress" -> Sort.by(Sort.Direction.DESC, "progressRate")
                    .and(Sort.by(Sort.Direction.DESC, "lastAccessedAt"))
                    .and(Sort.by(Sort.Direction.DESC, "enrollmentId"));
            case "recent", "latest", "last" -> Sort.by(Sort.Direction.DESC, "lastAccessedAt")
                    .and(Sort.by(Sort.Direction.DESC, "enrollmentId"));
            default -> Sort.by(Sort.Direction.DESC, "lastAccessedAt")
                    .and(Sort.by(Sort.Direction.DESC, "enrollmentId"));
        };
    }

    private MyEnrollmentItemDTO toMyEnrollmentItemDTO(Enrollment e) {
        return new MyEnrollmentItemDTO(
                e.getEnrollmentId(),
                e.getLecture().getLectureId(),
                e.getLecture().getTitle(),
                e.getLecture().getCountry(),
                e.getLecture().getLanguage(),
                e.getLecture().getProfessor().getUserId(),
                e.getLecture().getProfessor().getUserNickname(),
                e.getStatus().name(),
                e.getProgressRate(),
                e.getLastWatchedTime(),
                e.getTotalDuration(),
                e.getLastAccessedAt(),
                e.getCompletedAt()
        );
    }

    private int resolveProgress(ProgressUpdateRequestDTO req, int lastWatched, int totalDuration) {
        // 1) progress 명시 우선
        if (req.getProgress() != null) {
            return clamp(req.getProgress(), 0, 100);
        }

        // 2) 없으면 시간 기반 계산
        if (totalDuration <= 0) {
            throw new BadRequestException("progress가 없으면 totalDuration(영상 길이)이 필요합니다.");
        }

        int pct = (int) Math.floor((lastWatched * 100.0) / totalDuration);
        return clamp(pct, 0, 100);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private EnrollmentResponseDTO toResponse(Enrollment e) {
        return new EnrollmentResponseDTO(
                e.getEnrollmentId(),
                e.getLecture().getLectureId(),
                e.getProgressRate(),
                e.getLastWatchedTime(),
                e.getTotalDuration(),
                e.getStatus().name()
        );
    }
}
