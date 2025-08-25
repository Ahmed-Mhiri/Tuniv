```mermaid
erDiagram
    User {
        int userId PK
        string username UK
        string email UK
        string password
        string profile_photo_url
        text bio
        string major
        int reputation_score
    }

    University {
        int universityId PK
        string name
    }

    Module {
        int moduleId PK
        string name
        int universityId FK
    }

    UniversityMembership {
        int userId PK, FK
        int universityId PK, FK
        string role
    }

    Question {
        int questionId PK
        string title
        text body
        datetime created_at
        int userId FK
        int moduleId FK
    }

    Answer {
        int answerId PK
        text body
        boolean is_solution
        datetime created_at
        int questionId FK
        int userId FK
    }

    Comment {
        int commentId PK
        text body
        datetime created_at
        int answerId FK
        int userId FK
    }

    QuestionVote {
        int userId PK, FK
        int questionId PK, FK
        int value
    }
    AnswerVote {
        int userId PK, FK
        int answerId PK, FK
        int value
    }
    CommentVote {
        int userId PK, FK
        int commentId PK, FK
        int value
    }

    Attachment {
        int attachmentId PK
        string file_name
        string file_url
        string file_type
        long file_size
        datetime uploaded_at
        int post_id
        string post_type
    }

    Conversation {
        int conversationId PK
        datetime created_at
    }
    ConversationParticipant {
        int userId PK, FK
        int conversationId PK, FK
    }
    Message {
        int messageId PK
        text content
        datetime sent_at
        int conversationId FK
        int senderId FK
        string file_url
    }

    User ||--|{ UniversityMembership : "is member of"
    University ||--|{ UniversityMembership : "has member"
    University ||--o{ Module : "offers"
    Module ||--o{ Question : "contains"
    User ||--o{ Question : "posts"
    User ||--o{ Answer : "provides"
    Question ||--o{ Answer : "has"
    Answer ||--o{ Comment : "has"
    User ||--o{ Comment : "posts"
    User ||--o{ QuestionVote : "casts"
    Question ||--o{ QuestionVote : "receives"
    User ||--o{ AnswerVote : "casts"
    Answer ||--o{ AnswerVote : "receives"
    User ||--o{ CommentVote : "casts"
    Comment ||--o{ CommentVote : "receives"
    User }o--o{ ConversationParticipant : "participates in"
    Conversation ||--o{ ConversationParticipant : "has"
    Conversation ||--o{ Message : "contains"
    User ||--o{ Message : "sends"