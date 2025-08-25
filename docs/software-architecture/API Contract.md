```mermaid
graph TD
    subgraph "API Contract"
        API_ROOT["/api/v1"]

        subgraph "Authentication & Profiles"
            API_ROOT --> AUTH["/auth (POST)"]
            API_ROOT --> ME["/me (GET, PUT)"]
            API_ROOT --> USERS["/users/{userId} (GET)"]
        end

        subgraph "University & Content Browsing"
            API_ROOT --> UNIVERSITIES["/universities (GET)"]
            UNIVERSITIES --> UNI_MODULES["/{uniId}/modules (GET)"]
            UNI_MODULES --> QUESTIONS_LIST["/{modId}/questions (GET)"]
            QUESTIONS_LIST --> Q_DETAIL["/{qId} (GET)"]
        end

        subgraph "Content Creation & Interaction"
            MODULES["/modules/{moduleId}/questions (POST)"]
            QUESTIONS["/questions/{questionId}/answers (POST)"]
            ANSWERS["/answers/{answerId}/comments (POST)"]
        end
        
        subgraph "Voting"
            VOTE_Q["/questions/{questionId}/vote (POST)"]
            VOTE_A["/answers/{answerId}/vote (POST)"]
            VOTE_C["/comments/{commentId}/vote (POST)"]
        end

        subgraph "File & Chat Services"
            UPLOAD["/files/upload (POST)"]
            CHAT["/conversations (GET, POST)"]
            CHAT --> MESSAGES["/{convoId}/messages (GET)"]
        end
    end