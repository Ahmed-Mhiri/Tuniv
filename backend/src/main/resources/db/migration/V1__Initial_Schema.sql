-- =================================================================
-- USERS & AUTHENTICATION
-- =================================================================
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_photo_url VARCHAR(255),
    bio TEXT,
    major VARCHAR(255),
    reputation_score INT NOT NULL DEFAULT 0,
    reset_password_token VARCHAR(255),
    reset_password_token_expiry TIMESTAMP WITH TIME ZONE,
    is_2fa_enabled BOOLEAN NOT NULL DEFAULT false,
    two_factor_auth_secret VARCHAR(255),
    verification_token VARCHAR(255),
    is_enabled BOOLEAN NOT NULL DEFAULT false
);

-- =================================================================
-- UNIVERSITY & ACADEMICS
-- =================================================================
CREATE TABLE universities (
    university_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE modules (
    module_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    university_id INT NOT NULL,
    CONSTRAINT fk_module_university FOREIGN KEY (university_id) REFERENCES universities(university_id) ON DELETE CASCADE
);

CREATE TABLE university_memberships (
    user_id INT NOT NULL,
    university_id INT NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, university_id),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_university FOREIGN KEY (university_id) REFERENCES universities(university_id) ON DELETE CASCADE
);

-- =================================================================
-- QUESTIONS, ANSWERS, COMMENTS (Q&A CORE)
-- =================================================================
CREATE TABLE questions (
    question_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    user_id INT NOT NULL,
    module_id INT NOT NULL,
    CONSTRAINT fk_question_author FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_question_module FOREIGN KEY (module_id) REFERENCES modules(module_id)
);

CREATE TABLE answers (
    answer_id SERIAL PRIMARY KEY,
    body TEXT NOT NULL,
    is_solution BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    question_id INT NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_author FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE comments (
    comment_id SERIAL PRIMARY KEY,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    answer_id INT NOT NULL,
    user_id INT NOT NULL,
    parent_comment_id INT,
    CONSTRAINT fk_comment_answer FOREIGN KEY (answer_id) REFERENCES answers(answer_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(comment_id) ON DELETE CASCADE
);

-- =================================================================
-- VOTES (FOR ALL POST TYPES)
-- =================================================================
CREATE TABLE question_votes (
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    "value" SMALLINT NOT NULL CHECK ("value" IN (-1, 1)),
    PRIMARY KEY (user_id, question_id),
    CONSTRAINT fk_qvote_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_qvote_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

CREATE TABLE answer_votes (
    user_id INT NOT NULL,
    answer_id INT NOT NULL,
    "value" SMALLINT NOT NULL CHECK ("value" IN (-1, 1)),
    PRIMARY KEY (user_id, answer_id),
    CONSTRAINT fk_avote_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_avote_answer FOREIGN KEY (answer_id) REFERENCES answers(answer_id) ON DELETE CASCADE
);

CREATE TABLE comment_votes (
    user_id INT NOT NULL,
    comment_id INT NOT NULL,
    "value" SMALLINT NOT NULL CHECK ("value" IN (-1, 1)),
    PRIMARY KEY (user_id, comment_id),
    CONSTRAINT fk_cvote_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_cvote_comment FOREIGN KEY (comment_id) REFERENCES comments(comment_id) ON DELETE CASCADE
);

-- =================================================================
-- ATTACHMENTS (POLYMORPHIC FOR ALL POST TYPES)
-- =================================================================
CREATE TABLE attachments (
    attachment_id SERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    post_id INT NOT NULL,
    post_type VARCHAR(50) NOT NULL -- e.g., 'QUESTION', 'ANSWER', 'COMMENT', 'MESSAGE'
);

-- =================================================================
-- MESSAGING
-- =================================================================
CREATE TABLE conversations (
    conversation_id SERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE conversation_participants (
    user_id INT NOT NULL,
    conversation_id INT NOT NULL,
    last_read_timestamp TIMESTAMP WITH TIME ZONE, -- ✅ ADDED: Tracks when a user last read messages in this conversation.
    PRIMARY KEY (user_id, conversation_id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE
);

CREATE TABLE messages (
    message_id SERIAL PRIMARY KEY,
    content TEXT,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE
);
-- =================================================================
-- NOTIFICATIONS
-- =================================================================
CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    recipient_id INT NOT NULL,
    actor_id INT, -- Can be NULL for system notifications (e.g., welcome messages)
    type VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    link VARCHAR(512) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_actor FOREIGN KEY (actor_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Add an index on the recipient_id for fast retrieval of a user's notifications.
CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);