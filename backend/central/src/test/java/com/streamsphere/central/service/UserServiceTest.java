package com.streamsphere.central.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.streamsphere.central.event.model.NotificationMessage;
import com.streamsphere.central.event.producer.RabbitMqService;
import com.streamsphere.central.dto.request.UserCredentialDTO;
import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.repository.AppUserRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepo appUserRepo;

    @Mock
    private RabbitMqService rabbitMqService;

    private UserService userService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepo, rabbitMqService);
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
    }

    private UserCredentialDTO credential(String email, String password) {
        UserCredentialDTO c = new UserCredentialDTO();
        c.setEmail(email);
        c.setPassword(password);
        return c;
    }

    @Test
    void loginReturnsEmailWhenPasswordMatches() {
        AppUser user = new AppUser();
        user.setEmail("a@b.com");
        user.setPassword(passwordEncoder.encode("secret123"));
        when(appUserRepo.findByEmail("a@b.com")).thenReturn(user);

        assertEquals("a@b.com", userService.userLogin(credential("a@b.com", "secret123")));
    }

    @Test
    void loginReturnsIncorrectPasswordWhenPasswordMismatches() {
        AppUser user = new AppUser();
        user.setEmail("a@b.com");
        user.setPassword(passwordEncoder.encode("secret123"));
        when(appUserRepo.findByEmail("a@b.com")).thenReturn(user);

        assertEquals("Incorrect Password", userService.userLogin(credential("a@b.com", "wrong")));
    }

    @Test
    void loginReturnsUserNotFoundWhenUnknownEmail() {
        when(appUserRepo.findByEmail("x@y.com")).thenReturn(null);
        assertEquals("User Not Found", userService.userLogin(credential("x@y.com", "secret123")));
    }

    @Test
    void registerHashesPasswordAndPublishesNotification() {
        AppUser user = new AppUser();
        user.setEmail("new@user.com");
        user.setName("New User");
        user.setPassword("plaintext1");

        userService.registerUser(user);

        assertNotEquals("plaintext1", user.getPassword());
        assertTrue(passwordEncoder.matches("plaintext1", user.getPassword()));
        assertNotNull(user.getCreatedAt());
        verify(appUserRepo).save(user);
        verify(rabbitMqService).insertMessageToQueue(any(NotificationMessage.class));
    }

    @Test
    void getUserByIdDelegatesToRepo() {
        UUID id = UUID.randomUUID();
        AppUser user = new AppUser();
        when(appUserRepo.findById(id)).thenReturn(java.util.Optional.of(user));
        assertSame(user, userService.getUserById(id));
        when(appUserRepo.findById(id)).thenReturn(java.util.Optional.empty());
        assertNull(userService.getUserById(id));
    }
}


