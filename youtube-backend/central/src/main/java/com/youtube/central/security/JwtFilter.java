package com.youtube.central.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

//By extending class oncePerRequestFilter class we will be able to write our own Filter
@Component
public class JwtFilter extends OncePerRequestFilter {
    //“OncePerRequestFilter is a base class in Spring that ensures my custom filter runs only once for each HTTP request. I usually extend it when writing filters for authentication, logging, or request validation.”
    @Autowired
    JwtUtil jwtUtil;

    // FrontEnd axios.post(url).body().header({Authorization: Bearer}
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // bearer 2u92dwdwkdebdjebd
        // 01234567(index, because actual token is JWT TOKEN but not Bearer ,it is just a best practices)
        // If token is invalid then we will not set the authentication and we will directly call doFilter
        // If token is valid then we will set the authentication and after that we will call do filter method and it will see authentication is set then it will pass the request
        String bToken = request.getHeader("Authorization");
        if(bToken != null && bToken.startsWith("Bearer")){
            String token = bToken.substring(7);
            // Validate token -> JWT UTIL
            boolean isValid = jwtUtil.isValidToken(token);
            if(isValid == false){
                // If token was invalid then i am not setting any authentication and returning filterchain from here itself
                filterChain.doFilter(request, response);
                return;
            }
            // If token is valid then we are setting authentication for filter chain
            String credentials = jwtUtil.decryptToken(token);
            UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(credentials,null,Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        filterChain.doFilter(request, response);
    }
}
