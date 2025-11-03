Excellent! Let's move to **Day 3: Data Access with Spring Data JPA**.

## Day 3: Real Database Integration with Spring Data JPA

### What We'll Accomplish Today
By the end of today, you'll have:
1. Replaced in-memory storage with a real database
2. Used Spring Data JPA for automatic CRUD operations
3. Understood JPA annotations (`@Entity`, `@Id`, `@GeneratedValue`)
4. Created repository interfaces that automatically generate SQL
5. Explored the H2 database console

---

### Step 1: Add Dependencies

First, let's modify our `pom.xml` to add database dependencies. Add these inside the `<dependencies>` section:

**`pom.xml`** (add these dependencies):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

Your dependencies should now look like:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### Step 2: Configure H2 Database

Add database configuration to `src/main/resources/application.properties`:

**`application.properties`**:
```properties
# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# H2 Console (Web-based database viewer)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**What this configuration does:**
- `spring.datasource.url`: Uses in-memory H2 database named "testdb"
- `spring.jpa.hibernate.ddl-auto=create-drop`: Automatically creates tables from entities on startup, drops them on shutdown
- `spring.jpa.show-sql=true`: Shows SQL queries in console (great for learning!)
- `spring.h2.console.enabled=true`: Enables web-based database viewer

### Step 3: Convert Book to JPA Entity

Modify your `Book` class to be a proper JPA entity:

**`src/main/java/com/sujan/springbootmastery/model/Book.java`**
```java
package com.sujan.springbootmastery.model;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String author;
    
    @Column(name = "publication_year")
    private Integer publicationYear;
    
    // Constructors
    public Book() {}
    
    public Book(String title, String author, Integer publicationYear) {
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
    }
    
    // Getters and Setters (same as before)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    
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

**Key JPA Annotations:**
- `@Entity`: Marks this class as a JPA entity (maps to a database table)
- `@Table(name = "books")`: Specifies the table name (optional)
- `@Id`: Marks this field as the primary key
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Auto-generates ID values
- `@Column`: Optional, specifies column details like nullability

### Step 4: Create Repository Interface

This is where Spring Data JPA magic happens! Create a repository interface:

**`src/main/java/com/sujan/springbootmastery/repository/BookRepository.java`**
```java
package com.sujan.springbootmastery.repository;

import com.sujan.springbootmastery.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Custom query method - Spring Data JPA implements this automatically!
    List<Book> findByAuthorContainingIgnoreCase(String author);
    
    // Find books published after a certain year
    List<Book> findByPublicationYearAfter(Integer year);
    
    // Find books by title containing text (case-insensitive)
    List<Book> findByTitleContainingIgnoreCase(String title);
    
    // Find books by author and year
    List<Book> findByAuthorAndPublicationYear(String author, Integer year);
}
```

**The Magic of Spring Data JPA:**
- `JpaRepository<Book, Long>` gives us CRUD methods automatically:
  - `save()`, `findAll()`, `findById()`, `deleteById()`, `count()`, etc.
- **No implementation needed!** Spring creates the implementation at runtime
- **Custom queries** by method naming convention - Spring generates the SQL!

### Step 5: Update the Controller to Use Repository

Replace the in-memory list with the repository:

**`src/main/java/com/sujan/springbootmastery/controller/BookController.java`**
```java
package com.sujan.springbootmastery.controller;

import com.sujan.springbootmastery.model.Book;
import com.sujan.springbootmastery.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/books")
public class BookController {
    
    @Autowired
    private BookRepository bookRepository;
    
    // Initialize with sample data
    @PostConstruct
    public void init() {
        // Clear any existing data and add sample books
        bookRepository.deleteAll();
        
        bookRepository.save(new Book("The Great Gatsby", "F. Scott Fitzgerald", 1925));
        bookRepository.save(new Book("To Kill a Mockingbird", "Harper Lee", 1960));
        bookRepository.save(new Book("1984", "George Orwell", 1949));
        bookRepository.save(new Book("The Hobbit", "J.R.R. Tolkien", 1937));
    }
    
    // GET /api/books - Get all books
    @GetMapping
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    // GET /api/books/{id} - Get a specific book by ID
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        Optional<Book> book = bookRepository.findById(id);
        return book.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    // POST /api/books - Create a new book
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookRepository.save(book);
    }
    
    // PUT /api/books/{id} - Update an existing book
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        Optional<Book> optionalBook = bookRepository.findById(id);
        
        if (optionalBook.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Book book = optionalBook.get();
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setPublicationYear(bookDetails.getPublicationYear());
        
        Book updatedBook = bookRepository.save(book);
        return ResponseEntity.ok(updatedBook);
    }
    
    // DELETE /api/books/{id} - Delete a book
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return ResponseEntity.ok("Book deleted successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // GET /api/books/search - Search books by author (using custom repository method)
    @GetMapping("/search")
    public List<Book> searchBooksByAuthor(@RequestParam String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }
    
    // GET /api/books/published-after/{year} - Using custom repository method
    @GetMapping("/published-after/{year}")
    public List<Book> getBooksPublishedAfter(@PathVariable Integer year) {
        return bookRepository.findByPublicationYearAfter(year);
    }
    
    // GET /api/books/title-search - Search by title
    @GetMapping("/title-search")
    public List<Book> searchBooksByTitle(@RequestParam String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }
}
```

**Important Notes:**
- `@Autowired`: Injects the BookRepository dependency
- `@PostConstruct`: Runs after dependency injection to initialize sample data
- `Optional<T>`: Better handling of potentially null values
- `ResponseEntity`: Provides more control over HTTP responses

### Step 6: Run and Test Your Application

Start your application and watch the console. You should see SQL output like:
```
Hibernate: drop table if exists books cascade
Hibernate: create table books (id bigint generated by default as identity, author varchar(255) not null, publication_year integer, title varchar(255) not null, primary key (id))
Hibernate: insert into books (author, publication_year, title) values (?, ?, ?)
```

#### Test the Endpoints (same URLs as yesterday, but now with real database):

**1. GET All Books**
```bash
curl http://localhost:8080/api/books
```

**2. Create a New Book**
```bash
curl -X POST http://localhost:8080/api/books \
-H "Content-Type: application/json" \
-d '{
  "title": "Pride and Prejudice",
  "author": "Jane Austen", 
  "publicationYear": 1813
}'
```

**3. Search by Author**
```bash
curl "http://localhost:8080/api/books/search?author=orwell"
```

**4. Books Published After Year**
```bash
curl http://localhost:8080/api/books/published-after/1950
```

### Step 7: Explore H2 Database Console

One of the coolest features for development! Open your browser and go to:
```
http://localhost:8080/h2-console
```

**Login Information:**
- JDBC URL: `jdbc:h2:mem:testdb`
- User Name: `sa`
- Password: (leave empty)

Click "Connect" and you can:
- See the `BOOKS` table structure
- Run SQL queries: `SELECT * FROM BOOKS;`
- See all your data visually!

### Step 8: Add Data Validation (Bonus)

Let's add some validation to our Book entity. First, add this dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Then update your Book entity:

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "books")
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is mandatory")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;
    
    @NotBlank(message = "Author is mandatory")
    private String author;
    
    // ... rest of the class unchanged
}
```

Update the controller method to use validation:

```java
@PostMapping
public ResponseEntity<Book> createBook(@Valid @RequestBody Book book) {
    Book savedBook = bookRepository.save(book);
    return ResponseEntity.ok(savedBook);
}
```

Now if you try to create a book with empty title:
```bash
curl -X POST http://localhost:8080/api/books \
-H "Content-Type: application/json" \
-d '{"title": "", "author": "Test Author", "publicationYear": 2024}'
```

You'll get a validation error!

### Day 3 Challenge

1. **Add a new field** `genre` to the Book entity and update all endpoints
2. **Create a custom query method** in the repository to find books by genre
3. **Add an endpoint** to get book statistics (count by author, etc.)

### Solution to Challenge #1

**Add to Book entity:**
```java
private String genre;

// Add getter and setter
public String getGenre() { return genre; }
public void setGenre(String genre) { this.genre = genre; }
```

**Add to BookRepository:**
```java
List<Book> findByGenre(String genre);
```

**The table will auto-update on restart!**

### Key Takeaways from Day 3

✅ **Spring Data JPA**: Automatic repository implementation  
✅ **JPA Entities**: Map Java objects to database tables  
✅ **H2 Database**: In-memory database for development  
✅ **Custom Query Methods**: Spring generates SQL from method names  
✅ **H2 Console**: Visual database inspection  
✅ **Data Validation**: Input validation with Spring Boot  

### What's Next?

Tomorrow (Day 4) we'll learn about **Configuration & Profiles** - how to manage different environments (dev, prod) and externalize configuration!

**When you're ready for Day 4, just say "next"!** Practice today's concepts by adding more fields and custom query methods to your Book entity.