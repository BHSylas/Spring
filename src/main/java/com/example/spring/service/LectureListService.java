package com.example.spring.service;

import com.example.spring.dto.LectureListResponseDTO;
import com.example.spring.entity.Lecture;
import com.example.spring.entity.User;
import com.example.spring.repository.EnrollmentRepository;
import com.example.spring.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureListService {
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;

    public Page<LectureListResponseDTO> getLectureList(User user, String language, String enrolling
    ,int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        boolean isAllLanguage = "ALL".equalsIgnoreCase(language);

        List<Long> enrolledLectureIds = enrollmentRepository.findLectureIdsByUserId(user.getUserId());
        Page<Lecture> lectures;

        switch (enrolling.toUpperCase()) {
            case "TURE" -> {
                lectures = isAllLanguage ?
                        lectureRepository.findByIdIn(enrolledLectureIds, pageable) :
                        lectureRepository.findByIdInAndLanguage(
                        enrolledLectureIds, language, pageable);
            }
            case "FALSE" -> {
                lectures = isAllLanguage ?
                        lectureRepository.findByIdNotIn(enrolledLectureIds, pageable) :
                        lectureRepository.findByIdNotInAndLanguage(
                        enrolledLectureIds, language, pageable);
            }
            default -> { lectures = isAllLanguage ?
                    lectureRepository.findAll(pageable) :
                    lectureRepository.findByLanguage(language, pageable);
            }
        }

        return lectures.map(l -> new LectureListResponseDTO(l.getId(),
                l.getTitle(),
                l.getDescription(),
                l.getCountry(),
                l.getLanguage()));
    }
}
