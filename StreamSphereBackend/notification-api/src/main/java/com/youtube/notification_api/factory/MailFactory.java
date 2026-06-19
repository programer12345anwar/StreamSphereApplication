package com.youtube.notification_api.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;

public class MailFactory{

    @Autowired
    JavaMailSender javaMailSender;
    public MimeMessage createMimeMailMessage() throws Exception{
        return javaMailSender.createMimeMessage();
    }

    public MimeMessageHelper createMimeMailMessageHelper(MimeMessage mimeMessage){
        return new MimeMessageHelper(mimeMessage);
    }
}
