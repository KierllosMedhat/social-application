package daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FriendshipDao {
    private Connection connection;

    public FriendshipDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Send a friend request (status = 'pending').
     */
    public boolean sendFriendRequest(int requesterId, int addresseeId) {
        String query = "INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?, ?, 'pending')";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, requesterId);
            stmt.setInt(2, addresseeId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Accept a pending friend request.
     */
    public boolean acceptFriendRequest(int requesterId, int addresseeId) {
        String query = "UPDATE friendships SET status = 'accepted' WHERE requester_id = ? AND addressee_id = ? AND status = 'pending'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, requesterId);
            stmt.setInt(2, addresseeId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reject a pending friend request.
     */
    public boolean rejectFriendRequest(int requesterId, int addresseeId) {
        String query = "UPDATE friendships SET status = 'rejected' WHERE requester_id = ? AND addressee_id = ? AND status = 'pending'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, requesterId);
            stmt.setInt(2, addresseeId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove an accepted friendship (both directions).
     */
    public boolean removeFriend(int userId, int friendId) {
        String query = "DELETE FROM friendships WHERE " +
                "((requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)) " +
                "AND status = 'accepted'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, friendId);
            stmt.setInt(3, friendId);
            stmt.setInt(4, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get list of accepted friend IDs for a user.
     */
    public List<Integer> getFriends(int userId) {
        List<Integer> friends = new ArrayList<>();
        String query = "SELECT addressee_id FROM friendships WHERE requester_id = ? AND status = 'accepted' " +
                "UNION " +
                "SELECT requester_id FROM friendships WHERE addressee_id = ? AND status = 'accepted'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                friends.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }

    /**
     * Get list of user IDs who sent pending requests to this user.
     */
    public List<Integer> getPendingRequests(int userId) {
        List<Integer> requesters = new ArrayList<>();
        String query = "SELECT requester_id FROM friendships WHERE addressee_id = ? AND status = 'pending'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requesters.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requesters;
    }

    /**
     * Check if two users are accepted friends.
     */
    public boolean areFriends(int userId, int friendId) {
        String query = "SELECT 1 FROM friendships WHERE " +
                "((requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)) " +
                "AND status = 'accepted' LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, friendId);
            stmt.setInt(3, friendId);
            stmt.setInt(4, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a friendship or request already exists between two users (any
     * status).
     */
    public boolean friendshipExists(int userId, int otherId) {
        String query = "SELECT 1 FROM friendships WHERE " +
                "((requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)) " +
                "LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, otherId);
            stmt.setInt(3, otherId);
            stmt.setInt(4, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cancel a pending friend request sent by the user.
     */
    public boolean cancelFriendRequest(int requesterId, int addresseeId) {
        String query = "DELETE FROM friendships WHERE requester_id = ? AND addressee_id = ? AND status = 'pending'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, requesterId);
            stmt.setInt(2, addresseeId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
