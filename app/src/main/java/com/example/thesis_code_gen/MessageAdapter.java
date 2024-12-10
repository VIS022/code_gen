package com.example.thesis_code_gen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<String> messages = new ArrayList<>();

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        String message = messages.get(position);
        if (message.startsWith("User: ")) {
            holder.rightChat.setVisibility(View.VISIBLE);
            holder.leftChat.setVisibility(View.GONE);
            holder.rightText.setText(message.substring(6));
        } else if (message.startsWith("AI: ")) {
            holder.leftChat.setVisibility(View.VISIBLE);
            holder.rightChat.setVisibility(View.GONE);
            holder.leftText.setText(message.substring(4));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Add this method to clear messages
    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    public void addMessage(String message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView leftText, rightText;
        ConstraintLayout leftChat, rightChat;

        MessageViewHolder(View itemView) {
            super(itemView);
            leftText = itemView.findViewById(R.id.left_text);
            rightText = itemView.findViewById(R.id.right_text);
            leftChat = itemView.findViewById(R.id.left_chat);
            rightChat = itemView.findViewById(R.id.right_chat);
        }
    }
}