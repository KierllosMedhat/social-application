package daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


import models.Comment;

public class CommentDao {
  private Connection connection;

  public CommentDao(Connection connection) {
    this.connection = connection;
  }

  public Comment addComment(Comment comment) throws SQLException {
    String sql = "INSERT INTO comments (post_id, user_id, content) VALUES (?, ? , ?)";
    try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, comment.getPostId());
      stmt.setInt(2, comment.getUserId());
      stmt.setString(3, comment.getContent());

      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating comment failed, no rows affected.");
      }

      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          comment.setCommentId(generatedKeys.getInt(1));
        } else {
          throw new SQLException("Creating comment failed, no ID obtained.");
        }
      }
    }
    return comment;
  }

  public List<Comment> getCommentsByPostId(int postId) throws SQLException {
    List<Comment> comments = new ArrayList<>();
    String sql = "SELECT * FROM comments WHERE post_id = ? ORDER BY created_at ASC";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, postId);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          comments.add(mapRowToComment(rs));
        }
      }
    } catch (SQLException e) {
      throw new SQLException("Error fetching comments for postId: " + postId, e);
    }
    return comments;
  }

  boolean updateComment(Comment comment) throws SQLException {
    String sql = "UPDATE comments SET content = ? WHERE comment_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, comment.getContent());
      stmt.setInt(2, comment.getCommentId());
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Updating comment failed, no rows affected.");
      }
      return affectedRows > 0;
    } catch (SQLException e) {
      throw new SQLException("Error updating comment", e);
    }
  }

  public boolean deleteComment(int commentId) throws SQLException {
    String sql = "DELETE FROM comments WHERE comment_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, commentId);
      int affectedRows = stmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Deleting comment failed, no rows affected.");
      }
      return affectedRows > 0;
    } catch (SQLException e) {
      throw new SQLException("Error deleting comment", e);
    }
  }

  /**
   * Helper method to map a single row to a Comment object.
   * Following the DRY principle (Don't Repeat Yourself).
   */
  private Comment mapRowToComment(ResultSet rs) throws SQLException {
    Comment comment = new Comment();
    comment.setCommentId(rs.getInt("comment_id"));
    comment.setPostId(rs.getInt("post_id"));
    comment.setUserId(rs.getInt("user_id"));
    comment.setContent(rs.getString("content"));
    comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    return comment;
  }


}
