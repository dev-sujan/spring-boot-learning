Great! Let's move to **Day 2: Building a RESTful Web Service**.

## Day 2: Building a Proper REST API

### What We'll Accomplish Today
By the end of today, you'll have:
1. Created a proper REST API with multiple endpoints
2. Used different HTTP methods (GET, POST, PUT, DELETE)
3. Worked with `@PathVariable` and `@RequestBody`
4. Implemented proper REST conventions

---

### Step 1: Understanding REST Principles

REST (Representational State Transfer) is an architectural style for designing networked applications. Key concepts:

- **Resources**: Everything is a resource (books, users, products)
- **HTTP Methods**: 
  - `GET` - Retrieve resources
  - `POST` - Create new resources
  - `PUT` - Update existing resources
  - `DELETE` - Delete resources
- **Stateless**: Each request contains all information needed to process it
- **URI Design**: Clean, meaningful URLs (`/books`, `/books/1`)

### Step 2: Create the Domain Model

Let's create a **Book Management API**. First, create a `Book` model class:

**`src/main/java/com/sujan/springbootmastery/model/Book.java`**
```java
package com.sujan.springbootmastery.model;

public class Book {
    private Long id;
    private String title;
    private String author;
    private Integer publicationYear;
    
    // Constructors
    public Book() {}
    
    public Book(Long id, String title, String author, Integer publicationYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public Integer getPublicationYear() {
        return publicationYear;
    }
    
    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }
    
    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publicationYear=" + publicationYear +
                '}';
    }
}
```

### Step 3: Create the REST Controller

Now, let's create a comprehensive REST controller:

**`src/main/java/com/sujan/springbootmastery/controller/BookController.java`**
```java
package com.sujan.springbootmastery.controller;

import com.sujan.springbootmastery.model.Book;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/books")  // Base path for all endpoints in this controller
public class BookController {
    
    // Temporary in-memory "database" - we'll replace this with real database tomorrow
    private List<Book> books = new ArrayList<>();
    private AtomicLong nextId = new AtomicLong(1);  // Thread-safe ID generator
    
    // Initialize with some sample data
    public BookController() {
        books.add(new Book(nextId.getAndIncrement(), "The Great Gatsby", "F. Scott Fitzgerald", 1925));
        books.add(new Book(nextId.getAndIncrement(), "To Kill a Mockingbird", "Harper Lee", 1960));
        books.add(new Book(nextId.getAndIncrement(), "1984", "George Orwell", 1949));
    }
    
    // GET /api/books - Get all books
    @GetMapping
    public List<Book> getAllBooks() {
        return books;
    }
    
    // GET /api/books/{id} - Get a specific book by ID
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return books.stream()
                .filter(book -> book.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }
    
    // POST /api/books - Create a new book
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        book.setId(nextId.getAndIncrement());
        books.add(book);
        return book;
    }
    
    // PUT /api/books/{id} - Update an existing book
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        Book existingBook = getBookById(id);
        existingBook.setTitle(bookDetails.getTitle());
        existingBook.setAuthor(bookDetails.getAuthor());
        existingBook.setPublicationYear(bookDetails.getPublicationYear());
        return existingBook;
    }
    
    // DELETE /api/books/{id} - Delete a book
    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable Long id) {
        Book bookToDelete = getBookById(id);
        books.remove(bookToDelete);
        return "Book deleted successfully: " + bookToDelete.getTitle();
    }
    
    // GET /api/books/search - Search books by author
    @GetMapping("/search")
    public List<Book> searchBooksByAuthor(@RequestParam String author) {
        return books.stream()
                .filter(book -> book.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .toList();
    }
}
```

### Step 4: Understanding the Annotations

Let's break down the new annotations:

- **`@RequestMapping("/api/books")`**: Sets the base URL for all methods in this controller
- **`@PathVariable****: Extracts values from the URL path (`/api/books/1` → `id=1`)
- **`@RequestBody`**: Binds the HTTP request body to a method parameter (converts JSON to Java object)
- **`@RequestParam`**: Extracts query parameters from the URL (`/api/books/search?author=Orwell`)

### Step 5: Test Your REST API

Start your application and use Postman (recommended) or curl to test:

#### **1. GET All Books**
```bash
# Using curl
curl http://localhost:8080/api/books
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald",
    "publicationYear": 1925
  },
  {
    "id": 2,
    "title": "To Kill a Mockingbird",
    "author": "Harper Lee",
    "publicationYear": 1960
  },
  {
    "id": 3,
    "title": "1984",
    "author": "George Orwell",
    "publicationYear": 1949
  }
]
```

#### **2. GET Book by ID**
```bash
curl http://localhost:8080/api/books/2
```

**Expected Response:**
```json
{
  "id": 2,
  "title": "To Kill a Mockingbird",
  "author": "Harper Lee",
  "publicationYear": 1960
}
```

#### **3. POST - Create New Book**
```bash
curl -X POST http://localhost:8080/api/books \
-H "Content-Type: application/json" \
-d '{
  "title": "The Hobbit",
  "author": "J.R.R. Tolkien",
  "publicationYear": 1937
}'
```

**Expected Response:**
```json
{
  "id": 4,
  "title": "The Hobbit",
  "author": "J.R.R. Tolkien",
  "publicationYear": 1937
}
```

#### **4. PUT - Update Book**
```bash
curl -X PUT http://localhost:8080/api/books/1 \
-H "Content-Type: application/json" \
-d '{
  "title": "The Great Gatsby (Updated)",
  "author": "F. Scott Fitzgerald",
  "publicationYear": 1925
}'
```

#### **5. DELETE - Remove Book**
```bash
curl -X DELETE http://localhost:8080/api/books/3
```

#### **6. GET - Search Books**
```bash
curl "http://localhost:8080/api/books/search?author=Fitzgerald"
```

### Step 6: Improve Error Handling

Let's add basic error handling. Modify the `getBookById` method and add a helper:

```java
// Add this method to handle not found scenarios consistently
private Book findBookById(Long id) {
    return books.stream()
            .filter(book -> book.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
}

// Update the existing method to use the helper
@GetMapping("/{id}")
public Book getBookById(@PathVariable Long id) {
    return findBookById(id);
}

// Also update PUT and DELETE methods to use findBookById instead of getBookById
```

### Step 7: Test the Error Cases

Try accessing a non-existent book:
```bash
curl http://localhost:8080/api/books/999
```

You should see an error response (we'll improve this with proper exception handling later).

### Day 2 Challenge

1. **Add a new endpoint** `GET /api/books/published-after/{year}` that returns books published after a given year
2. **Add validation** to ensure that when creating a book, title and author are not empty
3. **Create a new resource** `Author` with its own controller and endpoints

### Solution to Challenge #1

```java
// Add this to BookController
@GetMapping("/published-after/{year}")
public List<Book> getBooksPublishedAfter(@PathVariable Integer year) {
    return books.stream()
            .filter(book -> book.getPublicationYear() > year)
            .toList();
}
```

Test it with:
```bash
curl http://localhost:8080/api/books/published-after/1950
```

### Key Takeaways from Day 2

✅ **RESTful Design**: Clean URL structures and proper HTTP method usage  
✅ **Spring MVC Annotations**: `@PathVariable`, `@RequestBody`, `@RequestParam`  
✅ **CRUD Operations**: Implemented all Create, Read, Update, Delete operations  
✅ **API Testing**: Learned to test APIs with curl/Postman  
✅ **Temporary Storage**: Used in-memory list (tomorrow we'll replace with real database)  

### What's Next?

Tomorrow (Day 3) we'll replace this temporary in-memory storage with a **real database using Spring Data JPA**! This is where Spring Boot really shines.

**Quick Preview of Day 3**: We'll:
- Add a real database (H2 in-memory for development)
- Use Spring Data JPA for automatic CRUD operations
- Replace our manual list with a proper repository

When you're ready to continue, just say "next"! Practice today's concepts until you're comfortable creating and testing all the endpoints.