```mermaid
graph TD
    subgraph "API Contract"
        API_ROOT["/api/v1"]

        subgraph "User Profiles & Memberships"
            API_ROOT --> ME["/me (GET, PUT)"]
            ME --> DESC_ME["Get or Update current user's profile"]
            API_ROOT --> USERS["/users/{userId} (GET)"]
            
            API_ROOT --> UNIVERSITIES["/universities"]
            UNIVERSITIES --> UNI_ID["/{universityId}"]
            UNI_ID --> MEMBERS["/members (GET, POST)"]
            MEMBERS --> DESC_MEMBERS["List members or Join university"]
        end

        subgraph "Q&A (Unchanged)"
            API_ROOT --> MODULES["/modules/{moduleId}/questions"]
        end
        
        subgraph "Direct Messaging (New)"
            API_ROOT --> CONVERSATIONS["/conversations"]
            CONVERSATIONS -- "GET, POST" --> LIST_CREATE_C["List conversations or Start new one"]
            CONVERSATIONS --> CONVO_ID["/{conversationId}"]
            CONVO_ID --> MESSAGES["/messages (GET, POST)"]
            MESSAGES --> LIST_CREATE_M["List messages or Send a new message"]
        end

        subgraph "Authentication"
            API_ROOT --> AUTH["/auth"]
        end
    end