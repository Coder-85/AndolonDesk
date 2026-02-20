package org.amjonota.model;

public class User {
    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String provider;
    private String providerId;
    private String dateOfBirth;
    private String createdAt;

    public User(int id, String name, String email, String passwordHash, String provider, String providerId, String dateOfBirth) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerId = providerId;
        this.dateOfBirth = dateOfBirth;
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
