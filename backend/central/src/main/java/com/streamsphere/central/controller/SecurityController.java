package com.streamsphere.central.controller;

import com.streamsphere.central.dto.response.IsValidDTO;
import com.streamsphere.central.dto.response.SecurityCredential;
import com.streamsphere.central.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/central/security")//to make it not secure add it to jwtconfig file
public class SecurityController {
    @Autowired
    JwtUtil jwtUtil;
    @GetMapping("/validate-token/{token}")
    public IsValidDTO isValidToken(@PathVariable(name = "token") String token){
        boolean isValid=jwtUtil.isValidToken(token);
        IsValidDTO validDTO=new IsValidDTO();
        validDTO.setSuccess(isValid);
        return validDTO;
    }

    @GetMapping("/get-credential/{token}")
    public SecurityCredential getCredentialsFromToken(@PathVariable(name = "token") String token){
        SecurityCredential credential=new SecurityCredential();
        credential.setCredential(jwtUtil.decryptToken(token));
        return credential;
    }
}

