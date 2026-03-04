package daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import models.Notification;
import models.Notification.NotificationType;

public class NotificationDao {
  private Connection connection;

  public NotificationDao(Connection connection) {
    this.connection = connection;
  }

  public Notification createNotification(Notification notification) throws SQLException {
    String sql = "INSERT INTO notifications (user_id, type, reference_id, message) VALUES (?, ?, ?, ?)";

    try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setInt(1, notification.getUserId());
      stmt.setString(2, notification.getType().name()); // Store enum as string
      stmt.setInt(3, notification.getReferenceId());
      stmt.setString(4, notification.getMessage());

      int affectedRows = stmt.executeUpdate();

      if (affectedRows == 0) {
        throw new SQLException("Creating notification failed, no rows affected.");
      }

      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          notification.setNotificationId(generatedKeys.getInt(1));
        } else {
          throw new SQLException("Creating notification failed, no ID obtained.");
        }
      }
    } catch (SQLException e) {
      throw new SQLException("Error creating notification", e);
    }
    return notification;
  }

  /**
     * Fetch latest notifications for a user.
     * Logic: We usually want the newest first.
     */
    public List<Notification> getByUserId(int userId , int limit) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error fetching notifications for userId: " + userId, e);
        }
        return notifications;
    }

    /**
     * Updates the status of a notification to 'read'.
     * Logic: Returns boolean to indicate if the record actually existed.
     */
    public boolean markAsRead(int notificationId) throws SQLException {
      String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";

      try (PreparedStatement stmt = connection.prepareStatement(sql)) {

        stmt.setInt(1, notificationId);
        int affectedRows = stmt.executeUpdate(); // Use executeUpdate for UPDATE/INSERT/DELETE
        if (affectedRows == 0) {
          throw new SQLException("Marking notification as read failed, no rows affected.");
        }
        return affectedRows > 0;
      } catch (SQLException e) {
        throw new SQLException("Error marking notification as read", e);
      }
    }

    public boolean deleteNotification(int notificationId) throws SQLException {
      String sql = "DELETE FROM notifications WHERE notification_id = ?";
      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setInt(1, notificationId);
        int affectedRows = stmt.executeUpdate();
        if (affectedRows == 0) {
          throw new SQLException("Deleting notification failed, no rows affected.");
        }
        return affectedRows > 0;

      } catch (SQLException e) {
        throw new SQLException("Error deleting notification", e);
      }
    }


    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
      Notification notification = new Notification();

      String typeStr = rs.getString("type");

      if (typeStr != null) {
        try {
          // Convert to uppercase to match Java standards and map it
          notification.setType(NotificationType.valueOf(typeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
          //  Handle unexpected values
          System.err.println("Unknown notification type: " + typeStr);

        }
      }

        notification.setNotificationId(rs.getInt("notification_id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setReferenceId(rs.getInt("reference_id"));
        notification.setMessage(rs.getString("message"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return notification;
    }


}
