```mermaid

graph TD
    subgraph Core["Ask a Question Flow"]
        A[User is on Homepage] -->|Clicks 'Ask Question'| B{Is User Logged In?};
        B -- No --> C[Login/Register];
        C --> E;
        B -- Yes --> E[Show Question Editor Page];
        E --> F[Selects University/Module, enters Title, Body];
        F --> G{Submit};
        G --> I(Backend: Validate & Save);
        I --> N[Redirect to Question Page];
        N --> O[Question is live];
    end

    subgraph Answering["Answering & Resolution Flow"]
        O --> P[Another user finds the question];
        P --> R[Writes a detailed answer];
        R --> S{Submit Answer};
        S --> T(Backend: Save & Notify OP);
        T --> V[Original poster reviews answers];
        V --> W{Is answer helpful?};
        W -- Yes --> Y[Upvotes & Marks as Solution];
        W -- No --> Y2[Downvotes or Reports];
    end