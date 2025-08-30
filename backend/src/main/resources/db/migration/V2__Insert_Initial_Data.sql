-- This script uses simple INSERTs, which are compatible with both PostgreSQL and H2.

-- =================================================================
-- SECTION 1: USERS
-- Passwords are pre-hashed with BCrypt. Plain text is 'password123'
-- =================================================================
INSERT INTO users (user_id, username, email, password, bio, major, reputation_score, is_enabled) VALUES
(1, 'amine_dev', 'amine@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', '2nd year CS student at INSAT, passionate about algorithms.', 'Computer Science', 25, true),
(2, 'sarra_prof', 'sarra@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', 'Professor of Database Systems at ENSI. Over 10 years of experience.', 'Database Systems', 150, true),
(3, 'yassin_student', 'yassin@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', 'Just trying to pass my exams at FST!', 'Physics', 10, true),
(4, 'ahmed', 'ahmed@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', 'New user exploring the platform.', 'General', 5, true);


-- =================================================================
-- SECTION 2: UNIVERSITIES
-- =================================================================
INSERT INTO universities (university_id, name) VALUES
(1, 'INSAT - Institut National des Sciences Appliquées et de Technologie'),
(2, 'ENSI - École Nationale des Sciences de l''Informatique'),
(3, 'FST - Faculté des Sciences de Tunis');


-- =================================================================
-- SECTION 3: MODULES
-- =================================================================
INSERT INTO modules (module_id, name, university_id) VALUES
(1, 'Algorithmique et Structures de Données', 1),
(2, 'Systèmes d''Exploitation', 1),
(3, 'Bases de Données', 2),
(4, 'Compilation', 2),
(5, 'Thermodynamique', 3);


-- =================================================================
-- SECTION 4: UNIVERSITY MEMBERSHIPS
-- =================================================================
INSERT INTO university_memberships (user_id, university_id, role) VALUES
(1, 1, 'student'),
(2, 2, 'professor'),
(3, 3, 'student'),
(4, 2, 'student'); -- Ahmed joins ENSI


-- =================================================================
-- SECTION 5: QUESTIONS
-- =================================================================
INSERT INTO questions (question_id, title, body, user_id, module_id) VALUES
(1, 'How to efficiently implement Dijkstra''s algorithm for a dense graph?', 'I am working on a project for my ASD module at INSAT. I understand the basics of Dijkstra''s using a priority queue, but my graph has many edges. Is an adjacency matrix better than an adjacency list in this specific case? What are the time complexity implications?', 1, 1),
(2, 'What is the main difference between LEFT JOIN and INNER JOIN in SQL?', 'For my database class at ENSI, I am confused about when to use LEFT JOIN versus INNER JOIN. Can someone provide a simple, practical example that shows the difference in the result set?', 2, 3),
(3, 'What are the fundamental principles of process synchronization in an OS?', 'In my Operating Systems class at INSAT, we are covering semaphores and mutexes. Can someone explain the core problem that synchronization aims to solve, like the critical-section problem?', 1, 2),
(4, 'Can anyone provide a summary of the First Law of Thermodynamics?', 'I''m studying for my exam at FST and need a clear, concise explanation of the First Law of Thermodynamics, especially the formula ΔU = Q - W. An example would be very helpful!', 3, 5);


-- =================================================================
-- SECTION 6: ANSWERS
-- =================================================================
INSERT INTO answers (answer_id, body, is_solution, question_id, user_id) VALUES
(1, 'For a dense graph, where the number of edges is close to V^2, using an adjacency matrix with a simple array to find the minimum distance vertex is often more efficient. While a priority queue with an adjacency list gives you O(E log V), the matrix approach gives you O(V^2). Since E is approximately V^2 in a dense graph, O(V^2) is better than O(V^2 log V).', false, 1, 2),
(2, 'Great question! Think of it this way: `INNER JOIN` only returns rows where the join condition is met in *both* tables. `LEFT JOIN` returns *all* rows from the left table, and the matched rows from the right table. If there is no match, the result is NULL on the right side. For example, if you LEFT JOIN Customers to Orders, you will get all customers, even those who have never placed an order.', true, 2, 2),
(3, 'To add to the excellent answer, `LEFT JOIN` is crucial for finding things that *don''t* have a match. For example, finding all users who have not yet posted a question. You would `LEFT JOIN users u ON questions q` and look for cases `WHERE q.id IS NULL`. An `INNER JOIN` could never show you those users.', false, 2, 1);


-- =================================================================
-- SECTION 7: COMMENTS
-- =================================================================
INSERT INTO comments (comment_id, body, answer_id, user_id, parent_comment_id) VALUES
(1, 'This is a very clear explanation, thank you! It really helped me understand the complexity trade-offs.', 1, 1, NULL),
(2, 'You''re welcome! Glad I could help clarify it.', 1, 2, 1); -- Reply to comment 1


-- =================================================================
-- SECTION 8: ATTACHMENTS
-- Assumes file URLs are placeholders.
-- =================================================================
INSERT INTO attachments (attachment_id, file_name, file_url, file_type, file_size, post_id, post_type) VALUES
(1, 'thermo_formulas.pdf', '/uploads/questions/thermo_formulas.pdf', 'application/pdf', 102400, 4, 'QUESTION'),
(2, 'join_examples.sql', '/uploads/answers/join_examples.sql', 'application/sql', 5120, 3, 'ANSWER'),
(3, 'clarification.png', '/uploads/comments/clarification.png', 'image/png', 20480, 2, 'COMMENT');


-- =================================================================
-- SECTION 9: CHAT DATA
-- =================================================================
-- Create a conversation between user 1 (amine_dev) and 2 (sarra_prof)
INSERT INTO conversations (conversation_id) VALUES (1);
INSERT INTO conversation_participants (conversation_id, user_id) VALUES (1, 1), (1, 2);

-- Add some messages to the conversation
INSERT INTO messages (message_id, conversation_id, sender_id, content) VALUES
(1, 1, 1, 'Hello Professor, I had a question about the Dijkstra explanation you provided.'),
(2, 1, 2, 'Of course, Amine. How can I help?'),
(3, 1, 1, 'In the case of a sparse graph, is it always better to use a priority queue?'),
(4, 1, 2, 'Yes, for sparse graphs, O(E log V) is significantly better than O(V^2). The priority queue is key.');


-- =================================================================
-- SECTION 10: VOTES
-- =================================================================
INSERT INTO question_votes (user_id, question_id, "value") VALUES 
(2, 1, 1),
(4, 2, 1);

INSERT INTO answer_votes (user_id, answer_id, "value") VALUES 
(1, 1, 1),
(3, 2, 1),
(4, 2, 1), -- Ahmed upvotes Sarra's good SQL answer
(1, 2, -1); -- Amine downvotes Sarra's answer (maybe he found a mistake)

INSERT INTO comment_votes (user_id, comment_id, "value") VALUES
(3, 1, 1);


-- =================================================================
-- SECTION 11: SEQUENCE SYNCHRONIZATION
-- =================================================================
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('universities_university_id_seq', (SELECT MAX(university_id) FROM universities));
SELECT setval('modules_module_id_seq', (SELECT MAX(module_id) FROM modules));
SELECT setval('questions_question_id_seq', (SELECT MAX(question_id) FROM questions));
SELECT setval('answers_answer_id_seq', (SELECT MAX(answer_id) FROM answers));
SELECT setval('comments_comment_id_seq', (SELECT MAX(comment_id) FROM comments));
SELECT setval('attachments_attachment_id_seq', (SELECT MAX(attachment_id) FROM attachments));
SELECT setval('conversations_conversation_id_seq', (SELECT MAX(conversation_id) FROM conversations));
SELECT setval('messages_message_id_seq', (SELECT MAX(message_id) FROM messages));