package com.tuniv.backend.qa.model;

import com.tuniv.backend.user.model.User;

public interface Vote {
    User getUser();
    short getValue();
    Integer getPostId(); // This is the key for generic processing
    Post getPost(); // ✅ ADD THIS LINE

}
