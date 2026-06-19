package com.youtube.notification_api.configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.docs.external-url:https://docs.streamsphere.com/notification}")
    private String externalDocsUrl;

    @Bean
    public OpenAPI notificationServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("🔔 Notification Service API")
                        .description("The **Notification Service** is responsible for handling **real-time alerts, " +
                                "emails, and event-driven notifications** within the YouTube Clone platform. \n\n" +
                                "It listens to messages from **RabbitMQ** and ensures users are notified about " +
                                "important activities such as uploads, subscriptions, and other events.")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Md Anwar Alam")
                                .email("mdanwar40212@gmail.com")
                                .url("https://github.com/programer12345anwar"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("📖 Notification Service Wiki & Extended Documentation")
                        .url(externalDocsUrl));
    }
}
