package com.example.spring.Controller;

import com.example.spring.DTO.EnrollmentRequestDTO;
import com.example.spring.Entity.Enrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/lecture")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PostMapping("/enroll")
    public void enrollment(@RequestBody EnrollmentRequestDTO requestDTO, @AuthenticationPrincipal User user) {
        enrollmentService.enroll(user, requestDTO.getLectureId());
    }


}
