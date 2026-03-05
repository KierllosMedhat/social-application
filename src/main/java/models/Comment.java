package models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Comment {
    int commentId;
    int postId;
    int userId;
    String content;
    LocalDateTime createdAt;

    //  No-Args for Frameworks (Hibernate/Jackson)
    public Comment() {
    }

    // Constructor for creating new comments (without commentId and createdAt)
    public Comment(int postId, int userId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor for fetching from Database
    public Comment(int commentId, int postId, int userId, String content, LocalDateTime createdAt) {
        this.commentId = commentId;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


}
