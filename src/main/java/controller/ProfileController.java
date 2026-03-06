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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Comment;
import models.Post;
import models.Profile;
import models.User;
import services.AuthService;
import services.PostService;
import services.ProfileService;
import utils.DatabaseConnection;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProfileController {

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

  // Profile Header
  @FXML
  private Circle profileAvatar;
  @FXML
  private Button changeAvatarBtn;
  @FXML
  private Label profileNameLabel;
  @FXML
  private Label profileEmailLabel;
  @FXML
  private Label profileBioLabel;
  @FXML
  private Label joinedDateLabel;
  @FXML
  private Button editProfileBtn;

  // Stats
  @FXML
  private Label postsCount;
  @FXML
  private Label friendsCount;
  @FXML
  private Label likesCount;

  // Edit Form
  @FXML
  private VBox editProfileForm;
  @FXML
  private TextField editNameField;
  @FXML
  private TextArea editBioField;
  @FXML
  private Label editErrorLabel;

  // Tabs
  @FXML
  private Button postsTabBtn;
  @FXML
  private Button aboutTabBtn;
  @FXML
  private VBox postsSection;
  @FXML
  private VBox aboutSection;
  @FXML
  private VBox userPostsContainer;
  @FXML
  private VBox noPostsMessage;
  @FXML
  private HBox loadingPosts;

  // About Section
  @FXML
  private Label aboutEmailLabel;
  @FXML
  private Label aboutJoinedLabel;
  @FXML
  private Label aboutBioLabel;

  // Services
  private AuthService authService;
  private ProfileService profileService;
  private PostService postService;
  private User currentUser;

  @FXML
  public void initialize() {
    authService = AuthService.getInstance();
    currentUser = authService.getCurrentUser();

    try {
      Connection connection = DatabaseConnection.getConnection();
      profileService = ProfileService.getInstance(connection);
      postService = new PostService(connection);
    } catch (SQLException e) {
      showError("Failed to connect to database");
      e.printStackTrace();
    }

    setupUI();
    loadProfileData();
    loadUserPosts();
  }

  private void setupUI() {
    // Set avatar colors
    sidebarAvatar.setFill(Color.web("#4f46e5"));
    profileAvatar.setFill(Color.web("#4f46e5"));

    // Set active nav button
    setActiveNavButton(profileBtn);

    // Initially hide edit form
    editProfileForm.setVisible(false);
    editProfileForm.setManaged(false);
  }

  private void loadProfileData() {
    if (currentUser == null)
      return;

    Profile profile = currentUser.getProfile();

    // Sidebar info
    sidebarNameLabel.setText(profile != null ? profile.getName() : "User");
    sidebarEmailLabel.setText(currentUser.getEmail());

    // Profile header
    profileNameLabel.setText(profile != null ? profile.getName() : "User");
    profileEmailLabel.setText(currentUser.getEmail());
    profileBioLabel.setText(profile != null && profile.getBio() != null ? profile.getBio()
        : "No bio yet. Click 'Edit Profile' to add one!");

    // Format joined date
    if (currentUser.getCreatedAt() != null) {
      String formattedDate = currentUser.getCreatedAt()
          .format(DateTimeFormatter.ofPattern("MMMM yyyy"));
      joinedDateLabel.setText("Joined " + formattedDate);
      aboutJoinedLabel.setText(formattedDate);
    }

    // About section
    aboutEmailLabel.setText(currentUser.getEmail());
    aboutBioLabel.setText(profile != null && profile.getBio() != null ? profile.getBio() : "No bio provided");

    // Stats (placeholder - implement actual count from services)
    postsCount.setText("0");
    friendsCount.setText("0");
    likesCount.setText("0");
  }

  private void loadUserPosts() {
    loadingPosts.setVisible(true);
    loadingPosts.setManaged(true);
    noPostsMessage.setVisible(false);
    noPostsMessage.setManaged(false);

    Task<List<Post>> loadTask = new Task<>() {
      @Override
      protected List<Post> call() throws Exception {
        return postService.getUserPosts(currentUser.getId());
      }
    };

    loadTask.setOnSucceeded(event -> {
      loadingPosts.setVisible(false);
      loadingPosts.setManaged(false);

      List<Post> posts = loadTask.getValue();
      displayUserPosts(posts);
      postsCount.setText(String.valueOf(posts.size()));
    });

    loadTask.setOnFailed(event -> {
      loadingPosts.setVisible(false);
      loadingPosts.setManaged(false);
      showError("Failed to load posts");
    });

    new Thread(loadTask).start();
  }

  private void displayUserPosts(List<Post> posts) {
    userPostsContainer.getChildren().clear();

    if (posts == null || posts.isEmpty()) {
      noPostsMessage.setVisible(true);
      noPostsMessage.setManaged(true);
      return;
    }

    for (Post post : posts) {
      VBox postCard = createPostCard(post);
      userPostsContainer.getChildren().add(postCard);
    }
  }

  private VBox createPostCard(Post post) {
    VBox card = new VBox(12);
    card.getStyleClass().add("post-card");
    card.setPadding(new Insets(16));

    // Header
    HBox header = new HBox(12);
    header.setAlignment(Pos.CENTER_LEFT);

    Label timestamp = new Label(formatTimestamp(post.getCreatedAt()));
    timestamp.getStyleClass().add("post-timestamp");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label privacyBadge = new Label(getPrivacyIcon(post.getPrivacy()));
    privacyBadge.getStyleClass().add("privacy-badge");

    MenuButton optionsBtn = new MenuButton("⋯");
    optionsBtn.getStyleClass().add("post-options-btn");
    MenuItem editItem = new MenuItem("Edit");
    editItem.setOnAction(e -> handleEditPost(post));
    MenuItem deleteItem = new MenuItem("Delete");
    deleteItem.setOnAction(e -> handleDeletePost(post));
    optionsBtn.getItems().addAll(editItem, deleteItem);

    header.getChildren().addAll(timestamp, spacer, privacyBadge, optionsBtn);

    // Content
    Label content = new Label(post.getContent());
    content.getStyleClass().add("post-content");
    content.setWrapText(true);

    // Actions
    HBox actions = new HBox(16);
    actions.setAlignment(Pos.CENTER_LEFT);
    actions.getStyleClass().add("post-actions");

    Button likeBtn = new Button("❤️ " + post.getLikes());
    likeBtn.getStyleClass().add("action-button");
    try {
      if (postService.hasUserLiked(post.getPostId(), currentUser.getId())) {
        likeBtn.getStyleClass().add("liked");
      }
    } catch (SQLException ignored) {
    }
    likeBtn.setOnAction(e -> handleLikePost(post, likeBtn));

    int commentCount = 0;
    try {
      commentCount = postService.getCommentCount(post.getPostId());
    } catch (SQLException ignored) {
    }
    Button commentBtn = new Button("💬 " + commentCount);
    commentBtn.getStyleClass().add("action-button");
    commentBtn.setOnAction(e -> handleShowComments(post));

    actions.getChildren().addAll(likeBtn, commentBtn);

    card.getChildren().addAll(header, content, actions);
    return card;
  }

  private String formatTimestamp(LocalDateTime dateTime) {
    if (dateTime == null)
      return "";
    return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
  }

  private String getPrivacyIcon(Post.Privacy privacy) {
    if (privacy == null)
      return "🌍";
    return switch (privacy) {
      case PUBLIC -> "🌍 Public";
      case FRIENDS -> "👥 Friends";
      case PRIVATE -> "🔒 Private";
    };
  }

  // ==================== PROFILE EDITING ====================

  @FXML
  private void handleEditProfile() {
    Profile profile = currentUser.getProfile();

    editNameField.setText(profile != null ? profile.getName() : "");
    editBioField.setText(profile != null && profile.getBio() != null ? profile.getBio() : "");

    editProfileForm.setVisible(true);
    editProfileForm.setManaged(true);
    editProfileBtn.setDisable(true);
  }

  @FXML
  private void handleSaveProfile() {
    String newName = editNameField.getText().trim();
    String newBio = editBioField.getText().trim();

    if (newName.isEmpty() || newName.length() < 2) {
      showEditError("Name must be at least 2 characters");
      return;
    }

    Task<Void> saveTask = new Task<>() {
      @Override
      protected Void call() throws Exception {
        profileService.updateProfile(currentUser.getId(), newName, newBio, null);
        return null;
      }
    };

    saveTask.setOnSucceeded(event -> {
      // Update local user object
      Profile profile = currentUser.getProfile();
      if (profile != null) {
        profile.setName(newName);
        profile.setBio(newBio);
      }

      handleCancelEdit();
      loadProfileData();
      showSuccess("Profile updated successfully!");
    });

    saveTask.setOnFailed(event -> {
      showEditError("Failed to update profile");
      saveTask.getException().printStackTrace();
    });

    new Thread(saveTask).start();
  }

  @FXML
  private void handleCancelEdit() {
    editProfileForm.setVisible(false);
    editProfileForm.setManaged(false);
    editProfileBtn.setDisable(false);
    editErrorLabel.setVisible(false);
  }

  @FXML
  private void handleChangeAvatar() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Choose Profile Picture");
    fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

    File file = fileChooser.showOpenDialog(changeAvatarBtn.getScene().getWindow());
    if (file != null) {
      // TODO: Upload and save profile picture
      showSuccess("Profile picture feature coming soon!");
    }
  }

  private void showEditError(String message) {
    editErrorLabel.setText(message);
    editErrorLabel.setVisible(true);
  }

  // ==================== TABS ====================

  @FXML
  private void handlePostsTab() {
    postsTabBtn.getStyleClass().add("tab-button-active");
    aboutTabBtn.getStyleClass().remove("tab-button-active");

    postsSection.setVisible(true);
    postsSection.setManaged(true);
    aboutSection.setVisible(false);
    aboutSection.setManaged(false);
  }

  @FXML
  private void handleAboutTab() {
    aboutTabBtn.getStyleClass().add("tab-button-active");
    postsTabBtn.getStyleClass().remove("tab-button-active");

    aboutSection.setVisible(true);
    aboutSection.setManaged(true);
    postsSection.setVisible(false);
    postsSection.setManaged(false);
  }

  @FXML
  private void handleRefreshPosts() {
    loadUserPosts();
  }

  private void handleEditPost(Post post) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Edit Post");
    dialog.setHeaderText(null);

    VBox content = new VBox(12);
    content.setPrefWidth(400);
    content.setPadding(new Insets(16));

    Label titleLabel = new Label("Edit Post");
    titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

    TextArea textArea = new TextArea(post.getContent());
    textArea.setWrapText(true);
    textArea.setPrefRowCount(5);
    textArea.setStyle("-fx-font-size: 14px;");

    content.getChildren().addAll(titleLabel, textArea);
    dialog.getDialogPane().setContent(content);

    ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

    dialog.setResultConverter(btn -> {
      if (btn == saveType) {
        return textArea.getText().trim();
      }
      return null;
    });

    dialog.showAndWait().ifPresent(newContent -> {
      if (newContent.isEmpty()) {
        showError("Post content cannot be empty");
        return;
      }
      try {
        if (postService.updatePost(post.getPostId(), currentUser.getId(), newContent)) {
          loadUserPosts();
          showSuccess("Post updated");
        } else {
          showError("Failed to update post");
        }
      } catch (SQLException ex) {
        showError("Failed to update post");
        ex.printStackTrace();
      }
    });
  }

  private void handleDeletePost(Post post) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Delete Post");
    confirm.setHeaderText("Are you sure?");
    confirm.setContentText("This action cannot be undone.");

    confirm.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        try {
          if (postService.deletePost(post.getPostId(), currentUser.getId())) {
            loadUserPosts();
            showSuccess("Post deleted");
          }
        } catch (SQLException e) {
          showError("Failed to delete post");
        }
      }
    });
  }

  private void handleLikePost(Post post, Button likeBtn) {
    Task<Boolean> likeTask = new Task<>() {
      @Override
      protected Boolean call() throws Exception {
        return postService.toggleLike(post.getPostId(), currentUser.getId());
      }
    };

    likeTask.setOnSucceeded(event -> {
      boolean liked = likeTask.getValue();
      int newLikes = post.getLikes() + (liked ? 1 : -1);
      post.setLikes(newLikes);
      likeBtn.setText("❤️ " + newLikes);
      if (liked) {
        if (!likeBtn.getStyleClass().contains("liked")) {
          likeBtn.getStyleClass().add("liked");
        }
      } else {
        likeBtn.getStyleClass().remove("liked");
      }
    });

    likeTask.setOnFailed(event -> {
      showError("Failed to update like");
    });

    new Thread(likeTask).start();
  }

  private void handleShowComments(Post post) {
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Comments");
    dialog.setHeaderText(null);
    dialog.setResizable(true);

    VBox dialogContent = new VBox(12);
    dialogContent.setPrefWidth(450);
    dialogContent.setPrefHeight(400);
    dialogContent.setPadding(new Insets(16));

    Label titleLabel = new Label("Comments");
    titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

    VBox commentsBox = new VBox(8);
    ScrollPane commentsScroll = new ScrollPane(commentsBox);
    commentsScroll.setFitToWidth(true);
    commentsScroll.setPrefHeight(280);
    commentsScroll.setStyle("-fx-background-color: transparent;");

    Label loadingLabel = new Label("Loading comments...");
    loadingLabel.setStyle("-fx-text-fill: #94a3b8;");
    commentsBox.getChildren().add(loadingLabel);

    HBox inputBox = new HBox(8);
    inputBox.setAlignment(Pos.CENTER_LEFT);
    TextField commentInput = new TextField();
    commentInput.setPromptText("Write a comment...");
    commentInput.setPrefWidth(350);
    HBox.setHgrow(commentInput, Priority.ALWAYS);
    Button sendBtn = new Button("Send");
    sendBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-background-radius: 8;");
    inputBox.getChildren().addAll(commentInput, sendBtn);

    dialogContent.getChildren().addAll(titleLabel, commentsScroll, inputBox);

    loadCommentsIntoBox(post, commentsBox);

    sendBtn.setOnAction(e -> {
      String text = commentInput.getText().trim();
      if (text.isEmpty())
        return;
      commentInput.setDisable(true);
      sendBtn.setDisable(true);

      Task<Comment> addTask = new Task<>() {
        @Override
        protected Comment call() throws Exception {
          return postService.addComment(post.getPostId(), currentUser.getId(), text);
        }
      };

      addTask.setOnSucceeded(ev -> {
        commentInput.clear();
        commentInput.setDisable(false);
        sendBtn.setDisable(false);
        loadCommentsIntoBox(post, commentsBox);
      });

      addTask.setOnFailed(ev -> {
        commentInput.setDisable(false);
        sendBtn.setDisable(false);
        showError("Failed to add comment");
      });

      new Thread(addTask).start();
    });

    commentInput.setOnAction(e -> sendBtn.fire());

    dialog.getDialogPane().setContent(dialogContent);
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    dialog.showAndWait();
  }

  private void loadCommentsIntoBox(Post post, VBox commentsBox) {
    Task<List<Comment>> loadTask = new Task<>() {
      @Override
      protected List<Comment> call() throws Exception {
        return postService.getComments(post.getPostId());
      }
    };

    loadTask.setOnSucceeded(event -> {
      commentsBox.getChildren().clear();
      List<Comment> comments = loadTask.getValue();

      if (comments.isEmpty()) {
        Label empty = new Label("No comments yet. Be the first to comment!");
        empty.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20 0;");
        commentsBox.getChildren().add(empty);
        return;
      }

      for (Comment c : comments) {
        VBox commentCard = createCommentCard(c);
        commentsBox.getChildren().add(commentCard);
      }
    });

    loadTask.setOnFailed(event -> {
      commentsBox.getChildren().clear();
      Label error = new Label("Failed to load comments");
      error.setStyle("-fx-text-fill: #ef4444;");
      commentsBox.getChildren().add(error);
    });

    new Thread(loadTask).start();
  }

  private VBox createCommentCard(Comment comment) {
    VBox card = new VBox(4);
    card.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 10; -fx-background-radius: 8;");

    String authorName;
    try {
      authorName = postService.getAuthorName(comment.getUserId());
    } catch (SQLException e) {
      authorName = "User #" + comment.getUserId();
    }

    HBox header = new HBox(8);
    header.setAlignment(Pos.CENTER_LEFT);
    Label nameLabel = new Label(authorName);
    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label timeLabel = new Label(formatTimestamp(comment.getCreatedAt()));
    timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

    header.getChildren().addAll(nameLabel, spacer, timeLabel);

    Label contentLabel = new Label(comment.getContent());
    contentLabel.setWrapText(true);
    contentLabel.setStyle("-fx-font-size: 13px;");

    if (comment.getUserId() == currentUser.getId()) {
      Button editBtn = new Button("Edit");
      editBtn.setStyle(
          "-fx-background-color: #e0e7ff; -fx-text-fill: #4f46e5; -fx-cursor: hand; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 2 8;");
      editBtn.setOnAction(e -> {
        int contentIndex = card.getChildren().indexOf(contentLabel);
        if (contentIndex == -1)
          return;

        TextField editField = new TextField(contentLabel.getText());
        editField.setStyle("-fx-font-size: 13px;");

        HBox editActions = new HBox(6);
        editActions.setAlignment(Pos.CENTER_RIGHT);
        Button saveBtn = new Button("Save");
        saveBtn.setStyle(
            "-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 12;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 12;");
        editActions.getChildren().addAll(saveBtn, cancelBtn);

        card.getChildren().set(contentIndex, editField);
        card.getChildren().add(contentIndex + 1, editActions);
        editField.requestFocus();

        cancelBtn.setOnAction(ev -> {
          int editIndex = card.getChildren().indexOf(editField);
          card.getChildren().remove(editActions);
          card.getChildren().set(editIndex, contentLabel);
        });

        saveBtn.setOnAction(ev -> {
          String newText = editField.getText().trim();
          if (newText.isEmpty())
            return;
          try {
            postService.updateComment(comment.getCommentId(), newText);
            contentLabel.setText(newText);
            int editIndex = card.getChildren().indexOf(editField);
            card.getChildren().remove(editActions);
            card.getChildren().set(editIndex, contentLabel);
          } catch (SQLException ex) {
            showError("Failed to update comment");
          }
        });

        editField.setOnAction(ev -> saveBtn.fire());
      });

      Button deleteBtn = new Button("Delete");
      deleteBtn.setStyle(
          "-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 2 8;");
      deleteBtn.setOnAction(e -> {
        try {
          postService.deleteComment(comment.getCommentId());
          card.setVisible(false);
          card.setManaged(false);
        } catch (SQLException ex) {
          showError("Failed to delete comment");
        }
      });
      header.getChildren().addAll(editBtn, deleteBtn);
    }

    card.getChildren().addAll(header, contentLabel);
    return card;
  }

  // ==================== NAVIGATION ====================

  @FXML
  private void handleFeedNav() {
    navigateTo("/fxml/main_feed.fxml");
  }

  @FXML
  private void handleProfileNav() {
    // Already on profile
  }

  @FXML
  private void handleFriendsNav() {
    navigateTo("/fxml/friends.fxml");
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
      Stage stage = (Stage) profileBtn.getScene().getWindow();
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
}
