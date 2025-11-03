Project Information (Spring Initializr):

    When creating a project with Spring Initializr (start.spring.io), users define project-level metadata such as:
        Group ID: The identifier for the organization or group.
        Artifact ID: The unique identifier for the project within the group.
        Version: The project's version number.
        Name: A human-readable name for the project.
        Description: A brief explanation of the project's purpose.
        Package Name: The base Java package for the project.
        Packaging: (e.g., JAR, WAR) The type of executable artifact.
        Java Version: The target Java version.
        Dependencies: Libraries and frameworks included in the project. 

Build System Metadata (Maven/Gradle):

    Build tools like Maven and Gradle also manage project metadata within their respective build files (pom.xml for Maven, build.gradle for Gradle). This includes dependencies, plugins, build configurations, and other project-specific settings.



-------------------



JAR (Java Archive):

    Purpose:
    Primarily used for packaging and distributing general-purpose Java applications, libraries, and utilities. It bundles Java classes, resources (like images or configuration files), and metadata into a single, compressed file.
    Contents:
    Typically contains .class files, manifest files (META-INF/MANIFEST.MF), and other resources.
    Deployment:
    Can be executed as standalone applications or included as libraries in other Java projects. 

WAR (Web Application Archive):

    Purpose:
    Specifically designed for packaging and deploying Java web applications that adhere to the Java Servlet specification. These applications are intended to run on web servers or application servers (e.g., Tomcat, JBoss).
    Contents:
    Includes web components like servlets, JSP pages, HTML files, CSS, JavaScript, static resources, and a web.xml deployment descriptor in the WEB-INF directory.
    Deployment:
    Must be deployed to a compatible web or application server for execution. It cannot be run as a standalone application. 

Key Differences Summarized:
Feature
	
JAR (Java Archive)
	
WAR (Web Application Archive)
Purpose
	
General Java applications/libraries
	
Java web applications
Contents
	
Java classes, resources, metadata
	
Web components (Servlets, JSPs, HTML)
Structure
	
Flexible, application-dependent
	
Specific, defined by Servlet spec
Deployment
	
Standalone or as library
	
Deployed on web/application servers


-----



The most commonly used HTTP methods include:

    GET:
    Used to request data from a specified resource. It should only retrieve data and have no other effect on the server.
    POST:
    Used to send data to a server to create a new resource. The data is included in the body of the request.
    PUT:
    Used to update or replace an existing resource with the provided data. If the resource does not exist, it may create a new one.
    PATCH:
    Used to apply partial modifications to a resource. It only updates the specified fields, leaving others unchanged.
    DELETE:
    Used to remove a specified resource from the server. 

These five methods often correspond to the CRUD (Create, Read, Update, Delete) operations in database management:

    Create: POST
    Read: GET
    Update: PUT (full replacement) or PATCH (partial update)
    Delete: DELETE


