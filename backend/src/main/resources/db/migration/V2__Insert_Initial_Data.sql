-- This script uses MERGE INTO for H2 compatibility in tests.
MERGE INTO users (user_id, username, email, password, bio, major, reputation_score) KEY(user_id) VALUES
(1, 'amine_dev', 'amine@test.com', '$2a$10$fS4a.x6g4s4Y9.L8A/Mi/..g97b.flRNCXoR.LzC1x9bB5g.Gv.yq', '2nd year CS student at INSAT, passionate about algorithms.', 'Computer Science', 25),
(2, 'sarra_prof', 'sarra@test.com', '$2a$10$fS4a.x6g4s4Y9.L8A/Mi/..g97b.flRNCXoR.LzC1x9bB5g.Gv.yq', 'Professor of Database Systems at ENSI. Over 10 years of experience.', 'Database Systems', 150),
(3, 'yassin_student', 'yassin@test.com', '$2a$10$fS4a.x6g4s4Y9.L8A/Mi/..g97b.flRNCXoR.LzC1x9bB5g.Gv.yq', 'Just trying to pass my exams at FST!', 'Physics', 10);

MERGE INTO universities (university_id, name) KEY(university_id) VALUES
(1, 'INSAT - Institut National des Sciences Appliquées et de Technologie'),
(2, 'ENSI - École Nationale des Sciences de l''Informatique'),
(3, 'FST - Faculté des Sciences de Tunis');

MERGE INTO modules (module_id, name, university_id) KEY(module_id) VALUES
(1, 'Algorithmique et Structures de Données', 1),
(2, 'Systèmes d''Exploitation', 1),
(3, 'Bases de Données', 2),
(4, 'Compilation', 2),
(5, 'Thermodynamique', 3);

MERGE INTO university_memberships (user_id, university_id, role) KEY(user_id, university_id) VALUES
(1, 1, 'student'),
(2, 2, 'professor'),
(3, 3, 'student');

MERGE INTO questions (question_id, title, body, created_at, user_id, module_id) KEY(question_id) VALUES
(1, 'How to efficiently implement Dijkstra''s algorithm for a dense graph?', 'I am working on a project for my ASD module at INSAT. I understand the basics of Dijkstra''s using a priority queue, but my graph has many edges. Is an adjacency matrix better than an adjacency list in this specific case? What are the time complexity implications?', '2025-08-22 10:00:00', 1, 1),
(2, 'What is the main difference between LEFT JOIN and INNER JOIN in SQL?', 'For my database class at ENSI, I am confused about when to use LEFT JOIN versus INNER JOIN. Can someone provide a simple, practical example that shows the difference in the result set?', '2025-08-22 10:15:00', 2, 3);

MERGE INTO answers (answer_id, body, is_solution, created_at, question_id, user_id) KEY(answer_id) VALUES
(1, 'For a dense graph, where the number of edges is close to V^2, using an adjacency matrix with a simple array to find the minimum distance vertex is often more efficient. While a priority queue with an adjacency list gives you O(E log V), the matrix approach gives you O(V^2). Since E is approximately V^2 in a dense graph, O(V^2) is better than O(V^2 log V).', false, '2025-08-22 10:25:00', 1, 2),
(2, 'Great question! Think of it this way: `INNER JOIN` only returns rows where the join condition is met in *both* tables. `LEFT JOIN` returns *all* rows from the left table, and the matched rows from the right table. If there is no match, the result is NULL on the right side. For example, if you LEFT JOIN Customers to Orders, you will get all customers, even those who have never placed an order.', false, '2025-08-22 10:20:00', 2, 2);

MERGE INTO question_votes (user_id, question_id, "value") KEY(user_id, question_id) VALUES (2, 1, 1);
MERGE INTO answer_votes (user_id, answer_id, "value") KEY(user_id, answer_id) VALUES (1, 1, 1);
MERGE INTO answer_votes (user_id, answer_id, "value") KEY(user_id, answer_id) VALUES (3, 2, 1);

-- Note: The setval commands are not needed with Flyway as it manages a clean schema.