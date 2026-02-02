package com.example.spring.service;

import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.dto.EnrollmentResponseDTO;
import com.example.spring.dto.LectureStudentEnrollmentItemDTO;
import com.example.spring.dto.MyEnrollmentItemDTO;
import com.example.spring.dto.ProgressUpdateRequestDTO;
import com.example.spring.entity.*;
import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.repository.*;
import com.example.spring.security.RoleGuard;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public EnrollmentResponseDTO enroll(Long currentUserId, Long lectureId) {
        User student = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        RoleGuard.requireUser(student);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        if (lecture.getStatus() != LectureStatus.APPROVED) {
            throw new BadRequestException("승인된 강의만 수강할 수 있습니다.");
        }

        Enrollment enrollment = enrollmentRepository
                .findByUser_UserIdAndLecture_LectureId(currentUserId, lectureId)
                .orElseGet(() -> enrollmentRepository.save(Enrollment.create(student, lecture)));

        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponseDTO updateProgress(Long currentUserId, Long lectureId, ProgressUpdateRequestDTO req) {
        Enrollment enrollment = enrollmentRepository
                .findByUser_UserIdAndLecture_LectureId(currentUserId, lectureId)
                .orElseThrow(() -> new NotFoundException("수강 정보가 없습니다. 먼저 수강 신청하세요."));

        int lastWatched = req.getLastWatchedTime();
        int totalDuration = req.getTotalDuration();

        // progress 결정
        int progress;
        if (req.getProgress() != null) {
            progress = clamp(req.getProgress(), 0, 100);
        } else {
            progress = (int) Math.floor((lastWatched * 100.0) / totalDuration);
            progress = clamp(progress, 0, 100);
        }

        enrollment.updateVideoProgress(progress, lastWatched, totalDuration);
        return toResponse(enrollment);
    }

    // 내 수강 목록(진도 상태 포함)
    @Transactional
    public Page<MyEnrollmentItemDTO> listMyEnrollments(Long currentUserId, Pageable pageable) {
        // (선택) 학생만 허용하고 싶으면 아래 주석 해제
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        RoleGuard.requireUser(me);

        return enrollmentRepository.findAllByUser_UserId(currentUserId, pageable)
                .map(e -> new MyEnrollmentItemDTO(
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
                ));
    }

    /**
     * 교수: 특정 강의 수강생 목록(+상태 필터)
     * - PROFESSOR: 본인 강의만 조회 가능
     * - ADMIN: 모든 강의 조회 가능
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

        // 교수면 본인 강의만
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

    private int resolveProgress(ProgressUpdateRequestDTO req, int lastWatched, int totalDuration) {
        // 1) progress 명시 우선
        if (req.getProgress() != null) {
            return clamp(req.getProgress(), 0, 100);
        }

        // 2) 없으면 시간 기반 계산
        if (totalDuration <= 0) {
            throw new BadRequestException("progress 또는 (currentTime/duration) 또는 (lastWatchedTime/totalDuration) 중 하나가 필요합니다.");
        }

        int pct = (int) Math.floor((lastWatched * 100.0) / totalDuration);
        return clamp(pct, 0, 100);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private Integer firstNonNull(Integer a, Integer b) {
        return a != null ? a : b;
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
