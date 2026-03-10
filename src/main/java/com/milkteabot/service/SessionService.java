package com.milkteabot.service;

import com.milkteabot.model.UserSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getSession(Long chatId) {
        return sessions.computeIfAbsent(chatId, id -> new UserSession());
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
}
