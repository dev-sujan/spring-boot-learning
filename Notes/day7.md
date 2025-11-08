Excellent! Let's move to **Day 7: Exception Handling & Proper Error Responses**.

## Day 7: Robust Error Handling in Spring Boot

### What We'll Accomplish Today
By the end of today, you'll have:
1. Created global exception handling with `@ControllerAdvice`
2. Implemented custom exception classes
3. Built consistent error response structures
4. Handled validation errors properly
5. Created specific HTTP status codes for different errors

---

### Step 1: Understanding the Problem with Current Error Handling

Currently, our application throws raw exceptions like:
- `RuntimeException` with generic messages
- `IllegalArgumentException` without proper HTTP status codes
- No consistent error response structure

This results in:
- **500 Internal Server Error** for all exceptions
- **Inconsistent error formats**
- **No proper logging** of errors
- **Poor client experience**

### Step 2: Create Custom Exception Classes

Let's create a hierarchy of custom exceptions:

**`src/main/java/com/sujan/springbootmastery/exception/ResourceNotFoundException.java`**
```java
package com.sujan.springbootmastery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
```

**`src/main/java/com/sujan/springbootmastery/exception/BadRequestException.java`**
```java
package com.sujan.springbootmastery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**`src/main/java/com/sujan/springbootmastery/exception/ValidationException.java`**
```java
package com.sujan.springbootmastery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {
    
    private final Map<String, String> errors;
    
    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
}
```

### Step 3: Create a Standard Error Response Structure

**`src/main/java/com/sujan/springbootmastery/dto/ErrorResponse.java`**
```java
package com.sujan.springbootmastery.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private int status;
    private String error;
    private String message;
    private String path;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private Map<String, String> fieldErrors;
    private List<String> globalErrors;
    
    // Constructors
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
    
    // Static factory methods
    public static ErrorResponse create(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path);
    }
    
    // Getters and Setters
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
    
    public List<String> getGlobalErrors() { return globalErrors; }
    public void setGlobalErrors(List<String> globalErrors) { this.globalErrors = globalErrors; }
}
```

### Step 4: Create Global Exception Handler

This is the core of our exception handling strategy:

**`src/main/java/com/sujan/springbootmastery/exception/GlobalExceptionHandler.java`**
```java
package com.sujan.springbootmastery.exception;

import com.sujan.springbootmastery.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // Handle Resource Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {
        
        logger.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.create(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    // Handle Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, 
            HttpServletRequest request) {
        
        logger.warn("Bad request: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.create(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle Validation Errors (from @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        logger.warn("Validation error: {}", ex.getMessage());
        
        // Extract field errors
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    fieldError -> fieldError.getDefaultMessage() != null ? 
                                fieldError.getDefaultMessage() : "Invalid value"
                ));
        
        // Extract global errors
        var globalErrors = ex.getBindingResult()
                .getGlobalErrors()
                .stream()
                .map(error -> error.getObjectName() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.create(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "One or more fields are invalid",
            request.getRequestURI()
        );
        
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setGlobalErrors(globalErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle ValidationException (our custom validation exception)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, 
            HttpServletRequest request) {
        
        logger.warn("Custom validation error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.create(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        errorResponse.setFieldErrors(ex.getErrors());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.create(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // Handle all other exceptions (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(
            Exception ex, 
            HttpServletRequest request) {
        
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.create(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### Step 5: Update Service Layer to Use Custom Exceptions

Now let's update our `BookServiceImpl` to use the custom exceptions:

**`src/main/java/com/sujan/springbootmastery/service/BookServiceImpl.java`**
```java
package com.sujan.springbootmastery.service;

// Import our custom exceptions
import com.sujan.springbootmastery.exception.BadRequestException;
import com.sujan.springbootmastery.exception.ResourceNotFoundException;
import com.sujan.springbootmastery.exception.ValidationException;

// ... existing imports ...

@Service
public class BookServiceImpl implements BookService {
    
    // ... existing fields and constructor ...
    
    @Override
    public Optional<Book> getBookById(Long id) {
        logger.debug("Fetching book with id: {}", id);
        if (id == null || id <= 0) {
            throw new BadRequestException("Invalid book ID: " + id);
        }
        return bookRepository.findById(id);
    }
    
    @Override
    public Book createBook(Book book) {
        logger.debug("Creating new book: {}", book.getTitle());
        
        // Business logic validation
        Map<String, String> errors = new HashMap<>();
        
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            errors.put("title", "Book title cannot be empty");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            errors.put("author", "Book author cannot be empty");
        }
        
        // Validate publication year
        if (book.getPublicationYear() != null) {
            int currentYear = java.time.Year.now().getValue();
            if (book.getPublicationYear() > currentYear) {
                errors.put("publicationYear", "Publication year cannot be in the future");
            }
            if (book.getPublicationYear() < 1000) {
                errors.put("publicationYear", "Publication year seems invalid");
            }
        }
        
        // If there are validation errors, throw ValidationException
        if (!errors.isEmpty()) {
            throw new ValidationException("Book validation failed", errors);
        }
        
        // Additional business logic: check if book with same title/author already exists
        boolean exists = bookRepository.findByTitleAndAuthor(
            book.getTitle(), book.getAuthor()).isPresent();
        if (exists) {
            throw new BadRequestException(
                "A book with the same title and author already exists: " + 
                book.getTitle() + " by " + book.getAuthor()
            );
        }
        
        return bookRepository.save(book);
    }
    
    @Override
    public Book updateBook(Long id, Book bookDetails) {
        logger.debug("Updating book with id: {}", id);
        
        // Find existing book
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id.toString()));
        
        // Validate input
        Map<String, String> errors = new HashMap<>();
        
        if (bookDetails.getTitle() != null) {
            if (bookDetails.getTitle().trim().isEmpty()) {
                errors.put("title", "Book title cannot be empty");
            } else {
                existingBook.setTitle(bookDetails.getTitle());
            }
        }
        
        if (bookDetails.getAuthor() != null) {
            if (bookDetails.getAuthor().trim().isEmpty()) {
                errors.put("author", "Book author cannot be empty");
            } else {
                existingBook.setAuthor(bookDetails.getAuthor());
            }
        }
        
        if (bookDetails.getPublicationYear() != null) {
            int currentYear = java.time.Year.now().getValue();
            if (bookDetails.getPublicationYear() > currentYear) {
                errors.put("publicationYear", "Publication year cannot be in the future");
            } else if (bookDetails.getPublicationYear() < 1000) {
                errors.put("publicationYear", "Publication year seems invalid");
            } else {
                existingBook.setPublicationYear(bookDetails.getPublicationYear());
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Book update validation failed", errors);
        }
        
        return bookRepository.save(existingBook);
    }
    
    @Override
    public void deleteBook(Long id) {
        logger.debug("Deleting book with id: {}", id);
        
        // Check if book exists
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book", "id", id.toString());
        }
        
        bookRepository.deleteById(id);
        logger.info("Book with id {} deleted successfully", id);
    }
    
    // ... rest of the methods remain similar ...
}
```

### Step 6: Add Validation to Book Entity

Let's enhance our Book entity with proper validation annotations:

**`src/main/java/com/sujan/springbootmastery/model/Book.java`**
```java
package com.sujan.springbootmastery.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "books")
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    @Column(nullable = false)
    private String title;
    
    @NotBlank(message = "Author is required")
    @Size(min = 1, max = 255, message = "Author must be between 1 and 255 characters")
    @Column(nullable = false)
    private String author;
    
    @Min(value = 1000, message = "Publication year must be after 1000")
    @Max(value = 2100, message = "Publication year seems too far in the future")
    @Column(name = "publication_year")
    private Integer publicationYear;
    
    // ... constructors, getters, setters, toString ...
}
```

### Step 7: Update Controller to Use @Valid

Update the controller to use `@Valid` for request body validation:

**`src/main/java/com/sujan/springbootmastery/controller/BookController.java`**
```java
package com.sujan.springbootmastery.controller;

// Add this import
import jakarta.validation.Valid;

// ... existing imports ...

@RestController
@RequestMapping("/api/books")
public class BookController {
    
    // ... existing fields and constructor ...
    
    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book) {
        logger.info("POST /api/books - Creating new book: {}", book.getTitle());
        try {
            Book createdBook = bookService.createBook(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
        } catch (ValidationException e) {
            // This will be handled by GlobalExceptionHandler
            throw e;
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @Valid @RequestBody Book bookDetails) {
        logger.info("PUT /api/books/{} - Updating book", id);
        Book updatedBook = bookService.updateBook(id, bookDetails);
        return ResponseEntity.ok(updatedBook);
    }
    
    // ... rest of the methods remain the same ...
}
```

### Step 8: Test the Exception Handling

Let's test our new exception handling. Start the application and test various error scenarios:

#### Test 1: Resource Not Found
```bash
# Try to get a non-existent book
curl -v http://localhost:8080/api/books/9999
```

**Expected Response:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Book not found with id : '9999'",
  "path": "/api/books/9999",
  "timestamp": "2024-01-15 10:30:00"
}
```

#### Test 2: Validation Error (Empty Title)
```bash
# Try to create a book with empty title
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title": "", "author": "Test Author", "publicationYear": 2024}' \
  -v
```

**Expected Response:**
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "path": "/api/books",
  "timestamp": "2024-01-15 10:31:00",
  "fieldErrors": {
    "title": "Title is required"
  }
}
```

#### Test 3: Custom Business Logic Validation
```bash
# Try to create a book with future publication year
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title": "Future Book", "author": "Test Author", "publicationYear": 2030}' \
  -v
```

**Expected Response:**
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Book validation failed",
  "path": "/api/books",
  "timestamp": "2024-01-15 10:32:00",
  "fieldErrors": {
    "publicationYear": "Publication year cannot be in the future"
  }
}
```

#### Test 4: Duplicate Book Prevention
```bash
# Try to create the same book twice
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title": "The Great Gatsby", "author": "F. Scott Fitzgerald", "publicationYear": 1925}'
```

**Expected Response:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "A book with the same title and author already exists: The Great Gatsby by F. Scott Fitzgerald",
  "path": "/api/books",
  "timestamp": "2024-01-15 10:33:00"
}
```

### Step 9: Add Repository Method for Duplicate Check

Add this method to `BookRepository`:

```java
Optional<Book> findByTitleAndAuthor(String title, String author);
```

### Step 10: Update Tests for Exception Handling

Update your tests to verify the new exception handling:

**Add to `BookServiceTest.java`:**
```java
@Test
void createBook_WithDuplicateTitleAndAuthor_ShouldThrowException() {
    // Arrange
    Book existingBook = new Book("Duplicate Book", "Same Author", 2020);
    when(bookRepository.findByTitleAndAuthor("Duplicate Book", "Same Author"))
            .thenReturn(Optional.of(existingBook));
    
    Book newBook = new Book("Duplicate Book", "Same Author", 2021);
    
    // Act & Assert
    assertThatThrownBy(() -> bookService.createBook(newBook))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("A book with the same title and author already exists");
    
    verify(bookRepository, never()).save(any(Book.class));
}

@Test
void updateBook_WithNonExistentId_ShouldThrowResourceNotFoundException() {
    // Arrange
    Book updatedDetails = new Book("Updated Title", "Updated Author", 2000);
    when(bookRepository.findById(999L)).thenReturn(Optional.empty());
    
    // Act & Assert
    assertThatThrownBy(() -> bookService.updateBook(999L, updatedDetails))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Book not found with id : '999'");
}
```

### Day 7 Challenge

1. **Create a new custom exception** `ConflictException` for 409 Conflict scenarios
2. **Add rate limiting protection** that throws a custom exception when too many requests are made
3. **Create specific error handlers** for database constraint violations

### Solution to Challenge #1

**Create `ConflictException.java`:**
```java
package com.sujan.springbootmastery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
}
```

**Add handler to `GlobalExceptionHandler`:**
```java
@ExceptionHandler(ConflictException.class)
public ResponseEntity<ErrorResponse> handleConflictException(
        ConflictException ex, 
        HttpServletRequest request) {
    
    logger.warn("Conflict: {}", ex.getMessage());
    
    ErrorResponse errorResponse = ErrorResponse.create(
        HttpStatus.CONFLICT.value(),
        "Conflict",
        ex.getMessage(),
        request.getRequestURI()
    );
    
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
}
```

### Key Takeaways from Day 7

✅ **Global Exception Handling**: `@ControllerAdvice` for centralized error handling  
✅ **Custom Exceptions**: Domain-specific exceptions with proper HTTP status codes  
✅ **Consistent Error Responses**: Standardized error response structure  
✅ **Validation Integration**: Proper handling of `@Valid` and validation errors  
✅ **Proper HTTP Status Codes**: 400, 404, 409, 500 etc. for different scenarios  
✅ **Error Logging**: Structured logging for different error types  

### What's Next?

Tomorrow (Day 8) we'll learn about **Database Migrations with Flyway** - version-controlled database schema management!

**When you're ready for Day 8, just say "next"!** Practice today's concepts by testing different error scenarios and adding more specific exception types.