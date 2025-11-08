Excellent! Let's move to **Day 10: Spring Boot Actuator & Monitoring**.

## Day 10: Production-Ready Monitoring and Management

### What We'll Accomplish Today
By the end of today, you'll have:
1. Set up Spring Boot Actuator for application monitoring
2. Exposed various health checks and metrics endpoints
3. Created custom health indicators and metrics
4. Integrated with monitoring tools
5. Learned about production-ready features

---

### Step 1: Add Actuator Dependencies

Add Spring Boot Actuator and Micrometer (for metrics) to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Step 2: Configure Actuator Endpoints

Update your configuration to expose and configure actuator endpoints:

**`src/main/resources/application.yml`**
```yaml
# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,beans,loggers,prometheus
      base-path: /management  # Custom base path instead of /actuator
    enabled-by-default: true
  endpoint:
    health:
      show-details: always
      show-components: always
      probes:
        enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true
  info:
    env:
      enabled: true
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

# Application Info
info:
  app:
    name: "Spring Boot Mastery API"
    version: "2.0.0"
    description: "Learning Spring Boot in 2 Weeks - Day 10: Actuator & Monitoring"
    environment: "@spring.profiles.active@"
```

**Environment-specific configurations:**

**`src/main/resources/application-dev.yml`**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"  # Expose all endpoints in dev
  endpoint:
    shutdown:
      enabled: true  # Only in dev - be careful!
```

**`src/main/resources/application-prod.yml`**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # Limited in production
  endpoint:
    shutdown:
      enabled: false  # Never in production!
  server:
    port: 9000  # Separate port for management endpoints
```

### Step 3: Test Basic Actuator Endpoints

Start your application and test the actuator endpoints:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

**Test the endpoints:**

```bash
# Health check
curl http://localhost:8080/management/health

# Application info
curl http://localhost:8080/management/info

# Metrics
curl http://localhost:8080/management/metrics

# Specific metric
curl http://localhost:8080/management/metrics/http.server.requests

# Prometheus metrics (for monitoring systems)
curl http://localhost:8080/management/prometheus

# Environment variables
curl http://localhost:8080/management/env

# List all beans
curl http://localhost:8080/management/beans

# View and change log levels
curl http://localhost:8080/management/loggers/com.sujan.springbootmastery
```

### Step 4: Create Custom Health Indicators

Let's create custom health indicators for our application components:

**`src/main/java/com/sujan/springbootmastery/health/DatabaseHealthIndicator.java`**
```java
package com.sujan.springbootmastery.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Health health() {
        try {
            // Test database connectivity and basic operations
            List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT 1 as test");
            
            if (result != null && !result.isEmpty()) {
                // Check if our main tables exist
                int bookCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM books", Integer.class);
                int userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
                
                return Health.up()
                        .withDetail("database", "Connected")
                        .withDetail("books_table_count", bookCount)
                        .withDetail("users_table_count", userCount)
                        .withDetail("message", "Database is healthy")
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", "No response from test query")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

**`src/main/java/com/sujan/springbootmastery/health/BookServiceHealthIndicator.java`**
```java
package com.sujan.springbootmastery.health;

import com.sujan.springbootmastery.service.BookService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class BookServiceHealthIndicator implements HealthIndicator {
    
    private final BookService bookService;
    
    public BookServiceHealthIndicator(BookService bookService) {
        this.bookService = bookService;
    }
    
    @Override
    public Health health() {
        try {
            Long bookCount = bookService.getTotalBookCount();
            boolean serviceAvailable = bookCount >= 0; // Basic sanity check
            
            if (serviceAvailable) {
                return Health.up()
                        .withDetail("service", "BookService")
                        .withDetail("status", "Operational")
                        .withDetail("total_books", bookCount)
                        .withDetail("message", "Book service is healthy")
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "BookService")
                        .withDetail("status", "Degraded")
                        .withDetail("message", "Book service returned unexpected count")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "BookService")
                    .withDetail("status", "Down")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

**`src/main/java/com/sujan/springbootmastery/health/ExternalServiceHealthIndicator.java`**
```java
package com.sujan.springbootmastery.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Example: Check if a critical external service is available
        String[] servicesToCheck = {
            "https://api.github.com",
            "https://jsonplaceholder.typicode.com"
        };
        
        StringBuilder status = new StringBuilder();
        int upCount = 0;
        
        for (String serviceUrl : servicesToCheck) {
            try {
                URL url = new URL(serviceUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    status.append(serviceUrl).append(": UP, ");
                    upCount++;
                } else {
                    status.append(serviceUrl).append(": DOWN (").append(responseCode).append("), ");
                }
                
                connection.disconnect();
            } catch (Exception e) {
                status.append(serviceUrl).append(": ERROR (").append(e.getMessage()).append("), ");
            }
        }
        
        if (upCount == servicesToCheck.length) {
            return Health.up()
                    .withDetail("external_services", status.toString())
                    .withDetail("available_services", upCount + "/" + servicesToCheck.length)
                    .build();
        } else if (upCount > 0) {
            return Health.unknown()
                    .withDetail("external_services", status.toString())
                    .withDetail("available_services", upCount + "/" + servicesToCheck.length)
                    .withDetail("message", "Some external services are unavailable")
                    .build();
        } else {
            return Health.down()
                    .withDetail("external_services", status.toString())
                    .withDetail("available_services", upCount + "/" + servicesToCheck.length)
                    .withDetail("message", "All external services are down")
                    .build();
        }
    }
}
```

### Step 5: Create Custom Metrics

Let's add custom metrics to track our application's business logic:

**`src/main/java/com/sujan/springbootmastery/metrics/BookMetrics.java`**
```java
package com.sujan.springbootmastery.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BookMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter booksCreatedCounter;
    private final Counter booksUpdatedCounter;
    private final Counter booksDeletedCounter;
    private final Counter bookSearchCounter;
    private final Timer bookCreationTimer;
    private final ConcurrentHashMap<String, AtomicInteger> genreCounter;
    
    public BookMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.genreCounter = new ConcurrentHashMap<>();
        
        // Initialize counters
        this.booksCreatedCounter = Counter.builder("books.created")
                .description("Total number of books created")
                .register(meterRegistry);
        
        this.booksUpdatedCounter = Counter.builder("books.updated")
                .description("Total number of books updated")
                .register(meterRegistry);
        
        this.booksDeletedCounter = Counter.builder("books.deleted")
                .description("Total number of books deleted")
                .register(meterRegistry);
        
        this.bookSearchCounter = Counter.builder("books.searches")
                .description("Total number of book searches performed")
                .register(meterRegistry);
        
        this.bookCreationTimer = Timer.builder("books.creation.time")
                .description("Time taken to create books")
                .register(meterRegistry);
        
        // Initialize gauge for total books (will be updated separately)
        Gauge.builder("books.total", this, value -> getTotalBooksCount())
                .description("Total number of books in the system")
                .register(meterRegistry);
    }
    
    public void incrementBooksCreated() {
        booksCreatedCounter.increment();
    }
    
    public void incrementBooksUpdated() {
        booksUpdatedCounter.increment();
    }
    
    public void incrementBooksDeleted() {
        booksDeletedCounter.increment();
    }
    
    public void incrementBookSearches() {
        bookSearchCounter.increment();
    }
    
    public void recordBookCreationTime(long duration, TimeUnit unit) {
        bookCreationTimer.record(duration, unit);
    }
    
    public void incrementGenreCount(String genre) {
        if (genre != null) {
            genreCounter.computeIfAbsent(genre, k -> new AtomicInteger(0))
                       .incrementAndGet();
            
            // Update gauge for this specific genre
            Gauge.builder("books.genre.count", genreCounter.get(genre), AtomicInteger::get)
                    .tag("genre", genre)
                    .description("Number of books by genre")
                    .register(meterRegistry);
        }
    }
    
    private double getTotalBooksCount() {
        // This would typically query your service or repository
        // For now, return a simple sum of genre counts
        return genreCounter.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
    }
}
```

### Step 6: Update Services to Use Metrics

Update your BookService to track metrics:

**`src/main/java/com/sujan/springbootmastery/service/BookServiceImpl.java`**
```java
package com.sujan.springbootmastery.service;

// Add this import
import com.sujan.springbootmastery.metrics.BookMetrics;

// ... existing imports ...

@Service
public class BookServiceImpl implements BookService {
    
    // ... existing fields ...
    private final BookMetrics bookMetrics;
    
    @Autowired
    public BookServiceImpl(BookRepository bookRepository, BookMetrics bookMetrics) {
        this.bookRepository = bookRepository;
        this.bookMetrics = bookMetrics;
    }
    
    @Override
    public Book createBook(Book book) {
        long startTime = System.nanoTime();
        
        try {
            // ... existing validation logic ...
            
            Book savedBook = bookRepository.save(book);
            
            // Track metrics
            bookMetrics.incrementBooksCreated();
            bookMetrics.incrementGenreCount(book.getGenre());
            
            return savedBook;
        } finally {
            long duration = System.nanoTime() - startTime;
            bookMetrics.recordBookCreationTime(duration, TimeUnit.NANOSECONDS);
        }
    }
    
    @Override
    public Book updateBook(Long id, Book bookDetails) {
        // ... existing logic ...
        
        Book updatedBook = bookRepository.save(existingBook);
        bookMetrics.incrementBooksUpdated();
        
        return updatedBook;
    }
    
    @Override
    public void deleteBook(Long id) {
        // ... existing logic ...
        
        bookRepository.deleteById(id);
        bookMetrics.incrementBooksDeleted();
        
        logger.info("Book with id {} deleted successfully", id);
    }
    
    @Override
    public List<Book> searchBooksByAuthor(String author) {
        bookMetrics.incrementBookSearches();
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }
    
    @Override
    public List<Book> searchBooksByTitle(String title) {
        bookMetrics.incrementBookSearches();
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }
    
    // ... rest of the methods ...
}
```

### Step 7: Create Custom Info Contributor

Let's add custom information to the `/management/info` endpoint:

**`src/main/java/com/sujan/springbootmastery/info/ApplicationInfoContributor.java`**
```java
package com.sujan.springbootmastery.info;

import com.sujan.springbootmastery.service.BookService;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApplicationInfoContributor implements InfoContributor {
    
    private final BookService bookService;
    
    public ApplicationInfoContributor(BookService bookService) {
        this.bookService = bookService;
    }
    
    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> appDetails = new HashMap<>();
        Map<String, Object> stats = new HashMap<>();
        
        // Application statistics
        stats.put("total_books", bookService.getTotalBookCount());
        stats.put("total_authors", bookService.getAllAuthors().size());
        stats.put("average_publication_year", bookService.getAveragePublicationYear());
        
        // System information
        appDetails.put("startup_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        appDetails.put("java_version", System.getProperty("java.version"));
        appDetails.put("available_processors", Runtime.getRuntime().availableProcessors());
        appDetails.put("free_memory", Runtime.getRuntime().freeMemory());
        appDetails.put("max_memory", Runtime.getRuntime().maxMemory());
        appDetails.put("total_memory", Runtime.getRuntime().totalMemory());
        
        // Build info
        Map<String, Object> buildInfo = new HashMap<>();
        buildInfo.put("artifact", "spring-boot-mastery");
        buildInfo.put("version", "2.0.0");
        buildInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        builder.withDetail("application", appDetails)
               .withDetail("statistics", stats)
               .withDetail("build", buildInfo)
               .withDetail("status", "Operational");
    }
}
```

### Step 8: Create Custom Endpoints

Let's create custom actuator endpoints for our application:

**`src/main/java/com/sujan/springbootmastery/endpoint/BooksEndpoint.java`**
```java
package com.sujan.springbootmastery.endpoint;

import com.sujan.springbootmastery.service.BookService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "books")
public class BooksEndpoint {
    
    private final BookService bookService;
    
    public BooksEndpoint(BookService bookService) {
        this.bookService = bookService;
    }
    
    @ReadOperation
    public Map<String, Object> booksInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("totalBooks", bookService.getTotalBookCount());
        info.put("authors", bookService.getAllAuthors());
        info.put("booksByAuthor", bookService.getBooksCountByAuthor());
        info.put("averagePublicationYear", bookService.getAveragePublicationYear());
        
        return info;
    }
    
    @ReadOperation
    public Map<String, Object> bookStats(@Selector String stat) {
        Map<String, Object> stats = new HashMap<>();
        
        switch (stat) {
            case "count":
                stats.put("totalBooks", bookService.getTotalBookCount());
                break;
            case "authors":
                stats.put("authors", bookService.getAllAuthors());
                stats.put("authorCount", bookService.getAllAuthors().size());
                break;
            case "publication":
                stats.put("averageYear", bookService.getAveragePublicationYear());
                stats.put("recentBooks", bookService.getRecentBooks(10));
                break;
            default:
                stats.put("error", "Unknown stat: " + stat);
        }
        
        return stats;
    }
    
    @WriteOperation
    public Map<String, String> clearBookCache() {
        // In a real application, this would clear caches
        Map<String, String> result = new HashMap<>();
        result.put("message", "Book cache cleared successfully");
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        return result;
    }
}
```

### Step 9: Test the Enhanced Actuator Endpoints

Restart your application and test the new endpoints:

```bash
# Custom books endpoint
curl http://localhost:8080/management/books

# Specific book stats
curl http://localhost:8080/management/books/count
curl http://localhost:8080/management/books/authors

# Health endpoint now includes our custom health indicators
curl http://localhost:8080/management/health

# Info endpoint with custom information
curl http://localhost:8080/management/info

# Metrics - check our custom metrics
curl http://localhost:8080/management/metrics/books.created
curl http://localhost:8080/management/metrics/books.searches
```

### Step 10: Create a Monitoring Dashboard (Optional)

Let's create a simple monitoring controller for a web-based dashboard:

**`src/main/java/com/sujan/springbootmastery/controller/MonitoringController.java`**
```java
package com.sujan.springbootmastery.controller;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MonitoringController {
    
    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;
    
    public MonitoringController(HealthEndpoint healthEndpoint, MetricsEndpoint metricsEndpoint) {
        this.healthEndpoint = healthEndpoint;
        this.metricsEndpoint = metricsEndpoint;
    }
    
    @GetMapping("/monitoring")
    public String monitoringDashboard(Model model) {
        // Health information
        HealthComponent health = healthEndpoint.health();
        model.addAttribute("health", health);
        
        // Key metrics
        List<String> interestingMetrics = List.of(
            "jvm.memory.used",
            "jvm.memory.max",
            "http.server.requests",
            "books.created",
            "books.searches",
            "books.total"
        );
        
        var metrics = interestingMetrics.stream()
                .collect(Collectors.toMap(
                    metric -> metric,
                    metric -> metricsEndpoint.metric(metric, null)
                ));
        
        model.addAttribute("metrics", metrics);
        
        return "monitoring";
    }
}
```

Create a simple Thymeleaf template:

**`src/main/resources/templates/monitoring.html`**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Application Monitoring</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .health-up { color: green; }
        .health-down { color: red; }
        .metric { margin: 10px 0; padding: 10px; border: 1px solid #ddd; }
    </style>
</head>
<body>
    <h1>Application Monitoring Dashboard</h1>
    
    <h2>Health Status</h2>
    <div th:class="${health.status == 'UP'} ? 'health-up' : 'health-down'">
        Status: <span th:text="${health.status}">UNKNOWN</span>
    </div>
    
    <h2>Key Metrics</h2>
    <div th:each="metric : ${metrics}">
        <div class="metric">
            <strong th:text="${metric.key}">Metric Name</strong>: 
            <span th:text="${metric.value != null ? metric.value.measurements[0].value : 'N/A'}">Value</span>
        </div>
    </div>
    
    <h2>Quick Links</h2>
    <ul>
        <li><a href="/management/health">Detailed Health</a></li>
        <li><a href="/management/metrics">All Metrics</a></li>
        <li><a href="/management/info">Application Info</a></li>
        <li><a href="/management/prometheus">Prometheus Metrics</a></li>
    </ul>
</body>
</html>
```

### Step 11: Security for Actuator Endpoints

Let's secure our actuator endpoints (especially important for production):

**Update WebSecurityConfig:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors().and().csrf().disable()
        .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/management/health").permitAll()
            .requestMatchers("/management/info").permitAll()
            .requestMatchers("/management/**").hasRole("ADMIN")  // Secure other actuator endpoints
            .requestMatchers("/h2-console/**").permitAll()
            .anyRequest().authenticated()
        );
    
    // ... rest of the configuration
}
```

### Day 10 Challenge

1. **Create a custom metric** that tracks the average response time for book searches
2. **Implement a readiness probe** that checks if your application is ready to receive traffic
3. **Add a custom endpoint** to view application cache statistics
4. **Set up alerting** rules for critical metrics

### Solution to Challenge #1

**Add to BookMetrics:**
```java
private final Timer bookSearchTimer;

// In constructor:
this.bookSearchTimer = Timer.builder("books.search.time")
        .description("Time taken to search books")
        .register(meterRegistry);

public void recordBookSearchTime(long duration, TimeUnit unit) {
    bookSearchTimer.record(duration, unit);
}
```

**Update BookService:**
```java
@Override
public List<Book> searchBooksByAuthor(String author) {
    long startTime = System.nanoTime();
    try {
        bookMetrics.incrementBookSearches();
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    } finally {
        long duration = System.nanoTime() - startTime;
        bookMetrics.recordBookSearchTime(duration, TimeUnit.NANOSECONDS);
    }
}
```

### Key Takeaways from Day 10

✅ **Spring Boot Actuator**: Production-ready monitoring endpoints  
✅ **Health Checks**: Custom health indicators for application components  
✅ **Metrics**: Custom business metrics with Micrometer  
✅ **Custom Endpoints**: Application-specific management endpoints  
✅ **Security**: Proper security for management endpoints  
✅ **Monitoring**: Integration with monitoring systems like Prometheus  

### What's Next?

Tomorrow (Day 11) we'll dive into **Building for Production** - packaging, deployment, Docker, and production best practices!

**When you're ready for Day 11, just say "next"!** Practice today's concepts by adding more custom metrics and health checks for your specific application needs.