package com.streamsphere.central.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.streamsphere.central.dto.response.AdminChannelDTO;
import com.streamsphere.central.dto.response.AdminUserDTO;
import com.streamsphere.central.dto.response.AdminVideoDTO;
import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.entity.Channel;
import com.streamsphere.central.entity.Video;
import com.streamsphere.central.repository.AppUserRepo;
import com.streamsphere.central.repository.ChannelRepo;
import com.streamsphere.central.repository.VideoRepo;

@Service
public class AdminService {

    private final AppUserRepo appUserRepo;
    private final VideoRepo videoRepo;
    private final ChannelRepo channelRepo;

    public AdminService(AppUserRepo appUserRepo, VideoRepo videoRepo, ChannelRepo channelRepo) {
        this.appUserRepo = appUserRepo;
        this.videoRepo = videoRepo;
        this.channelRepo = channelRepo;
    }

    /** Global totals for the admin dashboard (live values, not hard-coded). */
    public Map<String, Long> getAnalytics() {
        long totalViews = videoRepo.findAll().stream().mapToLong(Video::getViews).sum();
        return Map.of(
                "totalUsers", appUserRepo.count(),
                "totalVideos", videoRepo.count(),
                "totalChannels", channelRepo.count(),
                "totalViews", totalViews
        );
    }

    public Page<AdminUserDTO> getUsers(int page, int size) {
        Page<AppUser> users = appUserRepo.findAll(PageRequest.of(page, size));
        return users.map(AdminService::toAdminUserDTO);
    }

    public AdminUserDTO setUserRole(UUID userId, String role) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (!"USER".equals(role) && !"CREATOR".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Unsupported role: " + role);
        }
        user.setRole(role);
        return toAdminUserDTO(appUserRepo.save(user));
    }

    public void deleteUser(UUID userId) {
        appUserRepo.deleteById(userId);
    }

    public Page<AdminVideoDTO> getVideos(int page, int size) {
        return videoRepo.findAll(PageRequest.of(page, size)).map(AdminService::toAdminVideoDTO);
    }

    public void deleteVideo(String videoId) {
        videoRepo.deleteById(videoId);
    }

    public Page<AdminChannelDTO> getChannels(int page, int size) {
        return channelRepo.findAll(PageRequest.of(page, size)).map(AdminService::toAdminChannelDTO);
    }

    private static AdminUserDTO toAdminUserDTO(AppUser u) {
        return new AdminUserDTO(u.getId(), u.getName(), u.getEmail(), u.getPhoneNumber(), u.getRole(), u.getCreatedAt());
    }

    private static AdminVideoDTO toAdminVideoDTO(Video v) {
        Channel channel = v.getChannel();
        return new AdminVideoDTO(
                v.getId(),
                v.getName(),
                v.getDescription(),
                v.getVideoLink(),
                v.getThumbnailLink(),
                v.getViews(),
                v.getUploadDateTime(),
                channel == null ? null : channel.getId(),
                channel == null ? null : channel.getName()
        );
    }

    private static AdminChannelDTO toAdminChannelDTO(Channel c) {
        AppUser owner = c.getUser();
        return new AdminChannelDTO(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getTotalSubs(),
                c.getTotalViews(),
                c.isMonetized(),
                owner == null ? null : owner.getEmail(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}

