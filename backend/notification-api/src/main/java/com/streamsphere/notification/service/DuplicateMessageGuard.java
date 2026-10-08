package com.streamsphere.notification.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.streamsphere.notification.event.model.NotificationMessage;

/**
 * Best-effort in-memory idempotency guard: a broker redelivery (at-least-once)
 * must not trigger a second email. Bounded to avoid unbounded memory growth;
 * entries are pruned FIFO.
 */
@Component
public class DuplicateMessageGuard {

    private static final int MAX_ENTRIES = 1000;

    private final Map<String, Boolean> seen = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    public boolean isDuplicate(NotificationMessage message) {
        String key = message.getType() + "|" + message.getEmail() + "|" + message.getName();
        synchronized (seen) {
            if (seen.containsKey(key)) {
                return true;
            }
            seen.put(key, Boolean.TRUE);
            return false;
        }
    }
}

