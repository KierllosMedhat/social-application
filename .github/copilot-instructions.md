# Social Media Application - AI Agent Instructions

## Project Overview

JavaFX desktop social media application with MySQL backend. 3-tier layered architecture: **Controllers** (JavaFX/FXML) → **Services** (business logic) → **DAOs** (data access) → **MySQL Database**.

## Technology Stack

- **UI:** JavaFX 25.0.2 with FXML views and CSS styling
- **Database:** MySQL 8.0.33 via JDBC (despite pom showing mysql-connector, schema uses MySQL syntax)
- **Security:** BCrypt password hashing (12 rounds) via jBCrypt
- **Config:** dotenv for environment variables
- **Build:** Maven (Java 25)

## Architecture & Patterns

### Package Structure

```
controller/     - JavaFX controllers with @FXML annotations
services/       - Business logic, validation, service orchestration
daos/           - Data access objects with SQL operations
models/         - POJOs (User, Post, Profile, Friendship, Comment, Notification)
utils/          - DatabaseConnection (singleton), PasswordUtil
Main/           - Application entry point (note: uppercase package name)
```

### Database Connection Pattern

- **Singleton:** `DatabaseConnection.getConnection()` manages single connection instance
- **Configuration:** Load from `.env` file (DB_URL, DB_USER, DB_PASSWORD) using dotenv library
- **Example:** All DAOs receive `Connection` via constructor injection

### DAO Pattern

- Constructor injection: `new UserDao(connection)`
- PreparedStatements for all queries (SQL injection prevention)
- ResultSet mapping methods: `mapResultSetToUser(rs)`, `mapResultSetToPost(rs)`
- Auto-increment IDs retrieved via `Statement.RETURN_GENERATED_KEYS`
- **JOIN pattern:** UserDao joins users with profiles in queries, populates nested Profile object

### Service Layer

- **Singleton with session:** `AuthService.getInstance()` maintains `currentUser` for active session
- Other services: Constructor injection of DAOs (e.g., `FriendshipService(FriendshipDao)`)
- **Validation:** Business rules enforced in services (password requirements, email format), not just controllers
- **Example:** `AuthService.register()` validates email format, password strength, checks duplicates before creating user + profile

### Controller Pattern

- FXML views loaded from `/fxml/*.fxml` (note leading slash for classpath resources)
- CSS from `/css/style.css` added to scenes
- Navigation: Manual `FXMLLoader.load()` + `stage.setScene(new Scene(root))`
- Error display: Controllers have error labels toggled via `setVisible(true)`
- Service access: `AuthService.getInstance()` called in `@FXML initialize()` method

### Model Relationships

- `User` contains nested `Profile` object (populated by UserDao join queries)
- Foreign keys enforce referential integrity (ON DELETE CASCADE)
- Privacy levels: `Post.Privacy` enum maps to DB ENUM('public', 'friends', 'private')

## Critical Conventions

### Password Security

- **Always hash:** `PasswordUtil.hashPassword(plainPassword)` before storing
- **Verification:** `PasswordUtil.verifyPassword(plainPassword, hashedPassword)` for login
- Never store or log plain passwords

### Resource Loading

- FXML views: `getClass().getResource("/fxml/login.fxml")`
- CSS: `scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm())`
- Resources live in `src/main/resources/` directory

### Database Schema

- Primary keys: `user_id`, `post_id`, `profile_id`, etc. (not just `id`)
- Timestamps: `created_at` defaults to CURRENT_TIMESTAMP
- Unique constraints: `users.email`, `profiles.user_id`, `friendships(requester_id, addressee_id)`

### Naming Conventions

- Packages: lowercase (controller, daos, models, services, utils)
- **Exception:** Main class in `Main/` package (uppercase)
- Database tables: lowercase with underscores (`social_app_db`, `password_hash`)

## Development Workflows

### Running the Application

```bash
# Using Maven wrapper (Windows)
mvnw.cmd clean javafx:run

# Linux/Mac
./mvnw clean javafx:run
```

Main class configured: `main.Main`

### Database Setup

1. Create `.env` file in project root:

```
DB_URL=jdbc:mysql://localhost:3306/social_app_db
DB_USER=your_username
DB_PASSWORD=your_password
```

2. Execute schema: `src/database/app_database.sql`

### Adding New Features

#### New Database Entity

1. Add table to `app_database.sql` with appropriate foreign keys
2. Create model POJO in `models/` with getters/setters
3. Create DAO in `daos/` with `mapResultSetToModel()` method
4. Use PreparedStatements for all SQL operations

#### New UI Screen

1. Create FXML in `src/main/resources/fxml/`
2. Create controller in `controller/` package
3. Add `@FXML` annotations for UI elements
4. Initialize services in `@FXML initialize()` method
5. Load scene: `FXMLLoader.load(getClass().getResource("/fxml/yourview.fxml"))`

#### New Business Logic

1. Create service class in `services/`
2. Inject DAOs via constructor
3. Implement validation before DAO calls
4. Throw `IllegalArgumentException` for business rule violations

## Common Patterns

### User Authentication Flow

1. Controller calls `AuthService.getInstance().login(email, password)`
2. AuthService retrieves User via `UserDao.getUserByEmail()`
3. Verify password: `PasswordUtil.verifyPassword()`
4. Set `currentUser` in AuthService singleton
5. Navigate to main feed on success

### Creating Related Entities

```java
// User + Profile created together in AuthService.register()
User user = userDao.createUser(new User(email, hashedPassword));
Profile profile = new Profile(user.getId(), name);
profileDao.createProfile(profile);
```

### Querying with Relationships

```java
// UserDao joins users with profiles, populates nested object
User user = userDao.getUserByEmail(email); // Returns User with profile
Profile profile = user.getProfile(); // Already populated
```

## Testing Notes

- Manual testing via GUI (no automated tests currently)
- Database changes visible in MySQL (use `mysql` CLI or GUI tools)
- Check console for stack traces from try-catch blocks
