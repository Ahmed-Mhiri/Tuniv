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

    subgraph "Cloud Provider VPC"
        LB[Load Balancer]
        
        subgraph "Auto-Scaling Group"
            AS1[App Server 1]
            AS2[App Server 2]
        end

        subgraph "Data Tier"
            DB[PostgreSQL Cluster]
            Cache[Redis Cluster]
        end

        subgraph Monitoring["Monitoring & Observability Stack"]
            style Monitoring fill:#e8f5e9
            PRO[Prometheus - Metrics]
            GRA[Grafana - Dashboards]
            JAE[Jaeger - Tracing]
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
    LB -- Distributes Traffic --> AS1 & AS2;
    AS1 & AS2 <--> DB & Cache;
    AS1 & AS2 -- File Uploads --> OS;
    AS1 & AS2 -- Search/Email --> Search & Email;
    AS1 & AS2 -- Send Telemetry --> Monitoring;