Excellent! Let's move to **Day 6: Testing - The Key to Reliable Applications**.

## Day 6: Mastering Testing in Spring Boot

### What We'll Accomplish Today
By the end of today, you'll have:
1. Written comprehensive unit tests with JUnit 5 and Mockito
2. Created integration tests for repositories and controllers
3. Used `@SpringBootTest`, `@DataJpaTest`, and `@WebMvcTest`
4. Learned mocking with `@MockBean`
5. Understood test profiles and test configuration

---

### Step 1: Understanding Spring Boot Testing Annotations

Spring Boot provides several testing annotations for different scenarios:

- **`@SpringBootTest`**: Full application context integration tests
- **`@DataJpaTest`**: Repository layer tests with embedded database
- **`@WebMvcTest`**: Web layer tests (controllers) with mocked services
- **`@MockBean`**: Creates and injects Mockito mocks

### Step 2: Set Up Test Dependencies

Spring Boot Starter Test already includes everything we need. Check your `pom.xml` has:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

This includes:
- JUnit 5
- Mockito
- AssertJ
- Spring Test
- JSON Assert
- JSON Path

### Step 3: Create Unit Tests for Service Layer

Let's start with unit testing our `BookService`. We'll use Mockito to mock the repository.

**`src/test/java/com/sujan/springbootmastery/service/BookServiceTest.java`**
```java
package com.sujan.springbootmastery.service;

import com.sujan.springbootmastery.model.Book;
import com.sujan.springbootmastery.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @InjectMocks
    private BookServiceImpl bookService;
    
    private Book book1;
    private Book book2;
    
    @BeforeEach
    void setUp() {
        // Setup test data
        book1 = new Book("The Great Gatsby", "F. Scott Fitzgerald", 1925);
        book1.setId(1L);
        
        book2 = new Book("To Kill a Mockingbird", "Harper Lee", 1960);
        book2.setId(2L);
    }
    
    @Test
    void getAllBooks_ShouldReturnAllBooks() {
        // Arrange
        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));
        
        // Act
        List<Book> books = bookService.getAllBooks();
        
        // Assert
        assertThat(books).hasSize(2);
        assertThat(books).contains(book1, book2);
        verify(bookRepository, times(1)).findAll();
    }
    
    @Test
    void getBookById_WithValidId_ShouldReturnBook() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));
        
        // Act
        Optional<Book> foundBook = bookService.getBookById(1L);
        
        // Assert
        assertThat(foundBook).isPresent();
        assertThat(foundBook.get().getTitle()).isEqualTo("The Great Gatsby");
        verify(bookRepository, times(1)).findById(1L);
    }
    
    @Test
    void getBookById_WithInvalidId_ShouldReturnEmpty() {
        // Arrange
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        Optional<Book> foundBook = bookService.getBookById(999L);
        
        // Assert
        assertThat(foundBook).isEmpty();
        verify(bookRepository, times(1)).findById(999L);
    }
    
    @Test
    void getBookById_WithNullId_ShouldReturnEmpty() {
        // Act
        Optional<Book> foundBook = bookService.getBookById(null);
        
        // Assert
        assertThat(foundBook).isEmpty();
        verify(bookRepository, never()).findById(anyLong());
    }
    
    @Test
    void createBook_WithValidBook_ShouldSaveAndReturnBook() {
        // Arrange
        Book newBook = new Book("New Book", "New Author", 2024);
        when(bookRepository.save(any(Book.class))).thenReturn(newBook);
        
        // Act
        Book createdBook = bookService.createBook(newBook);
        
        // Assert
        assertThat(createdBook).isNotNull();
        assertThat(createdBook.getTitle()).isEqualTo("New Book");
        verify(bookRepository, times(1)).save(newBook);
    }
    
    @Test
    void createBook_WithEmptyTitle_ShouldThrowException() {
        // Arrange
        Book invalidBook = new Book("", "Author", 2024);
        
        // Act & Assert
        assertThatThrownBy(() -> bookService.createBook(invalidBook))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book title cannot be empty");
        
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    void createBook_WithNullTitle_ShouldThrowException() {
        // Arrange
        Book invalidBook = new Book(null, "Author", 2024);
        
        // Act & Assert
        assertThatThrownBy(() -> bookService.createBook(invalidBook))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book title cannot be empty");
        
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    void createBook_WithFuturePublicationYear_ShouldThrowException() {
        // Arrange
        Book invalidBook = new Book("Future Book", "Author", 2030);
        
        // Act & Assert
        assertThatThrownBy(() -> bookService.createBook(invalidBook))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Publication year cannot be in the future");
        
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    void updateBook_WithValidId_ShouldUpdateAndReturnBook() {
        // Arrange
        Book updatedDetails = new Book("Updated Title", "Updated Author", 2000);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));
        when(bookRepository.save(any(Book.class))).thenReturn(book1);
        
        // Act
        Book updatedBook = bookService.updateBook(1L, updatedDetails);
        
        // Assert
        assertThat(updatedBook).isNotNull();
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(book1);
    }
    
    @Test
    void updateBook_WithInvalidId_ShouldThrowException() {
        // Arrange
        Book updatedDetails = new Book("Updated Title", "Updated Author", 2000);
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> bookService.updateBook(999L, updatedDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Book not found with id: 999");
        
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    void deleteBook_WithValidId_ShouldDeleteBook() {
        // Arrange
        when(bookRepository.existsById(1L)).thenReturn(true);
        
        // Act
        bookService.deleteBook(1L);
        
        // Assert
        verify(bookRepository, times(1)).existsById(1L);
        verify(bookRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void deleteBook_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(bookRepository.existsById(999L)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> bookService.deleteBook(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Book not found with id: 999");
        
        verify(bookRepository, never()).deleteById(anyLong());
    }
    
    @Test
    void searchBooksByAuthor_ShouldReturnMatchingBooks() {
        // Arrange
        when(bookRepository.findByAuthorContainingIgnoreCase("Fitzgerald"))
                .thenReturn(Arrays.asList(book1));
        
        // Act
        List<Book> books = bookService.searchBooksByAuthor("Fitzgerald");
        
        // Assert
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getAuthor()).contains("Fitzgerald");
        verify(bookRepository, times(1)).findByAuthorContainingIgnoreCase("Fitzgerald");
    }
    
    @Test
    void getTotalBookCount_ShouldReturnCount() {
        // Arrange
        when(bookRepository.count()).thenReturn(5L);
        
        // Act
        Long count = bookService.getTotalBookCount();
        
        // Assert
        assertThat(count).isEqualTo(5L);
        verify(bookRepository, times(1)).count();
    }
    
    @Test
    void isBookAvailable_WithExistingBook_ShouldReturnTrue() {
        // Arrange
        when(bookRepository.existsById(1L)).thenReturn(true);
        
        // Act
        boolean available = bookService.isBookAvailable(1L);
        
        // Assert
        assertThat(available).isTrue();
        verify(bookRepository, times(1)).existsById(1L);
    }
}
```

### Step 4: Create Repository Integration Tests

Now let's test the repository layer with an embedded database:

**`src/test/java/com/sujan/springbootmastery/repository/BookRepositoryTest.java`**
```java
package com.sujan.springbootmastery.repository;

import com.sujan.springbootmastery.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Test
    void whenFindById_thenReturnBook() {
        // Given
        Book book = new Book("Test Book", "Test Author", 2020);
        Book savedBook = entityManager.persistAndFlush(book);
        
        // When
        Optional<Book> found = bookRepository.findById(savedBook.getId());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Book");
    }
    
    @Test
    void whenFindAll_thenReturnAllBooks() {
        // Given
        Book book1 = new Book("Book 1", "Author 1", 2020);
        Book book2 = new Book("Book 2", "Author 2", 2021);
        
        entityManager.persist(book1);
        entityManager.persist(book2);
        entityManager.flush();
        
        // When
        List<Book> books = bookRepository.findAll();
        
        // Then
        assertThat(books).hasSize(2);
        assertThat(books).extracting(Book::getTitle)
                        .containsExactlyInAnyOrder("Book 1", "Book 2");
    }
    
    @Test
    void whenFindByAuthor_thenReturnBooks() {
        // Given
        Book book1 = new Book("Book 1", "sujan Doe", 2020);
        Book book2 = new Book("Book 2", "sujan Doe", 2021);
        Book book3 = new Book("Book 3", "Jane Smith", 2022);
        
        entityManager.persist(book1);
        entityManager.persist(book2);
        entityManager.persist(book3);
        entityManager.flush();
        
        // When
        List<Book> sujansBooks = bookRepository.findByAuthorContainingIgnoreCase("sujan");
        
        // Then
        assertThat(sujansBooks).hasSize(2);
        assertThat(sujansBooks).extracting(Book::getAuthor)
                             .allMatch(author -> author.equals("sujan Doe"));
    }
    
    @Test
    void whenFindByPublicationYearAfter_thenReturnBooks() {
        // Given
        Book book1 = new Book("Book 1", "Author 1", 2018);
        Book book2 = new Book("Book 2", "Author 2", 2020);
        Book book3 = new Book("Book 3", "Author 3", 2022);
        
        entityManager.persist(book1);
        entityManager.persist(book2);
        entityManager.persist(book3);
        entityManager.flush();
        
        // When
        List<Book> recentBooks = bookRepository.findByPublicationYearAfter(2019);
        
        // Then
        assertThat(recentBooks).hasSize(2);
        assertThat(recentBooks).extracting(Book::getPublicationYear)
                              .allMatch(year -> year > 2019);
    }
    
    @Test
    void whenFindByTitleContaining_thenReturnBooks() {
        // Given
        Book book1 = new Book("Spring Boot Guide", "Author 1", 2020);
        Book book2 = new Book("Java Programming", "Author 2", 2021);
        Book book3 = new Book("Advanced Spring", "Author 3", 2022);
        
        entityManager.persist(book1);
        entityManager.persist(book2);
        entityManager.persist(book3);
        entityManager.flush();
        
        // When
        List<Book> springBooks = bookRepository.findByTitleContainingIgnoreCase("spring");
        
        // Then
        assertThat(springBooks).hasSize(2);
        assertThat(springBooks).extracting(Book::getTitle)
                              .allMatch(title -> title.toLowerCase().contains("spring"));
    }
    
    @Test
    void whenSaveBook_thenBookIsPersisted() {
        // Given
        Book newBook = new Book("New Book", "New Author", 2024);
        
        // When
        Book savedBook = bookRepository.save(newBook);
        
        // Then
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.getId()).isNotNull();
        assertThat(savedBook.getTitle()).isEqualTo("New Book");
        
        // Verify it's actually in the database
        Book foundBook = entityManager.find(Book.class, savedBook.getId());
        assertThat(foundBook).isNotNull();
        assertThat(foundBook.getTitle()).isEqualTo("New Book");
    }
    
    @Test
    void whenDeleteBook_thenBookIsRemoved() {
        // Given
        Book book = new Book("To Delete", "Author", 2020);
        Book savedBook = entityManager.persistAndFlush(book);
        Long bookId = savedBook.getId();
        
        // When
        bookRepository.deleteById(bookId);
        
        // Then
        Book deletedBook = entityManager.find(Book.class, bookId);
        assertThat(deletedBook).isNull();
    }
}
```

### Step 5: Create Controller Tests with Mocked Services

Now let's test the web layer by mocking the service:

**`src/test/java/com/sujan/springbootmastery/controller/BookControllerTest.java`**
```java
package com.sujan.springbootmastery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sujan.springbootmastery.model.Book;
import com.sujan.springbootmastery.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private BookService bookService;
    
    private Book createSampleBook() {
        Book book = new Book("Test Book", "Test Author", 2020);
        book.setId(1L);
        return book;
    }
    
    @Test
    void getAllBooks_ShouldReturnBooks() throws Exception {
        // Given
        Book book1 = createSampleBook();
        Book book2 = new Book("Another Book", "Another Author", 2021);
        book2.setId(2L);
        
        List<Book> books = Arrays.asList(book1, book2);
        
        when(bookService.getAllBooks()).thenReturn(books);
        
        // When & Then
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Book")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].title", is("Another Book")));
        
        verify(bookService, times(1)).getAllBooks();
    }
    
    @Test
    void getBookById_WithValidId_ShouldReturnBook() throws Exception {
        // Given
        Book book = createSampleBook();
        when(bookService.getBookById(1L)).thenReturn(Optional.of(book));
        
        // When & Then
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Book")))
                .andExpect(jsonPath("$.author", is("Test Author")));
        
        verify(bookService, times(1)).getBookById(1L);
    }
    
    @Test
    void getBookById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        when(bookService.getBookById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound());
        
        verify(bookService, times(1)).getBookById(999L);
    }
    
    @Test
    void createBook_WithValidBook_ShouldReturnCreated() throws Exception {
        // Given
        Book newBook = new Book("New Book", "New Author", 2024);
        Book savedBook = new Book("New Book", "New Author", 2024);
        savedBook.setId(1L);
        
        when(bookService.createBook(any(Book.class))).thenReturn(savedBook);
        
        // When & Then
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("New Book")));
        
        verify(bookService, times(1)).createBook(any(Book.class));
    }
    
    @Test
    void createBook_WithInvalidBook_ShouldReturnBadRequest() throws Exception {
        // Given
        Book invalidBook = new Book("", "Author", 2024); // Empty title
        
        when(bookService.createBook(any(Book.class)))
                .thenThrow(new IllegalArgumentException("Book title cannot be empty"));
        
        // When & Then
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBook)))
                .andExpect(status().isBadRequest());
        
        verify(bookService, times(1)).createBook(any(Book.class));
    }
    
    @Test
    void updateBook_WithValidId_ShouldReturnUpdatedBook() throws Exception {
        // Given
        Book updatedDetails = new Book("Updated Title", "Updated Author", 2020);
        Book updatedBook = new Book("Updated Title", "Updated Author", 2020);
        updatedBook.setId(1L);
        
        when(bookService.updateBook(eq(1L), any(Book.class))).thenReturn(updatedBook);
        
        // When & Then
        mockMvc.perform(put("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Title")));
        
        verify(bookService, times(1)).updateBook(eq(1L), any(Book.class));
    }
    
    @Test
    void updateBook_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        Book updatedDetails = new Book("Updated Title", "Updated Author", 2020);
        
        when(bookService.updateBook(eq(999L), any(Book.class)))
                .thenThrow(new RuntimeException("Book not found"));
        
        // When & Then
        mockMvc.perform(put("/api/books/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isNotFound());
        
        verify(bookService, times(1)).updateBook(eq(999L), any(Book.class));
    }
    
    @Test
    void deleteBook_WithValidId_ShouldReturnSuccess() throws Exception {
        // Given
        doNothing().when(bookService).deleteBook(1L);
        
        // When & Then
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Book deleted successfully"));
        
        verify(bookService, times(1)).deleteBook(1L);
    }
    
    @Test
    void deleteBook_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        doThrow(new RuntimeException("Book not found")).when(bookService).deleteBook(999L);
        
        // When & Then
        mockMvc.perform(delete("/api/books/999"))
                .andExpect(status().isNotFound());
        
        verify(bookService, times(1)).deleteBook(999L);
    }
    
    @Test
    void searchBooksByAuthor_ShouldReturnMatchingBooks() throws Exception {
        // Given
        Book book = createSampleBook();
        when(bookService.searchBooksByAuthor("Test")).thenReturn(Arrays.asList(book));
        
        // When & Then
        mockMvc.perform(get("/api/books/search")
                .param("author", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].author", is("Test Author")));
        
        verify(bookService, times(1)).searchBooksByAuthor("Test");
    }
    
    @Test
    void getTotalBookCount_ShouldReturnCount() throws Exception {
        // Given
        when(bookService.getTotalBookCount()).thenReturn(5L);
        
        // When & Then
        mockMvc.perform(get("/api/books/stats/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
        
        verify(bookService, times(1)).getTotalBookCount();
    }
}
```

### Step 6: Create Integration Test

Let's create a full integration test that starts the complete application:

**`src/test/java/com/sujan/springbootmastery/SpringBootMasteryApplicationTest.java`**
```java
package com.sujan.springbootmastery;

import com.sujan.springbootmastery.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SpringBootMasteryApplicationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void contextLoads() {
        // Basic test to verify application starts successfully
    }
    
    @Test
    void whenGetAllBooks_thenReturnBooks() {
        // Given
        String url = "http://localhost:" + port + "/api/books";
        
        // When
        ResponseEntity<Book[]> response = restTemplate.getForEntity(url, Book[].class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
    
    @Test
    void whenCreateBook_thenBookIsCreated() {
        // Given
        String url = "http://localhost:" + port + "/api/books";
        Book newBook = new Book("Integration Test Book", "Test Author", 2024);
        
        // When
        ResponseEntity<Book> response = restTemplate.postForEntity(url, newBook, Book.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Integration Test Book");
    }
}
```

### Step 7: Create Test Configuration

Create a test-specific configuration:

**`src/test/resources/application-test.yml`**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: false

logging:
  level:
    com.sujan.springbootmastery: INFO
    org.springframework: WARN
```

### Step 8: Run the Tests

You can run tests in several ways:

**Run all tests:**
```bash
./mvnw test
```

**Run specific test class:**
```bash
./mvnw test -Dtest=BookServiceTest
```

**Run tests with specific profile:**
```bash
./mvnw test -Dspring.profiles.active=test
```

**Run from IDE:**
- Right-click on test class or method and select "Run"

### Step 9: Test Coverage Report

Generate a test coverage report with JaCoCo. Add to your `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Generate report:
```bash
./mvnw test jacoco:report
```

The report will be available at: `target/site/jacoco/index.html`

### Day 6 Challenge

1. **Write tests** for the business logic methods in `BookService` (getBooksCountByAuthor, getRecentBooks, etc.)
2. **Create a test** that verifies the validation rules for publication year
3. **Add error case tests** for edge cases and error conditions

### Solution to Challenge #1

```java
@Test
void getBooksCountByAuthor_ShouldReturnCountPerAuthor() {
    // Arrange
    when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));
    
    // Act
    Map<String, Long> countByAuthor = bookService.getBooksCountByAuthor();
    
    // Assert
    assertThat(countByAuthor).hasSize(2);
    assertThat(countByAuthor.get("F. Scott Fitzgerald")).isEqualTo(1L);
    assertThat(countByAuthor.get("Harper Lee")).isEqualTo(1L);
}

@Test
void getRecentBooks_ShouldReturnBooksFromLastYears() {
    // Arrange
    Book recentBook = new Book("Recent", "Author", 2023);
    Book oldBook = new Book("Old", "Author", 2010);
    when(bookRepository.findAll()).thenReturn(Arrays.asList(recentBook, oldBook));
    
    // Act
    List<Book> recentBooks = bookService.getRecentBooks(5); // Last 5 years
    
    // Assert
    assertThat(recentBooks).hasSize(1);
    assertThat(recentBooks.get(0).getTitle()).isEqualTo("Recent");
}
```

### Key Takeaways from Day 6

✅ **Unit Testing**: Test individual components in isolation with mocks  
✅ **Integration Testing**: Test components working together  
✅ **Repository Testing**: `@DataJpaTest` with embedded database  
✅ **Controller Testing**: `@WebMvcTest` with mocked services  
✅ **Mocking**: `@MockBean` for dependency mocking  
✅ **Test Profiles**: Separate configuration for testing  
✅ **Test Coverage**: Measure how much code is tested  

### What's Next?

Tomorrow (Day 7) we'll focus on **Exception Handling** - creating global exception handlers and proper error responses!

**When you're ready for Day 7, just say "next"!** Practice today's concepts by writing more tests for edge cases and different scenarios.