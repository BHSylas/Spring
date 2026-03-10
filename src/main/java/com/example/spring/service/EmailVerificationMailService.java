package com.example.spring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailService {

    private final JavaMailSender mailSender;

    public EmailVerificationMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[LMS] 이메일 인증번호 안내");
        message.setText("인증번호는 [" + code + "] 입니다.\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }

    public void sendPasswordResetCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[LMS] 비밀번호 재설정 인증번호 안내");
        message.setText("비밀번호 재설정 인증번호는 [" + code + "] 입니다.\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }

    public void sendFoundEmailNotice(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[LMS] 가입 계정 안내");
        message.setText("요청하신 계정 찾기 결과,\n해당 이메일 주소가 LMS 가입 계정으로 확인되었습니다.\n이 메일을 받으셨다면 해당 이메일로 로그인해주세요.");
        mailSender.send(message);
    }
}