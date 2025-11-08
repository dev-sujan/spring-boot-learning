Excellent! Let's move to **Day 12: Advanced Spring Boot Features**.

## Day 12: Advanced Spring Boot Capabilities

### What We'll Accomplish Today
By the end of today, you'll have:
1. Implemented caching for performance optimization
2. Added asynchronous processing with `@Async`
3. Created WebSocket real-time communication
4. Used Spring Boot's event publishing system
5. Implemented retry mechanisms and circuit breakers
6. Added scheduling and background tasks

---

### Step 1: Caching with Spring Boot

Let's start by implementing caching to improve performance:

**Add cache dependency to `pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**Enable caching in your main application:**
```java
@SpringBootApplication
@EnableCaching
public class SpringBootMasteryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootMasteryApplication.class, args);
    }
}
```

**Configure cache in `application.yml`:**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=300s
```

**Create cache configuration:**
```java
package com.sujan.springbootmastery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "books", "authors", "reviews", "bookStats"
        );
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }
    
    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }
}
```

**Update BookService with caching:**
```java
@Service
public class BookServiceImpl implements BookService {
    
    // ... existing code ...
    
    @Cacheable(value = "books", key = "#id")
    @Override
    public Optional<Book> getBookById(Long id) {
        logger.debug("Fetching book from database with id: {}", id);
        if (id == null || id <= 0) {
            throw new BadRequestException("Invalid book ID: " + id);
        }
        return bookRepository.findById(id);
    }
    
    @Cacheable(value = "books", key = "'all'")
    @Override
    public List<Book> getAllBooks() {
        logger.debug("Fetching all books from database");
        return bookRepository.findAll();
    }
    
    @Cacheable(value = "authors", key = "#author")
    @Override
    public List<Book> searchBooksByAuthor(String author) {
        logger.debug("Searching books by author from database: {}", author);
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }
    
    @Cacheable(value = "bookStats", key = "'count'")
    @Override
    public Long getTotalBookCount() {
        logger.debug("Counting books in database");
        return bookRepository.count();
    }
    
    @CacheEvict(value = {"books", "authors", "bookStats"}, allEntries = true)
    @Override
    public Book createBook(Book book) {
        // ... existing validation logic ...
        return bookRepository.save(book);
    }
    
    @CacheEvict(value = {"books", "authors", "bookStats"}, allEntries = true)
    @Override
    public Book updateBook(Long id, Book bookDetails) {
        // ... existing logic ...
        return bookRepository.save(existingBook);
    }
    
    @CacheEvict(value = {"books", "authors", "bookStats"}, allEntries = true)
    @Override
    public void deleteBook(Long id) {
        // ... existing logic ...
        bookRepository.deleteById(id);
    }
    
    // Clear specific cache
    @CacheEvict(value = "books", key = "#id")
    public void evictBookCache(Long id) {
        logger.debug("Evicting cache for book id: {}", id);
    }
}
```

**Create a cache management endpoint:**
```java
package com.sujan.springbootmastery.controller;

import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/cache")
public class CacheController {
    
    private final CacheManager cacheManager;
    
    public CacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                var nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
                stats.put(cacheName, nativeCache.stats());
            }
        });
        
        return stats;
    }
    
    @DeleteMapping("/{cacheName}")
    public String clearCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return "Cache '" + cacheName + "' cleared successfully";
        }
        return "Cache '" + cacheName + "' not found";
    }
    
    @DeleteMapping("/{cacheName}/{key}")
    public String clearCacheEntry(@PathVariable String cacheName, @PathVariable String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            return "Cache entry '" + key + "' cleared from '" + cacheName + "'";
        }
        return "Cache '" + cacheName + "' not found";
    }
    
    @PostMapping("/clear-all")
    public String clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        return "All caches cleared successfully";
    }
}
```

### Step 2: Asynchronous Processing with @Async

**Enable async processing:**
```java
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class SpringBootMasteryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootMasteryApplication.class, args);
    }
}
```

**Configure async task executor:**
```java
package com.sujan.springbootmastery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("EmailThread-");
        executor.initialize();
        return executor;
    }
}
```

**Create async service:**
```java
package com.sujan.springbootmastery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    
    @Async("taskExecutor")
    public CompletableFuture<String> processBookData(Long bookId) {
        logger.info("Starting async processing for book ID: {}", bookId);
        
        try {
            // Simulate processing time
            Thread.sleep(5000);
            
            // Simulate some data processing
            String result = "Processed book data for ID: " + bookId;
            logger.info("Completed async processing for book ID: {}", bookId);
            
            return CompletableFuture.completedFuture(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture("Processing interrupted for book ID: " + bookId);
        }
    }
    
    @Async("emailExecutor")
    public CompletableFuture<Void> sendNotification(String recipient, String message) {
        logger.info("Sending notification to: {}", recipient);
        
        try {
            // Simulate email sending time
            Thread.sleep(2000);
            
            // Simulate email sending logic
            logger.info("Notification sent to {}: {}", recipient, message);
            
            return CompletableFuture.completedFuture(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Notification sending interrupted for: {}", recipient);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    @Async
    public void backgroundCleanup() {
        logger.info("Starting background cleanup task");
        
        try {
            // Simulate cleanup work
            Thread.sleep(30000); // 30 seconds
            
            logger.info("Background cleanup completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Background cleanup interrupted");
        }
    }
}
```

**Update BookController to use async:**
```java
@RestController
@RequestMapping("/api/books")
public class BookController {
    
    // ... existing fields ...
    private final AsyncService asyncService;
    
    public BookController(BookService bookService, AsyncService asyncService) {
        this.bookService = bookService;
        this.asyncService = asyncService;
    }
    
    // ... existing endpoints ...
    
    @PostMapping("/{id}/process")
    public CompletableFuture<ResponseEntity<Map<String, String>>> processBookData(@PathVariable Long id) {
        logger.info("Starting async processing for book ID: {}", id);
        
        return asyncService.processBookData(id)
                .thenApply(result -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", result);
                    response.put("timestamp", LocalDateTime.now().toString());
                    return ResponseEntity.accepted().body(response);
                })
                .exceptionally(ex -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("message", "Processing failed: " + ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                });
    }
    
    @PostMapping("/{id}/notify")
    public ResponseEntity<Map<String, String>> sendBookNotification(@PathVariable Long id) {
        logger.info("Sending notification for book ID: {}", id);
        
        // This will run asynchronously
        asyncService.sendNotification("admin@example.com", "Book processed: " + id);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "accepted");
        response.put("message", "Notification queued for book ID: " + id);
        
        return ResponseEntity.accepted().body(response);
    }
}
```

### Step 3: WebSocket Real-Time Communication

**Add WebSocket dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**Configure WebSocket:**
```java
package com.sujan.springbootmastery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
```

**Create WebSocket message models:**
```java
package com.sujan.springbootmastery.websocket;

import java.time.LocalDateTime;

public class WebSocketMessage {
    private String from;
    private String text;
    private LocalDateTime timestamp;
    
    // Constructors, getters, setters
    public WebSocketMessage() {}
    
    public WebSocketMessage(String from, String text) {
        this.from = from;
        this.text = text;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

package com.sujan.springbootmastery.websocket;

public class NotificationMessage {
    private String type;
    private String title;
    private String message;
    private Object data;
    
    // Constructors, getters, setters
    public NotificationMessage() {}
    
    public NotificationMessage(String type, String title, String message) {
        this.type = type;
        this.title = title;
        this.message = message;
    }
    
    public NotificationMessage(String type, String title, String message, Object data) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.data = data;
    }
    
    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
```

**Create WebSocket controller:**
```java
package com.sujan.springbootmastery.controller;

import com.sujan.springbootmastery.model.Book;
import com.sujan.springbootmastery.websocket.NotificationMessage;
import com.sujan.springbootmastery.websocket.WebSocketMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public WebSocketMessage handleChatMessage(WebSocketMessage message) {
        return message;
    }
    
    @MessageMapping("/notifications")
    public void handleNotification(NotificationMessage notification) {
        // Broadcast to all connected clients
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
    
    // Method to send book-related notifications
    public void sendBookNotification(Book book, String action) {
        NotificationMessage notification = new NotificationMessage(
            "BOOK_" + action.toUpperCase(),
            "Book " + action,
            "Book '" + book.getTitle() + "' has been " + action,
            book
        );
        
        messagingTemplate.convertAndSend("/topic/books", notification);
    }
    
    // Method to send system notifications
    public void sendSystemNotification(String title, String message) {
        NotificationMessage notification = new NotificationMessage(
            "SYSTEM",
            title,
            message
        );
        
        messagingTemplate.convertAndSend("/topic/system", notification);
    }
}
```

**Update BookService to send WebSocket notifications:**
```java
@Service
public class BookServiceImpl implements BookService {
    
    // ... existing fields ...
    private final WebSocketController webSocketController;
    
    public BookServiceImpl(BookRepository bookRepository, BookMetrics bookMetrics, 
                          WebSocketController webSocketController) {
        this.bookRepository = bookRepository;
        this.bookMetrics = bookMetrics;
        this.webSocketController = webSocketController;
    }
    
    @Override
    public Book createBook(Book book) {
        // ... existing validation logic ...
        
        Book savedBook = bookRepository.save(book);
        
        // Send WebSocket notification
        webSocketController.sendBookNotification(savedBook, "created");
        
        // Track metrics
        bookMetrics.incrementBooksCreated();
        bookMetrics.incrementGenreCount(book.getGenre());
        
        return savedBook;
    }
    
    @Override
    public Book updateBook(Long id, Book bookDetails) {
        // ... existing logic ...
        
        Book updatedBook = bookRepository.save(existingBook);
        
        // Send WebSocket notification
        webSocketController.sendBookNotification(updatedBook, "updated");
        
        bookMetrics.incrementBooksUpdated();
        
        return updatedBook;
    }
    
    @Override
    public void deleteBook(Long id) {
        // Check if book exists
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id.toString()));
        
        bookRepository.deleteById(id);
        
        // Send WebSocket notification
        webSocketController.sendBookNotification(book, "deleted");
        
        bookMetrics.incrementBooksDeleted();
        
        logger.info("Book with id {} deleted successfully", id);
    }
}
```

### Step 4: Spring Events and ApplicationListener

**Create custom events:**
```java
package com.sujan.springbootmastery.event;

import com.sujan.springbootmastery.model.Book;
import org.springframework.context.ApplicationEvent;

public class BookCreatedEvent extends ApplicationEvent {
    
    private final Book book;
    
    public BookCreatedEvent(Object source, Book book) {
        super(source);
        this.book = book;
    }
    
    public Book getBook() {
        return book;
    }
}

package com.sujan.springbootmastery.event;

import com.sujan.springbootmastery.model.Book;
import org.springframework.context.ApplicationEvent;

public class BookDeletedEvent extends ApplicationEvent {
    
    private final Book book;
    
    public BookDeletedEvent(Object source, Book book) {
        super(source);
        this.book = book;
    }
    
    public Book getBook() {
        return book;
    }
}
```

**Create event listeners:**
```java
package com.sujan.springbootmastery.listener;

import com.sujan.springbootmastery.event.BookCreatedEvent;
import com.sujan.springbootmastery.event.BookDeletedEvent;
import com.sujan.springbootmastery.service.AsyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class BookEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BookEventListener.class);
    private final AsyncService asyncService;
    
    public BookEventListener(AsyncService asyncService) {
        this.asyncService = asyncService;
    }
    
    @EventListener
    @Async
    public void handleBookCreated(BookCreatedEvent event) {
        logger.info("Handling book created event for: {}", event.getBook().getTitle());
        
        // Send notification asynchronously
        asyncService.sendNotification(
            "admin@example.com", 
            "New book created: " + event.getBook().getTitle()
        );
        
        // Perform other async tasks
        asyncService.backgroundCleanup();
    }
    
    @EventListener
    @Async
    public void handleBookDeleted(BookDeletedEvent event) {
        logger.info("Handling book deleted event for: {}", event.getBook().getTitle());
        
        // Send notification asynchronously
        asyncService.sendNotification(
            "admin@example.com", 
            "Book deleted: " + event.getBook().getTitle()
        );
    }
    
    @EventListener
    public void handleBookCreatedSync(BookCreatedEvent event) {
        // Synchronous processing
        logger.info("Synchronous processing for new book: {}", event.getBook().getTitle());
        
        // Update search index, send immediate notifications, etc.
    }
}
```

**Update BookService to publish events:**
```java
@Service
public class BookServiceImpl implements BookService {
    
    // ... existing fields ...
    private final ApplicationEventPublisher eventPublisher;
    
    public BookServiceImpl(BookRepository bookRepository, BookMetrics bookMetrics, 
                          WebSocketController webSocketController, 
                          ApplicationEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.bookMetrics = bookMetrics;
        this.webSocketController = webSocketController;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Book createBook(Book book) {
        // ... existing validation logic ...
        
        Book savedBook = bookRepository.save(book);
        
        // Publish event
        eventPublisher.publishEvent(new BookCreatedEvent(this, savedBook));
        
        // Send WebSocket notification
        webSocketController.sendBookNotification(savedBook, "created");
        
        // Track metrics
        bookMetrics.incrementBooksCreated();
        bookMetrics.incrementGenreCount(book.getGenre());
        
        return savedBook;
    }
    
    @Override
    public void deleteBook(Long id) {
        // Check if book exists
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id.toString()));
        
        bookRepository.deleteById(id);
        
        // Publish event
        eventPublisher.publishEvent(new BookDeletedEvent(this, book));
        
        // Send WebSocket notification
        webSocketController.sendBookNotification(book, "deleted");
        
        bookMetrics.incrementBooksDeleted();
        
        logger.info("Book with id {} deleted successfully", id);
    }
}
```

### Step 5: Retry Mechanism and Resilience

**Add resilience4j dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>2.1.0</version>
</dependency>
```

**Configure retry and circuit breaker:**
```yaml
# Add to application.yml
resilience4j:
  retry:
    instances:
      externalService:
        max-attempts: 3
        wait-duration: 2s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
  circuitbreaker:
    instances:
      externalService:
        failure-rate-threshold: 50
        slow-call-rate-threshold: 100
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
  timelimiter:
    instances:
      externalService:
        timeout-duration: 5s
```

**Create a resilient service:**
```java
package com.sujan.springbootmastery.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Service
public class ExternalApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalApiService.class);
    private final RestTemplate restTemplate;
    
    public ExternalApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Retry(name = "externalService")
    @CircuitBreaker(name = "externalService")
    public String callExternalApi(String url) {
        logger.info("Calling external API: {}", url);
        
        // Simulate external API call
        try {
            // In real scenario: restTemplate.getForObject(url, String.class)
            Thread.sleep(1000);
            
            // Simulate occasional failures
            if (Math.random() > 0.7) {
                throw new RuntimeException("External API temporarily unavailable");
            }
            
            return "Response from " + url;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Call interrupted", e);
        }
    }
    
    @TimeLimiter(name = "externalService")
    @CircuitBreaker(name = "externalService")
    @Retry(name = "externalService")
    public CompletableFuture<String> callExternalApiAsync(String url) {
        return CompletableFuture.supplyAsync(() -> callExternalApi(url));
    }
    
    // Fallback method for circuit breaker
    public String externalApiFallback(String url, Exception e) {
        logger.warn("Using fallback for external API call to: {}", url);
        return "Fallback response for " + url;
    }
}
```

### Step 6: Scheduling and Background Tasks

**Create scheduled tasks:**
```java
package com.sujan.springbootmastery.scheduler;

import com.sujan.springbootmastery.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private final BookService bookService;
    
    public ScheduledTasks(BookService bookService) {
        this.bookService = bookService;
    }
    
    // Run every 30 minutes
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void reportBookStatistics() {
        logger.info("Generating book statistics report");
        
        Long totalBooks = bookService.getTotalBookCount();
        Double averageYear = bookService.getAveragePublicationYear();
        
        logger.info("Book Statistics - Total: {}, Average Publication Year: {}", 
                   totalBooks, averageYear);
    }
    
    // Run every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyCleanup() {
        logger.info("Starting daily cleanup task");
        
        // Perform cleanup operations
        // - Remove temporary files
        // - Archive old logs
        // - Update statistics
        
        logger.info("Daily cleanup completed");
    }
    
    // Run every hour
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyHealthCheck() {
        logger.info("Performing hourly health check");
        
        // Check database connectivity
        // Verify external services
        // Send health report
        
        logger.info("Hourly health check completed");
    }
    
    // Run with initial delay and fixed rate
    @Scheduled(initialDelay = 60000, fixedRate = 300000) // 1 min delay, then every 5 mins
    public void cacheWarmup() {
        logger.info("Warming up frequently accessed data");
        
        // Pre-load cache with frequently accessed data
        bookService.getAllBooks();
        bookService.getTotalBookCount();
        
        logger.info("Cache warmup completed");
    }
}
```

### Step 7: Test Advanced Features

**Test caching:**
```bash
# First call - should hit database
curl http://localhost:8080/api/books/1

# Second call - should return from cache
curl http://localhost:8080/api/books/1

# Check cache statistics
curl http://localhost:8080/api/cache/stats

# Clear cache
curl -X DELETE http://localhost:8080/api/cache/books
```

**Test async processing:**
```bash
# Start async processing
curl -X POST http://localhost:8080/api/books/1/process

# Send notification (async)
curl -X POST http://localhost:8080/api/books/1/notify
```

**Test WebSocket (using browser console):**
```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to topics
    stompClient.subscribe('/topic/books', function(message) {
        console.log('Book notification:', JSON.parse(message.body));
    });
    
    stompClient.subscribe('/topic/notifications', function(message) {
        console.log('General notification:', JSON.parse(message.body));
    });
    
    // Send a message
    stompClient.send("/app/chat", {}, JSON.stringify({
        from: 'User',
        text: 'Hello WebSocket!'
    }));
});
```

### Day 12 Challenge

1. **Implement a cache warming strategy** that pre-loads frequently accessed data on startup
2. **Create a WebSocket-based real-time dashboard** showing application metrics
3. **Add rate limiting** to your async tasks to prevent resource exhaustion
4. **Implement a dead letter queue** for failed async operations

### Solution to Challenge #1 (Cache Warming)

```java
@Component
public class CacheWarmupService {
    
    private final BookService bookService;
    
    public CacheWarmupService(BookService bookService) {
        this.bookService = bookService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCaches() {
        // Warm up frequently accessed data on application startup
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting cache warmup...");
                
                // Pre-load books cache
                bookService.getAllBooks();
                
                // Pre-load book count
                bookService.getTotalBookCount();
                
                // Pre-load authors
                bookService.getAllAuthors();
                
                logger.info("Cache warmup completed successfully");
            } catch (Exception e) {
                logger.error("Cache warmup failed", e);
            }
        });
    }
}
```

### Key Takeaways from Day 12

✅ **Caching**: Improved performance with Caffeine cache  
✅ **Async Processing**: Non-blocking operations with `@Async`  
✅ **WebSocket**: Real-time bidirectional communication  
✅ **Events**: Loose coupling with Spring's event system  
✅ **Resilience**: Retry, circuit breaker, and timeout patterns  
✅ **Scheduling**: Automated background tasks with `@Scheduled`  
✅ **Performance**: Comprehensive performance optimization strategies  

### What's Next?

Tomorrow (Day 13) we'll focus on **API Documentation & Testing** - Swagger/OpenAPI documentation, advanced testing strategies, and API client generation!

**When you're ready for Day 13, just say "next"!** Practice today's concepts by implementing caching in your services and adding real-time features with WebSocket.