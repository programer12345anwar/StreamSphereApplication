package com.streamsphere.notification.service;

import static org.junit.jupiter.api.Assertions.*;

import com.streamsphere.notification.event.model.NotificationMessage;

import org.junit.jupiter.api.Test;

class DuplicateMessageGuardTest {

    private NotificationMessage msg(String type, String email, String name) {
        NotificationMessage m = new NotificationMessage();
        m.setType(type);
        m.setEmail(email);
        m.setName(name);
        return m;
    }

    @Test
    void firstMessageIsNotDuplicate() {
        DuplicateMessageGuard guard = new DuplicateMessageGuard();
        assertFalse(guard.isDuplicate(msg("user_registration", "a@b.com", "A")));
    }

    @Test
    void sameMessageIsDuplicate() {
        DuplicateMessageGuard guard = new DuplicateMessageGuard();
        guard.isDuplicate(msg("user_registration", "a@b.com", "A"));
        assertTrue(guard.isDuplicate(msg("user_registration", "a@b.com", "A")));
    }

    @Test
    void differentMessagesAreNotDuplicates() {
        DuplicateMessageGuard guard = new DuplicateMessageGuard();
        guard.isDuplicate(msg("user_registration", "a@b.com", "A"));
        assertFalse(guard.isDuplicate(msg("new_video", "a@b.com", "A")));
        assertFalse(guard.isDuplicate(msg("user_registration", "x@y.com", "A")));
        assertFalse(guard.isDuplicate(msg("user_registration", "a@b.com", "B")));
    }
}

