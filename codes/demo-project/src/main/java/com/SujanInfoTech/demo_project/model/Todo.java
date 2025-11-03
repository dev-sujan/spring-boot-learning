package com.SujanInfoTech.demo_project.model;

public class Todo {
    private Long id;
    private String title;
    private String description;
    private Boolean completed;

    // Constructors
    public Todo() {};
    public Todo(Long id, String title, String description, Boolean completed){
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = completed;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public Boolean getCompleted(){
        return completed;
    }

    public void setCompleted(Boolean completed){
        this.completed = completed;
    }


    @Override
    public String toString(){
        return "Todo{" +
                "id=" + id +
                ", title=" + title +
                ", description=" + description +
                ", completed=" + description + 
            "}";
    }
}
