package com.example.thesis_code_gen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thesis_code_gen.database.ConversationDao;
import com.example.thesis_code_gen.database.ConversationDatabase;
import com.example.thesis_code_gen.database.ConversationMessage;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText messageInput;
    private ImageView centerImage;
    private TextView helpText;
    private String lastUserMessage = "";
    private List<String> conversationHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 10;

    // ConversationDatabase and ConversationDao
    private ConversationDatabase conversationDatabase;
    private ConversationDao conversationDao;

    // New Chat Button
    private Button btnNewChat;

    // Conversation ID and Title
    private int currentConversationId;
    private String conversationTitle;

    // NavigationView
    private NavigationView navigationViewLeft;
    private NavigationView navigationViewRight; // Add this

    private Executor mainExecutor;

    // Add ProgressBar field
    private ProgressBar loadingIndicator;

    private static final String THEME_PREFERENCES = "theme_preferences";
    private static final String SELECTED_THEME = "selected_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        loadTheme(); // Load selected theme before calling super.onCreate

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Disable screenshots
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        navigationViewRight = findViewById(R.id.navigation_view_right); // Initialize right NavigationView

        // Initialize the ProgressBar
        loadingIndicator = findViewById(R.id.loading_indicator);

        // Initialize the database and DAO
        conversationDatabase = ConversationDatabase.getDatabase(this);
        conversationDao = conversationDatabase.conversationDao();

        mainExecutor = ContextCompat.getMainExecutor(this);

        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.message_input);
        centerImage = findViewById(R.id.center_image);
        helpText = findViewById(R.id.help_text);

        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationViewLeft = findViewById(R.id.navigation_view_left);

        // Initialize conversation ID and title
        initializeConversation();

        // Set up the NavigationView listener
        setupNavigationView();

        setupRightNavigationView(); // New method for right NavigationView


        // Disable "Previous 7 Days" and "Previous 30 Days"
        Menu menu = navigationViewLeft.getMenu();
        menu.findItem(R.id.menu_previous_7_days).setEnabled(false);
        menu.findItem(R.id.menu_previous_30_days).setEnabled(false);

        // Get header view and setup New Chat click listener
        View headerView = navigationViewLeft.getHeaderView(0);
        btnNewChat = headerView.findViewById(R.id.btnNewChat);
        btnNewChat.setOnClickListener(v -> {
            startNewChat();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.menu_icon).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        findViewById(R.id.settings_icon).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        // Update the previous conversations menu
        updatePreviousConversationsMenu();
    }

    private void loadTheme() {
        SharedPreferences prefs = getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE);
        String themeName = prefs.getString(SELECTED_THEME, "light"); // Default to light theme

        switch (themeName) {
            case "dark":
                setTheme(R.style.Theme_Thesis_Code_Gen_Dark);
                break;
            case "red":
                setTheme(R.style.Theme_Thesis_Code_Gen_Red);
                break;
            case "light":
            default:
                setTheme(R.style.Theme_Thesis_Code_Gen_Light);
                break;
        }
    }

    private void setupRightNavigationView() {
        navigationViewRight.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.delete_all) {
                showDeleteAllChatsConfirmationDialog();
            } else if (id == R.id.ol_comp) {
                openOnlineCompiler();
            } else if (id == R.id.theme_dark) {
                setThemeDark();
            } else if (id == R.id.theme_light) {
                setThemeLight();
            } else if (id == R.id.theme_red) {
                setThemeRed();
            }

            drawerLayout.closeDrawer(GravityCompat.END); // Close right drawer
            return true;
        });
    }

    private void setThemeLight() {
        saveTheme("light");
        recreate(); // Restarts activity to apply the new theme
    }

    private void setThemeDark() {
        saveTheme("dark");
        recreate();
    }

    private void setThemeRed() {
        saveTheme("red");
        recreate();
    }

    private void saveTheme(String themeName) {
        SharedPreferences prefs = getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SELECTED_THEME, themeName);
        editor.apply();
    }

    private void initializeConversation() {
        new Thread(() -> {
            Integer maxId = conversationDao.getMaxConversationId();
            currentConversationId = (maxId != null && maxId >= 1) ? maxId + 1 : 1;

            // Initialize conversationTitle to null
            conversationTitle = null;
        }).start();
    }

    private void setupNavigationView() {
        navigationViewLeft.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.menu_previous_7_days) {
                loadMessagesFromDays(7);
            } else if (id == R.id.menu_previous_30_days) {
                loadMessagesFromDays(30);
            } else if (id >= 1000) {
                // Handle single-clicks for dynamic conversation items
                int conversationId = id - 1000;
                loadConversation(conversationId);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void updatePreviousConversationsMenu() {
        new Thread(() -> {
            // Fetch conversation IDs and titles from the database
            List<Integer> conversationIds = conversationDao.getAllConversationIds();

            // Prepare conversation titles
            List<String> conversationTitles = new ArrayList<>();
            for (Integer id : conversationIds) {
                String title = conversationDao.getConversationTitleById(id);
                if (title == null || title.isEmpty()) {
                    title = "Conversation " + id;
                }
                conversationTitles.add(title);
            }

            // Update the menu on the main UI thread
            runOnUiThread(() -> {
                Menu menu = navigationViewLeft.getMenu();
                MenuItem previousItem = menu.findItem(R.id.menu_previous_conversations);
                SubMenu subMenu = previousItem.getSubMenu();

                // Clear the submenu to avoid duplicates
                subMenu.clear();

                // Add the conversations to the menu
                for (int i = 0; i < conversationIds.size(); i++) {
                    int conversationId = conversationIds.get(i);
                    String conversationTitle = conversationTitles.get(i);

                    // Add the menu item
                    MenuItem menuItem = subMenu.add(R.id.group_static_items, 1000 + conversationId, Menu.NONE, conversationTitle);

                    // Handle single-clicks to load the conversation
                    menuItem.setOnMenuItemClickListener(item -> {
                        loadConversation(conversationId);
                        return true;
                    });

                    // Handle long-press to show the delete dialog
                    View itemView = navigationViewLeft.findViewById(menuItem.getItemId());
                    if (itemView != null) {
                        itemView.setOnLongClickListener(v -> {
                            showDeleteConfirmationDialog(conversationId);
                            return true;
                        });
                    }
                }
            });
        }).start();
    }

    private void showDeleteConfirmationDialog(int conversationId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete this conversation?")
                .setPositiveButton("Yes", (dialog, which) -> deleteConversation(conversationId))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteConversation(int conversationId) {
        new Thread(() -> {
            // Delete the conversation from the database
            conversationDao.deleteConversationById(conversationId);

            // Refresh the menu on the main thread
            runOnUiThread(() -> {
                updatePreviousConversationsMenu();
                Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void loadConversation(int conversationId) {
        new Thread(() -> {
            currentConversationId = conversationId;

            List<ConversationMessage> messages = conversationDao.getMessagesByConversationId(conversationId);

            // If messages are not empty, get the conversation title from the first message
            if (!messages.isEmpty()) {
                conversationTitle = messages.get(0).getConversationTitle();
            } else {
                conversationTitle = "Conversation " + conversationId;
            }

            // Update UI on the main thread
            runOnUiThread(() -> displayMessages(messages));
        }).start();
    }

    public void buttonCallOnlineLLM(View view) {
        String userMessage = messageInput.getText().toString().trim();
        Log.d(TAG, "User message: " + userMessage);

        if (!userMessage.isEmpty() && !userMessage.equals(lastUserMessage)) {
            if (adapter.getItemCount() == 0) {
                centerImage.setVisibility(View.GONE);
                helpText.setVisibility(View.GONE);

                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();
                params.topToBottom = R.id.viewIn; // Adjust as necessary
                recyclerView.setLayoutParams(params);
            }

            addToConversationHistory("User", userMessage);
            adapter.addMessage("User: " + userMessage);
            messageInput.setText("");
            lastUserMessage = userMessage;

            sendToOnlineLLM(userMessage);
        } else if (userMessage.isEmpty()) {
            Log.w(TAG, "Empty user message, not sending request");
        } else {
            Log.w(TAG, "Duplicate user message, not sending request");
        }
    }

    private void sendToOnlineLLM(String userMessage) {
        // Show the ProgressBar
        runOnUiThread(() -> loadingIndicator.setVisibility(View.VISIBLE));

        // Replace placeholders with your actual encrypted model name and API key
        String encryptedModel = "Z2VtaW5pLTEuNS1mbGFzaA==";
        String encryptedKey = "QUl6YVN5Q3pLLWVoX1dtbXFiZWpyaDZPZ2k0OEEyM0p6bzV5ZURz";

        // Decrypt the model name and API key (if you use encryption)
        String decryptedModel = new String(android.util.Base64.decode(encryptedModel, android.util.Base64.DEFAULT));
        String decryptedKey = new String(android.util.Base64.decode(encryptedKey, android.util.Base64.DEFAULT));

        // Initialize your AI model with the decrypted model name and API key
        GenerativeModel gm = new GenerativeModel(decryptedModel, decryptedKey);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Build the conversation history
        StringBuilder historyBuilder = new StringBuilder();
        for (String message : conversationHistory) {
            historyBuilder.append(message).append("\n");
        }

        // Customize your prompt (unchanged as per your request)
        String customizedPrompt = "You are a programming mentor specializing in Java, Python, and C#. Here's the conversation history:\n\n" +
                historyBuilder.toString() +
                "\nBased on this conversation history, please provide a response to the latest query. " +
                "If the query is not related to programming in Java, Python, or C#, respond with 'NOT_PROGRAMMING'. " +
                "Otherwise, provide a helpful response using the following format:\n" +
                "1. Briefly explain the overall approach.\n" +
                "2. For each step:\n" +
                "   a) Explain the purpose of the step.\n" +
                "   b) Provide a small code snippet (if applicable).\n" +
                "   c) Explain how the code works.\n" +
                "3. Conclude with any additional tips or best practices.\n" +
                "Remember to focus on teaching and explanation rather than providing a complete solution. " +
                "If the user asks about a specific step or part of your previous explanation, refer back to it and provide more details. " +
                "Do not use asterisks (*) for emphasis or formatting in your response.";

        // Prepare the content for the AI model
        Content content = new Content.Builder()
                .addText(customizedPrompt)
                .build();

        Executor mainExecutor = ContextCompat.getMainExecutor(this);

        Log.d(TAG, "Sending customized request to Online LLM");
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(
                response,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String resultText = result.getText();
                        resultText = removeAsterisks(resultText); // Remove asterisks
                        Log.d(TAG, "AI response received: " + resultText);
                        String finalResultText = resultText;
                        runOnUiThread(() -> {
                            // Hide the ProgressBar
                            loadingIndicator.setVisibility(View.GONE);

                            if (finalResultText.trim().equalsIgnoreCase("NOT_PROGRAMMING")) {
                                String aiResponse = "I apologize, but I can only assist with questions related to Java, Python, or C# programming.";                                addToConversationHistory("AI", aiResponse);
                                adapter.addMessage("AI: " + aiResponse);
                            } else {
                                addToConversationHistory("AI", finalResultText);
                                adapter.addMessage("AI: " + finalResultText);
                            }
                            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Error in LLM call: ", t);
                        runOnUiThread(() -> {
                            // Hide the ProgressBar
                            loadingIndicator.setVisibility(View.GONE);

                            adapter.addMessage("Error: " + t.toString());
                            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        });
                    }
                },
                mainExecutor);
    }

    private String removeAsterisks(String text) {
        return text.replaceAll("\\*", "");
    }

    private void addToConversationHistory(String sender, String messageText) {
        Log.d(TAG, "addToConversationHistory called with sender: " + sender + ", messageText: " + messageText);

        String fullMessage = sender + ": " + messageText;

        // Set conversationTitle when the first user message is added
        if ((conversationTitle == null || conversationTitle.isEmpty()) && sender.equals("User")) {
            conversationTitle = messageText;
            Log.d(TAG, "Conversation title set to: " + conversationTitle);
        }

        conversationHistory.add(fullMessage);

        // Save the message with the conversationTitle
        ConversationMessage message = new ConversationMessage(
                System.currentTimeMillis(),
                sender,
                messageText,
                currentConversationId,
                conversationTitle
        );
        saveMessageToDatabase(message);

        // Maintain max history size
        if (conversationHistory.size() > MAX_HISTORY_SIZE) {
            conversationHistory.remove(0);
        }
    }

    private void saveMessageToDatabase(ConversationMessage message) {
        new Thread(() -> {
            // Insert the new message
            conversationDao.insertMessage(message);
            // No need to update the conversation title here
        }).start();
    }

    private void loadMessagesFromDays(int days) {
        new Thread(() -> {
            long currentTime = System.currentTimeMillis();
            long startTime = currentTime - (days * 24 * 60 * 60 * 1000L);
            List<ConversationMessage> messages = conversationDao.getMessagesFrom(startTime);
            runOnUiThread(() -> displayMessages(messages));
        }).start();
    }

    private void displayMessages(List<ConversationMessage> messages) {
        adapter.clearMessages();
        conversationHistory.clear(); // Clear the in-memory history
        for (ConversationMessage msg : messages) {
            String displayText = msg.getSender() + ": " + msg.getMessage();
            adapter.addMessage(displayText);
            conversationHistory.add(displayText); // Update the in-memory history
        }
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);

        // Hide center image and help text if there are messages
        if (adapter.getItemCount() > 0) {
            centerImage.setVisibility(View.GONE);
            helpText.setVisibility(View.GONE);

            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();
            params.topToBottom = R.id.viewIn;
            recyclerView.setLayoutParams(params);
        }
    }

    private void startNewChat() {
        new Thread(() -> {
            currentConversationId = getNextConversationId();

            // Reset conversationTitle to null
            conversationTitle = null;

            // Clear in-memory history
            conversationHistory.clear();

            // Update UI on main thread
            runOnUiThread(() -> {
                adapter.clearMessages();

                // Reset UI elements
                centerImage.setVisibility(View.VISIBLE);
                helpText.setVisibility(View.VISIBLE);

                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();
                params.topToBottom = R.id.help_text;
                recyclerView.setLayoutParams(params);

                // Reset last user message
                lastUserMessage = "";

                // Update the previous conversations menu
                updatePreviousConversationsMenu();
            });
        }).start();
    }

    private Integer getNextConversationId() {
        Integer maxId = conversationDao.getMaxConversationId();
        if (maxId != null) {
            return maxId + 1;
        } else {
            return 1;
        }
    }

    private void openOnlineCompiler() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.onlinegdb.com/"));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.e(TAG, "No browser app available to handle the intent!");
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START) || drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.delete_all) {
            // Show confirmation dialog
            showDeleteAllChatsConfirmationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void showDeleteAllChatsConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Chats")
                .setMessage("Do you want to delete all chats?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete all chats
                    deleteAllChats();
                })
                .setNegativeButton("No", null)
                .show();
    }
    private void deleteAllChats() {
        new Thread(() -> {
            // Delete all messages from the database
            conversationDao.deleteAllMessages();

            // Reset conversation ID and title
            currentConversationId = getNextConversationId();
            conversationTitle = null;

            // Clear in-memory history
            conversationHistory.clear();

            // Update UI on the main thread
            runOnUiThread(() -> {
                adapter.clearMessages();

                // Reset UI elements
                centerImage.setVisibility(View.VISIBLE);
                helpText.setVisibility(View.VISIBLE);

                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();
                params.topToBottom = R.id.help_text;
                recyclerView.setLayoutParams(params);

                // Reset last user message
                lastUserMessage = "";

                // Update the previous conversations menu
                updatePreviousConversationsMenu();

                Toast.makeText(this, "All chats have been deleted.", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}