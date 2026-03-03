package services;

import java.sql.Connection;

import daos.ProfileDao;
import models.Profile;




public class ProfileService {
    private static ProfileService instance;
    private ProfileDao profileDao;
    
    public ProfileService(Connection connection) {
        this.profileDao = new ProfileDao(connection);
    }
    
    public static ProfileService getInstance(Connection connection) {
        if (instance == null) {
            instance = new ProfileService(connection);
        }
        return instance;
    }
    
    public void createProfile(int userId, String name) throws Exception {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        
        Profile profile = new Profile(userId, name);
        profileDao.createProfile(profile);
    }
    
    public Profile getProfile(int userId) throws Exception {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        return profileDao.getProfileById(userId);
    }
    
    public void updateProfile(int userId, String name, String bio, String profilePicture) throws Exception {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        
        Profile profile = profileDao.getProfileById(userId);
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found");
        }
        
        if (name != null && !name.trim().isEmpty()) {
            profile.setName(name);
        }
        if (bio != null) {
            profile.setBio(bio);
        }
        if (profilePicture != null) {
            profile.setProfilePicture(profilePicture);
        }
        
        profileDao.updateProfile(profile);
    }
    
    public void deleteProfile(int userId) throws Exception {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        profileDao.deleteProfile(userId);
    }
}