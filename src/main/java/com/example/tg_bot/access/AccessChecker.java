package com.example.tg_bot.access;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AccessChecker {

    private final Set<Long> userIds;

    public AccessChecker(@Value("${bot.list.permitted.users-id}") Set<Long> userIds) {
        this.userIds = userIds;
    }

    public boolean isAllowed(Long userId) {
        return userIds.contains(userId);
    }
}
