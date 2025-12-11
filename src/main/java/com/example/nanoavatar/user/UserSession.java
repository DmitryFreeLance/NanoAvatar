package com.example.nanoavatar.user;

public class UserSession {
    private String currentNodeId;
    private String selectedFilterId;
    private SessionState state = SessionState.BROWSING;
    private Integer pendingTopupAmount;

    public UserSession(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public String getSelectedFilterId() { return selectedFilterId; }
    public void setSelectedFilterId(String selectedFilterId) { this.selectedFilterId = selectedFilterId; }

    public SessionState getState() { return state; }
    public void setState(SessionState state) { this.state = state; }

    public Integer getPendingTopupAmount() { return pendingTopupAmount; }
    public void setPendingTopupAmount(Integer pendingTopupAmount) { this.pendingTopupAmount = pendingTopupAmount; }
}