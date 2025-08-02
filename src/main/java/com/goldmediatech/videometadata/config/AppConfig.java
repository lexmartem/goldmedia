package com.goldmediatech.videometadata.config;

import com.goldmediatech.videometadata.security.CustomUserDetailsService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application configuration class.
 * 
 * This class handles application-specific configuration properties
 * and provides beans for configuration-dependent components.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private JwtConfig jwt;
    private List<CustomUserDetailsService.UserConfig> users;
    private ExternalApisConfig externalApis;
    private ImportConfig importConfig;

    // Default constructor
    public AppConfig() {}

    // Getters and Setters
    public JwtConfig getJwt() {
        return jwt;
    }

    public void setJwt(JwtConfig jwt) {
        this.jwt = jwt;
    }

    public List<CustomUserDetailsService.UserConfig> getUsers() {
        return users;
    }

    public void setUsers(List<CustomUserDetailsService.UserConfig> users) {
        this.users = users;
    }

    public ExternalApisConfig getExternalApis() {
        return externalApis;
    }

    public void setExternalApis(ExternalApisConfig externalApis) {
        this.externalApis = externalApis;
    }

    public ImportConfig getImportConfig() {
        return importConfig;
    }

    public void setImportConfig(ImportConfig importConfig) {
        this.importConfig = importConfig;
    }

    /**
     * JWT Configuration
     */
    public static class JwtConfig {
        private String secret;
        private Long expiration;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Long getExpiration() {
            return expiration;
        }

        public void setExpiration(Long expiration) {
            this.expiration = expiration;
        }
    }

    /**
     * External APIs Configuration
     */
    public static class ExternalApisConfig {
        private YoutubeConfig youtube;
        private VimeoConfig vimeo;
        private MockConfig mock;

        public YoutubeConfig getYoutube() {
            return youtube;
        }

        public void setYoutube(YoutubeConfig youtube) {
            this.youtube = youtube;
        }

        public VimeoConfig getVimeo() {
            return vimeo;
        }

        public void setVimeo(VimeoConfig vimeo) {
            this.vimeo = vimeo;
        }

        public MockConfig getMock() {
            return mock;
        }

        public void setMock(MockConfig mock) {
            this.mock = mock;
        }
    }

    /**
     * YouTube API Configuration
     */
    public static class YoutubeConfig {
        private String apiKey;
        private String baseUrl;
        private Integer timeout;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Vimeo API Configuration
     */
    public static class VimeoConfig {
        private String accessToken;
        private String baseUrl;
        private Integer timeout;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Mock API Configuration
     */
    public static class MockConfig {
        private String baseUrl;
        private Boolean enabled;
        private Integer timeout;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Import Configuration
     */
    public static class ImportConfig {
        private Integer batchSize;
        private Integer maxRetries;
        private Integer retryDelay;

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Integer getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Integer retryDelay) {
            this.retryDelay = retryDelay;
        }
    }
} 