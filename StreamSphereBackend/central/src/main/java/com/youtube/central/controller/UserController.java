package com.youtube.central.controller;

import com.youtube.central.dto.UserCredentialDTO;
import com.youtube.central.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.youtube.central.models.AppUser;
import com.youtube.central.service.UserService;

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
    public ResponseEntity<java.util.Map<String, String>> registerUser(@RequestBody AppUser user){
        userService.registerUser(user);
        String credential=user.getEmail()+":"+user.getPassword();
        String token = jwtUtil.generateToken(credential);
        return ResponseEntity.ok(java.util.Map.of("token", token));
    }




    @PostMapping("/login")
    public ResponseEntity<?> loginUserPost(@RequestBody UserCredentialDTO credential){
        return loginUserInternal(credential);
    }

    private ResponseEntity<?> loginUserInternal(UserCredentialDTO credential) {
        String resp= userService.userLogin(credential);
        if(resp.equals("Incorrect Password") || resp.equals("User Not Found")){
            return new ResponseEntity<>(java.util.Map.of("error", resp), HttpStatus.UNAUTHORIZED);
        }
        String token=jwtUtil.generateToken(resp);
        return new ResponseEntity<>(java.util.Map.of("token", token),HttpStatus.OK);
    }



}
