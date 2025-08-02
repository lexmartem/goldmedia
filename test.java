import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin123";
        String storedHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa";
        
        System.out.println("Testing BCrypt password encoding:");
        System.out.println("Password: " + password);
        System.out.println("Stored hash: " + storedHash);
        System.out.println("Hash matches: " + encoder.matches(password, storedHash));
        
        // Generate new hash for admin123
        String newHash = encoder.encode(password);
        System.out.println("New hash for admin123: " + newHash);
        System.out.println("New hash matches: " + encoder.matches(password, newHash));
        
        // Test user123
        String userPassword = "user123";
        String userStoredHash = "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a";
        System.out.println("\nTesting user123:");
        System.out.println("User password: " + userPassword);
        System.out.println("User stored hash: " + userStoredHash);
        System.out.println("User hash matches: " + encoder.matches(userPassword, userStoredHash));
        
        // Generate new hash for user123
        String newUserHash = encoder.encode(userPassword);
        System.out.println("New user hash: " + newUserHash);
        System.out.println("New user hash matches: " + encoder.matches(userPassword, newUserHash));
    }
} 