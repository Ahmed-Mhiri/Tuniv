```mermaid

graph TD
    subgraph "Social Interaction & Messaging Flow"
        A[User finds a helpful answer] --> B[Clicks on the author's profile];
        B --> C[Views Profile <br> Sees Role, Reputation, etc.];
        C --> E[Clicks 'Send Message'];
        subgraph BackendInteraction["Backend Interaction"]
            E --> F(Client requests to start conversation);
            F --> I[API Gateway establishes <b>WebSocket</b> connection];
            I --> J[Connection is routed to the <b>Chat Service</b>];
        end
        J --> K[Chat window opens];
        K --> L[User sends message];
        L --> M(<b>Chat Service</b> pushes message in real-time);
        M --> O[Recipient gets a notification & message];
        
        style BackendInteraction fill:#e1f5fe
    end