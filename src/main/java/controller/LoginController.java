package controller;

import java.io.IOException;
import java.sql.Connection;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.AuthService;

public class LoginController {
    
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    
    private AuthService authService;

    @FXML
    public void initialize() {
       authService = AuthService.getInstance();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        try {
            User user = authService.login(email, password);
            loadMainScreen();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    @FXML
    private void handleRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));

            Stage stage = (Stage) registerLink.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            // Show error alert
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Navigation Error");
            alert.setContentText("Could not load registration page: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void loadMainScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/main_feed.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}