package com.example.spring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailVerificationMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromEmail);
        message.setSubject("[LMS] 이메일 인증번호 안내");
        message.setText(
                "안녕하세요.\n\n" +
                        "이메일 인증번호는 아래와 같습니다.\n\n" +
                        code + "\n\n" +
                        "인증번호 유효시간은 5분입니다."
        );

        mailSender.send(message);
    }
}