Excellent! Let's dive into **Day 1: Bootstrapping & The Magic of Autoconfiguration**.

## Day 1: Hello Spring Boot!

### What We'll Accomplish Today
By the end of today, you'll have:
1. Created your first Spring Boot project
2. Built a simple REST endpoint
3. Understood Spring Boot's core philosophy
4. Run your application and tested it

---

### Step 1: Create Your Spring Boot Project

Go to [Spring Initializr](https://start.spring.io/) and configure your project:

- **Project**: Maven (default)
- **Language**: Java (default)
- **Spring Boot**: Latest stable version (3.2.x or 3.3.x)
- **Project Metadata**:
  - Group: `com.yourname` (e.g., `com.sujan`)
  - Artifact: `spring-boot-mastery` (or any name you prefer)
  - Name: `spring-boot-mastery`
  - Package name: `com.sujan.springbootmastery`
  - Packaging: Jar
  - Java: 17 or 21 (recommended)

- **Dependencies**: Click "Add Dependencies" and select:
  - **Spring Web** (this gives us everything needed for web applications)

Your Spring Initializr should look like this:
```
Project: Maven
Language: Java
Spring Boot: 3.2.5
Group: com.sujan
Artifact: spring-boot-mastery
Name: spring-boot-mastery
Description: Spring Boot Mastery in 2 Weeks
Package name: com.sujan.springbootmastery
Packaging: Jar
Java: 17
Dependencies: Spring Web
```

Click **Generate** to download a zip file, then extract it and open in your IDE.

### Step 2: Explore the Project Structure

After opening in your IDE (IntelliJ IDEA, Eclipse, or VS Code), you'll see this structure:

```
spring-boot-mastery/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sujan/
│   │   │           └── springbootmastery/
│   │   │               └── SpringBootMasteryApplication.java  ← Main class
│   │   └── resources/
│   │       └── application.properties  ← Configuration file
│   └── test/... (for tests)
├── pom.xml  ← Maven dependencies
```

Let's examine the key files:

**1. `SpringBootMasteryApplication.java`**
```java
package com.sujan.springbootmastery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootMasteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootMasteryApplication.class, args);
    }
}
```

**What's happening here?**
- `@SpringBootApplication`: This single annotation enables:
  - **Component scanning**: Spring looks for other components in this package and subpackages
  - **Auto-configuration**: Spring Boot automatically configures beans based on your dependencies
  - **Configuration**: Marks this as a configuration class

**2. `pom.xml`** - Notice the parent and dependencies:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
    <relativePath/>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### Step 3: Create Your First REST Controller

Create a new Java class called `HelloController` in the same package as your main application:

**`HelloController.java`**
```java
package com.sujan.springbootmastery;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, Spring Boot! Day 1 of mastery!";
    }
}
```

**Let's break this down:**
- `@RestController`: Tells Spring this class will handle web requests and the return values should be written directly to the HTTP response body (as JSON/XML/text)
- `@GetMapping("/hello")`: Maps HTTP GET requests to the `/hello` path to this method
- The method returns a simple String that will be sent as the HTTP response

### Step 4: Run Your Application

**Option 1: From your IDE**
- Right-click on `SpringBootMasteryApplication.java`
- Select "Run" or "Debug"

**Option 2: From command line**
```bash
./mvnw spring-boot:run
# Or on Windows:
# mvnw.cmd spring-boot:run
```

You should see output like this:
```
2024-01-15T10:30:00.000-05:00  INFO 12345 --- [  restartedMain] c.j.s.SpringBootMasteryApplication       : Starting SpringBootMasteryApplication using Java 17...
2024-01-15T10:30:00.000-05:00  INFO 12345 --- [  restartedMain] c.j.s.SpringBootMasteryApplication       : No active profile set, using default profile: default
2024-01-15T10:30:01.000-05:00  INFO 12345 --- [  restartedMain] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080
2024-01-15T10:30:01.000-05:00  INFO 12345 --- [  restartedMain] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2024-01-15T10:30:01.000-05:00  INFO 12345 --- [  restartedMain] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat]
2024-01-15T10:30:01.000-05:00  INFO 12345 --- [  restartedMain] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2024-01-15T10:30:01.000-05:00  INFO 12345 --- [  restartedMain] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1000 ms
2024-01-15T10:30:02.000-05:00  INFO 12345 --- [  restartedMain] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path ''
2024-01-15T10:30:02.000-05:00  INFO 12345 --- [  restartedMain] c.j.s.SpringBootMasteryApplication       : Started SpringBootMasteryApplication in 2.5 seconds (process running for 3.0)
```

**Notice:** Tomcat started on port 8080 automatically!

### Step 5: Test Your Application

Open your web browser and go to:
```
http://localhost:8080/hello
```

You should see:
```
Hello, Spring Boot! Day 1 of mastery!
```

### Step 6: Understanding the Magic - Auto-configuration

Let's see what Spring Boot configured for us automatically:

1. **Embedded Tomcat Server**: We didn't configure any server - Spring Boot included and configured Tomcat automatically because we added `spring-boot-starter-web`

2. **Default Port 8080**: The server runs on port 8080 by convention

3. **Spring MVC Setup**: All the complex Spring MVC configuration is done automatically

4. **DispatcherServlet**: The front controller is automatically set up

### Optional: Change the Server Port

Create/Edit `src/main/resources/application.properties`:
```properties
server.port=9090
```

Restart your application and now visit:
```
http://localhost:9090/hello
```

### Day 1 Challenge (Optional)

1. Create another endpoint at `/greeting/{name}` that returns "Hello, {name}!"
   - Hint: Use `@PathVariable`

2. Create an endpoint at `/about` that returns information about your learning journey

### Solution to Challenge

```java
@GetMapping("/greeting/{name}")
public String greetByName(@PathVariable String name) {
    return "Hello, " + name + "! Welcome to Spring Boot!";
}

@GetMapping("/about")
public String about() {
    return "I'm mastering Spring Boot in 2 weeks! Today is Day 1 - Bootstrapping!";
}
```

### Key Takeaways from Day 1

✅ **Spring Boot's Power**: With just a few annotations, we have a fully working web application  
✅ **Convention Over Configuration**: Sensible defaults mean less code to write  
✅ **Starter Dependencies**: `spring-boot-starter-web` brought in everything we needed  
✅ **Embedded Server**: No need to deploy to an external server during development  

**Tomorrow**: We'll build a proper REST API with more endpoints and learn about the different HTTP methods!

When you're ready, let me know if you have any questions about today's material or when you want to move to **Day 2**.
