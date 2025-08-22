CREATE TABLE attachments (
    attachment_id SERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP NOT NULL,
    
    -- Polymorphic relationship columns
    post_id INT NOT NULL,
    post_type VARCHAR(50) NOT NULL -- Will store 'QUESTION', 'ANSWER', 'COMMENT', etc.
);

-- Index for faster lookups
CREATE INDEX idx_attachments_on_post ON attachments (post_id, post_type);