-- Allow the text content of a message to be null (for file-only messages)
ALTER TABLE messages ALTER COLUMN content DROP NOT NULL;

-- Add new columns to store file information
ALTER TABLE messages ADD COLUMN file_url VARCHAR(1024);
ALTER TABLE messages ADD COLUMN file_name VARCHAR(255);
ALTER TABLE messages ADD COLUMN file_type VARCHAR(100);