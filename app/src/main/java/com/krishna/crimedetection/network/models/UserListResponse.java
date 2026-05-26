package com.krishna.crimedetection.network.models;

import java.util.List;

public class UserListResponse {
    private List<User> users;
    private int total;

    public List<User> getUsers() { return users; }
    public int getTotal() { return total; }

    public static class User {
        private int id;
        private String username;
        private String email;
        private String role;
        private boolean isActive;
        private int incidentCount;
        private String lastLogin;

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isActive() { return isActive; }
        public int getIncidentCount() { return incidentCount; }
        public String getLastLogin() { return lastLogin; }
    }
}
