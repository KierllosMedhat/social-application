package models;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Post {
    public enum Privacy {PUBLIC, FRIENDS, PRIVATE}

    private int postId;
    private int userId;
    private String content;
    private String imagePath;
    private Privacy privacy;
    private int likes;
    private List<Comment> comments;
    private LocalDateTime createdAt;
    private User auhor;

    public Post() {
        this.likes = 0;
        this.comments = new ArrayList<>();
        this.privacy = Privacy.PUBLIC;
    }

    public Post(int userId, String content, Privacy privacy) {
        this();
        this.userId = userId;
        this.content = content;
        this.privacy = privacy;
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Privacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy(Privacy privacy) {
        this.privacy = privacy;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getAuhor() {
        return auhor;
    }

    public void setAuhor(User auhor) {
        this.auhor = auhor;
    }
}