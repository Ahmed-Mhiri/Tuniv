CREATE TABLE comments (
    comment_id SERIAL PRIMARY KEY,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    answer_id INT NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_comment_answer FOREIGN KEY (answer_id) REFERENCES answers(answer_id),
    CONSTRAINT fk_comment_author FOREIGN KEY (user_id) REFERENCES users(user_id)
);