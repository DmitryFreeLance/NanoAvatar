package com.example.nanoavatar.user;

import java.util.HashSet;
import java.util.Set;

/**
 * Хранение состояния диалога в памяти.
 *
 * currentNodeId       — где находимся в меню настроек
 * activeOptionIds     — множество включённых опций (галочки)
 * state               — вспомогательное состояние (пока нужно только для пополнения)
 * pendingTopupAmount  — сумма пополнения, которую пользователь ввёл
 */
public class UserSession {
    private String currentNodeId;
    private SessionState state = SessionState.BROWSING;
    private Integer pendingTopupAmount;

    // мультивыбор опций
    private final Set<String> activeOptionIds = new HashSet<>();

    public UserSession(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public SessionState getState() { return state; }
    public void setState(SessionState state) { this.state = state; }

    public Integer getPendingTopupAmount() { return pendingTopupAmount; }
    public void setPendingTopupAmount(Integer pendingTopupAmount) { this.pendingTopupAmount = pendingTopupAmount; }

    public Set<String> getActiveOptionIds() {
        return activeOptionIds;
    }
}