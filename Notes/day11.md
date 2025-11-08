Excellent! Let's move to **Day 11: Building for Production & Deployment**.

## Day 11: Production-Ready Deployment Strategies

### What We'll Accomplish Today
By the end of today, you'll have:
1. Built production-ready JAR files with optimized configurations
2. Containerized your application with Docker
3. Created Docker Compose setups for development and production
4. Learned production deployment strategies
5. Set up production monitoring and logging
6. Understood performance optimization techniques

---

### Step 1: Production Build Configuration

Let's optimize our Maven build for production:

**Update `pom.xml` with production optimizations:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <!-- ... existing configuration ... -->
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                    <executable>true</executable>
                    <layers>
                        <enabled>true</enabled>
                    </layers>
                </configuration>
            </plugin>
            
            <!-- Docker image build plugin -->
            <plugin>
                <groupId>com.google.cloud.tools</groupId>
                <artifactId>jib-maven-plugin</artifactId>
                <version>3.4.0</version>
                <configuration>
                    <to>
                        <image>spring-boot-mastery:latest</image>
                    </to>
                    <container>
                        <ports>
                            <port>8080</port>
                        </ports>
                        <environment>
                            <SPRING_PROFILES_ACTIVE>prod</SPRING_PROFILES_ACTIVE>
                        </environment>
                        <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
                    </container>
                </configuration>
            </plugin>
            
            <!-- Build time optimization -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <parameters>true</parameters>
                </configuration>
            </plugin>
            
            <!-- Resource filtering for production properties -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <delimiters>
                        <delimiter>@</delimiter>
                    </delimiters>
                    <useDefaultDelimiters>false</useDefaultDelimiters>
                </configuration>
            </plugin>
        </plugins>
        
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
                <includes>
                    <include>**/application*.yml</include>
                    <include>**/application*.yaml</include>
                    <include>**/application*.properties</include>
                </includes>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
                <excludes>
                    <exclude>**/application*.yml</exclude>
                    <exclude>**/application*.yaml</exclude>
                    <exclude>**/application*.properties</exclude>
                </excludes>
            </resource>
        </resources>
    </build>
    
    <profiles>
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>
        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <configuration>
                            <profiles>prod</profiles>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### Step 2: Production Configuration

Enhance your production configuration:

**`src/main/resources/application-prod.yml`**
```yaml
# Production Configuration
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:bookdb_prod}
    username: ${DB_USERNAME:prod_user}
    password: ${DB_PASSWORD:prod_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  sql:
    init:
      mode: never
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    min-response-size: 1024
  tomcat:
    max-connections: 10000
    max-http-header-size: 8KB
    threads:
      max: 200
      min-spare: 10

# Actuator Configuration (Production-safe)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /internal/actuator
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized
      probes:
        enabled: true
    shutdown:
      enabled: false
  server:
    port: 8081  # Separate port for management

# Logging Configuration
logging:
  level:
    com.sujan.springbootmastery: INFO
    org.springframework: WARN
    org.hibernate: WARN
    org.apache.coyote: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/spring-boot-mastery/application.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
      total-size-cap: 1GB

# Security Configuration
app:
  jwt:
    secret: ${JWT_SECRET:your-production-jwt-secret-key-here-make-it-very-long-and-secure}
    expirationMs: 86400000  # 24 hours

# Custom Application Settings
app:
  cache:
    enabled: true
    ttl: 3600000  # 1 hour
  rate-limit:
    enabled: true
    requests-per-minute: 100
```

### Step 3: Create Dockerfile

**`Dockerfile`**
```dockerfile
# Multi-stage build for optimized production image
FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests -Pprod

# Production stage
FROM eclipse-temurin:17-jre-jammy as production

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

# Copy the executable JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directory for logs with proper permissions
USER root
RUN mkdir -p /var/log/spring-boot-mastery && \
    chown -R spring:spring /var/log/spring-boot-mastery
USER spring:spring

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/internal/actuator/health || exit 1

EXPOSE 8080
EXPOSE 8081

# Use shell form to allow environment variable expansion
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
```

### Step 4: Create Docker Compose for Development

**`docker-compose.dev.yml`**
```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
      - "8081:8081"  # Actuator port
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=bookdb_dev
      - DB_USERNAME=dev_user
      - DB_PASSWORD=dev_password
      - JWT_SECRET=dev-jwt-secret-key
    volumes:
      - ./:/app
      - maven-data:/root/.m2
    depends_on:
      - postgres
    networks:
      - spring-network

  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=bookdb_dev
      - POSTGRES_USER=dev_user
      - POSTGRES_PASSWORD=dev_password
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./src/main/resources/db/migration:/docker-entrypoint-initdb.d
    networks:
      - spring-network

  pgadmin:
    image: dpage/pgadmin4
    environment:
      - PGADMIN_DEFAULT_EMAIL=admin@example.com
      - PGADMIN_DEFAULT_PASSWORD=admin
    ports:
      - "5050:80"
    depends_on:
      - postgres
    networks:
      - spring-network

volumes:
  postgres-data:
  maven-data:

networks:
  spring-network:
    driver: bridge
```

**`Dockerfile.dev`**
```dockerfile
FROM eclipse-temurin:17-jdk-jammy

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

EXPOSE 8080 8081

# For development, we can run the app directly
CMD ["./mvnw", "spring-boot:run", "-Dspring-boot.run.arguments=--spring.profiles.active=dev"]
```

### Step 5: Create Docker Compose for Production

**`docker-compose.prod.yml`**
```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=bookdb_prod
      - DB_USERNAME=prod_user
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx512m
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
    restart: unless-stopped
    depends_on:
      - postgres
    networks:
      - spring-network-prod
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.api.rule=Host(`api.example.com`)"
      - "traefik.http.services.api.loadbalancer.server.port=8080"

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=bookdb_prod
      - POSTGRES_USER=prod_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres-data-prod:/var/lib/postgresql/data
      - ./backups:/backups
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M
    restart: unless-stopped
    networks:
      - spring-network-prod
    command: >
      postgres
      -c shared_preload_libraries=pg_stat_statements
      -c pg_stat_statements.track=all
      -c max_connections=100

  # Monitoring stack
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    restart: unless-stopped
    networks:
      - spring-network-prod

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/datasources:/etc/grafana/provisioning/datasources
    restart: unless-stopped
    networks:
      - spring-network-prod

volumes:
  postgres-data-prod:
  prometheus-data:
  grafana-data:

networks:
  spring-network-prod:
    driver: bridge
```

### Step 6: Create Environment Files

**`.env.prod`**
```properties
# Database
DB_PASSWORD=your_secure_production_password_here

# JWT
JWT_SECRET=your-very-long-and-secure-jwt-secret-key-for-production

# Grafana
GRAFANA_PASSWORD=admin123

# Application
SPRING_PROFILES_ACTIVE=prod
```

**`.env.dev`**
```properties
# Database
DB_PASSWORD=dev_password

# JWT
JWT_SECRET=dev-jwt-secret-key

# Application
SPRING_PROFILES_ACTIVE=dev
```

### Step 7: Create Monitoring Configuration

**`monitoring/prometheus.yml`**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/internal/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['app:8080']
        labels:
          application: 'spring-boot-mastery'
          environment: 'production'

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

**`monitoring/datasources/datasource.yml`**
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

**`monitoring/dashboards/dashboard.yml`**
```yaml
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
```

### Step 8: Build and Run Scripts

**`scripts/build.sh`**
```bash
#!/bin/bash

# Build script for Spring Boot Mastery application

set -e

echo "Building Spring Boot Mastery application..."

# Check if we're building for production or development
ENVIRONMENT=${1:-dev}

case $ENVIRONMENT in
    "prod")
        echo "Building for PRODUCTION..."
        ./mvnw clean package -Pprod -DskipTests
        ;;
    "dev")
        echo "Building for DEVELOPMENT..."
        ./mvnw clean package -DskipTests
        ;;
    *)
        echo "Usage: $0 {prod|dev}"
        exit 1
        ;;
esac

echo "Build completed successfully!"
```

**`scripts/start.sh`**
```bash
#!/bin/bash

# Start script for Spring Boot Mastery application

set -e

echo "Starting Spring Boot Mastery application..."

ENVIRONMENT=${1:-dev}

case $ENVIRONMENT in
    "prod")
        echo "Starting PRODUCTION environment..."
        docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
        ;;
    "dev")
        echo "Starting DEVELOPMENT environment..."
        docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d
        ;;
    *)
        echo "Usage: $0 {prod|dev}"
        exit 1
        ;;
esac

echo "Application started successfully!"
echo "Main application: http://localhost:8080"
echo "Actuator endpoints: http://localhost:8081/internal/actuator"
echo "Database admin: http://localhost:5050 (dev only)"
echo "Grafana: http://localhost:3000 (prod only)"
```

**`scripts/stop.sh`**
```bash
#!/bin/bash

# Stop script for Spring Boot Mastery application

set -e

echo "Stopping Spring Boot Mastery application..."

ENVIRONMENT=${1:-dev}

case $ENVIRONMENT in
    "prod")
        echo "Stopping PRODUCTION environment..."
        docker-compose -f docker-compose.prod.yml down
        ;;
    "dev")
        echo "Stopping DEVELOPMENT environment..."
        docker-compose -f docker-compose.dev.yml down
        ;;
    *)
        echo "Usage: $0 {prod|dev}"
        exit 1
        ;;
esac

echo "Application stopped successfully!"
```

Make the scripts executable:
```bash
chmod +x scripts/*.sh
```

### Step 9: Test the Production Build

Let's test our production setup:

**Build the production JAR:**
```bash
./scripts/build.sh prod
```

**Test the JAR directly:**
```bash
java -jar target/spring-boot-mastery-*.jar --spring.profiles.active=prod
```

**Or use Docker Compose for production:**
```bash
./scripts/start.sh prod
```

### Step 10: Performance Optimization Configuration

**Create a performance configuration class:**

**`src/main/java/com/sujan/springbootmastery/config/PerformanceConfig.java`**
```java
package com.sujan.springbootmastery.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class PerformanceConfig {
    
    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        
        // Performance optimizations for Tomcat
        factory.addConnectorCustomizers(connector -> {
            connector.setProperty("relaxedQueryChars", "|{}[]");
            connector.setProperty("maxThreads", "200");
            connector.setProperty("minSpareThreads", "10");
            connector.setProperty("maxConnections", "10000");
            connector.setProperty("connectionTimeout", "30000");
            connector.setProperty("keepAliveTimeout", "60000");
            connector.setProperty("maxKeepAliveRequests", "100");
        });
        
        return factory;
    }
}
```

### Step 11: Create Production Health Check Controller

**`src/main/java/com/sujan/springbootmastery/controller/HealthCheckController.java`**
```java
package com.sujan.springbootmastery.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", appVersion);
        health.put("profile", activeProfile);
        health.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> info = new HashMap<>();
        info.put("application", "Spring Boot Mastery API");
        info.put("version", appVersion);
        info.put("status", "Running");
        info.put("profile", activeProfile);
        info.put("documentation", "/swagger-ui.html");
        info.put("health", "/health");
        
        return ResponseEntity.ok(info);
    }
}
```

### Step 12: Test Everything

**Test the production setup:**
```bash
# Build and start production environment
./scripts/build.sh prod
./scripts/start.sh prod

# Check if services are running
docker ps

# Test the application
curl http://localhost:8080/health
curl http://localhost:8080/internal/actuator/health

# Test the database connection
curl http://localhost:8080/api/books

# Check logs
docker logs <container_name>
```

### Day 11 Challenge

1. **Set up CI/CD pipeline** with GitHub Actions
2. **Add database backup** scripts to your Docker setup
3. **Implement graceful shutdown** handling
4. **Create a load testing script** to verify performance

### Solution to Challenge #3 (Graceful Shutdown)

**Add to `application-prod.yml`:**
```yaml
server:
  shutdown: graceful
  tomcat:
    connection-timeout: 30s
    keep-alive-timeout: 60s

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**Create a shutdown hook:**
```java
@Component
public class GracefulShutdown {
    
    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdown.class);
    
    @EventListener(ContextClosedEvent.class)
    public void onShutdown(ContextClosedEvent event) {
        logger.info("Application is shutting down gracefully...");
        
        // Add any cleanup logic here
        // Close connections, save state, etc.
        
        try {
            Thread.sleep(5000); // Wait for current requests to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Graceful shutdown completed");
    }
}
```

### Key Takeaways from Day 11

✅ **Production Builds**: Optimized JAR builds with layered architecture  
✅ **Docker Containerization**: Multi-stage builds and container optimization  
✅ **Docker Compose**: Complete environment setup for dev and prod  
✅ **Monitoring Stack**: Prometheus and Grafana integration  
✅ **Performance Optimization**: JVM and Tomcat tuning  
✅ **Production Security**: Secure configurations and environment variables  
✅ **Deployment Scripts**: Automated build and deployment processes  

### What's Next?

Tomorrow (Day 12) we'll explore **Advanced Spring Boot Features** - caching, async processing, WebSocket, and more advanced topics!

**When you're ready for Day 12, just say "next"!** Practice today's concepts by deploying your application and testing the production setup.