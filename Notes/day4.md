
Excellent! Let's move to **Day 4: Configuration & Profiles**.

## Day 4: Managing Environments with Spring Profiles

### What We'll Accomplish Today
By the end of today, you'll have:
1. Mastered externalized configuration with `application.properties` and `application.yml`
2. Created and used Spring Profiles for different environments
3. Configured different databases for dev, test, and prod
4. Learned about configuration properties and @Value annotation
5. Understood profile-specific configuration

---

### Step 1: Understanding Configuration Files

Spring Boot supports two configuration file formats:

1. **`application.properties`** (what we've been using)
2. **`application.yml`** (YAML format - more readable for complex configurations)

Let's convert our properties file to YAML and explore both.

### Step 2: Convert to YAML Configuration

Rename `src/main/resources/application.properties` to `src/main/resources/application.yml` and update the content:

**`src/main/resources/application.yml`**
```yaml
# Common configuration for all environments
spring:
  profiles:
    active: dev
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true
      path: /h2-console

# Logging configuration
logging:
  level:
    com.sujan.springbootmastery: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# Server configuration
server:
  port: 8080
  servlet:
    context-path: /
```

**Benefits of YAML:**
- Hierarchical structure
- No repetition of prefixes
- More readable for complex configurations
- Supports lists and maps naturally

### Step 3: Create Profile-Specific Configuration Files

Now let's create environment-specific configurations:

**`src/main/resources/application-dev.yml`** (Development)
```yaml
# Development profile - H2 in-memory database
spring:
  datasource:
    url: jdbc:h2:mem:devdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop  # Create tables, drop on shutdown

# Development-specific settings
logging:
  level:
    org.springframework: DEBUG
```

**`src/main/resources/application-test.yml`** (Testing)
```yaml
# Test profile - H2 file-based database (persists between restarts)
spring:
  datasource:
    url: jdbc:h2:file:./testdb/testdb
    username: testuser
    password: testpass
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop

# Test-specific settings
server:
  port: 8081  # Different port for testing

logging:
  level:
    org.springframework: WARN
```

**`src/main/resources/application-prod.yml`** (Production)
```yaml
# Production profile - PostgreSQL database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bookdb
    username: produser
    password: prodpass
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Don't auto-create tables in production!
    show-sql: false  # Don't log SQL in production

# Production-specific settings
server:
  port: 8080

logging:
  level:
    com.sujan.springbootmastery: INFO
    org.springframework: WARN

# Production security
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Step 4: Add PostgreSQL Dependency (for Production Profile)

Add PostgreSQL driver to your `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Step 5: Create Configuration Properties Class

Let's create a custom configuration class to demonstrate how to read properties:

**`src/main/java/com/sujan/springbootmastery/config/AppConfig.java`**
```java
package com.sujan.springbootmastery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String name;
    private String version;
    private String description;
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return "AppConfig{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
```

Now add these properties to your configuration files:

**Add to `application.yml` (common):**
```yaml
app:
  name: "Spring Boot Mastery API"
  version: "1.0.0"
  description: "Learning Spring Boot in 2 Weeks"
```

**Add to `application-dev.yml`:**
```yaml
app:
  description: "Development Environment - H2 Database"
```

**Add to `application-prod.yml`:**
```yaml
app:
  description: "Production Environment - PostgreSQL Database"
```

### Step 6: Create a Configuration Controller

Let's create a controller to demonstrate profile-based configuration:

**`src/main/java/com/sujan/springbootmastery/controller/ConfigController.java`**
```java
package com.sujan.springbootmastery.controller;

import com.sujan.springbootmastery.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private AppConfig appConfig;
    
    // Reading values directly from properties file
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String appVersion;
    
    @Value("${spring.datasource.url:Not configured}")
    private String datasourceUrl;
    
    @Value("${server.port}")
    private String serverPort;
    
    @GetMapping("/info")
    public Map<String, String> getConfigInfo() {
        Map<String, String> config = new HashMap<>();
        config.put("appName", appName);
        config.put("appVersion", appVersion);
        config.put("activeProfiles", Arrays.toString(environment.getActiveProfiles()));
        config.put("datasourceUrl", datasourceUrl);
        config.put("serverPort", serverPort);
        config.put("description", appConfig.getDescription());
        
        return config;
    }
    
    @GetMapping("/profiles")
    public Map<String, String[]> getProfiles() {
        Map<String, String[]> profiles = new HashMap<>();
        profiles.put("activeProfiles", environment.getActiveProfiles());
        profiles.put("defaultProfiles", environment.getDefaultProfiles());
        
        return profiles;
    }
    
    @GetMapping("/app-config")
    public AppConfig getAppConfig() {
        return appConfig;
    }
}
```

### Step 7: Test Different Profiles

#### Test 1: Default Profile (dev)
Start your application normally:
```bash
./mvnw spring-boot:run
```

Test the config endpoint:
```bash
curl http://localhost:8080/api/config/info
```

You should see development configuration with H2 database.

#### Test 2: Test Profile
Stop the application and restart with test profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=test
```

Or set environment variable:
```bash
export SPRING_PROFILES_ACTIVE=test
./mvnw spring-boot:run
```

Test again:
```bash
curl http://localhost:8081/api/config/info
```

Notice the different port and database URL!

#### Test 3: Production Profile (Simulated)
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
```

The app will start but may fail to connect to PostgreSQL (which is expected unless you have it running).

### Step 8: Profile-Specific Beans

You can also create beans that are only active for specific profiles. Let's create a data initializer that behaves differently per profile:

**`src/main/java/com/sujan/springbootmastery/config/DataInitializer.java`**
```java
package com.sujan.springbootmastery.config;

import com.sujan.springbootmastery.model.Book;
import com.sujan.springbootmastery.repository.BookRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private BookRepository bookRepository;
    
    // This runs only in dev profile
    @Profile("dev")
    @PostConstruct
    public void initDevData() {
        logger.info("Initializing DEVELOPMENT data...");
        
        bookRepository.deleteAll();
        
        bookRepository.save(new Book("The Great Gatsby", "F. Scott Fitzgerald", 1925));
        bookRepository.save(new Book("To Kill a Mockingbird", "Harper Lee", 1960));
        bookRepository.save(new Book("1984", "George Orwell", 1949));
        bookRepository.save(new Book("The Hobbit", "J.R.R. Tolkien", 1937));
        
        logger.info("Development data initialized with {} books", bookRepository.count());
    }
    
    // This runs only in test profile
    @Profile("test")
    @PostConstruct
    public void initTestData() {
        logger.info("Initializing TEST data...");
        
        bookRepository.deleteAll();
        
        // Minimal test data
        bookRepository.save(new Book("Test Book 1", "Test Author", 2020));
        bookRepository.save(new Book("Test Book 2", "Test Author", 2021));
        
        logger.info("Test data initialized with {} books", bookRepository.count());
    }
    
    // This runs only in prod profile (or when no profile is specified)
    @Profile("prod")
    @PostConstruct
    public void initProdData() {
        logger.info("Initializing PRODUCTION data...");
        // In production, we typically don't auto-create test data
        // But we might run data migrations instead
        logger.info("Production data initialization complete");
    }
}
```

### Step 9: Remove Old Initialization Code

Remove the `@PostConstruct` method from your `BookController` since we've moved data initialization to the profile-specific configuration.

### Step 10: Test Profile-Specific Behavior

Start with different profiles and observe the logs:

**Development Profile:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```
You should see 4 books created.

**Test Profile:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=test
```
You should see 2 test books created.

### Step 11: Using @Value with Default Values

You can provide default values when properties might not be defined:

Update the ConfigController:

```java
@Value("${app.support.email:support@example.com}")
private String supportEmail;

@Value("${app.feature.flags:}")
private String featureFlags;

@GetMapping("/defaults")
public Map<String, String> showDefaults() {
    Map<String, String> defaults = new HashMap<>();
    defaults.put("supportEmail", supportEmail);
    defaults.put("featureFlags", featureFlags);
    defaults.put("undefinedPropertyWithDefault", 
        environment.getProperty("app.undefined.property", "Default Value"));
    
    return defaults;
}
```

### Day 4 Challenge

1. **Create a new profile** `local` that uses MySQL instead of H2 or PostgreSQL
2. **Add profile-specific API documentation** - different descriptions for each environment
3. **Create a configuration property** for feature toggles that changes behavior per environment

### Solution to Challenge #1

**Add to `pom.xml`:**
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Create `application-local.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookdb
    username: localuser
    password: localpass
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update

app:
  description: "Local Development - MySQL Database"

logging:
  level:
    com.sujan.springbootmastery: DEBUG
```

### Key Takeaways from Day 4

✅ **Profiles**: Environment-specific configuration management  
✅ **YAML Configuration**: Clean, hierarchical configuration format  
✅ **@ConfigurationProperties**: Type-safe configuration binding  
✅ **@Value**: Direct property injection  
✅ **Profile-specific Beans**: Conditional bean creation  
✅ **Externalized Config**: Separate configuration from code  

### What's Next?

Tomorrow (Day 5) we'll learn about **Dependency Injection & The Service Layer** - proper application architecture with clear separation of concerns!

**When you're ready for Day 5, just say "next"!** Practice today's concepts by creating more profile-specific configurations and testing them.