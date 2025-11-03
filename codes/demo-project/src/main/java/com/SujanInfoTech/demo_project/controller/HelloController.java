package com.SujanInfoTech.demo_project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String Index() {
        return "Hello World! This is index page";
    }

    @GetMapping("/hello")
    public String helloSujan() {
        return "Hello Sujan";
    }

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable("name") String name) {
        return "Hello " + name;
    }

    @GetMapping("/users")
    public String users() {
        return "Hello Users";
    }
}
