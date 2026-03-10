package com.milkteabot.service;

import com.milkteabot.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionService {

    private static final int SESSION_TIMEOUT_MINUTES = 10;

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getSession(Long chatId) {
        UserSession session = sessions.computeIfAbsent(chatId, id -> new UserSession());
        session.touch();
        return session;
    }

    public void resetSession(Long chatId) {
        sessions.put(chatId, new UserSession());
    }

    public UserSession.State getState(Long chatId) {
        return getSession(chatId).getState();
    }

    public void setState(Long chatId, UserSession.State state) {
        getSession(chatId).setState(state);
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minus(SESSION_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().getLastActivity().isBefore(cutoff));
        int removed = before - sessions.size();
        if (removed > 0) {
            log.info("Cleaned up {} expired sessions", removed);
        }
    }
}
