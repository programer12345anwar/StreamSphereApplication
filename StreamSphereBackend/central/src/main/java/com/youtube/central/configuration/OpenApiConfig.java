package com.youtube.central.configuration;

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

    @Value("${app.docs.external-url:https://docs.streamsphere.com/central}")
    private String externalDocsUrl;

    @Bean
    public OpenAPI centralApiService() {
        return new OpenAPI()
                .info(new Info()
                        .title("🛡️ Central API Service")
                        .description("This is the **Core Central Service** of the YouTube Clone project. " +
                                "It manages **authentication, authorization, user registration, JWT tokens, " +
                                "and service-to-service security**. \n\n" +
                                "All other microservices (Video, Notification, etc.) communicate securely " +
                                "through this Central Service.")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Md Anwar Alam")
                                .email("mdanwar40212@gmail.com")
                                .url("https://github.com/programer12345anwar"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("📖 Central API Service Wiki & Documentation")
                        .url(externalDocsUrl));
    }
}
