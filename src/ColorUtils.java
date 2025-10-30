package src;

public class ColorUtils {
    // ANSI color codes
    public static final String RESET = "\u001B[0m";
    public static final String ORANGE = "\u001B[38;5;214m";  // Bright orange
    public static final String YELLOW = "\u001B[33m";         // Yellow
    public static final String BOLD = "\u001B[1m";            // Bold text
    
    public static String orange(String text) {
        return ORANGE + text + RESET;
    }
    
    public static String yellow(String text) {
        return YELLOW + text + RESET;
    }
    
    public static String boldOrange(String text) {
        return BOLD + ORANGE + text + RESET;
    }
    
    public static String boldYellow(String text) {
        return BOLD + YELLOW + text + RESET;
    }
}
