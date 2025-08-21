```mermaid
graph TD
    subgraph "End Users"
        direction LR
        UA[Web Client - Angular]
        UM1[Android App - Kotlin]
        UM2[iOS App - Swift]
    end

    subgraph "Global Infrastructure"
        CDN[CDN & WAF - e.g., Cloudflare]
    end

    subgraph "Cloud Provider VPC - e.g., AWS, GCP, DO"
        LB[Load Balancer]
        
        subgraph "Auto-Scaling Group"
            AS1[App Server 1 - Spring Boot Container]
            AS2[App Server 2 - Spring Boot Container]
            AS3[...]
        end

        subgraph "Data Tier"
            DB[Managed PostgreSQL DB]
            Cache[Managed Redis Cache]
        end
    end
    
    subgraph "External Services"
        OS[Object Storage - S3 / Spaces]
        Search[Search Service - Algolia]
        Email[Email Service - SendGrid]
    end

    UA -- HTTPS --> CDN;
    UM1 -- HTTPS --> CDN;
    UM2 -- HTTPS --> CDN;
    CDN -- Forwards Traffic --> LB;
    LB -- Distributes Traffic --> AS1;
    LB -- Distributes Traffic --> AS2;
    LB -- Distributes Traffic --> AS3;
    AS1 <--> DB;
    AS2 <--> DB;
    AS1 <--> Cache;
    AS2 <--> Cache;
    AS1 -- File Uploads/Downloads --> OS;
    AS2 -- Search Indexing --> Search;
    AS2 -- Sends Emails --> Email;