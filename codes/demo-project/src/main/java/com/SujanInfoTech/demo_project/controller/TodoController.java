package com.SujanInfoTech.demo_project.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SujanInfoTech.demo_project.model.Todo;

@RestController
@RequestMapping("/todos")
public class TodoController {
    private List<Todo> todos = new ArrayList<>();

    private static AtomicLong idGenerator = new AtomicLong(0);

    public TodoController(){
        todos.add(new Todo(idGenerator.incrementAndGet(), "Todo 1", "Create a todo REST API", false));
        todos.add(new Todo(idGenerator.incrementAndGet(), "Todo 2", "Create a todo model", false));
        todos.add(new Todo(idGenerator.incrementAndGet(), "Todo 3", "Create a todo controller", false));
    }

    private Todo findTodobyId(Long id){
        return todos.stream()
            .filter(todos -> todos.getId().equals(id))
            .findFirst()
            .orElseThrow(()->new RuntimeException("Todo not found with id " + id));
    }

    @GetMapping
    public List<Todo> getAllTodos(){
        return todos;
    }

    @GetMapping("/{id}")
    public Todo getTodoById(@PathVariable Long id){
        return findTodobyId(id);
    }

    @PostMapping
    public Todo postTodoById(@RequestBody Todo todo){
        todo.setId(idGenerator.incrementAndGet());
        todos.add(todo);
        return todo;
    }

    @PutMapping("/{id}")
    public Todo putTodoById(@PathVariable Long id, @RequestBody Todo todo){
        Todo existingTodo = findTodobyId(id);
        existingTodo.setTitle(todo.getTitle());
        existingTodo.setDescription(todo.getDescription());
        existingTodo.setCompleted(todo.getCompleted());  
        return existingTodo;
    }

    @DeleteMapping("/{id}")
    public Todo deleteTodoById(@PathVariable Long id){
        Todo todoToDelete = findTodobyId(id);
        todos.remove(todoToDelete);
        return todoToDelete;
    }

}