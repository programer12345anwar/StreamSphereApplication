package com.youtube.video_service.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.youtube.video_service.util.ApiTemplate;
import com.youtube.video_service.util.ApiTemplateImpl;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;

@org.springframework.context.annotation.Configuration
public class AppConfig {

    @Value("${IMAGE_URL:http://localhost:8080}")
    String urlEndPoint;
    @Value("${IMAGE_PRIVATE_KEY:}")
    String privateKey;
    @Value("${IMAGE_PUBLIC_KEY:}")
    String publicKey;

    @Bean
    public ImageKit getImageKit() throws Exception{
         ImageKit imageKit = ImageKit.getInstance();
         Configuration configuration = new Configuration(publicKey, privateKey, urlEndPoint);//this will helps to connect with imagekit server
         imageKit.setConfig(configuration);
         return imageKit;
    }
    

    @Bean
    public ModelMapper getMapper(){
        //ModelMapper is a library that helps to map one object to another (e.g., Entity → DTO, DTO → Entity).
        //Instead of writing boilerplate setters, ModelMapper copies fields automatically.
        //So with this bean, whenever you @Autowired ModelMapper, you’ll get the same object.
        return new ModelMapper();
    }

    @Bean
    public ApiTemplate getApiTemplate(){
        return new ApiTemplateImpl();
    }
    

    @Bean
    public RestTemplate getRestTemplate(){
        //RestTemplate → makes API calls to other services.
        return new RestTemplate();
    }
        

}
