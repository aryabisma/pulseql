# Deployment Architecture for Pulsar-PulseQL Integration

## Executive Summary

This document outlines deployment architecture options for integrating PulseQL with Pulsar, covering infrastructure requirements, deployment patterns, scaling strategies, and operational considerations.

## Deployment Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Load Balancer                        │
│                   (nginx/HAProxy)                        │
└────────────┬────────────────────────────┬────────────────┘
             │                            │
             │                            │
    ┌────────▼────────┐          ┌───────▼────────┐
    │     Pulsar      │          │    PulseQL     │
    │   Application   │◄────────►│  Application   │
    │   (Java/Node)   │   API    │     (Java)     │
    └────────┬────────┘          └───────┬────────┘
             │                            │
             │                            │
    ┌────────▼────────┐          ┌───────▼────────┐
    │  Pulsar DB      │          │  PulseQL DB    │
    │  (PostgreSQL)   │          │  (PostgreSQL)  │
    └─────────────────┘          └────────────────┘
```

## Deployment Options

### Option 1: Separate Deployment (Recommended)

Deploy Pulsar and PulseQL as independent applications with API integration.

**Architecture**:

```
Internet
    │
    ├─→ pulsar.example.com → Pulsar App (Port 8080)
    │                            ├─→ Pulsar DB
    │                            └─→ Redis (Sessions)
    │
    └─→ pulseql.example.com → PulseQL App (Port 8978)
                                 ├─→ PulseQL DB
                                 └─→ Target Databases
```

**Advantages**:
- Independent scaling
- Separate deployment cycles
- Isolated failures
- Easy rollback
- Clear separation of concerns

**Disadvantages**:
- More infrastructure to manage
- Additional network hops
- CORS configuration needed

**Configuration**:

```yaml
# docker-compose-separate.yml
version: '3.8'

services:
  # Pulsar application
  pulsar:
    image: pulsar:latest
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=pulsar-db
      - PULSEQL_URL=http://pulseql:8978
      - PULSEQL_SSO_SECRET=${SSO_SECRET}
    depends_on:
      - pulsar-db
      - redis
    networks:
      - pulsar-network
  
  pulsar-db:
    image: postgres:15
    environment:
      - POSTGRES_DB=pulsar
      - POSTGRES_USER=pulsar
      - POSTGRES_PASSWORD=${PULSAR_DB_PASSWORD}
    volumes:
      - pulsar-db-data:/var/lib/postgresql/data
    networks:
      - pulsar-network
  
  redis:
    image: redis:7-alpine
    networks:
      - pulsar-network
  
  # PulseQL application
  pulseql:
    image: dbeaver/cloudbeaver:latest
    ports:
      - "8978:8978"
    environment:
      - CB_SERVER_NAME=PulseQL
      - CB_ADMIN_NAME=admin
      - CB_ADMIN_PASSWORD=${PULSEQL_ADMIN_PASSWORD}
      - PULSAR_BASE_URL=http://pulsar:8080
      - PULSAR_SSO_SECRET=${SSO_SECRET}
    volumes:
      - pulseql-workspace:/opt/cloudbeaver/workspace
      - ./pulseql-config:/opt/cloudbeaver/conf
    depends_on:
      - pulseql-db
    networks:
      - pulsar-network
      - pulseql-network
  
  pulseql-db:
    image: postgres:15
    environment:
      - POSTGRES_DB=pulseql
      - POSTGRES_USER=pulseql
      - POSTGRES_PASSWORD=${PULSEQL_DB_PASSWORD}
    volumes:
      - pulseql-db-data:/var/lib/postgresql/data
    networks:
      - pulseql-network
  
  # Reverse proxy
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./certs:/etc/nginx/certs
    depends_on:
      - pulsar
      - pulseql
    networks:
      - pulsar-network

networks:
  pulsar-network:
  pulseql-network:

volumes:
  pulsar-db-data:
  pulseql-workspace:
  pulseql-db-data:
```

### Option 2: Co-located Deployment

Deploy both applications on the same server/container host.

**Architecture**:

```
Server (example.com)
    │
    ├─→ :8080 → Pulsar App
    │              └─→ Shared DB (PostgreSQL)
    │
    └─→ :8978 → PulseQL App
                   └─→ Shared DB (PostgreSQL)
```

**Advantages**:
- Simpler infrastructure
- Lower latency between apps
- Shared resources possible
- Easier local development

**Disadvantages**:
- Resource contention
- No independent scaling
- Coupled deployments
- Single point of failure

### Option 3: Kubernetes Deployment (Enterprise)

Deploy both applications in Kubernetes for production scalability.

**Architecture**:

```yaml
# kubernetes-deployment.yml
---
# Pulsar Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pulsar
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pulsar
  template:
    metadata:
      labels:
        app: pulsar
    spec:
      containers:
      - name: pulsar
        image: pulsar:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_HOST
          value: pulsar-db-service
        - name: PULSEQL_URL
          value: http://pulseql-service:8978
        - name: PULSEQL_SSO_SECRET
          valueFrom:
            secretKeyRef:
              name: sso-secrets
              key: shared-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 5

---
# Pulsar Service
apiVersion: v1
kind: Service
metadata:
  name: pulsar-service
  namespace: production
spec:
  type: LoadBalancer
  selector:
    app: pulsar
  ports:
  - port: 80
    targetPort: 8080

---
# PulseQL Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pulseql
  namespace: production
spec:
  replicas: 2
  selector:
    matchLabels:
      app: pulseql
  template:
    metadata:
      labels:
        app: pulseql
    spec:
      containers:
      - name: pulseql
        image: dbeaver/cloudbeaver:latest
        ports:
        - containerPort: 8978
        env:
        - name: CB_SERVER_NAME
          value: PulseQL
        - name: PULSAR_BASE_URL
          value: http://pulsar-service
        - name: PULSAR_SSO_SECRET
          valueFrom:
            secretKeyRef:
              name: sso-secrets
              key: shared-secret
        volumeMounts:
        - name: workspace
          mountPath: /opt/cloudbeaver/workspace
        - name: config
          mountPath: /opt/cloudbeaver/conf
        resources:
          requests:
            memory: "1Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "4000m"
        livenessProbe:
          httpGet:
            path: /api/health
            port: 8978
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/ready
            port: 8978
          initialDelaySeconds: 30
          periodSeconds: 5
      volumes:
      - name: workspace
        persistentVolumeClaim:
          claimName: pulseql-workspace-pvc
      - name: config
        configMap:
          name: pulseql-config

---
# PulseQL Service
apiVersion: v1
kind: Service
metadata:
  name: pulseql-service
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: pulseql
  ports:
  - port: 8978
    targetPort: 8978

---
# PulseQL Persistent Volume Claim
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pulseql-workspace-pvc
  namespace: production
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 50Gi
  storageClassName: standard

---
# ConfigMap for PulseQL
apiVersion: v1
kind: ConfigMap
metadata:
  name: pulseql-config
  namespace: production
data:
  cloudbeaver.conf: |
    {
      "server": {
        "serverPort": 8978,
        "serverName": "PulseQL",
        "workspaceLocation": "workspace",
        "contentRoot": "web",
        "driversLocation": "drivers",
        "enabledAuthProviders": [
          "local",
          "pulsar-sso"
        ]
      },
      "app": {
        "anonymousAccessEnabled": false,
        "anonymousUserRole": "user",
        "authenticationEnabled": true,
        "enabledFeatures": [
          "sql-editor",
          "data-viewer"
        ],
        "disabledFeatures": [
          "connection-create",
          "user-management"
        ]
      }
    }

---
# Secrets
apiVersion: v1
kind: Secret
metadata:
  name: sso-secrets
  namespace: production
type: Opaque
data:
  shared-secret: <base64-encoded-secret>

---
# Ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pulsar-pulseql-ingress
  namespace: production
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - pulsar.example.com
    - pulseql.example.com
    secretName: pulsar-tls
  rules:
  - host: pulsar.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: pulsar-service
            port:
              number: 80
  - host: pulseql.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: pulseql-service
            port:
              number: 8978
```

**Advantages**:
- Automatic scaling
- Self-healing
- Rolling updates
- Resource optimization
- High availability
- Load balancing

**Disadvantages**:
- Complex setup
- Requires Kubernetes expertise
- Additional overhead
- Higher cost

## Infrastructure Requirements

### Minimum Requirements

**Pulsar Server**:
- CPU: 2 cores
- RAM: 4 GB
- Disk: 20 GB SSD
- Network: 100 Mbps

**PulseQL Server**:
- CPU: 2 cores
- RAM: 4 GB
- Disk: 50 GB SSD (for workspace)
- Network: 100 Mbps

**Database Servers** (if separate):
- CPU: 2 cores
- RAM: 4 GB
- Disk: 100 GB SSD
- Network: 1 Gbps

### Recommended Production Requirements

**Pulsar Server** (per instance):
- CPU: 4 cores
- RAM: 8 GB
- Disk: 50 GB SSD
- Network: 1 Gbps

**PulseQL Server** (per instance):
- CPU: 4 cores
- RAM: 8 GB
- Disk: 100 GB SSD
- Network: 1 Gbps

**Database Servers**:
- CPU: 8 cores
- RAM: 16 GB
- Disk: 500 GB SSD (NVMe preferred)
- Network: 10 Gbps
- Backup: Daily automated backups

**Load Balancer**:
- CPU: 2 cores
- RAM: 4 GB
- Network: 10 Gbps

## Network Architecture

### Network Configuration

```
┌─────────────────────────────────────────────────────┐
│                  Internet (Public)                   │
└─────────────────────┬───────────────────────────────┘
                      │
                      │ HTTPS (443)
                      │
┌─────────────────────▼───────────────────────────────┐
│              Load Balancer / WAF                     │
│              (SSL Termination)                       │
└──────────┬──────────────────────┬────────────────────┘
           │                      │
           │ HTTP (internal)      │ HTTP (internal)
           │                      │
┌──────────▼──────────┐  ┌────────▼────────────┐
│  Pulsar App Tier    │  │  PulseQL App Tier   │
│  (DMZ Network)      │  │  (DMZ Network)      │
│  10.0.1.0/24        │  │  10.0.2.0/24        │
└──────────┬──────────┘  └────────┬────────────┘
           │                      │
           │ PostgreSQL (5432)    │ PostgreSQL (5432)
           │                      │
┌──────────▼──────────┐  ┌────────▼────────────┐
│  Pulsar DB          │  │  PulseQL DB         │
│  (Private Network)  │  │  (Private Network)  │
│  10.0.10.0/24       │  │  10.0.11.0/24       │
└─────────────────────┘  └─────────────────────┘
                              │
                              │ DB Connections
                              │ (various ports)
                              │
                    ┌─────────▼──────────┐
                    │  Target Databases  │
                    │  (Private Network) │
                    │  10.0.20.0/24      │
                    └────────────────────┘
```

### Firewall Rules

**Inbound Rules**:

```bash
# Load Balancer
- Port 80 (HTTP) from Internet → Redirect to 443
- Port 443 (HTTPS) from Internet → Allow

# Pulsar App Tier
- Port 8080 from Load Balancer → Allow
- Port 8080 from PulseQL → Allow (API calls)

# PulseQL App Tier
- Port 8978 from Load Balancer → Allow
- Port 8978 from Pulsar → Allow (API calls)

# Database Tier
- Port 5432 from Pulsar App → Allow
- Port 5432 from PulseQL App → Allow

# Target Databases
- Various ports from PulseQL App → Allow (as configured)
```

**Outbound Rules**:

```bash
# Pulsar App Tier
- Port 8978 to PulseQL → Allow (SSO, API)
- Port 5432 to Pulsar DB → Allow
- Port 443 to Internet → Allow (updates, external APIs)

# PulseQL App Tier
- Port 8080 to Pulsar → Allow (session validation)
- Port 5432 to PulseQL DB → Allow
- Various ports to Target DBs → Allow
- Port 443 to Internet → Allow (driver downloads)
```

## Load Balancing

### Nginx Configuration

```nginx
# nginx.conf
upstream pulsar_backend {
    least_conn;
    server pulsar-1:8080 max_fails=3 fail_timeout=30s;
    server pulsar-2:8080 max_fails=3 fail_timeout=30s;
    server pulsar-3:8080 max_fails=3 fail_timeout=30s;
}

upstream pulseql_backend {
    ip_hash;  # Sticky sessions for WebSocket support
    server pulseql-1:8978 max_fails=3 fail_timeout=30s;
    server pulseql-2:8978 max_fails=3 fail_timeout=30s;
}

# Pulsar domain
server {
    listen 80;
    server_name pulsar.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name pulsar.example.com;
    
    ssl_certificate /etc/nginx/certs/pulsar.crt;
    ssl_certificate_key /etc/nginx/certs/pulsar.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    client_max_body_size 100M;
    
    location / {
        proxy_pass http://pulsar_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Health check endpoint
    location /health {
        access_log off;
        proxy_pass http://pulsar_backend/health;
    }
}

# PulseQL domain
server {
    listen 80;
    server_name pulseql.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name pulseql.example.com;
    
    ssl_certificate /etc/nginx/certs/pulseql.crt;
    ssl_certificate_key /etc/nginx/certs/pulseql.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    client_max_body_size 500M;  # Larger for data exports
    
    # CORS headers for Pulsar integration
    add_header Access-Control-Allow-Origin "https://pulsar.example.com" always;
    add_header Access-Control-Allow-Methods "GET, POST, OPTIONS" always;
    add_header Access-Control-Allow-Headers "Authorization, Content-Type" always;
    add_header Access-Control-Allow-Credentials "true" always;
    
    location / {
        proxy_pass http://pulseql_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Longer timeouts for long-running queries
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }
    
    # GraphQL endpoint
    location /api/gql {
        proxy_pass http://pulseql_backend/api/gql;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # CORS for GraphQL
        if ($request_method = OPTIONS) {
            return 204;
        }
    }
    
    # Health check endpoint
    location /api/health {
        access_log off;
        proxy_pass http://pulseql_backend/api/health;
    }
}
```

## High Availability

### Database Replication

**PostgreSQL Streaming Replication**:

```bash
# Primary database configuration
# postgresql.conf
wal_level = replica
max_wal_senders = 3
wal_keep_size = 1GB
hot_standby = on

# pg_hba.conf
host replication replicator 10.0.10.0/24 md5
```

**Automatic Failover with Patroni**:

```yaml
# patroni.yml
scope: pulsar-cluster
namespace: /db/
name: pulsar-db-1

restapi:
  listen: 0.0.0.0:8008
  connect_address: 10.0.10.10:8008

etcd:
  hosts: etcd-1:2379,etcd-2:2379,etcd-3:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    postgresql:
      use_pg_rewind: true
      parameters:
        max_connections: 200
        shared_buffers: 2GB
        effective_cache_size: 6GB

postgresql:
  listen: 0.0.0.0:5432
  connect_address: 10.0.10.10:5432
  data_dir: /var/lib/postgresql/15/main
  pgpass: /tmp/pgpass
  authentication:
    replication:
      username: replicator
      password: ${REPLICATION_PASSWORD}
    superuser:
      username: postgres
      password: ${POSTGRES_PASSWORD}
```

### Application HA

**Health Checks**:

```java
// Pulsar health check endpoint
@RestController
public class HealthCheckController {
    
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        HealthStatus status = HealthStatus.builder()
            .status("UP")
            .timestamp(Instant.now())
            .checks(performHealthChecks())
            .build();
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/ready")
    public ResponseEntity<ReadinessStatus> ready() {
        boolean ready = checkDatabase() && 
                       checkPulseQLConnection() &&
                       checkRedis();
        
        if (ready) {
            return ResponseEntity.ok(new ReadinessStatus("READY"));
        }
        return ResponseEntity.status(503)
            .body(new ReadinessStatus("NOT_READY"));
    }
}
```

## Monitoring and Observability

### Metrics Collection

**Prometheus Configuration**:

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'pulsar'
    static_configs:
      - targets: ['pulsar-1:8080', 'pulsar-2:8080', 'pulsar-3:8080']
    metrics_path: /actuator/prometheus
  
  - job_name: 'pulseql'
    static_configs:
      - targets: ['pulseql-1:8978', 'pulseql-2:8978']
    metrics_path: /api/metrics
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx:9113']
```

**Grafana Dashboards**:

```json
{
  "dashboard": {
    "title": "Pulsar-PulseQL Integration",
    "panels": [
      {
        "title": "SSO Authentication Success Rate",
        "targets": [
          {
            "expr": "rate(pulseql_sso_auth_success_total[5m]) / rate(pulseql_sso_auth_total[5m])"
          }
        ]
      },
      {
        "title": "Active User Sessions",
        "targets": [
          {
            "expr": "pulseql_active_sessions{auth_provider='pulsar-sso'}"
          }
        ]
      },
      {
        "title": "Query Execution Time",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(pulseql_query_duration_seconds_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Permission Check Failures",
        "targets": [
          {
            "expr": "rate(pulseql_permission_denied_total[5m])"
          }
        ]
      }
    ]
  }
}
```

### Logging

**Centralized Logging with ELK Stack**:

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/pulsar/*.log
    fields:
      app: pulsar
      env: production
  
  - type: log
    enabled: true
    paths:
      - /opt/cloudbeaver/workspace/.metadata/.log
    fields:
      app: pulseql
      env: production

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "logs-%{[fields.app]}-%{+yyyy.MM.dd}"

processors:
  - add_host_metadata: ~
  - add_cloud_metadata: ~
```

## Backup and Disaster Recovery

### Backup Strategy

**Database Backups**:

```bash
#!/bin/bash
# backup-databases.sh

# Pulsar database backup
pg_dump -h pulsar-db -U pulsar pulsar | \
  gzip > /backups/pulsar-$(date +%Y%m%d-%H%M%S).sql.gz

# PulseQL database backup
pg_dump -h pulseql-db -U pulseql pulseql | \
  gzip > /backups/pulseql-$(date +%Y%m%d-%H%M%S).sql.gz

# Workspace backup
tar -czf /backups/pulseql-workspace-$(date +%Y%m%d-%H%M%S).tar.gz \
  /opt/cloudbeaver/workspace

# Upload to S3
aws s3 sync /backups s3://backups-bucket/pulsar-pulseql/

# Cleanup old backups (keep 30 days)
find /backups -name "*.gz" -mtime +30 -delete
```

**Automated Backup with Cron**:

```cron
# Daily backups at 2 AM
0 2 * * * /usr/local/bin/backup-databases.sh

# Weekly full system backup
0 3 * * 0 /usr/local/bin/full-system-backup.sh
```

### Disaster Recovery Plan

1. **Database Recovery**:
   ```bash
   # Restore Pulsar database
   gunzip < pulsar-20241212-020000.sql.gz | \
     psql -h pulsar-db -U pulsar pulsar
   
   # Restore PulseQL database
   gunzip < pulseql-20241212-020000.sql.gz | \
     psql -h pulseql-db -U pulseql pulseql
   ```

2. **Workspace Recovery**:
   ```bash
   # Restore PulseQL workspace
   tar -xzf pulseql-workspace-20241212-020000.tar.gz -C /
   ```

3. **RTO/RPO Targets**:
   - Recovery Time Objective (RTO): < 4 hours
   - Recovery Point Objective (RPO): < 24 hours

## Security Considerations

### SSL/TLS Configuration

```bash
# Generate SSL certificates
certbot certonly --standalone \
  -d pulsar.example.com \
  -d pulseql.example.com

# Auto-renewal
0 0 * * * certbot renew --quiet
```

### Network Security

```bash
# Enable firewall
ufw enable
ufw default deny incoming
ufw default allow outgoing

# Allow necessary ports
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
```

### Secret Management

```yaml
# Using Vault for secrets
vault kv put secret/pulsar/db \
  username=pulsar \
  password=${DB_PASSWORD}

vault kv put secret/pulseql/sso \
  secret=${SSO_SECRET}
```

## Cost Optimization

### Resource Optimization

1. **Auto-scaling**: Scale PulseQL instances based on active users
2. **Database pooling**: Reuse connections efficiently
3. **Caching**: Use Redis for session and query result caching
4. **CDN**: Serve static assets from CDN

### Estimated Costs (AWS)

**Small Deployment** (100 users):
- EC2 (2x t3.medium): $60/month
- RDS (db.t3.medium): $80/month
- Load Balancer: $20/month
- Storage (200GB): $20/month
- **Total**: ~$180/month

**Medium Deployment** (500 users):
- EC2 (4x t3.large): $240/month
- RDS (db.r5.large): $220/month
- Load Balancer: $20/month
- Storage (500GB): $50/month
- **Total**: ~$530/month

**Large Deployment** (2000+ users):
- EKS Cluster: $150/month
- EC2 instances (auto-scaled): $800/month
- RDS (db.r5.xlarge): $500/month
- Storage (2TB): $200/month
- **Total**: ~$1,650/month

## Conclusion

The deployment architecture provides:

1. **Flexibility**: Multiple deployment options for different scales
2. **High Availability**: Redundancy at all layers
3. **Scalability**: Horizontal and vertical scaling capabilities
4. **Security**: Defense-in-depth approach
5. **Observability**: Comprehensive monitoring and logging
6. **Disaster Recovery**: Automated backups and recovery procedures

Choose the deployment option that best fits your requirements, starting with separate deployment for flexibility and moving to Kubernetes for enterprise scale.
