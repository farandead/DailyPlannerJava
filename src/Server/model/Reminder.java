package Server.model;

import java.io.Serial;
import  java.io.Serializable;
import java.time.LocalDateTime;

public class Reminder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String taskDescription;
    private LocalDateTime dateTime;
    private int id;
    private String userName;

    public Reminder(int id, String userName, String taskDescription, LocalDateTime dateTime) {
        this.id = id;
        this.userName = userName;
        this.taskDescription = taskDescription;
        this.dateTime = dateTime;
    }

    // Getters and setters

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
