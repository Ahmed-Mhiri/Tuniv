package com.tuniv.backend.university.model;

import org.springframework.data.jpa.domain.Specification;

public class UniversitySpecification {
    
    /**
     * Creates a specification to find universities where the name contains the search term (case-insensitive).
     * @param searchTerm The string to search for in university names.
     * @return a Specification for the query.
     */
    public static Specification<University> searchByName(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Return no restrictions if search term is empty
            }
            // Corresponds to: WHERE LOWER(university.name) LIKE LOWER('%searchTerm%')
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")), 
                "%" + searchTerm.toLowerCase() + "%"
            );
        };
    }
}
