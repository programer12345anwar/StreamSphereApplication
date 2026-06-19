package com.youtube.central.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.youtube.central.dto.UserCredentialDTO;
import com.youtube.central.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.youtube.central.dto.NotificationMessage;
import com.youtube.central.models.AppUser;
import com.youtube.central.repository.AppUserRepo;

@Service
public class UserService {

    @Autowired
    @org.springframework.context.annotation.Lazy
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    private AppUserRepo appUserRepo;
    private RabbitMqService rabbitMqService;
    @Autowired
    public UserService(AppUserRepo appUserRepo, RabbitMqService rabbitMqService) {
        this.appUserRepo = appUserRepo;
        this.rabbitMqService = rabbitMqService;
    }


    public AppUser getUserByEmail(String email) {
        return appUserRepo.findByEmail(email);
    }

    public String userLogin(UserCredentialDTO credential){
        String email=credential.getEmail();
        AppUser user=this.getUserByEmail(email);
        if (user == null) {
            return "User Not Found";
        }
        if(passwordEncoder.matches(credential.getPassword(), user.getPassword())){
            return user.getEmail();

        }
        return "Incorrect Password";
    }
    public void registerUser(AppUser user){
        // Call repository layer to save the user
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        appUserRepo.save(user);
        // Insert user registration message payload inside rabbit mq queue.
        NotificationMessage message = new NotificationMessage();
        message.setEmail(user.getEmail());
        message.setType("user_registration");
        message.setName(user.getName());
        rabbitMqService.insertMessageToQueue(message);
    }

     public AppUser getUserById(UUID userId){
        return appUserRepo.findById(userId).orElse(null);
    }
    
}
