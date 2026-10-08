package com.streamsphere.central.controller;

import com.streamsphere.central.dto.request.UserCredentialDTO;
import com.streamsphere.central.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.service.UserService;

@RestController
@RequestMapping("/api/central/user")
public class UserController {

    @Autowired
    JwtUtil jwtUtil;

    private UserService userService;

    @Autowired //constructor based autowiring
    public UserController(UserService userService) {
        this.userService=userService;
    }

    @PostMapping("/register")
    public ResponseEntity<java.util.Map<String, String>> registerUser(@Valid @RequestBody AppUser user){
        userService.registerUser(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(java.util.Map.of("token", token, "role", user.getRole() == null ? "USER" : user.getRole()));
    }




    @PostMapping("/login")
    public ResponseEntity<?> loginUserPost(@Valid @RequestBody UserCredentialDTO credential){
        return loginUserInternal(credential);
    }

    private ResponseEntity<?> loginUserInternal(UserCredentialDTO credential) {
        String resp= userService.userLogin(credential);
        if(resp.equals("Incorrect Password") || resp.equals("User Not Found")){
            return new ResponseEntity<>(java.util.Map.of("error", resp), HttpStatus.UNAUTHORIZED);
        }
        AppUser user = userService.getUserByEmail(credential.getEmail());
        String role = user != null && user.getRole() != null ? user.getRole() : "USER";
        String token=jwtUtil.generateToken(resp, role);
        return new ResponseEntity<>(java.util.Map.of("token", token, "role", role),HttpStatus.OK);
    }



}

