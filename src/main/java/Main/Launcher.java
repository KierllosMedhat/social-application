package Main;

/**
 * Launcher class to start the JavaFX application.
 * This bypasses the JavaFX runtime check that occurs when 
 * the main class extends Application directly.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
