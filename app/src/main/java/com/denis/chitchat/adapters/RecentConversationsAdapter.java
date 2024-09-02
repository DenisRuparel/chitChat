package com.denis.chitchat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.denis.chitchat.databinding.ItemContainerRecentConversionBinding;
import com.denis.chitchat.listeners.ConversionListener;
import com.denis.chitchat.models.ChatMessage;
import com.denis.chitchat.models.User;

import java.util.List;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.CoversionViewHolder>{

    private final List<ChatMessage> chatMessage;
    private final ConversionListener conversionListener;

    public RecentConversationsAdapter(List<ChatMessage> chatMessage, ConversionListener conversionListener) {
        this.chatMessage = chatMessage;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public CoversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CoversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull CoversionViewHolder holder, int position) {
        holder.setData(chatMessage.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessage.size();
    }

    class CoversionViewHolder extends RecyclerView.ViewHolder{

        ItemContainerRecentConversionBinding binding;

        public CoversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage){
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;
                conversionListener.onConversionclicked(user);
            });
        }
    }

    private Bitmap getConversionImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
