package com.youtube.central.security;

import com.youtube.central.models.AppUser;
import com.youtube.central.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.util.Date;

@Component
public class JwtUtil {
    @Autowired
    UserService userService;
    @Value("${central.security.secret.key}")
    String secretKey;

    Long expirationTime = 86400000L; // 24 hours in milliseconds

    //Create JWT Token on the basis of credential
    // in jwt token we are encrypting some information(we are going to encrypt user credentials)
    // credential = mdanwar40212@gmail.com:123456
    // We got the credentials generateToken function we will encrypt credentials with the help of algorithm and secret key

    public String generateToken(String credentials){
        return Jwts.builder().setSubject(credentials)
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }




    public String decryptToken(String token){
        String credentials =  Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return credentials;
    }



    public boolean isValidToken(String token){
        try {
            String email = this.decryptToken(token);
            return email != null && userService.getUserByEmail(email) != null;
        } catch(Exception e) {
            return false;
        }
    }

    /* public boolean isValidTokenOld(String token){
        // decrypt token
        String credentials = this.decryptToken(token);
        String email = credentials.split(":")[0];
        String password = credentials.split(":")[1];
        // validate this token is correct or not
        AppUser user  = userService.getUserByEmail(email);
        if(user == null){
            return false;
        }
        if(user.getPassword().equals(password)){
            return true;
        }
        return false;
    }*/

}
