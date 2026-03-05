package services;

import java.sql.Connection;

import daos.PostDao;
import models.Post;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PostService {
    private PostDao postDao;
    
    public PostService(Connection connection) {
        this.postDao = new PostDao(connection);
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
}