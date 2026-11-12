package com.example.tg_bot.access;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Проверяет право Telegram-пользователя обращаться к боту по настроенному списку ID.
 * Это правило доступа к командам и кнопкам; авторизация Google настраивается отдельно.
 */
@Component
public class UserAccessChecker {

    private final Set<Long> userIds;

    public UserAccessChecker(@Value("${bot.list.permitted.users-id}") Set<Long> userIds) {
        this.userIds = userIds;
    }

    /**
     * Проверяет принадлежность пользователя разрешённому набору.
     *
     * @param userId Telegram ID отправителя, полученный из обновления
     * @return {@code true}, если ID присутствует в настройке разрешённых пользователей
     */
    public boolean isAllowed(Long userId) {
        return userIds.contains(userId);
    }
}
