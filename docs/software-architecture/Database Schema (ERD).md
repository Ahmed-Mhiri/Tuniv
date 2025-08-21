```mermaid
erDiagram
    User {
        int userId PK
        string username UK
        string email UK
        string password_hash
        string profile_photo_url
        text bio
        string major
        int reputation_score
    }

    Question {
        int questionId PK
        string title
        int userId FK
        int moduleId FK
    }

    Answer {
        int answerId PK
        text body
        int questionId FK
        int userId FK
    }

    QuestionVote {
        int questionId PK, FK
        int userId PK, FK
        int value
    }

    AnswerVote {
        int answerId PK, FK
        int userId PK, FK
        int value
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

    User ||--|{ UniversityMembership : "is member of"
    University ||--|{ UniversityMembership : "has member"
    University ||--o{ Module : "offers"
    Module ||--o{ Question : "contains"
    User ||--o{ Question : "posts"
    User ||--o{ Answer : "provides"
    Question ||--o{ Answer : "has"
    User ||--o{ QuestionVote : "casts"
    Question ||--o{ QuestionVote : "receives"
    User ||--o{ AnswerVote : "casts"
    Answer ||--o{ AnswerVote : "receives"