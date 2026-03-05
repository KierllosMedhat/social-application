package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import models.User;
import services.AuthService;
import services.ProfileService;

import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextArea bioField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginLink;
    @FXML private ProgressIndicator loadingIndicator;

    private AuthService authService;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();

        // Hide status labels initially
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        loadingIndicator.setVisible(false);

        // Add real-time validation listeners
        setupValidationListeners();
    }

    private void setupValidationListeners() {
        // Email validation on focus lost
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                validateEmail();
            }
        });

        // Password strength indicator
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkPasswordStrength(newVal);
        });

        // Confirm password match check
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(passwordField.getText())) {
                confirmPasswordField.setStyle("-fx-border-color: red;");
            } else {
                confirmPasswordField.setStyle("-fx-border-color: green;");
            }
        });
    }

    private boolean validateEmail() {
        String email = emailField.getText().trim();
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";

        if (email.isEmpty()) {
            showError("Email is required");
            return false;
        }

        if (!email.matches(emailRegex)) {
            showError("Please enter a valid email address");
            return false;
        }

        // Check if email already exists
        if (authService.validEmail(email)) {
            showError("This email is already registered");
            return false;
        }

        return true;
    }

    private boolean validateForm() {
        // Clear previous errors
        hideMessages();

        // Validate name
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Full name is required");
            return false;
        }
        if (name.length() < 2 || name.length() > 50) {
            showError("Name must be between 2 and 50 characters");
            return false;
        }

        // Validate email
        if (!validateEmail()) {
            return false;
        }

        // Validate password
        String password = passwordField.getText();
        if (password.isEmpty()) {
            showError("Password is required");
            return false;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            showError("Password must contain at least one uppercase letter");
            return false;
        }
        if (!password.matches(".*[0-9].*")) {
            showError("Password must contain at least one number");
            return false;
        }

        // Validate password confirmation
        String confirmPassword = confirmPasswordField.getText();
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void checkPasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= 6) strength++;
        if (password.length() >= 10) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*].*")) strength++;

        // Update UI based on strength (optional - add a progress bar or label in FXML)
        Color color;
        String text;

        switch (strength) {
            case 0:
            case 1:
                color = Color.RED;
                text = "Weak";
                break;
            case 2:
            case 3:
                color = Color.ORANGE;
                text = "Medium";
                break;
            case 4:
            case 5:
                color = Color.GREEN;
                text = "Strong";
                break;
            default:
                color = Color.GRAY;
                text = "";
        }

        // You can add a label in FXML to show this
        // passwordStrengthLabel.setText(text);
        // passwordStrengthLabel.setTextFill(color);
    }

    @FXML
    private void handleRegister() {
        if (!validateForm()) {
            return;
        }

        // Show loading state
        setLoading(true);

        // Run registration in background thread to avoid freezing UI
        new Thread(() -> {
            try {
                String name = nameField.getText().trim();
                String email = emailField.getText().trim();
                String password = passwordField.getText();

                // Register user
                authService.register(email, password, name);



                // Success - update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    showSuccess("Registration successful! Redirecting to login...");

                    // Redirect to login after 2 seconds
                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                            javafx.util.Duration.seconds(2)
                    );
                    delay.setOnFinished(event -> navigateToLogin());
                    delay.play();
                });

            } catch (IllegalArgumentException e) {
                javafx.application.Platform.runLater(() -> {
                    showError(e.getMessage());
                    setLoading(false);
                });
            } catch (SQLException e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Database error: " + e.getMessage());
                    setLoading(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("An unexpected error occurred: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleLoginRedirect() {
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) loginLink.getScene().getWindow();

            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Login - Social Media App");

        } catch (Exception e) {
            showError("Could not load login page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setLoading(boolean loading) {
        registerButton.setDisable(loading);
        loadingIndicator.setVisible(loading);
        nameField.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        bioField.setDisable(loading);
        loginLink.setDisable(loading);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setTextFill(Color.GREEN);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    @FXML
    private void clearForm() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        bioField.clear();
        hideMessages();

        // Reset styles
        confirmPasswordField.setStyle("");
    }
}