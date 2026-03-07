package services;

import java.sql.Connection;

import daos.CommentDao;
import daos.PostDao;
import daos.UserDao;
import models.Comment;
import models.Post;
import models.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PostService {
    private PostDao postDao;
    private CommentDao commentDao;
    private UserDao userDao;

    public PostService(Connection connection) {
        this.postDao = new PostDao(connection);
        this.commentDao = new CommentDao(connection);
        this.userDao = new UserDao(connection);
    }

    public void createPost(int userId, String content, String imagePath,
            Post.Privacy privacy) throws SQLException {
        Post post = new Post(userId, content, privacy);
        post.setImagePath(imagePath);
        postDao.createPost(post);
    }

    // Algorithm: Pagination with efficient data loading
    public List<Post> getNewsFeed(int userId, int page, int pageSize) throws SQLException {
        // Get all visible posts for user
        List<Post> allPosts = postDao.getPostsForNewsFeed(userId);

        // Algorithm: Sort by timestamp (newest first) using Comparator
        Collections.sort(allPosts, new Comparator<Post>() {
            @Override
            public int compare(Post p1, Post p2) {
                return p2.getCreatedAt().compareTo(p1.getCreatedAt());
            }
        });

        // Algorithm: Pagination logic
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allPosts.size());

        if (startIndex >= allPosts.size()) {
            return new ArrayList<>(); // Empty list if page is beyond data
        }

        return allPosts.subList(startIndex, endIndex);
    }

    // Algorithm: Search posts using linear search with keyword matching
    public List<Post> searchPosts(String keyword, int userId) throws SQLException {
        List<Post> allPosts = postDao.getPostsForNewsFeed(userId);
        List<Post> results = new ArrayList<>();

        String lowerKeyword = keyword.toLowerCase();

        for (Post post : allPosts) {
            if (post.getContent() != null &&
                    post.getContent().toLowerCase().contains(lowerKeyword)) {
                results.add(post);
            }
        }

        return results;
    }

    public boolean deletePost(int postId, int userId) throws SQLException {
        // Verify ownership
        Post post = postDao.getPostById(postId);
        if (post == null || post.getUserId() != userId) {
            return false;
        }
        postDao.deletePost(postId);
        return true;
    }

    public boolean updatePost(int postId, int userId, String newContent) throws SQLException {
        Post post = postDao.getPostById(postId);
        if (post == null || post.getUserId() != userId) {
            return false;
        }
        post.setContent(newContent);
        postDao.updatePost(post);
        return true;
    }

    public List<Post> getUserPosts(int userId) throws SQLException {
        return postDao.getPostsByUserId(userId);
    }

    // ==================== LIKES ====================

    /**
     * Toggle like on a post. Returns true if now liked, false if unliked.
     */
    public boolean toggleLike(int postId, int userId) throws SQLException {
        return postDao.toggleLike(postId, userId);
    }

    public boolean hasUserLiked(int postId, int userId) throws SQLException {
        return postDao.hasUserLiked(postId, userId);
    }

    // ==================== COMMENTS ====================

    public Comment addComment(int postId, int userId, String content) throws SQLException {
        Comment comment = new Comment(postId, userId, content);
        return commentDao.addComment(comment);
    }

    public List<Comment> getComments(int postId) throws SQLException {
        return commentDao.getCommentsByPostId(postId);
    }

    public boolean deleteComment(int commentId) throws SQLException {
        return commentDao.deleteComment(commentId);
    }

    public boolean updateComment(int commentId, String newContent) throws SQLException {
        Comment comment = new Comment();
        comment.setCommentId(commentId);
        comment.setContent(newContent);
        return commentDao.updateComment(comment);
    }

    public int getCommentCount(int postId) throws SQLException {
        return postDao.getCommentCount(postId);
    }

    // ==================== USER INFO ====================

    public String getAuthorName(int userId) throws SQLException {
        User user = userDao.getUserById(userId);
        if (user != null && user.getProfile() != null) {
            return user.getProfile().getName();
        }
        return "User #" + userId;
    }
}
