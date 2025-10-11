package com.tuniv.backend.university.model;

public enum MembershipType {
    PRIMARY,      // Email-verified at this university
    ASSOCIATE,    // Joined as external member
    ALUMNI,       // Graduated
    FACULTY,      // Staff/Professor
    EXCHANGE      // Exchange student
}
