package com.youtube.notification_api.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    JavaMailSender javaMailSender;

    public void sendEmail(MimeMessage message){

        javaMailSender.send(message);
        /*
        javaMailSender.send() takes your email (that you already prepared) and passes it to the mail server (like Gmail, Mailtrap, etc.).
        The mail server then delivers it to the recipient’s inbox.

        So basically:
        You prepare → send() hands it to server → Server delivers.
         */
    }
}

/*
What happens under the hood:

Spring takes your MimeMessage.

Uses the SMTP connection defined in application.properties (host, port, username, password).

Calls the JavaMail API’s Transport.send(mimeMessage) method.

The SMTP server accepts the message and delivers it to the recipient’s inbox.


🔹 Variants of send()

JavaMailSender supports multiple overloads:

send(MimeMessage mimeMessage) → send one prepared email.

send(MimeMessage... mimeMessages) → send multiple emails.

send(MimeMessagePreparator mimeMessagePreparator) → functional style (prepare + send in one go).

send(SimpleMailMessage simpleMessage) → for plain text emails without HTML/attachments.
 */