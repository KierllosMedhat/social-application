package services;

import java.sql.Connection;

import daos.UserDao;
import models.User;
import utils.PasswordUtil;

public class AuthService {
    private static AuthService instance;
    private UserDao userDao;
    private User currentUser; // Singleton pattern for session management
    private Connection connection;

    private AuthService(Connection connection) {
        this.userDao = new UserDao(connection);
        this.connection = connection;
    }
    
    public static AuthService getInstance(Connection connection) {
        if (instance == null) {
            instance = new AuthService(connection);
        }
        return instance;
    }
    
    public User register(String email, String password, String name) throws Exception {
        // Validation
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        
        // Check if email exists
        if (userDao.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Create user
        String hashedPassword = PasswordUtil.hashPassword(password);
        User user = new User(email, hashedPassword);
        user = userDao.createUser(user);
        
        // Create profile
        ProfileService profileService = new ProfileService(connection);
        profileService.createProfile(user.getId(), name);
        
        return user;
    }
    
    public User login(String email, String password) throws Exception {
        User user = userDao.getUserByEmail(email);
        
        if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        this.currentUser = user;
        return user;
    }
    
    public void logout() {
        this.currentUser = null;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}