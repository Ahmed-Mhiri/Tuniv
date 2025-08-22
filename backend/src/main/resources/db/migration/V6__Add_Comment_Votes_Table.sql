CREATE TABLE comment_votes (
    user_id INT NOT NULL,
    comment_id INT NOT NULL,
    value INT NOT NULL,
    PRIMARY KEY (user_id, comment_id),
    CONSTRAINT fk_cvote_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_cvote_comment FOREIGN KEY (comment_id) REFERENCES comments(comment_id)
);