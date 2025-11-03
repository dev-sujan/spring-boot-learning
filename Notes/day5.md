Excellent! Let's move to **Day 5: Dependency Injection & The Service Layer**.

## Day 5: Proper Architecture with Services and Dependency Injection

### What We'll Accomplish Today
By the end of today, you'll have:
1. Implemented proper layered architecture (Controller → Service → Repository)
2. Mastered Dependency Injection with constructor injection
3. Created business logic in service classes
4. Understood Spring stereotypes (`@Service`, `@Component`)
5. Learned why service layer is crucial for maintainable code

---

### Step 1: Understanding the Problem with Our Current Architecture

Currently, our `BookController` has two responsibilities:
1. **HTTP handling** (request/response mapping)
2. **Business logic** (data validation, processing, rules)

This violates the **Single Responsibility Principle**. Let's fix this!

### Step 2: Create the Service Layer

First, let's create a service interface and implementation:

**`src/main/java/com/john/springbootmastery/service/BookService.java`** (Interface)
```java
package com.john.springbootmastery.service;

import com.john.springbootmastery.model.Book;

import java.util.List;
import java.util.Optional;

public interface BookService {
    
    // Get all books
    List<Book> getAllBooks();
    
    // Get book by ID
    Optional<Book> getBookById(Long id);
    
    // Create a new book
    Book createBook(Book book);
    
    // Update an existing book
    Book updateBook(Long id, Book bookDetails);
    
    // Delete a book
    void deleteBook(Long id);
    
    // Search books by author
    List<Book> searchBooksByAuthor(String author);
    
    // Find books published after a certain year
    List<Book> findBooksPublishedAfter(Integer year);
    
    // Search books by title
    List<Book> searchBooksByTitle(String title);
    
    // Get books by author and year
    List<Book> findByAuthorAndYear(String author, Integer year);
    
    // Business logic methods
    boolean isBookAvailable(Long id);
    Long getTotalBookCount();
    List<String> getAllAuthors();
}
```

Now, create the implementation:

**`src/main/java/com/john/springbootmastery/service/BookServiceImpl.java`**
```java
package com.john.springbootmastery.service;

import com.john.springbootmastery.model.Book;
import com.john.springbootmastery.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookServiceImpl implements BookService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);
    
    private final BookRepository bookRepository;
    
    // Constructor Injection (Recommended approach)
    @Autowired
    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        logger.info("BookService initialized with BookRepository");
    }
    
    @Override
    public List<Book> getAllBooks() {
        logger.debug("Fetching all books");
        return bookRepository.findAll();
    }
    
    @Override
    public Optional<Book> getBookById(Long id) {
        logger.debug("Fetching book with id: {}", id);
        if (id == null || id <= 0) {
            logger.warn("Invalid book ID: {}", id);
            return Optional.empty();
        }
        return bookRepository.findById(id);
    }
    
    @Override
    public Book createBook(Book book) {
        logger.debug("Creating new book: {}", book.getTitle());
        
        // Business logic validation
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Book author cannot be empty");
        }
        
        // Additional business logic could go here
        // For example: check if book with same title/author already exists
        
        return bookRepository.save(book);
    }
    
    @Override
    public Book updateBook(Long id, Book bookDetails) {
        logger.debug("Updating book with id: {}", id);
        
        // Find existing book
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        
        // Validate input
        if (bookDetails.getTitle() != null && !bookDetails.getTitle().trim().isEmpty()) {
            existingBook.setTitle(bookDetails.getTitle());
        }
        if (bookDetails.getAuthor() != null && !bookDetails.getAuthor().trim().isEmpty()) {
            existingBook.setAuthor(bookDetails.getAuthor());
        }
        if (bookDetails.getPublicationYear() != null) {
            existingBook.setPublicationYear(bookDetails.getPublicationYear());
        }
        
        return bookRepository.save(existingBook);
    }
    
    @Override
    public void deleteBook(Long id) {
        logger.debug("Deleting book with id: {}", id);
        
        // Check if book exists
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found with id: " + id);
        }
        
        bookRepository.deleteById(id);
        logger.info("Book with id {} deleted successfully", id);
    }
    
    @Override
    public List<Book> searchBooksByAuthor(String author) {
        logger.debug("Searching books by author: {}", author);
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }
    
    @Override
    public List<Book> findBooksPublishedAfter(Integer year) {
        logger.debug("Finding books published after: {}", year);
        return bookRepository.findByPublicationYearAfter(year);
    }
    
    @Override
    public List<Book> searchBooksByTitle(String title) {
        logger.debug("Searching books by title: {}", title);
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }
    
    @Override
    public List<Book> findByAuthorAndYear(String author, Integer year) {
        logger.debug("Finding books by author {} and year {}", author, year);
        return bookRepository.findByAuthorAndPublicationYear(author, year);
    }
    
    @Override
    public boolean isBookAvailable(Long id) {
        logger.debug("Checking availability for book id: {}", id);
        return bookRepository.existsById(id);
    }
    
    @Override
    public Long getTotalBookCount() {
        long count = bookRepository.count();
        logger.debug("Total book count: {}", count);
        return count;
    }
    
    @Override
    public List<String> getAllAuthors() {
        logger.debug("Fetching all unique authors");
        return bookRepository.findAll().stream()
                .map(Book::getAuthor)
                .distinct()
                .toList();
    }
}
```

### Step 3: Understanding Spring Stereotypes

**Spring Stereotypes:**
- `@Service`: Business logic facade, typically used at service layer
- `@Repository`: Data access layer, already used in our repository
- `@Component**: Generic stereotype for any Spring-managed component
- `@Controller`/`@RestController`: Web layer components

### Step 4: Refactor the Controller to Use Service Layer

Now let's simplify the controller to only handle HTTP concerns:

**`src/main/java/com/john/springbootmastery/controller/BookController.java`**
```java
package com.john.springbootmastery.controller;

import com.john.springbootmastery.model.Book;
import com.john.springbootmastery.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    
    private final BookService bookService;
    
    // Constructor Injection - BEST PRACTICE!
    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
        logger.info("BookController initialized with BookService");
    }
    
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        logger.info("GET /api/books - Fetching all books");
        List<Book> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        logger.info("GET /api/books/{} - Fetching book by ID", id);
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        logger.info("POST /api/books - Creating new book: {}", book.getTitle());
        try {
            Book createdBook = bookService.createBook(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error while creating book: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        logger.info("PUT /api/books/{} - Updating book", id);
        try {
            Book updatedBook = bookService.updateBook(id, bookDetails);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            logger.warn("Book not found for update: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {
        logger.info("DELETE /api/books/{} - Deleting book", id);
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok("Book deleted successfully");
        } catch (RuntimeException e) {
            logger.warn("Book not found for deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooksByAuthor(@RequestParam String author) {
        logger.info("GET /api/books/search - Searching by author: {}", author);
        List<Book> books = bookService.searchBooksByAuthor(author);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/published-after/{year}")
    public ResponseEntity<List<Book>> getBooksPublishedAfter(@PathVariable Integer year) {
        logger.info("GET /api/books/published-after/{} - Finding books", year);
        List<Book> books = bookService.findBooksPublishedAfter(year);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/title-search")
    public ResponseEntity<List<Book>> searchBooksByTitle(@RequestParam String title) {
        logger.info("GET /api/books/title-search - Searching by title: {}", title);
        List<Book> books = bookService.searchBooksByTitle(title);
        return ResponseEntity.ok(books);
    }
    
    // New endpoints using business logic from service
    
    @GetMapping("/stats/count")
    public ResponseEntity<Long> getTotalBookCount() {
        logger.info("GET /api/books/stats/count - Getting total book count");
        Long count = bookService.getTotalBookCount();
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/authors")
    public ResponseEntity<List<String>> getAllAuthors() {
        logger.info("GET /api/books/stats/authors - Getting all authors");
        List<String> authors = bookService.getAllAuthors();
        return ResponseEntity.ok(authors);
    }
    
    @GetMapping("/{id}/available")
    public ResponseEntity<Boolean> isBookAvailable(@PathVariable Long id) {
        logger.info("GET /api/books/{}/available - Checking availability", id);
        boolean available = bookService.isBookAvailable(id);
        return ResponseEntity.ok(available);
    }
}
```

### Step 5: Understanding Dependency Injection Types

Spring supports three types of dependency injection:

1. **Constructor Injection (RECOMMENDED)** - What we used
2. **Setter Injection**
3. **Field Injection** (not recommended)

Let's see the differences:

**Constructor Injection (Our approach - BEST):**
```java
private final BookService bookService;

@Autowired
public BookController(BookService bookService) {
    this.bookService = bookService;
}
```

**Setter Injection:**
```java
private BookService bookService;

@Autowired
public void setBookService(BookService bookService) {
    this.bookService = bookService;
}
```

**Field Injection (NOT RECOMMENDED):**
```java
@Autowired
private BookService bookService;
```

**Why Constructor Injection is Best:**
- Immutable dependencies (using `final`)
- Clear dependencies at construction time
- Easier testing
- No circular dependency issues

### Step 6: Update Data Initializer to Use Service

Update our data initializer to use the service layer:

**`src/main/java/com/john/springbootmastery/config/DataInitializer.java`**
```java
package com.john.springbootmastery.config;

import com.john.springbootmastery.model.Book;
import com.john.springbootmastery.service.BookService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final BookService bookService;
    
    @Autowired
    public DataInitializer(BookService bookService) {
        this.bookService = bookService;
    }
    
    @Profile("dev")
    @PostConstruct
    public void initDevData() {
        logger.info("Initializing DEVELOPMENT data using BookService...");
        
        // Service would handle any existing data cleanup
        try {
            bookService.createBook(new Book("The Great Gatsby", "F. Scott Fitzgerald", 1925));
            bookService.createBook(new Book("To Kill a Mockingbird", "Harper Lee", 1960));
            bookService.createBook(new Book("1984", "George Orwell", 1949));
            bookService.createBook(new Book("The Hobbit", "J.R.R. Tolkien", 1937));
            
            logger.info("Development data initialized with {} books", 
                       bookService.getTotalBookCount());
        } catch (Exception e) {
            logger.error("Error initializing dev data", e);
        }
    }
    
    @Profile("test")
    @PostConstruct
    public void initTestData() {
        logger.info("Initializing TEST data using BookService...");
        
        try {
            bookService.createBook(new Book("Test Book 1", "Test Author", 2020));
            bookService.createBook(new Book("Test Book 2", "Test Author", 2021));
            
            logger.info("Test data initialized with {} books", 
                       bookService.getTotalBookCount());
        } catch (Exception e) {
            logger.error("Error initializing test data", e);
        }
    }
}
```

### Step 7: Test the Refactored Application

Start your application and test the endpoints:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

**Test the new business endpoints:**
```bash
# Get total book count
curl http://localhost:8080/api/books/stats/count

# Get all authors
curl http://localhost:8080/api/books/stats/authors

# Check if book is available
curl http://localhost:8080/api/books/1/available
```

**Test CRUD operations (they should work exactly as before):**
```bash
# Get all books
curl http://localhost:8080/api/books

# Create a new book
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title": "New Service Layer Book", "author": "Service Expert", "publicationYear": 2024}'
```

### Step 8: Add More Business Logic

Let's add more complex business logic to demonstrate the power of the service layer:

**Add to `BookService` interface:**
```java
// Business analysis methods
Map<String, Long> getBooksCountByAuthor();
List<Book> getRecentBooks(int yearsBack);
Double getAveragePublicationYear();
```

**Add to `BookServiceImpl`:**
```java
@Override
public Map<String, Long> getBooksCountByAuthor() {
    logger.debug("Calculating book count by author");
    
    return bookRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                Book::getAuthor,
                Collectors.counting()
            ));
}

@Override
public List<Book> getRecentBooks(int yearsBack) {
    logger.debug("Finding books published in last {} years", yearsBack);
    
    int currentYear = java.time.Year.now().getValue();
    int targetYear = currentYear - yearsBack;
    
    return bookRepository.findAll().stream()
            .filter(book -> book.getPublicationYear() != null)
            .filter(book -> book.getPublicationYear() >= targetYear)
            .toList();
}

@Override
public Double getAveragePublicationYear() {
    logger.debug("Calculating average publication year");
    
    return bookRepository.findAll().stream()
            .filter(book -> book.getPublicationYear() != null)
            .mapToInt(Book::getPublicationYear)
            .average()
            .orElse(0.0);
}
```

**Add new endpoints to `BookController`:**
```java
@GetMapping("/stats/by-author")
public ResponseEntity<Map<String, Long>> getBooksCountByAuthor() {
    logger.info("GET /api/books/stats/by-author - Getting book count by author");
    Map<String, Long> stats = bookService.getBooksCountByAuthor();
    return ResponseEntity.ok(stats);
}

@GetMapping("/recent/{years}")
public ResponseEntity<List<Book>> getRecentBooks(@PathVariable int years) {
    logger.info("GET /api/books/recent/{} - Getting recent books", years);
    List<Book> recentBooks = bookService.getRecentBooks(years);
    return ResponseEntity.ok(recentBooks);
}

@GetMapping("/stats/average-year")
public ResponseEntity<Double> getAveragePublicationYear() {
    logger.info("GET /api/books/stats/average-year - Getting average publication year");
    Double averageYear = bookService.getAveragePublicationYear();
    return ResponseEntity.ok(averageYear);
}
```

### Day 5 Challenge

1. **Create a new service** `AuthorService` that manages author-specific operations
2. **Add validation** in the service layer for publication year (can't be in the future)
3. **Implement a feature** to find the most popular author (most books)

### Solution to Challenge #2

**Add to `BookServiceImpl.createBook()`:**
```java
// Validate publication year
if (book.getPublicationYear() != null) {
    int currentYear = java.time.Year.now().getValue();
    if (book.getPublicationYear() > currentYear) {
        throw new IllegalArgumentException("Publication year cannot be in the future");
    }
}
```

### Key Takeaways from Day 5

✅ **Layered Architecture**: Clear separation (Controller → Service → Repository)  
✅ **Dependency Injection**: Constructor injection best practices  
✅ **Spring Stereotypes**: `@Service`, `@Component` understanding  
✅ **Business Logic**: Moved from controller to service layer  
✅ **Single Responsibility**: Each layer has one clear purpose  
✅ **Testability**: Services are much easier to unit test  

### What's Next?

Tomorrow (Day 6) we'll focus on **Testing** - writing unit tests, integration tests, and mocking dependencies!

**When you're ready for Day 6, just say "next"!** Practice today's concepts by adding more business logic to your service layer.