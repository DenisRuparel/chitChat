package com.denis.chitchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.denis.chitchat.adapters.ChatAdapter;
import com.denis.chitchat.databinding.ActivityChatBinding;
import com.denis.chitchat.models.ChatMessage;
import com.denis.chitchat.models.User;
import com.denis.chitchat.utilities.Constants;
import com.denis.chitchat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }

    public void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        if (receiverUser != null) {
            chatAdapter = new ChatAdapter(
                    chatMessages,
                    getBitmapFromEncodedString(receiverUser.image),
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
            binding.chatRecyclerView.setAdapter(chatAdapter);
        } else {
            Log.e("ChatActivity", "receiverUser is null in init()");
        }
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        Log.d("ChatActivity", "sendMessage method called");

        preferenceManager = new PreferenceManager(getApplicationContext());
        HashMap<String, Object> message = new HashMap<>();
        String senderId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (senderId == null) {
            Log.e("ChatActivity", "Sender ID is null");
            return;
        }

        message.put(Constants.KEY_SENDER_ID, senderId);

        if (receiverUser != null) {
            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        } else {
            Log.e("ChatActivity", "receiverUser is null in sendMessage()");
            return;
        }

        String messageText = binding.inputMessage.getText().toString();
        if (messageText.isEmpty()) {
            Log.e("ChatActivity", "Message text is empty");
            return;
        }

        message.put(Constants.KEY_MESSAGE, messageText);
        message.put(Constants.KEY_TIMESTAMP, new Date());

        Log.d("ChatActivity", "Sending message: " + message);

        Log.d("ChatActivity", "Before database add call");

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d("ChatActivity", "Message sent successfully with ID: " + documentReference.getId());
                    binding.inputMessage.setText(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Error sending message", e);
                });
        Log.d("ChatActivity", "After database add call");

        if (conversionId != null){
            updateConversion(binding.inputMessage.getText().toString());
        }
        else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
    }

    private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0){
                chatAdapter.notifyDataSetChanged();
            }
            else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null){
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            Log.e("ChatActivity", "encodedImage is null in getBitmapFromEncodedString()");
            return null;
        }
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> {
            Log.d("ChatActivity", "Send button clicked");
            sendMessage();
        });
    }

    private void addConversion(HashMap<String, Object> conversion){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message){
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, new Date());
    }

    private void checkForConversion(){
        if (chatMessages.size() != 0){
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (receiverUser != null) {
            binding.textname.setText(receiverUser.name);
        } else {
            Log.e("ChatActivity", "receiverUser is null in loadReceiverDetails()");
        }
    }

    private String getReadableDateTime(Date date){
      return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };
}