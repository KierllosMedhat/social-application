package daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import models.Profile;
import models.User;

public class UserDao {
    private Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (email, password_hash) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getPasswordHash());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
        return user;
    }

    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT u.*, p.profile_id, p.name, p.bio, p.profile_picture " +
                "FROM users u LEFT JOIN profiles p ON u.user_id = p.user_id " +
                "WHERE u.email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT u.*, p.profile_id, p.name, p.bio, p.profile_picture " +
                "FROM users u LEFT JOIN profiles p ON u.user_id = p.user_id " +
                "WHERE u.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        // Map profile if exists
        String name = rs.getString("name");
        if (name != null) {
            Profile profile = new Profile(user.getId(), name);
            profile.setBio(rs.getString("bio"));
            profile.setProfilePicture(rs.getString("profile_picture"));
            user.setProfile(profile);
        }

        return user;
    }

    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET email = ?, password_hash = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getPasswordHash());
            stmt.setInt(3, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    /**
     * Search users by name or email. Returns users whose name or email contains the
     * query.
     * Excludes the searching user from results.
     */
    public List<User> searchUsers(String query, int excludeUserId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*, p.profile_id, p.name, p.bio, p.profile_picture " +
                "FROM users u LEFT JOIN profiles p ON u.user_id = p.user_id " +
                "WHERE u.user_id != ? AND (u.email LIKE ? OR p.name LIKE ?) " +
                "ORDER BY p.name ASC LIMIT 20";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            stmt.setInt(1, excludeUserId);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }
}
