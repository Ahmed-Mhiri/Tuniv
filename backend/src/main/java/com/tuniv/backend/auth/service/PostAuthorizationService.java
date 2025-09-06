package com.tuniv.backend.auth.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.model.Post;

@Service
public class PostAuthorizationService {

    /**
     * Checks if the current user is the author of the post.
     * Throws AccessDeniedException if they are not.
     * * @param post The Post entity (Question, Answer, or Comment).
     * @param currentUser The currently authenticated user.
     */
    public void checkOwnership(Post post, UserDetailsImpl currentUser) {
        if (!post.getAuthor().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access Denied: You are not the owner of this resource.");
        }
    }
}
