CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_photo_url VARCHAR(255),
    bio TEXT,
    major VARCHAR(255),
    reputation_score INT NOT NULL DEFAULT 0
);

CREATE TABLE universities (
    university_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE modules (
    module_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    university_id INT NOT NULL,
    CONSTRAINT fk_module_university FOREIGN KEY (university_id) REFERENCES universities(university_id)
);

CREATE TABLE university_memberships (
    user_id INT NOT NULL,
    university_id INT NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, university_id),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_membership_university FOREIGN KEY (university_id) REFERENCES universities(university_id)
);

CREATE TABLE questions (
    question_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    user_id INT NOT NULL,
    module_id INT NOT NULL,
    CONSTRAINT fk_question_author FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_question_module FOREIGN KEY (module_id) REFERENCES modules(module_id)
);

CREATE TABLE answers (
    answer_id SERIAL PRIMARY KEY,
    body TEXT NOT NULL,
    is_solution BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    question_id INT NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(question_id),
    CONSTRAINT fk_answer_author FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE question_votes (
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    "value" INT NOT NULL, -- <-- QUOTED
    PRIMARY KEY (user_id, question_id),
    CONSTRAINT fk_qvote_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_qvote_question FOREIGN KEY (question_id) REFERENCES questions(question_id)
);

CREATE TABLE answer_votes (
    user_id INT NOT NULL,
    answer_id INT NOT NULL,
    "value" INT NOT NULL, -- <-- QUOTED
    PRIMARY KEY (user_id, answer_id),
    CONSTRAINT fk_avote_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_avote_answer FOREIGN KEY (answer_id) REFERENCES answers(answer_id)
);

CREATE TABLE comments (
    comment_id SERIAL PRIMARY KEY,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    answer_id INT NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_comment_answer FOREIGN KEY (answer_id) REFERENCES answers(answer_id),
    CONSTRAINT fk_comment_author FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE comment_votes (
    user_id INT NOT NULL,
    comment_id INT NOT NULL,
    "value" INT NOT NULL, -- <-- QUOTED
    PRIMARY KEY (user_id, comment_id),
    CONSTRAINT fk_cvote_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_cvote_comment FOREIGN KEY (comment_id) REFERENCES comments(comment_id)
);

CREATE TABLE attachments (
    attachment_id SERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP NOT NULL,
    post_id INT NOT NULL,
    post_type VARCHAR(50) NOT NULL
);