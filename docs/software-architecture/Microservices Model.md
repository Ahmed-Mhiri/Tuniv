```mermaid
graph TD
    subgraph Clients
        C1[Web/Mobile Apps]
    end

    subgraph "Microservices Architecture (Final)"
        
        APIGateway_External[Dedicated API Gateway]
        
        subgraph Services
            MS1[Auth & User Service]
            MS2[University Service]
            MS3[Q&A Service]
            MS4[Vote Service]
            MS5[Notification Service]
            MS6[Chat Service]
            MS7[Attachment Service]
        end
        
        Broker[(Message Broker)]
        
        subgraph "External Dependencies"
            S3[Cloud Object Storage]
            EMAIL_SVC[Email Service]
        end

        C1 -- "REST & WebSocket" --> APIGateway_External

        APIGateway_External -- Routes Traffic --> MS1 & MS2 & MS3 & MS4 & MS5 & MS6 & MS7
        
        MS3 -- Publishes Events --> Broker
        Broker -- Delivers Events --> MS5 & MS6
        
        MS7 -- Uploads/Downloads --> S3
        MS5 -- Sends Emails --> EMAIL_SVC
    end