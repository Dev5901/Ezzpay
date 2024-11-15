package com.example.ezzpay;

public class User {

    private String userId;
    private String fullName;
    private String email;
    private String contactNumber;

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {
    }

    // Parameterized constructor
    public User(String userId, String fullName, String email, String contactNumber) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    // Optional: Override toString() for debugging purposes
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                '}';
    }
}
