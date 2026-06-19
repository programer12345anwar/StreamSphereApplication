package com.youtube.central.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.youtube.central.models.AppUser;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, UUID> {
   public AppUser findByEmail(String email); //hibernate will automatically implement this method to find the user by email

}
