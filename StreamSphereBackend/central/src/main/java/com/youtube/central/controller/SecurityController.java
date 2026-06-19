package com.youtube.central.controller;

import com.youtube.central.dto.IsValidDTO;
import com.youtube.central.dto.SecurityCredential;
import com.youtube.central.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/central/security")//to make it not secure add it to jwtconfig file
public class SecurityController {
    @Autowired
    JwtUtil jwtUtil;
    @GetMapping("/validate-token/{token}")
    public IsValidDTO isValidToken(@PathVariable String token){
        boolean isValid=jwtUtil.isValidToken(token);
        IsValidDTO validDTO=new IsValidDTO();
        validDTO.setSuccess(isValid);
        return validDTO;
    }

    @GetMapping("/get-credential/{token}")
    public SecurityCredential getCredentialsFromToken(@PathVariable String token){
        SecurityCredential credential=new SecurityCredential();
        credential.setCredential(jwtUtil.decryptToken(token));
        return credential;
    }
}
