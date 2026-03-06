package daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import models.Post;

public class PostDao {
    private Connection connection;

    public PostDao(Connection connection) {
        this.connection = connection;
    }

    public void createPost(Post post) throws SQLException {
        String sql = "INSERT INTO posts (user_id, content, image_path, privacy_level, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, post.getUserId());
            stmt.setString(2, post.getContent());
            stmt.setString(3, post.getImagePath());
            stmt.setString(4, post.getPrivacy() != null ? post.getPrivacy().name().toLowerCase() : "public");
            stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                post.setPostId(keys.getInt(1));
            }
        }
    }

    public Post getPostById(int id) throws SQLException {
        String sql = "SELECT * FROM posts WHERE post_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToPost(rs);
            }
        }
        return null;
    }

    public List<Post> getAllPosts() throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
        }
        return posts;
    }

    public List<Post> getPostsByUserId(int userId) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
        }
        return posts;
    }

    public List<Post> getPostsByPrivacy(int userId, Post.Privacy privacy) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts WHERE user_id = ? AND privacy_level = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, privacy.name().toLowerCase());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
        }
        return posts;
    }

    public List<Post> getPostsForNewsFeed(int userId) throws SQLException {
        List<Post> posts = new ArrayList<>();
        // Get posts from accepted friends + own posts
        // Friends can be either requester or addressee in the friendships table
        String sql = "SELECT * FROM posts WHERE user_id IN ("
                + "  SELECT addressee_id FROM friendships WHERE requester_id = ? AND status = 'accepted' "
                + "  UNION "
                + "  SELECT requester_id FROM friendships WHERE addressee_id = ? AND status = 'accepted'"
                + ") OR user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
        }
        return posts;
    }

    public void updatePost(Post post) throws SQLException {
        String sql = "UPDATE posts SET content = ? WHERE post_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, post.getContent());
            stmt.setInt(2, post.getPostId());
            stmt.executeUpdate();
        }
    }

    public void deletePost(int id) throws SQLException {
        String sql = "DELETE FROM posts WHERE post_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Toggle like: if user has liked the post, unlike it; otherwise like it.
     * Returns true if the post is now liked, false if unliked.
     */
    public boolean toggleLike(int postId, int userId) throws SQLException {
        if (hasUserLiked(postId, userId)) {
            // Unlike
            String sql = "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, postId);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }
            updateLikeCount(postId, -1);
            return false;
        } else {
            // Like
            String sql = "INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, postId);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }
            updateLikeCount(postId, 1);
            return true;
        }
    }

    public boolean hasUserLiked(int postId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private void updateLikeCount(int postId, int delta) throws SQLException {
        String sql = "UPDATE posts SET likes = likes + ? WHERE post_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, postId);
            stmt.executeUpdate();
        }
    }

    public int getCommentCount(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM comments WHERE post_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Post mapResultSetToPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setPostId(rs.getInt("post_id"));
        post.setUserId(rs.getInt("user_id"));
        post.setContent(rs.getString("content"));
        post.setImagePath(rs.getString("image_path"));
        post.setLikes(rs.getInt("likes"));

        String privacyStr = rs.getString("privacy_level");
        if (privacyStr != null) {
            post.setPrivacy(Post.Privacy.valueOf(privacyStr.toUpperCase()));
        }

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            post.setCreatedAt(ts.toLocalDateTime());
        }
        return post;
    }
}
