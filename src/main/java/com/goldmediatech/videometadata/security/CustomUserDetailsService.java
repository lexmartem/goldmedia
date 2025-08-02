package com.goldmediatech.videometadata.security;

import com.goldmediatech.videometadata.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom User Details Service for loading user information from configuration.
 * 
 * This service loads user information from application configuration
 * and provides it to Spring Security for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final Map<String, UserPrincipal> userCache = new ConcurrentHashMap<>();
    private final AppConfig appConfig;

    @Autowired
    public CustomUserDetailsService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserPrincipal user = userCache.get(username);
        
        if (user == null) {
            user = createUserFromConfig(username);
            if (user != null) {
                userCache.put(username, user);
            }
        }
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        
        return user;
    }

    /**
     * Create UserPrincipal from configuration.
     * 
     * @param username the username to find
     * @return UserPrincipal or null if not found
     */
    private UserPrincipal createUserFromConfig(String username) {
        List<UserConfig> users = appConfig.getUsers();
        if (users == null) {
            return null;
        }
        
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .map(user -> UserPrincipal.create(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getRoles()))
                .orElse(null);
    }

    /**
     * Configuration class for user properties.
     */
    public static class UserConfig {
        private Long id;
        private String username;
        private String password;
        private List<String> roles;

        // Default constructor
        public UserConfig() {}

        // Constructor with fields
        public UserConfig(Long id, String username, String password, List<String> roles) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.roles = roles;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        @Override
        public String toString() {
            return "UserConfig{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", password='[HIDDEN]'" +
                    ", roles=" + roles +
                    '}';
        }
    }
} 