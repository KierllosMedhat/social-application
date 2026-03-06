package controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import models.Profile;
import models.User;
import services.AuthService;
import services.FriendshipService;
import daos.FriendshipDao;
import daos.UserDao;
import utils.DatabaseConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FriendsController {

  // Sidebar
  @FXML
  private Circle sidebarAvatar;
  @FXML
  private Label sidebarNameLabel;
  @FXML
  private Label sidebarEmailLabel;
  @FXML
  private Button feedBtn;
  @FXML
  private Button profileBtn;
  @FXML
  private Button friendsBtn;
  @FXML
  private Button notificationsBtn;
  @FXML
  private Button logoutBtn;

  // Search
  @FXML
  private TextField searchField;

  // Tabs
  @FXML
  private Button allFriendsTab;
  @FXML
  private Button requestsTab;
  @FXML
  private Button findFriendsTab;

  // All Friends Section
  @FXML
  private VBox allFriendsSection;
  @FXML
  private Label friendsCountLabel;
  @FXML
  private FlowPane friendsGrid;
  @FXML
  private VBox noFriendsMessage;
  @FXML
  private HBox loadingFriends;

  // Requests Section
  @FXML
  private VBox requestsSection;
  @FXML
  private VBox requestsContainer;
  @FXML
  private VBox noRequestsMessage;

  // Find Friends Section
  @FXML
  private VBox findFriendsSection;
  @FXML
  private TextField findSearchField;
  @FXML
  private FlowPane suggestionsGrid;
  @FXML
  private VBox noSuggestionsMessage;

  // Services
  private AuthService authService;
  private FriendshipService friendshipService;
  private FriendshipDao friendshipDao;
  private UserDao userDao;
  private User currentUser;
  private Connection connection;

  @FXML
  public void initialize() {
    authService = AuthService.getInstance();
    currentUser = authService.getCurrentUser();

    try {
      connection = DatabaseConnection.getConnection();
      friendshipDao = new FriendshipDao(connection);
      friendshipService = new FriendshipService(friendshipDao);
      userDao = new UserDao(connection);
    } catch (SQLException e) {
      showError("Failed to connect to database");
      e.printStackTrace();
    }

    setupUI();
    loadFriends();
    setupSearchListener();
  }

  private void setupUI() {
    sidebarAvatar.setFill(Color.web("#4f46e5"));
    setActiveNavButton(friendsBtn);
    loadUserInfo();
  }

  private void loadUserInfo() {
    if (currentUser != null) {
      Profile profile = currentUser.getProfile();
      sidebarNameLabel.setText(profile != null ? profile.getName() : "User");
      sidebarEmailLabel.setText(currentUser.getEmail());
    }
  }

  private void setupSearchListener() {
    searchField.textProperty().addListener((obs, oldVal, newVal) -> {
      filterFriends(newVal);
    });
  }

  private void filterFriends(String query) {
    if (query == null || query.trim().isEmpty()) {
      loadFriends();
      return;
    }
    // Filter displayed friend cards by name
    String lowerQuery = query.toLowerCase();
    for (javafx.scene.Node node : friendsGrid.getChildren()) {
      if (node instanceof VBox card) {
        boolean match = false;
        for (javafx.scene.Node child : card.getChildren()) {
          if (child instanceof Label label && label.getStyleClass().contains("friend-name")) {
            match = label.getText().toLowerCase().contains(lowerQuery);
            break;
          }
        }
        card.setVisible(match);
        card.setManaged(match);
      }
    }
  }

  private void loadFriends() {
    showLoading(true);

    Task<List<Integer>> loadTask = new Task<>() {
      @Override
      protected List<Integer> call() {
        return friendshipService.getFriends(currentUser.getId());
      }
    };

    loadTask.setOnSucceeded(event -> {
      showLoading(false);
      List<Integer> friendIds = loadTask.getValue();
      displayFriends(friendIds);
    });

    loadTask.setOnFailed(event -> {
      showLoading(false);
      showError("Failed to load friends");
      loadTask.getException().printStackTrace();
    });

    new Thread(loadTask).start();
  }

  private void displayFriends(List<Integer> friendIds) {
    friendsGrid.getChildren().clear();

    if (friendIds == null || friendIds.isEmpty()) {
      noFriendsMessage.setVisible(true);
      noFriendsMessage.setManaged(true);
      friendsCountLabel.setText("0 Friends");
      return;
    }

    noFriendsMessage.setVisible(false);
    noFriendsMessage.setManaged(false);
    friendsCountLabel.setText(friendIds.size() + " Friend" + (friendIds.size() > 1 ? "s" : ""));

    for (Integer friendId : friendIds) {
      VBox friendCard = createFriendCard(friendId);
      friendsGrid.getChildren().add(friendCard);
    }
  }

  private VBox createFriendCard(int userId) {
    VBox card = new VBox(12);
    card.getStyleClass().add("friend-card");
    card.setPadding(new Insets(20));
    card.setAlignment(Pos.CENTER);
    card.setPrefWidth(200);

    // Avatar
    Circle avatar = new Circle(35);
    avatar.setFill(Color.web("#64748b"));
    avatar.getStyleClass().add("friend-avatar");

    // Name (fetch from database if needed)
    Label nameLabel = new Label("User #" + userId);
    nameLabel.getStyleClass().add("friend-name");

    // Try to load actual user info
    try {
      User friend = userDao.getUserById(userId);
      if (friend != null && friend.getProfile() != null) {
        nameLabel.setText(friend.getProfile().getName());
      }
    } catch (SQLException e) {
      // Keep default name
    }

    // Action buttons
    HBox actions = new HBox(8);
    actions.setAlignment(Pos.CENTER);

    Button removeBtn = new Button("Remove");
    removeBtn.getStyleClass().add("danger-button-small");
    removeBtn.setOnAction(e -> handleRemoveFriend(userId));

    actions.getChildren().add(removeBtn);

    card.getChildren().addAll(avatar, nameLabel, actions);
    return card;
  }

  private void handleRemoveFriend(int friendId) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Remove Friend");
    confirm.setHeaderText("Remove this friend?");
    confirm.setContentText("You can always add them back later.");

    confirm.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        boolean removed = friendshipService.removeFriend(currentUser.getId(), friendId);
        if (removed) {
          loadFriends();
          showSuccess("Friend removed");
        } else {
          showError("Could not remove friend");
        }
      }
    });
  }

  // ==================== TABS ====================

  @FXML
  private void handleAllFriendsTab() {
    setActiveTab(allFriendsTab);
    showSection(allFriendsSection);
    loadFriends();
  }

  @FXML
  private void handleRequestsTab() {
    setActiveTab(requestsTab);
    showSection(requestsSection);
    loadFriendRequests();
  }

  @FXML
  private void handleFindFriendsTab() {
    setActiveTab(findFriendsTab);
    showSection(findFriendsSection);
  }

  private void setActiveTab(Button activeTab) {
    allFriendsTab.getStyleClass().remove("tab-button-active");
    requestsTab.getStyleClass().remove("tab-button-active");
    findFriendsTab.getStyleClass().remove("tab-button-active");

    if (!activeTab.getStyleClass().contains("tab-button-active")) {
      activeTab.getStyleClass().add("tab-button-active");
    }
  }

  private void showSection(VBox section) {
    allFriendsSection.setVisible(false);
    allFriendsSection.setManaged(false);
    requestsSection.setVisible(false);
    requestsSection.setManaged(false);
    findFriendsSection.setVisible(false);
    findFriendsSection.setManaged(false);

    section.setVisible(true);
    section.setManaged(true);
  }

  // ==================== FRIEND REQUESTS ====================

  private void loadFriendRequests() {
    requestsContainer.getChildren().clear();

    Task<List<Integer>> loadTask = new Task<>() {
      @Override
      protected List<Integer> call() {
        return friendshipService.getPendingRequests(currentUser.getId());
      }
    };

    loadTask.setOnSucceeded(event -> {
      List<Integer> requesterIds = loadTask.getValue();
      if (requesterIds == null || requesterIds.isEmpty()) {
        noRequestsMessage.setVisible(true);
        noRequestsMessage.setManaged(true);
        return;
      }
      noRequestsMessage.setVisible(false);
      noRequestsMessage.setManaged(false);

      for (Integer requesterId : requesterIds) {
        HBox requestCard = createRequestCard(requesterId);
        requestsContainer.getChildren().add(requestCard);
      }
    });

    loadTask.setOnFailed(event -> {
      showError("Failed to load friend requests");
    });

    new Thread(loadTask).start();
  }

  private HBox createRequestCard(int requesterId) {
    HBox card = new HBox(12);
    card.setAlignment(Pos.CENTER_LEFT);
    card.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 12; " +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 4, 0, 0, 2);");

    Circle avatar = new Circle(24);
    avatar.setFill(Color.web("#64748b"));

    String name = "User #" + requesterId;
    try {
      User requester = userDao.getUserById(requesterId);
      if (requester != null && requester.getProfile() != null) {
        name = requester.getProfile().getName();
      }
    } catch (SQLException ignored) {
    }

    VBox info = new VBox(2);
    Label nameLabel = new Label(name);
    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    Label subLabel = new Label("Sent you a friend request");
    subLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
    info.getChildren().addAll(nameLabel, subLabel);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Button acceptBtn = new Button("Accept");
    acceptBtn
        .setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
    acceptBtn.setOnAction(e -> {
      boolean accepted = friendshipService.acceptFriendRequest(requesterId, currentUser.getId());
      if (accepted) {
        card.setVisible(false);
        card.setManaged(false);
        showSuccess("Friend request accepted!");
      } else {
        showError("Failed to accept request");
      }
    });

    Button rejectBtn = new Button("Reject");
    rejectBtn
        .setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
    rejectBtn.setOnAction(e -> {
      boolean rejected = friendshipService.rejectFriendRequest(requesterId, currentUser.getId());
      if (rejected) {
        card.setVisible(false);
        card.setManaged(false);
      } else {
        showError("Failed to reject request");
      }
    });

    card.getChildren().addAll(avatar, info, spacer, acceptBtn, rejectBtn);
    return card;
  }

  // ==================== FIND FRIENDS ====================

  @FXML
  private void handleSearchUsers() {
    String query = findSearchField.getText().trim();
    if (query.isEmpty()) {
      showError("Please enter a search term");
      return;
    }

    suggestionsGrid.getChildren().clear();
    Label searching = new Label("Searching...");
    searching.getStyleClass().add("hint-text");
    suggestionsGrid.getChildren().add(searching);

    Task<List<User>> searchTask = new Task<>() {
      @Override
      protected List<User> call() throws SQLException {
        return userDao.searchUsers(query, currentUser.getId());
      }
    };

    searchTask.setOnSucceeded(event -> {
      suggestionsGrid.getChildren().clear();
      List<User> results = searchTask.getValue();
      if (results == null || results.isEmpty()) {
        Label noResults = new Label("No users found matching \"" + query + "\"");
        noResults.getStyleClass().add("hint-text");
        suggestionsGrid.getChildren().add(noResults);
        return;
      }

      for (User user : results) {
        HBox card = createSearchResultCard(user);
        suggestionsGrid.getChildren().add(card);
      }
    });

    searchTask.setOnFailed(event -> {
      suggestionsGrid.getChildren().clear();
      showError("Search failed. Please try again.");
    });

    new Thread(searchTask).start();
  }

  private HBox createSearchResultCard(User user) {
    HBox card = new HBox(12);
    card.setAlignment(Pos.CENTER_LEFT);
    card.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 12; " +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 4, 0, 0, 2);");

    Circle avatar = new Circle(24);
    avatar.setFill(Color.web("#64748b"));

    String name = "Unknown";
    String email = user.getEmail();
    if (user.getProfile() != null && user.getProfile().getName() != null) {
      name = user.getProfile().getName();
    }

    VBox info = new VBox(2);
    Label nameLabel = new Label(name);
    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    Label emailLabel = new Label(email);
    emailLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
    info.getChildren().addAll(nameLabel, emailLabel);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Button actionBtn = new Button("Add Friend");
    actionBtn
        .setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");

    // Check existing relationship status
    try {
      if (friendshipService.areFriends(currentUser.getId(), user.getId())) {
        actionBtn.setText("Friends ✓");
        actionBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 8;");
        actionBtn.setDisable(true);
      } else if (friendshipService.friendshipExists(currentUser.getId(), user.getId())) {
        actionBtn.setText("Request Sent");
        actionBtn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-background-radius: 8;");
        actionBtn.setDisable(true);
      }
    } catch (Exception ignored) {
    }

    final int targetUserId = user.getId();
    actionBtn.setOnAction(e -> {
      boolean sent = friendshipService.sendFriendRequest(currentUser.getId(), targetUserId);
      if (sent) {
        actionBtn.setText("Request Sent");
        actionBtn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-background-radius: 8;");
        actionBtn.setDisable(true);
        showSuccess("Friend request sent!");
      } else {
        showError("Could not send friend request");
      }
    });

    card.getChildren().addAll(avatar, info, spacer, actionBtn);
    return card;
  }

  // ==================== NAVIGATION ====================

  @FXML
  private void handleFeedNav() {
    navigateTo("/fxml/main_feed.fxml");
  }

  @FXML
  private void handleProfileNav() {
    navigateTo("/fxml/profile.fxml");
  }

  @FXML
  private void handleLogout() {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Logout");
    confirm.setHeaderText("Are you sure you want to logout?");

    confirm.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        authService.logout();
        navigateTo("/fxml/login.fxml");
      }
    });
  }

  private void navigateTo(String fxmlPath) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
      Stage stage = (Stage) friendsBtn.getScene().getWindow();
      Scene scene = new Scene(root, 1200, 800);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
      stage.setScene(scene);
    } catch (IOException e) {
      showError("Could not navigate to page");
      e.printStackTrace();
    }
  }

  private void setActiveNavButton(Button activeBtn) {
    feedBtn.getStyleClass().remove("nav-button-active");
    profileBtn.getStyleClass().remove("nav-button-active");
    friendsBtn.getStyleClass().remove("nav-button-active");
    notificationsBtn.getStyleClass().remove("nav-button-active");

    if (!activeBtn.getStyleClass().contains("nav-button-active")) {
      activeBtn.getStyleClass().add("nav-button-active");
    }
  }

  // ==================== UI HELPERS ====================

  private void showLoading(boolean show) {
    Platform.runLater(() -> {
      loadingFriends.setVisible(show);
      loadingFriends.setManaged(show);
    });
  }

  private void showError(String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Error");
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
    });
  }

  private void showSuccess(String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Success");
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
    });
  }

  private void showInfo(String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Info");
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
    });
  }
}
