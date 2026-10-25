package com.streamsphere.central.controller;

import com.streamsphere.central.dto.request.UserCredentialDTO;
import com.streamsphere.central.dto.response.UserProfileDTO;
import com.streamsphere.central.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
        AppUser registeredUser = userService.getUserByEmail(user.getEmail());
        String role = registeredUser.getRole() == null ? "USER" : registeredUser.getRole();
        String token = jwtUtil.generateToken(registeredUser.getEmail(), role);
        return ResponseEntity.ok(java.util.Map.of(
                "token", token,
                "role", role,
                "id", registeredUser.getId().toString(),
                "name", registeredUser.getName(),
                "email", registeredUser.getEmail()));
    }

    @GetMapping("/me")
    public UserProfileDTO getCurrentUser(Authentication authentication) {
        AppUser user = userService.getUserByEmail(authentication.getName());
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Authenticated user was not found.");
        }
        return UserProfileDTO.from(user);
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
        return new ResponseEntity<>(java.util.Map.of(
                "token", token,
                "role", role,
                "id", user.getId().toString(),
                "name", user.getName(),
                "email", user.getEmail()), HttpStatus.OK);
    }



}
