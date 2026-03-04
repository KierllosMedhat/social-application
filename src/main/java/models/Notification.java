package models;

import java.time.LocalDateTime;

public class Notification {
    public enum Type {
        LIKE, COMMENT, FRIEND_REQUEST, FRIEND_ACCEPTED
    }

    private int notificationId;
    private int userId;
    private int referenceId; // postId, commentId, or friendId based on type
    private Type type;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;

    // for frameworks
    public Notification() {
    }

    // Basic constructor for creating new notifications (without notificationId and createdAt)
    public Notification(int userId, int referenceId, Type type, String message) {
        // Basic Validation: Don't allow a notification without a user
        if (userId <= 0)
            throw new IllegalArgumentException("User ID must be valid.");

        this.userId = userId;
        this.referenceId = referenceId;
        this.type = type;
        this.message = message;

        // System-managed defaults
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor for fetching from Database (with all fields)
    public Notification(int notificationId, int userId, int referenceId, Type type, String message, boolean isRead,
            LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.referenceId = referenceId;
        this.type = type;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(int referenceId) {
        this.referenceId = referenceId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }



}
