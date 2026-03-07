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
import models.Comment;
import models.Post;
import models.Profile;
import models.User;
import services.AuthService;
import services.FriendshipService;
import services.PostService;
import utils.DatabaseConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainFeedController {

  // Sidebar elements
  @FXML
  private Circle avatarCircle;
  @FXML
  private Label userNameLabel;
  @FXML
  private Label userEmailLabel;
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

  // Main content
  @FXML
  private Label pageTitle;
  @FXML
  private Button refreshBtn;
  @FXML
  private TextArea postContentArea;
  @FXML
  private ComboBox<String> privacyComboBox;
  @FXML
  private Button attachImageBtn;
  @FXML
  private Button postBtn;
  @FXML
  private VBox postsContainer;
  @FXML
  private ScrollPane postsScroll;
  @FXML
  private HBox loadingBox;
  @FXML
  private VBox emptyState;

  // Stats
  @FXML
  private Label postsCountLabel;
  @FXML
  private Label friendsCountLabel;
  @FXML
  private Label likesCountLabel;
  @FXML
  private VBox suggestionsContainer;

  // Services
  private AuthService authService;
  private PostService postService;
  private FriendshipService friendshipService;
  private User currentUser;

  // Pagination
  private int currentPage = 1;
  private static final int PAGE_SIZE = 10;

  @FXML
  public void initialize() {
    authService = AuthService.getInstance();
    currentUser = authService.getCurrentUser();

    try {
      Connection connection = DatabaseConnection.getConnection();
      postService = new PostService(connection);
    } catch (SQLException e) {
      showError("Failed to connect to database");
      e.printStackTrace();
    }

    setupUI();
    loadUserInfo();
    loadPosts();
    loadStats();
  }

  private void setupUI() {
    // Setup privacy dropdown
    privacyComboBox.getItems().addAll("Public", "Friends Only", "Private");
    privacyComboBox.setValue("Public");

    // Setup avatar color (placeholder for profile picture)
    avatarCircle.setFill(Color.web("#4f46e5"));

    // Set active nav button style
    setActiveNavButton(feedBtn);
  }

  private void loadUserInfo() {
    if (currentUser != null) {
      Profile profile = currentUser.getProfile();
      if (profile != null) {
        userNameLabel.setText(profile.getName());
      } else {
        userNameLabel.setText("User");
      }
      userEmailLabel.setText(currentUser.getEmail());
    }
  }

  private void loadPosts() {
    showLoading(true);

    Task<List<Post>> loadTask = new Task<>() {
      @Override
      protected List<Post> call() throws Exception {
        return postService.getNewsFeed(currentUser.getId(), currentPage, PAGE_SIZE);
      }
    };

    loadTask.setOnSucceeded(event -> {
      showLoading(false);
      List<Post> posts = loadTask.getValue();
      displayPosts(posts);
    });

    loadTask.setOnFailed(event -> {
      showLoading(false);
      showError("Failed to load posts");
      loadTask.getException().printStackTrace();
    });

    new Thread(loadTask).start();
  }

  private void displayPosts(List<Post> posts) {
    postsContainer.getChildren().clear();

    if (posts == null || posts.isEmpty()) {
      emptyState.setVisible(true);
      emptyState.setManaged(true);
      return;
    }

    emptyState.setVisible(false);
    emptyState.setManaged(false);

    for (Post post : posts) {
      VBox postCard = createPostCard(post);
      postsContainer.getChildren().add(postCard);
    }
  }

  private VBox createPostCard(Post post) {
    VBox card = new VBox(12);
    card.getStyleClass().add("post-card");
    card.setPadding(new Insets(16));

    // Header: Avatar + Name + Time
    HBox header = new HBox(12);
    header.setAlignment(Pos.CENTER_LEFT);

    Circle avatar = new Circle(20);
    avatar.setFill(Color.web("#64748b"));
    avatar.getStyleClass().add("post-avatar");

    VBox userInfo = new VBox(2);
    Label authorName = new Label(getAuthorName(post));
    authorName.getStyleClass().add("post-author");

    Label timestamp = new Label(formatTimestamp(post.getCreatedAt()));
    timestamp.getStyleClass().add("post-timestamp");

    userInfo.getChildren().addAll(authorName, timestamp);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    // Privacy badge
    Label privacyBadge = new Label(getPrivacyIcon(post.getPrivacy()));
    privacyBadge.getStyleClass().add("privacy-badge");

    // Options menu (only for own posts)
    MenuButton optionsBtn = new MenuButton("⋯");
    optionsBtn.getStyleClass().add("post-options-btn");

    if (post.getUserId() == currentUser.getId()) {
      MenuItem editItem = new MenuItem("Edit");
      editItem.setOnAction(e -> handleEditPost(post));
      MenuItem deleteItem = new MenuItem("Delete");
      deleteItem.setOnAction(e -> handleDeletePost(post));
      optionsBtn.getItems().addAll(editItem, deleteItem);
      header.getChildren().addAll(avatar, userInfo, spacer, privacyBadge, optionsBtn);
    } else {
      header.getChildren().addAll(avatar, userInfo, spacer, privacyBadge);
    }

    // Content
    Label content = new Label(post.getContent());
    content.getStyleClass().add("post-content");
    content.setWrapText(true);

    // Image (if exists)
    // TODO: Add image support when implementing image upload

    // Actions bar
    HBox actions = new HBox(16);
    actions.setAlignment(Pos.CENTER_LEFT);
    actions.getStyleClass().add("post-actions");

    Button likeBtn = new Button("❤️ " + post.getLikes());
    likeBtn.getStyleClass().add("action-button");
    // Check if current user already liked this post
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

  private String getAuthorName(Post post) {
    if (post.getUserId() == currentUser.getId()) {
      Profile profile = currentUser.getProfile();
      return profile != null ? profile.getName() : "You";
    }
    try {
      return postService.getAuthorName(post.getUserId());
    } catch (SQLException e) {
      return "User #" + post.getUserId();
    }
  }

  private String formatTimestamp(LocalDateTime dateTime) {
    if (dateTime == null)
      return "";

    LocalDateTime now = LocalDateTime.now();
    long minutes = java.time.Duration.between(dateTime, now).toMinutes();

    if (minutes < 1)
      return "Just now";
    if (minutes < 60)
      return minutes + "m ago";
    if (minutes < 1440)
      return (minutes / 60) + "h ago";
    if (minutes < 10080)
      return (minutes / 1440) + "d ago";

    return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
  }

  private String getPrivacyIcon(Post.Privacy privacy) {
    if (privacy == null)
      return "🌍";
    return switch (privacy) {
      case PUBLIC -> "🌍";
      case FRIENDS -> "👥";
      case PRIVATE -> "🔒";
    };
  }

  @FXML
  private void handleCreatePost() {
    String content = postContentArea.getText().trim();

    if (content.isEmpty()) {
      showError("Post content cannot be empty");
      return;
    }

    Post.Privacy privacy = getSelectedPrivacy();

    postBtn.setDisable(true);
    postBtn.setText("Posting...");

    Task<Void> createTask = new Task<>() {
      @Override
      protected Void call() throws Exception {
        postService.createPost(currentUser.getId(), content, null, privacy);
        return null;
      }
    };

    createTask.setOnSucceeded(event -> {
      postBtn.setDisable(false);
      postBtn.setText("Post");
      postContentArea.clear();
      loadPosts(); // Refresh feed
      showSuccess("Post created successfully!");
    });

    createTask.setOnFailed(event -> {
      postBtn.setDisable(false);
      postBtn.setText("Post");
      showError("Failed to create post");
      createTask.getException().printStackTrace();
    });

    new Thread(createTask).start();
  }

  private Post.Privacy getSelectedPrivacy() {
    String selected = privacyComboBox.getValue();
    if (selected == null)
      return Post.Privacy.PUBLIC;

    return switch (selected) {
      case "Friends Only" -> Post.Privacy.FRIENDS;
      case "Private" -> Post.Privacy.PRIVATE;
      default -> Post.Privacy.PUBLIC;
    };
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
          loadPosts();
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
            loadPosts();
            showSuccess("Post deleted");
          }
        } catch (SQLException e) {
          showError("Failed to delete post");
          e.printStackTrace();
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
      likeTask.getException().printStackTrace();
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

    // Comments list
    VBox commentsBox = new VBox(8);
    ScrollPane commentsScroll = new ScrollPane(commentsBox);
    commentsScroll.setFitToWidth(true);
    commentsScroll.setPrefHeight(280);
    commentsScroll.setStyle("-fx-background-color: transparent;");

    // Loading label
    Label loadingLabel = new Label("Loading comments...");
    loadingLabel.setStyle("-fx-text-fill: #94a3b8;");
    commentsBox.getChildren().add(loadingLabel);

    // New comment input
    HBox inputBox = new HBox(8);
    inputBox.setAlignment(Pos.CENTER_LEFT);
    TextField commentInput = new TextField();
    commentInput.setPromptText("Write a comment...");
    commentInput.setPrefWidth(350);
    HBox.setHgrow(commentInput, Priority.ALWAYS);
    Button sendBtn = new Button("Send");
    sendBtn.getStyleClass().add("primary-button");
    sendBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-background-radius: 8;");
    inputBox.getChildren().addAll(commentInput, sendBtn);

    dialogContent.getChildren().addAll(titleLabel, commentsScroll, inputBox);

    // Load comments async
    loadCommentsIntoBox(post, commentsBox, loadingLabel);

    // Send comment action
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
        loadCommentsIntoBox(post, commentsBox, null);
      });

      addTask.setOnFailed(ev -> {
        commentInput.setDisable(false);
        sendBtn.setDisable(false);
        showError("Failed to add comment");
      });

      new Thread(addTask).start();
    });

    // Allow Enter key to send
    commentInput.setOnAction(e -> sendBtn.fire());

    dialog.getDialogPane().setContent(dialogContent);
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    dialog.showAndWait();
  }

  private void loadCommentsIntoBox(Post post, VBox commentsBox, Label loadingLabel) {
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
    Label timeLabel = new Label(formatTimestamp(comment.getCreatedAt()));
    timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    header.getChildren().addAll(nameLabel, spacer, timeLabel);

    Label contentLabel = new Label(comment.getContent());
    contentLabel.setWrapText(true);
    contentLabel.setStyle("-fx-font-size: 13px;");

    // Edit and delete buttons for own comments
    if (comment.getUserId() == currentUser.getId()) {
      Button editBtn = new Button("Edit");
      editBtn.setStyle(
          "-fx-background-color: #e0e7ff; -fx-text-fill: #4f46e5; -fx-cursor: hand; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 2 8;");
      editBtn.setOnAction(e -> {
        // Replace content label with edit field at the same position
        int contentIndex = card.getChildren().indexOf(contentLabel);
        if (contentIndex == -1)
          return; // already editing

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

  @FXML
  private void handleRefresh() {
    currentPage = 1;
    loadPosts();
  }

  private void loadStats() {
    // TODO: Fetch actual stats from services
    postsCountLabel.setText("0");
    friendsCountLabel.setText("0");
    likesCountLabel.setText("0");
  }

  // ==================== NAVIGATION ====================

  @FXML
  private void handleFeedNav() {
    setActiveNavButton(feedBtn);
    pageTitle.setText("Your Feed");
    loadPosts();
  }

  @FXML
  private void handleProfileNav() {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/fxml/profile.fxml"));
      Stage stage = (Stage) profileBtn.getScene().getWindow();
      Scene scene = new Scene(root, 1200, 800);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
      stage.setScene(scene);
    } catch (IOException e) {
      showError("Could not load profile page");
      e.printStackTrace();
    }
  }

  @FXML
  private void handleFriendsNav() {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/fxml/friends.fxml"));
      Stage stage = (Stage) friendsBtn.getScene().getWindow();
      Scene scene = new Scene(root, 1200, 800);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
      stage.setScene(scene);
    } catch (IOException e) {
      showError("Could not load friends page");
      e.printStackTrace();
    }
  }

  @FXML
  private void handleNotificationsNav() {
    setActiveNavButton(notificationsBtn);
    pageTitle.setText("Notifications");
    // TODO: Load notifications
  }

  @FXML
  private void handleLogout() {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Logout");
    confirm.setHeaderText("Are you sure you want to logout?");

    confirm.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        authService.logout();
        navigateToLogin();
      }
    });
  }

  private void navigateToLogin() {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
      Stage stage = (Stage) logoutBtn.getScene().getWindow();
      Scene scene = new Scene(root, 1200, 800);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
      stage.setScene(scene);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void setActiveNavButton(Button activeBtn) {
    // Remove active class from all nav buttons
    feedBtn.getStyleClass().remove("nav-button-active");
    profileBtn.getStyleClass().remove("nav-button-active");
    friendsBtn.getStyleClass().remove("nav-button-active");
    notificationsBtn.getStyleClass().remove("nav-button-active");

    // Add active class to selected button
    if (!activeBtn.getStyleClass().contains("nav-button-active")) {
      activeBtn.getStyleClass().add("nav-button-active");
    }
  }

  // ==================== UI HELPERS ====================

  private void showLoading(boolean show) {
    Platform.runLater(() -> {
      loadingBox.setVisible(show);
      loadingBox.setManaged(show);
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
}
