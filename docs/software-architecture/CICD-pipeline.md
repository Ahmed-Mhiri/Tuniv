graph TD
    A[Dev pushes to GitHub] --> B{GitHub Actions Triggered};

    subgraph "Backend CI/CD Pipeline"
        B -- Backend Code --> D[Build & Test];
        D --> E[Build & Push Docker Image];
        E --> F[Deploy to Staging];
        F --> G{Manual Approval};
        G --> FETCH_SECRETS[<b style='color:crimson'>Fetch Production Secrets <br> from Vault / Secrets Manager</b>];
        FETCH_SECRETS --> DEPLOY_PROD[Trigger Zero-Downtime Deploy to Prod];
    end