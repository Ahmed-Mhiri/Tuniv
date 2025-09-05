-- =================================================================
-- ✅ PART 1: SCHEMA CREATION (DDL)
-- Tables are created in the correct order to satisfy foreign key constraints.
-- =================================================================

-- Base Tables (No Dependencies)
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

CREATE TABLE universities (
    university_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE conversations (
    conversation_id SERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Tables with Dependencies
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

CREATE TABLE conversation_participants (
    user_id INT NOT NULL,
    conversation_id INT NOT NULL,
    last_read_timestamp TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_id, conversation_id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    recipient_id INT NOT NULL,
    actor_id INT,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    link VARCHAR(512) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_actor FOREIGN KEY (actor_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Unified Posts Table
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    post_type VARCHAR(31) NOT NULL,
    user_id INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    body TEXT,
    title VARCHAR(255),
    module_id INT,
    is_solution BOOLEAN,
    question_id INT,
    answer_id INT,
    parent_comment_id INT,
    conversation_id INT,
    sent_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT false, -- ✅ ADDED: For soft-deleting messages
    CONSTRAINT fk_post_author FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_post_module FOREIGN KEY (module_id) REFERENCES modules(module_id),
    CONSTRAINT fk_post_question FOREIGN KEY (question_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_answer FOREIGN KEY (answer_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE
);

-- Votes, Attachments & Reactions (Depend on 'posts' and 'users' tables)
CREATE TABLE question_votes (
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    "value" SMALLINT NOT NULL CHECK ("value" IN (-1, 1)),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, question_id),
    CONSTRAINT fk_qvote_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_qvote_question FOREIGN KEY (question_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE answer_votes (
    user_id INT NOT NULL,
    answer_id INT NOT NULL,
    "value" SMALLINT NOT NULL CHECK ("value" IN (-1, 1)),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, answer_id),
    CONSTRAINT fk_avote_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_avote_answer FOREIGN KEY (answer_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE comment_votes (
    user_id INT NOT NULL,
    comment_id INT NOT NULL,
    "value" SMALLINT NOT NULL CHECK ("value" IN (-1, 1)),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, comment_id),
    CONSTRAINT fk_cvote_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_cvote_comment FOREIGN KEY (comment_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE attachments (
    attachment_id SERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    post_id INT NOT NULL,
    CONSTRAINT fk_attachment_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- ✅ ADDED: New table for message reactions
CREATE TABLE message_reactions (
    message_id INT NOT NULL,
    user_id INT NOT NULL,
    emoji VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id, emoji), -- Composite key to allow one user to add multiple different emojis
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- Indexes
CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_module_id ON posts(module_id);
CREATE INDEX idx_posts_question_id ON posts(question_id);
CREATE INDEX idx_posts_answer_id ON posts(answer_id);
CREATE INDEX idx_posts_post_type ON posts(post_type);
CREATE INDEX idx_attachments_post_id ON attachments(post_id);


-- =================================================================
-- ✅ PART 2: DATA INSERTION (DML)
-- Data is inserted in the correct order to satisfy foreign key constraints.
-- =================================================================

-- Base Data
INSERT INTO users (user_id, username, email, password, bio, major, reputation_score, is_enabled) VALUES
(1, 'amine_dev', 'amine@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', '2nd year CS student at INSAT, passionate about algorithms.', 'Computer Science', 25, true),
(2, 'sarra_prof', 'sarra@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', 'Professor of Database Systems at ENSI. Over 10 years of experience.', 'Database Systems', 150, true),
(3, 'yassin_student', 'yassin@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', 'Just trying to pass my exams at FST!', 'Physics', 10, true),
(4, 'ahmed', 'ahmed@test.com', '$2a$12$8etlMxbN1WWHTS5bKciPrO1LtE/YxJnfZk5lSfEN8FKmZAdk7czPW', 'New user exploring the platform.', 'General', 5, true);

INSERT INTO universities (university_id, name) VALUES
(1, 'Académie Militaire de Fondouk Jedid'),
(2, 'Académie Navale de Menzel Bourguiba'),
(3, 'Faculté de Droit de Sfax'),
(4, 'Faculté de Droit et des Sciences Politiques de Sousse'),
(5, 'Faculté de Droit et des Sciences Politiques de Tunis'),
(6, 'Faculté de Médecine Dentaire de Monastir'),
(7, 'Faculté de Médecine de Monastir'),
(8, 'Faculté de Médecine de Sfax'),
(9, 'Faculté de Médecine de Sousse'),
(10, 'Faculté de Médecine de Tunis'),
(11, 'Faculté de Pharmacie de Monastir'),
(12, 'Faculté des Lettres et des Sciences Humaines de Kairouan'),
(13, 'Faculté des Lettres et des Sciences Humaines de Sfax'),
(14, 'Faculté des Lettres et des Sciences Humaines de Sousse'),
(15, 'Faculté des Lettres, des Arts et des Humanités de la Manouba'),
(16, 'Faculté des Sciences Humaines et Sociales de Tunis'),
(17, 'Faculté des Sciences Juridiques, Politiques et Sociales de Tunis'),
(18, 'Faculté des Sciences Juridiques, Économiques et de Gestion de Jendouba'),
(19, 'Faculté des Sciences de Bizerte'),
(20, 'Faculté des Sciences de Gabès'),
(21, 'Faculté des Sciences de Gafsa'),
(22, 'Faculté des Sciences de Monastir'),
(23, 'Faculté des Sciences de Sfax'),
(24, 'Faculté des Sciences de Tunis'),
(25, 'Faculté des Sciences et Techniques de Sidi Bouzid'),
(26, 'Faculté des Sciences Économiques et de Gestion de Mahdia'),
(27, 'Faculté des Sciences Économiques et de Gestion de Nabeul'),
(28, 'Faculté des Sciences Économiques et de Gestion de Sfax'),
(29, 'Faculté des Sciences Économiques et de Gestion de Sousse'),
(30, 'Faculté des Sciences Économiques et de Gestion de Tunis'),
(31, 'Institut National de Technologie et des Sciences du Kef'),
(32, 'Institut National des Sciences Appliquées et de Technologie de Tunis'),
(33, 'Institut National du Travail et des Études Sociales de Tunis'),
(34, 'Institut Préparatoire aux Études Littéraires et de Sciences Humaines de Tunis'),
(35, 'Institut Préparatoire aux Études d''''Ingénieurs d''''El Manar'),
(36, 'Institut Préparatoire aux Études d''''Ingénieurs de Bizerte'),
(37, 'Institut Préparatoire aux Études d''''Ingénieurs de Gabès'),
(38, 'Institut Préparatoire aux Études d''''Ingénieurs de Gafsa'),
(39, 'Institut Préparatoire aux Études d''''Ingénieurs de Kairouan'),
(40, 'Institut Préparatoire aux Études d''''Ingénieurs de Monastir'),
(41, 'Institut Préparatoire aux Études d''''Ingénieurs de Nabeul'),
(42, 'Institut Préparatoire aux Études d''''Ingénieurs de Sfax'),
(43, 'Institut Préparatoire aux Études d''''Ingénieurs de Tunis'),
(44, 'Institut Supérieur Agronomique de Chott Mariem'),
(45, 'Institut Supérieur d''''Administration des Affaires de Sfax'),
(46, 'Institut Supérieur d''''Administration des Entreprises de Gafsa'),
(47, 'Institut Supérieur d''''Animation pour la Jeunesse et la Culture de Bir El Bey'),
(48, 'Institut Supérieur d''''Art Dramatique de Tunis'),
(49, 'Institut Supérieur d''''Informatique et de Gestion de Kairouan'),
(50, 'Institut Supérieur d''''Informatique et de Mathématiques de Monastir'),
(51, 'Institut Supérieur d''''Informatique et de Multimédia de Gabès'),
(52, 'Institut Supérieur d''''Informatique et de Multimédia de Sfax'),
(53, 'Institut Supérieur d''''Ingénierie Numérique de Tunis'),
(54, 'Institut Supérieur de Biologie Appliquée de Médenine'),
(55, 'Institut Supérieur de Biotechnologie de Béja'),
(56, 'Institut Supérieur de Biotechnologie de Monastir'),
(57, 'Institut Supérieur de Biotechnologie de Sfax'),
(58, 'Institut Supérieur de Biotechnologie de Sidi Thabet'),
(59, 'Institut Supérieur de Comptabilité et d''''Administration des Entreprises de Manouba'),
(60, 'Institut Supérieur de Documentation de Tunis'),
(61, 'Institut Supérieur de Finance et de Fiscalité de Sousse'),
(62, 'Institut Supérieur de Gestion de Bizerte'),
(63, 'Institut Supérieur de Gestion de Gabès'),
(64, 'Institut Supérieur de Gestion de Sousse'),
(65, 'Institut Supérieur de Gestion de Tunis'),
(66, 'Institut Supérieur de Musique de Sfax'),
(67, 'Institut Supérieur de Musique de Sousse'),
(68, 'Institut Supérieur de Musique de Tunis'),
(69, 'Institut Supérieur de Musique et de Théâtre du Kef'),
(70, 'Institut Supérieur de Théologie de Tunis'),
(71, 'Institut Supérieur de l''''Éducation Spécialisée'),
(72, 'Institut Supérieur de la Civilisation Islamique de Tunis'),
(73, 'Institut Supérieur de la Mode de Monastir'),
(74, 'Institut Supérieur des Arts du Multimédia de la Manouba'),
(75, 'Institut Supérieur des Arts et Métiers de Gabès'),
(76, 'Institut Supérieur des Arts et Métiers de Gafsa'),
(77, 'Institut Supérieur des Arts et Métiers de Kairouan'),
(78, 'Institut Supérieur des Arts et Métiers de Kasserine'),
(79, 'Institut Supérieur des Arts et Métiers de Mahdia'),
(80, 'Institut Supérieur des Arts et Métiers de Sfax'),
(81, 'Institut Supérieur des Arts et Métiers de Sidi Bouzid'),
(82, 'Institut Supérieur des Arts et Métiers de Siliana'),
(83, 'Institut Supérieur des Arts et Métiers de Tataouine'),
(84, 'Institut Supérieur des Beaux-Arts de Nabeul'),
(85, 'Institut Supérieur des Beaux-Arts de Sousse'),
(86, 'Institut Supérieur des Beaux-Arts de Tunis'),
(87, 'Institut Supérieur des Cadres de l''''Enfance de Carthage Dermech'),
(88, 'Institut Supérieur des Langues Appliquées et de l''''Informatique de Béja'),
(89, 'Institut Supérieur des Langues de Gabès'),
(90, 'Institut Supérieur des Langues de Moknine'),
(91, 'Institut Supérieur des Langues de Nabeul'),
(92, 'Institut Supérieur des Langues de Tunis'),
(93, 'Institut Supérieur des Métiers du Patrimoine de Tunis'),
(94, 'Institut Supérieur des Sciences Appliquées et de Technologie de Mahdia'),
(95, 'Institut Supérieur des Sciences Appliquées et de Technologie de Sousse'),
(96, 'Institut Supérieur des Sciences Biologiques Appliquées de Tunis'),
(97, 'Institut Supérieur des Sciences Humaines de Jendouba'),
(98, 'Institut Supérieur des Sciences Humaines de Médenine'),
(99, 'Institut Supérieur des Sciences Humaines de Tunis'),
(100, 'Institut Supérieur des Sciences Infirmières de Gabès'),
(101, 'Institut Supérieur des Sciences Infirmières de Sfax'),
(102, 'Institut Supérieur des Sciences Infirmières de Sousse'),
(103, 'Institut Supérieur des Sciences Infirmières de Tunis'),
(104, 'Institut Supérieur des Sciences Infirmières du Kef'),
(105, 'Institut Supérieur des Sciences Islamiques de Kairouan'),
(106, 'Institut Supérieur des Sciences Sociales et de l''''Éducation de Gafsa'),
(107, 'Institut Supérieur des Sciences de la Mer de Bizerte'),
(108, 'Institut Supérieur des Technologies Médicales de Tunis'),
(109, 'Institut Supérieur des Technologies de l''''Environnement, de l''''Urbanisme et du Bâtiment'),
(110, 'Institut Supérieur des Études Appliquées en Humanités de Gafsa'),
(111, 'Institut Supérieur des Études Appliquées en Humanités de Mahdia'),
(112, 'Institut Supérieur des Études Appliquées en Humanités de Sbeïtla'),
(113, 'Institut Supérieur des Études Appliquées en Humanités de Tozeur'),
(114, 'Institut Supérieur des Études Appliquées en Humanités de Zaghouan'),
(115, 'Institut Supérieur des Études Appliquées en Humanités du Kef'),
(116, 'Institut Supérieur des Études Juridiques de Gabès'),
(117, 'Institut Supérieur des Études Juridiques et Politiques de Kairouan'),
(118, 'Institut Supérieur des Études Préparatoires en Biologie et Géologie de la Soukra'),
(119, 'Institut Supérieur des Études Technologiques de Bizerte'),
(120, 'Institut Supérieur des Études Technologiques de Charguia'),
(121, 'Institut Supérieur des Études Technologiques de Djerba'),
(122, 'Institut Supérieur des Études Technologiques de Gafsa'),
(123, 'Institut Supérieur des Études Technologiques de Jendouba'),
(124, 'Institut Supérieur des Études Technologiques de Kairouan'),
(125, 'Institut Supérieur des Études Technologiques de Kasserine'),
(126, 'Institut Supérieur des Études Technologiques de Ksar Hellal'),
(127, 'Institut Supérieur des Études Technologiques de Kébili'),
(128, 'Institut Supérieur des Études Technologiques de Mahdia'),
(129, 'Institut Supérieur des Études Technologiques de Médenine'),
(130, 'Institut Supérieur des Études Technologiques de Nabeul'),
(131, 'Institut Supérieur des Études Technologiques de Radès'),
(132, 'Institut Supérieur des Études Technologiques de Sfax'),
(133, 'Institut Supérieur des Études Technologiques de Siliana'),
(134, 'Institut Supérieur des Études Technologiques de Sousse'),
(135, 'Institut Supérieur des Études Technologiques de Tataouine'),
(136, 'Institut Supérieur des Études Technologiques de Zaghouan'),
(137, 'Institut Supérieur des Études Technologiques du Kef'),
(138, 'Institut Supérieur des Études Touristiques de Sidi Dhrif'),
(139, 'Institut Supérieur du Sport et de l''''Éducation Physique de Gafsa'),
(140, 'Institut Supérieur du Sport et de l''''Éducation Physique de Ksar Saïd'),
(141, 'Institut Supérieur du Sport et de l''''Éducation Physique de Sfax'),
(142, 'Institut Supérieur du Sport et de l''''Éducation Physique du Kef'),
(143, 'Institut des Hautes Études Commerciales de Tunis'),
(144, 'École Nationale d''''Architecture et d''''Urbanisme de Tunis'),
(145, 'École Nationale d''''Ingénieurs de la Manouba'),
(146, 'École Supérieure d''''Agriculture de Mateur'),
(147, 'École Supérieure d''''Agriculture du Kef'),
(148, 'École Supérieure de Commerce de Manouba'),
(149, 'École Supérieure de Commerce de Sfax'),
(150, 'École Supérieure des Industries Alimentaires de Tunis'),
(151, 'École Supérieure des Sciences et Techniques de la Santé de Gafsa'),
(152, 'École Supérieure des Sciences et Techniques de la Santé de Monastir'),
(153, 'École Supérieure des Sciences et Techniques de la Santé de Sfax'),
(154, 'École Supérieure des Sciences et Techniques de la Santé de Sousse'),
(155, 'École Supérieure des Sciences et Techniques de la Santé de Tunis'),
(156, 'École Supérieure des Sciences et Technologies du Design'),
(157, 'École Supérieure des Sciences et de la Technologie de Hammam Sousse'),
(158, 'École Supérieure des Sciences Économiques et Commerciales de Tunis'),
(159, 'École de l''''Aviation de Borj El Amri'),
(160, 'École de la Santé Militaire');

-- Dependent Data
INSERT INTO modules (module_id, name, university_id) VALUES
(1, 'Algorithmique et Structures de Données', 1),
(2, 'Systèmes d''Exploitation', 1),
(3, 'Bases de Données', 2),
(4, 'Compilation', 2),
(5, 'Thermodynamique', 3);

INSERT INTO university_memberships (user_id, university_id, role) VALUES
(1, 1, 'student'),
(2, 2, 'professor'),
(3, 3, 'student'),
(4, 2, 'student');

-- Chat Data (MUST be inserted before Messages in the 'posts' table)
INSERT INTO conversations (conversation_id, created_at) VALUES (1, '2025-08-31 23:15:00+01');

INSERT INTO conversation_participants (conversation_id, user_id, last_read_timestamp) VALUES
(1, 1, '2025-08-31 23:20:00+01'),
(1, 2, '2025-08-31 23:21:00+01');

-- Posts Data (Questions, Answers, Comments, Messages)
INSERT INTO posts (id, post_type, title, body, user_id, module_id) VALUES
(1, 'QUESTION', 'How to efficiently implement Dijkstra''s algorithm for a dense graph?', 'I am working on a project for my ASD module at INSAT. I understand the basics of Dijkstra''s using a priority queue, but my graph has many edges. Is an adjacency matrix better than an adjacency list in this specific case? What are the time complexity implications?', 1, 1),
(2, 'QUESTION', 'What is the main difference between LEFT JOIN and INNER JOIN in SQL?', 'For my database class at ENSI, I am confused about when to use LEFT JOIN versus INNER JOIN. Can someone provide a simple, practical example that shows the difference in the result set?', 2, 3),
(3, 'QUESTION', 'What are the fundamental principles of process synchronization in an OS?', 'In my Operating Systems class at INSAT, we are covering semaphores and mutexes. Can someone explain the core problem that synchronization aims to solve, like the critical-section problem?', 1, 2),
(4, 'QUESTION', 'Can anyone provide a summary of the First Law of Thermodynamics?', 'I''m studying for my exam at FST and need a clear, concise explanation of the First Law of Thermodynamics, especially the formula ΔU = Q - W. An example would be very helpful!', 3, 5);

INSERT INTO posts (id, post_type, body, is_solution, question_id, user_id) VALUES
(5, 'ANSWER', 'For a dense graph, where the number of edges is close to V^2, using an adjacency matrix with a simple array to find the minimum distance vertex is often more efficient. While a priority queue with an adjacency list gives you O(E log V), the matrix approach gives you O(V^2). Since E is approximately V^2 in a dense graph, O(V^2) is better than O(V^2 log V).', false, 1, 2),
(6, 'ANSWER', 'Great question! Think of it this way: `INNER JOIN` only returns rows where the join condition is met in *both* tables. `LEFT JOIN` returns *all* rows from the left table, and the matched rows from the right table. If there is no match, the result is NULL on the right side. For example, if you LEFT JOIN Customers to Orders, you will get all customers, even those who have never placed an order.', false, 2, 2),
(7, 'ANSWER', 'To add to the excellent answer, `LEFT JOIN` is crucial for finding things that *don''t* have a match. For example, finding all users who have not yet posted a question. You would `LEFT JOIN users u ON questions q` and look for cases `WHERE q.id IS NULL`. An `INNER JOIN` could never show you those users.', false, 2, 1),
(8, 'ANSWER', 'This is correct. It represents the conservation of energy. Q is heat added to the system, and W is work done BY the system on its surroundings.', true, 4, 1);

INSERT INTO posts (id, post_type, body, answer_id, user_id, parent_comment_id) VALUES
(9, 'COMMENT', 'This is a very clear explanation, thank you! It really helped me understand the complexity trade-offs.', 5, 1, NULL),
(10, 'COMMENT', 'You''re welcome! Glad I could help clarify it.', 5, 2, 9);

-- ✅ MODIFIED: Added 'is_deleted' column for clarity, though it defaults to false.
INSERT INTO posts (id, post_type, body, conversation_id, user_id, created_at, is_deleted) VALUES
(11, 'MESSAGE', 'Hello Professor, I had a question about the Dijkstra explanation you provided.', 1, 1, '2025-08-31 23:16:00+01', false),
(12, 'MESSAGE', 'Of course, Amine. How can I help?', 1, 2, '2025-08-31 23:18:00+01', false),
(13, 'MESSAGE', 'In the case of a sparse graph, is it always better to use a priority queue?', 1, 1, '2025-08-31 23:19:00+01', false),
(14, 'MESSAGE', 'Yes, for sparse graphs, O(E log V) is significantly better than O(V^2). The priority queue is key.', 1, 2, '2025-08-31 23:25:00+01', false);

-- Attachments, Votes, and Reactions Data (Depends on Posts and Users)
INSERT INTO attachments (attachment_id, file_name, file_url, file_type, file_size, post_id) VALUES
(1, 'thermo_formulas.pdf', '/uploads/questions/thermo_formulas.pdf', 'application/pdf', 102400, 4),
(2, 'join_examples.sql', '/uploads/answers/join_examples.sql', 'application/sql', 5120, 7),
(3, 'clarification.png', '/uploads/comments/clarification.png', 'image/png', 20480, 10);

INSERT INTO question_votes (user_id, question_id, "value") VALUES
(2, 1, 1),
(4, 2, 1);

INSERT INTO answer_votes (user_id, answer_id, "value") VALUES
(1, 5, 1),
(3, 6, 1),
(4, 6, 1),
(1, 6, -1);

INSERT INTO comment_votes (user_id, comment_id, "value") VALUES
(3, 9, 1);

-- ✅ ADDED: Sample data for message reactions
INSERT INTO message_reactions (message_id, user_id, emoji) VALUES
(12, 1, '👍'), -- Amine thumbs-up the professor's reply
(13, 2, '❤️'); -- The professor loves Amine's follow-up question

-- Notifications Data
INSERT INTO notifications (recipient_id, actor_id, type, message, link, is_read, created_at) VALUES
(1, 2, 'NEW_ANSWER', '<strong>sarra_prof</strong> answered your question: "How to efficiently implement Dijkstra''s algorithm..."', '/questions/1?answerId=5', true, '2025-08-31 10:00:00+01'),
(2, 1, 'NEW_COMMENT_ON_ANSWER', '<strong>amine_dev</strong> commented on your answer for "How to efficiently implement Dijkstra''s algorithm..."', '/questions/1?answerId=5#comment-9', true, '2025-08-31 10:05:00+01'),
(1, 2, 'NEW_REPLY_TO_COMMENT', '<strong>sarra_prof</strong> replied to your comment.', '/questions/1?commentId=10#comment-10', false, '2025-08-31 10:10:00+01'),
(2, 1, 'NEW_ANSWER', '<strong>amine_dev</strong> answered your question: "What is the main difference between LEFT JOIN..."', '/questions/2?answerId=7', false, '2025-08-31 11:00:00+01'),
(2, 1, 'NEW_VOTE_ON_ANSWER', '<strong>amine_dev</strong> upvoted your answer.', '/questions/1?answerId=5', false, '2025-08-31 12:00:00+01'),
(1, 3, 'ANSWER_MARKED_AS_SOLUTION', 'Your answer to "Can anyone provide a summary of the First Law..." was marked as the solution!', '/questions/4?answerId=8', false, '2025-08-31 14:00:00+01'),
(4, 2, 'NEW_QUESTION_IN_UNI', 'A new question was asked in ENSI - École Nationale des Sciences de l''Informatique: "What is the main difference between LEFT JOIN..."', '/questions/2', false, '2025-08-31 15:00:00+01'),
(1, 2, 'NEW_CHAT_MESSAGE', 'You have a new message from <strong>sarra_prof</strong>.', '/chat/1', false, '2025-08-31 23:25:01+01');


-- =================================================================
-- ✅ PART 3: SEQUENCE SYNCHRONIZATION
-- This ensures the sequences are up-to-date after manual ID insertion.
-- =================================================================
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('universities_university_id_seq', (SELECT MAX(university_id) FROM universities));
SELECT setval('modules_module_id_seq', (SELECT MAX(module_id) FROM modules));
SELECT setval('posts_id_seq', (SELECT MAX(id) FROM posts));
SELECT setval('attachments_attachment_id_seq', (SELECT MAX(attachment_id) FROM attachments));
SELECT setval('conversations_conversation_id_seq', (SELECT MAX(conversation_id) FROM conversations));
SELECT setval('notifications_notification_id_seq', (SELECT MAX(notification_id) FROM notifications));