package com.streamsphere.notification.service;

import com.streamsphere.notification.event.model.NotificationMessage;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class CommonUserService {

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    MailService mailService;

    @Autowired
    JavaMailSender javaMailSender;

    @Value("${youtube.platform.name}")
    String platformName;

    @Value("${app.platform.base-url}")
    private String platformBaseUrl;

    @Value("${app.platform.logo-url}")
    private String platformLogoUrl;

    @Value("${spring.mail.host}")
    private String mailHost;

    public void sendUserRegistrationEmail(NotificationMessage notificationMessage) throws Exception{
        log.info("Inside Common user service: " + mailHost);
        Context context = new Context();
        context.setVariable("userName", notificationMessage.getName());
        addCommonBrandingVariables(context);
        context.setVariable("loginUrl", buildUrl("/login"));
        String htmlEmailContent = templateEngine.process("user-registration-email", context);
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
        Context context = new Context();
        context.setVariable("userName", message.getName());
        addCommonBrandingVariables(context);
        context.setVariable("dashboardUrl", buildUrl("/dashboard"));
        String htmlEmailContent = templateEngine.process("create-channel-email", context);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
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

    public void sendVideoLikedMail(NotificationMessage message) throws Exception {
        Context context = new Context();
        context.setVariable("videoName", message.getName());
        addCommonBrandingVariables(context);
        context.setVariable("dashboardUrl", buildUrl("/dashboard"));
        String htmlTemplate = templateEngine.process("video-liked", context);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setTo(message.getEmail());
        helper.setText(htmlTemplate, true);
        helper.setSubject("Someone liked your video!");
        mailService.sendEmail(mimeMessage);
    }

    public void sendNewCommentMail(NotificationMessage message) throws Exception {
        Context context = new Context();
        context.setVariable("videoName", message.getName());
        addCommonBrandingVariables(context);
        context.setVariable("dashboardUrl", buildUrl("/dashboard"));
        String htmlTemplate = templateEngine.process("new-comment", context);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setTo(message.getEmail());
        helper.setText(htmlTemplate, true);
        helper.setSubject("New Comment Alert");
        mailService.sendEmail(mimeMessage);
    }

    public void sendVideoProcessedMail(NotificationMessage message) throws Exception {
        Context context = new Context();
        context.setVariable("videoName", message.getName());
        addCommonBrandingVariables(context);
        context.setVariable("watchUrl", buildUrl("/watch/" + message.getName())); // Name is placeholder for link
        String htmlTemplate = templateEngine.process("video-processed", context);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setTo(message.getEmail());
        helper.setText(htmlTemplate, true);
        helper.setSubject("Your video is ready!");
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
