package com.example.spring.service;

import com.example.spring.entity.User;
import com.example.spring.dto.VideoProgressResponseDTO;
import com.example.spring.entity.Enrollment;
import com.example.spring.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgressService {
    private final EnrollmentRepository enrollmentRepository;

    public VideoProgressResponseDTO updateVideoProgress(
            User user,
            long lectureId,
            int currentTime,
            int duration
//            LocalDateTime clientLastAccessedAt
    ){
        Enrollment enrollment = enrollmentRepository.findByUserIdAndLectureId(user.getUserId(), lectureId)
                .orElseThrow(() -> new IllegalStateException("수강 정보 없음"));

        if (duration <=0 ){
            throw new IllegalArgumentException("영상 길이 오류");
        }

        if (currentTime <= enrollment.getLastWatchedTime()) {
            return new VideoProgressResponseDTO(enrollment.getStatus(), enrollment.getProgressRate());
        }

        int safeCurrentTime = Math.min(currentTime, duration);
        int progressRate  = (int) ((safeCurrentTime / (double) duration) * 100);


        enrollment.updateVideoProgress(
                progressRate,
                safeCurrentTime,
                duration
        );

        return new VideoProgressResponseDTO(enrollment.getStatus(),enrollment.getProgressRate());

    }
}
