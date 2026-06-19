package com.youtube.notification_api.service;

import com.youtube.notification_api.dto.NotificationMessage;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class CommonUserService {

    @Autowired
    TemplateEngine templateEngine; /* A template engine helps to generate dynamic content by replacing placeholders in templates with runtime data. In Spring Boot, Thymeleaf is commonly used for emails and web pages. */

    @Autowired
    MailService mailService;

    @Autowired
    JavaMailSender javaMailSender; /* JavaMailSender is a predefined Spring interface for sending emails. Spring Boot auto-configures its implementation (JavaMailSenderImpl) when spring-boot-starter-mail and mail properties are set.*/

    @Value("${youtube.platform.name}")
    String platformName;

    @Value("${app.platform.base-url}")
    private String platformBaseUrl;

    @Value("${app.platform.logo-url}")
    private String platformLogoUrl;

    @Value("${spring.mail.host}")
    private String mailHost;

    public void sendUserRegistrationEmail(NotificationMessage notificationMessage) throws Exception{
        // This function will send registration email to the user
        // So, email is of type html So we need to get html template
        // Before getting html template we need to create variables inside html template

        log.info("Inside Common user service: " + mailHost);
        Context context = new Context(); /* Context in Thymeleaf holds the variables (key-value pairs) that are injected into a template so that dynamic content (like user name, order ID, platform name) can be rendered in emails or web pages.*/
        /* Why is it created?

        To inject dynamic values into a template.

        Example: Suppose your email template (welcome-email.html) has this:

        <p>Hello, <span th:text="${userName}"></span>!</p>
        <p>Welcome to <span th:text="${platformName}"></span>.</p>

        The Context provides userName and platformName, so Thymeleaf replaces them when rendering:*/

        context.setVariable("userName", notificationMessage.getName());
        addCommonBrandingVariables(context);
        context.setVariable("loginUrl", buildUrl("/login"));
        // We need to get our html template inform of string and all the variables popluated inside html template
        String htmlEmailContent = templateEngine.process("user-registration-email", context);
        // templateEnine.process will insert values for all the variables defined inside html template
        // I need to set this html content inside MimeMessage
        //log.info("Email template loaded: " + htmlEmailContent);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
        mimeMessageHelper.setTo(notificationMessage.getEmail());
        mimeMessageHelper.setSubject("Welcome to Youtube!");
        mimeMessageHelper.setText(htmlEmailContent, true);
        log.info("Mimemessage created calling mail service to send mail");
        mailService.sendEmail(mimeMessage);
    }

    public void sendCreateChannelNotification(NotificationMessage message) throws Exception{
        log.info("CommonUserService:  Inside sendCreateChannelNotification method");
        // We need to send html kind of email that our channel got created over our portal
        Context context = new Context();//Context is a class from thymeleaf, it is just like a container of variables 
        context.setVariable("userName", message.getName());
        addCommonBrandingVariables(context);
        context.setVariable("dashboardUrl", buildUrl("/dashboard"));
        String htmlEmailContent = templateEngine.process("create-channel-email", context);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage(); /* MimeMessage is a standard JavaMail class for advanced emails (HTML, attachments). Spring’s JavaMailSender.createMimeMessage() is just a convenience method that creates and configures it using the SMTP settings from application.properties.*/
        /* 🔹 Predefined or Custom?
        MimeMessage → Predefined JavaMail class (comes from Jakarta/JavaMail library).
        createMimeMessage() → A helper method in Spring’s JavaMailSender that instantiates a MimeMessage for you (instead of you creating new MimeMessage(session) manually).*/
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
        mimeMessageHelper.setTo(message.getEmail());
        mimeMessageHelper.setSubject("Your Channel is Live!");
        mimeMessageHelper.setText(htmlEmailContent, true);
        log.info("Mimemessage created calling mail service to send mail");
        mailService.sendEmail(mimeMessage);
    }

    public void sendSubscriberAddedMail(NotificationMessage message) throws Exception{
        Context context = new Context();
        context.setVariable("channelName", message.getName());
        addCommonBrandingVariables(context);
        context.setVariable("dashboardUrl", buildUrl("/dashboard"));

        String htmlTemplate = templateEngine.process("subscriber-added", context);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setTo(message.getEmail());
        helper.setText(htmlTemplate, true);
        helper.setSubject("New Subscriber Alert!");
        mailService.sendEmail(mimeMessage);
    }


    public void notifyNewVideoUploadedToSubscriber(NotificationMessage notificationMessage) throws Exception{
        String subscriberEmail = notificationMessage.getEmail();

        Context context = new Context();
        context.setVariable("subscriberName", subscriberEmail);
        context.setVariable("videoLink", notificationMessage.getName());
        context.setVariable("watchUrl", notificationMessage.getName());
        addCommonBrandingVariables(context);

        String htmlTemplate = templateEngine.process("new-video-notification", context);
        log.info(htmlTemplate);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setTo(subscriberEmail);
        helper.setText(htmlTemplate, true);
        helper.setSubject("New Video Alert !!");
        mailService.sendEmail(mimeMessage);
    }

    private void addCommonBrandingVariables(Context context) {
        context.setVariable("platformName", platformName);
        context.setVariable("platformLogoUrl", platformLogoUrl);
    }

    private String buildUrl(String suffix) {
        if (platformBaseUrl.endsWith("/") && suffix.startsWith("/")) {
            return platformBaseUrl.substring(0, platformBaseUrl.length() - 1) + suffix;
        }
        if (!platformBaseUrl.endsWith("/") && !suffix.startsWith("/")) {
            return platformBaseUrl + "/" + suffix;
        }
        return platformBaseUrl + suffix;
    }
}
 
