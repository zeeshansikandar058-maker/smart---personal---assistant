package com.spa.auth;

public class User {

    public enum Role { ADMIN, USER }

    private final int id;
    private final String username;
    private final Role role;
    private final String city;
    private final String country;

    public User(int id, String username, Role role, String city, String country) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.city = city;
        this.country = country;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
