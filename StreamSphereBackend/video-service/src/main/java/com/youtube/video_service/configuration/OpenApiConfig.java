package com.youtube.video_service.configuration;

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

    @Value("${app.docs.external-url:https://docs.streamsphere.com/video}")
    private String externalDocsUrl;

    @Bean
    public OpenAPI videoServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎬 Video Service API")
                        .description("This is the REST API for Video Service in the YouTube clone project. " +
                                "It handles video upload, streaming, metadata, and integration with Central Service.")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Md Anwar Alam")
                                .email("mdanwar40212@gmail.com")
                                .url("https://github.com/programer12345anwar"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Video Service Wiki Documentation")
                        .url(externalDocsUrl));
    }
}

