```mermaid
graph TD
    subgraph "End Users"
        direction LR
        UA[Web/Mobile Clients]
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
    
    UA -- HTTPS --> LB;
    LB --> AS1 & AS2;
    AS1 & AS2 <--> DB & Cache;
    AS1 & AS2 -- Send Data --> PRO & JAE;