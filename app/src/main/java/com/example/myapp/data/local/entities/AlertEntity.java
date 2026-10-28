package com.example.myapp.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts")
public class AlertEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String content;
    public String priority; // DANGER, WARNING, INFO
    public String timestamp;

    public AlertEntity(String title, String content, String priority, String timestamp) {
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.timestamp = timestamp;
    }
}