package services;

import utils.DatabaseConnection;

import daos.ProfileDao;
import daos.UserDao;
import models.Profile;
import models.User;
import utils.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class AuthService {
    private static AuthService instance;
    private UserDao userDao;
    private ProfileDao profileDao;
    private User currentUser; // Singleton pattern for session management
    public AuthService(){

    }

    private AuthService(Connection connection) {
        this.userDao = new UserDao(connection);
        this.profileDao = new ProfileDao(connection);
    }
    
    public static AuthService getInstance() {
        if (instance == null) {
            try {
                Connection connection = DatabaseConnection.getConnection();
                return new AuthService(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }
    
    public User register(String email, String password, String name) throws Exception {
    // Validation (also done in controller for UX)
    if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
        throw new IllegalArgumentException("Invalid email format");
    }
    if (password == null || password.length() < 6) {
        throw new IllegalArgumentException("Password must be at least 6 characters");
    }
    if (!password.matches(".*[A-Z].*")) {
        throw new IllegalArgumentException("Password must contain uppercase letter");
    }
    if (!password.matches(".*[0-9].*")) {
        throw new IllegalArgumentException("Password must contain a number");
    }
    if (name == null || name.trim().isEmpty() || name.length() < 2) {
        throw new IllegalArgumentException("Name must be at least 2 characters");
    }
    
    // Check if email exists
    if (userDao.emailExists(email)) {
        throw new IllegalArgumentException("Email already registered");
    }
    
    // Create user with hashed password
    String hashedPassword = PasswordUtil.hashPassword(password);
    User user = new User(email, hashedPassword);
    user = userDao.createUser(user);
    
    // Create profile
    Profile profile = new Profile(user.getId(), name);
    profileDao.createProfile(profile);
    
    return user;
}

    public boolean validEmail(String email){
        try {
            return userDao.emailExists(email);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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