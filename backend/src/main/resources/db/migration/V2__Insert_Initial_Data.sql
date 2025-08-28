-- This script uses simple INSERTs, which are compatible with both PostgreSQL and H2.

-- =================================================================
-- SECTION 1: USERS
-- Passwords are pre-hashed with BCrypt. Plain text is 'password123'
-- =================================================================
-- FIX: Explicitly set is_enabled=true for seed users so they can log in immediately.
INSERT INTO users (user_id, username, email, password, bio, major, reputation_score, is_enabled) VALUES
(1, 'amine_dev', 'amine@test.com', '$2a$10$fS4a.x6g4s4Y9.L8A/Mi/..g97b.flRNCXoR.LzC1x9bB5g.Gv.yq', '2nd year CS student at INSAT, passionate about algorithms.', 'Computer Science', 25, true),
(2, 'sarra_prof', 'sarra@test.com', '$2a$10$fS4a.x6g4s4Y9.L8A/Mi/..g97b.flRNCXoR.LzC1x9bB5g.Gv.yq', 'Professor of Database Systems at ENSI. Over 10 years of experience.', 'Database Systems', 150, true),
(3, 'yassin_student', 'yassin@test.com', '$2a$10$fS4a.x6g4s4Y9.L8A/Mi/..g97b.flRNCXoR.LzC1x9bB5g.Gv.yq', 'Just trying to pass my exams at FST!', 'Physics', 10, true);


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
(3, 3, 'student');


-- =================================================================
-- SECTION 5: QUESTIONS
-- REFINEMENT: Removed created_at to let the database handle it with DEFAULT NOW().
-- =================================================================
INSERT INTO questions (question_id, title, body, user_id, module_id) VALUES
(1, 'How to efficiently implement Dijkstra''s algorithm for a dense graph?', 'I am working on a project for my ASD module at INSAT. I understand the basics of Dijkstra''s using a priority queue, but my graph has many edges. Is an adjacency matrix better than an adjacency list in this specific case? What are the time complexity implications?', 1, 1),
(2, 'What is the main difference between LEFT JOIN and INNER JOIN in SQL?', 'For my database class at ENSI, I am confused about when to use LEFT JOIN versus INNER JOIN. Can someone provide a simple, practical example that shows the difference in the result set?', 2, 3);


-- =================================================================
-- SECTION 6: ANSWERS
-- REFINEMENT: Removed created_at to let the database handle it with DEFAULT NOW().
-- =================================================================
INSERT INTO answers (answer_id, body, is_solution, question_id, user_id) VALUES
(1, 'For a dense graph, where the number of edges is close to V^2, using an adjacency matrix with a simple array to find the minimum distance vertex is often more efficient. While a priority queue with an adjacency list gives you O(E log V), the matrix approach gives you O(V^2). Since E is approximately V^2 in a dense graph, O(V^2) is better than O(V^2 log V).', false, 1, 2),
(2, 'Great question! Think of it this way: `INNER JOIN` only returns rows where the join condition is met in *both* tables. `LEFT JOIN` returns *all* rows from the left table, and the matched rows from the right table. If there is no match, the result is NULL on the right side. For example, if you LEFT JOIN Customers to Orders, you will get all customers, even those who have never placed an order.', false, 2, 2);


-- =================================================================
-- SECTION 7: VOTES
-- =================================================================
INSERT INTO question_votes (user_id, question_id, "value") VALUES (2, 1, 1);
INSERT INTO answer_votes (user_id, answer_id, "value") VALUES (1, 1, 1);
INSERT INTO answer_votes (user_id, answer_id, "value") VALUES (3, 2, 1);

-- =================================================================
-- SECTION 8: SEQUENCE SYNCHRONIZATION
-- All sequences with manual ID inserts are now synchronized.
-- =================================================================
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('universities_university_id_seq', (SELECT MAX(university_id) FROM universities));
SELECT setval('modules_module_id_seq', (SELECT MAX(module_id) FROM modules));
SELECT setval('questions_question_id_seq', (SELECT MAX(question_id) FROM questions));
SELECT setval('answers_answer_id_seq', (SELECT MAX(answer_id) FROM answers));
-- SELECT setval('comments_comment_id_seq', (SELECT MAX(comment_id) FROM comments));