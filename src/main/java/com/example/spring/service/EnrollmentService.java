package com.example.spring.service;

import com.example.spring.entity.Enrollment;
import com.example.spring.entity.EnrollmentStatus;
import com.example.spring.entity.Lecture;
import com.example.spring.entity.User;
import com.example.spring.repository.EnrollmentRepository;
import com.example.spring.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;

    public void enroll(User user, Long lectureId){
        if (enrollmentRepository.existsByUserUserIdAndLectureId(user.getUserId(), lectureId)){
            throw new IllegalStateException("이미 수강 중인 강의 입니다.");
        }

        Lecture lecture = lectureRepository.findById(lectureId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 강의 입니다."));

        if(!Boolean.TRUE.equals(lecture.getApproved())){
            throw new IllegalStateException("아직 승인되지 않은 강의 입니다.");
        }

        Enrollment enrollment = Enrollment.create(user, lecture);
        enrollmentRepository.save(enrollment);
    }
}
