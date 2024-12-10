package com.example.thesis_code_gen.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversation_messages")
public class ConversationMessage {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private long timestamp;
    private String sender;
    private String message;
    private int conversationId;
    private String conversationTitle;

    // Constructor with all fields
    public ConversationMessage(long timestamp, String sender, String message, int conversationId, String conversationTitle) {
        this.timestamp = timestamp;
        this.sender = sender;
        this.message = message;
        this.conversationId = conversationId;
        this.conversationTitle = conversationTitle;
    }

    // Empty constructor
    public ConversationMessage() {}

    // Getters and setters for all fields

    public int getId() {
        return id;
    }

    // Room uses setters to assign values when reading from the database
    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationTitle() {
        return conversationTitle;
    }

    public void setConversationTitle(String conversationTitle) {
        this.conversationTitle = conversationTitle;
    }
}