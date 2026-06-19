package com.youtube.video_service.security;

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
    @Autowired
    JwtUtil jwtUtil;

    // FrontEnd axios.post(url).body().header({Authorization: Bearer}
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String bToken = request.getHeader("Authorization");
        if(bToken != null && bToken.startsWith("Bearer")){
            String token = bToken.substring(7);
            // Validate token -> JWT UTIL
            boolean isValid = jwtUtil.isValidToken(token);
            if(isValid == false){
                // If token was invalid then i am not setting any authentication and returning filter chain from here itself
                filterChain.doFilter(request, response);
                return;
            }
            // If token is valid then we are setting authentication for filter chain
            String credentials = jwtUtil.decryptToken(token);
            UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(credentials,null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        filterChain.doFilter(request, response);
    }
}

