package com.example.spring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailVerificationMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = frontendBaseUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromEmail);
        message.setSubject("[LMS] 이메일 인증 안내");
        message.setText(
                "안녕하세요.\n\n" +
                        "아래 링크를 클릭해서 이메일 인증을 완료해주세요.\n\n" +
                        verifyUrl + "\n\n" +
                        "링크 유효시간은 30분입니다."
        );

        mailSender.send(message);
    }
}