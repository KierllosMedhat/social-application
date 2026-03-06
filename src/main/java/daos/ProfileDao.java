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

public class ProfileDao {
    private Connection connection;

    public ProfileDao(Connection connection) {
        this.connection = connection;
    }

    public void createProfile(Profile profile) throws SQLException {
        String sql = "INSERT INTO profiles (user_id, name, profile_picture, updated_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profile.getUserId());
            stmt.setString(2, profile.getName());
            stmt.setString(3, profile.getProfilePicture());
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
        }
    }

    public Profile getProfileById(int userId) throws SQLException {
        String sql = "SELECT * FROM profiles WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToProfile(rs);
            }
        }
        return null;
    }

    public List<Profile> getAllProfiles() throws SQLException {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM profiles";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                profiles.add(mapRowToProfile(rs));
            }
        }
        return profiles;
    }

    public void updateProfile(Profile profile) throws SQLException {
        String sql = "UPDATE profiles SET name = ?, bio = ?, profile_picture = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, profile.getName());
            stmt.setString(2, profile.getBio());
            stmt.setString(3, profile.getProfilePicture());
            stmt.setInt(4, profile.getUserId());
            stmt.executeUpdate();
        }
    }

    public void deleteProfile(int userId) throws SQLException {
        String sql = "DELETE FROM profiles WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private Profile mapRowToProfile(ResultSet rs) throws SQLException {
        Profile profile = new Profile();
        profile.setUserId(rs.getInt("user_id"));
        profile.setName(rs.getString("name"));
        profile.setBio(rs.getString("bio"));
        profile.setProfilePicture(rs.getString("profile_picture"));
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            profile.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return profile;
    }
}
