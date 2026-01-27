package com.example.spring.service;

import com.example.spring.dto.LectureEnterRequestDTO;
import com.example.spring.dto.LectureEnterResponseDTO;
import com.example.spring.entity.Enrollment;
import com.example.spring.entity.Lecture;
import com.example.spring.entity.User;
import com.example.spring.repository.EnrollmentRepository;
import com.example.spring.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureEnterService {
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LectureEnterResponseDTO enterLecture(User user, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 강의"));

        if(!Boolean.TRUE.equals(lecture.getApproved())){
            throw new IllegalStateException("아직 승인되지 않은 강의입니다.");
        }

        Enrollment enrollment  = enrollmentRepository.findByUserUserIdAndLectureId(user.getUserId(), lectureId)
                .orElseThrow(() -> new IllegalStateException("수강 신청이 필요합니다."));

        return new LectureEnterResponseDTO(
                lecture.getId(),
                lecture.getTitle(),
                enrollment.getStatus(),
                enrollment.getProgressRate(),
                enrollment.getLastWatchedTime(),
                enrollment.getTotalDuration()
        );
    }
}
