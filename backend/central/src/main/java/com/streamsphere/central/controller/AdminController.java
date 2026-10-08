package com.streamsphere.central.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.streamsphere.central.dto.response.AdminChannelDTO;
import com.streamsphere.central.dto.response.AdminUserDTO;
import com.streamsphere.central.dto.response.AdminVideoDTO;
import com.streamsphere.central.service.AdminService;

/**
 * Admin APIs — server-side authorization is mandatory: every handler
 * requires a valid JWT whose claims carry role=ADMIN. The admin frontend
 * is NOT a security boundary.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> analytics() {
        return adminService.getAnalytics();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminUserDTO> users(@RequestParam(name="page", defaultValue="0") int page,
                                    @RequestParam(name="size", defaultValue="20") int size) {
        return adminService.getUsers(page, size);
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserDTO setRole(@PathVariable(name="id") UUID id,
                                @RequestParam(name="role") String role) {
        return adminService.setUserRole(id, role);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable(name="id") UUID id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/videos")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminVideoDTO> videos(@RequestParam(name="page", defaultValue="0") int page,
                                      @RequestParam(name="size", defaultValue="20") int size) {
        return adminService.getVideos(page, size);
    }

    @DeleteMapping("/videos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVideo(@PathVariable(name="id") String id) {
        adminService.deleteVideo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/channels")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminChannelDTO> channels(@RequestParam(name="page", defaultValue="0") int page,
                                          @RequestParam(name="size", defaultValue="20") int size) {
        return adminService.getChannels(page, size);
    }
}

