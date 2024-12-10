package com.example.thesis_code_gen.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ConversationMessage message);

    @Query("SELECT * FROM conversation_messages ORDER BY timestamp ASC")
    List<ConversationMessage> getAllMessages();

    @Query("SELECT * FROM conversation_messages WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    List<ConversationMessage> getMessagesFrom(long startTime);

    @Query("SELECT DISTINCT conversationId FROM conversation_messages")
    List<Integer> getAllConversationIds();

    @Query("SELECT conversationTitle FROM conversation_messages WHERE conversationId = :conversationId LIMIT 1")
    String getConversationTitleById(int conversationId);

    @Query("SELECT * FROM conversation_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    List<ConversationMessage> getMessagesByConversationId(int conversationId);

    @Query("SELECT MAX(conversationId) FROM conversation_messages")
    Integer getMaxConversationId();

    @Query("UPDATE conversation_messages SET conversationTitle = :conversationTitle WHERE conversationId = :conversationId")
    void updateConversationTitleForId(int conversationId, String conversationTitle);

    @Query("DELETE FROM conversation_messages WHERE conversationId = :conversationId")
    void deleteConversationById(int conversationId);

    @Query("DELETE FROM conversation_messages")
    void deleteAllMessages();

}