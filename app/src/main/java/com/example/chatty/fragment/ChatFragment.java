package com.example.chatty.fragment;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chatty.R;
import com.example.chatty.chat.GroupMessageActivity;
import com.example.chatty.chat.MessageActivity;
import com.example.chatty.model.ChatModel;
import com.example.chatty.model.UserModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class ChatFragment extends Fragment {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.chatfragment_recyclerview);


        // Set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));

        // Set adapter with ChatRecyclerViewAdapter
        ChatRecyclerViewAdapter adapter = new ChatRecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        // ItemTouchHelper for swipe-to-delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Swipe action detected
                int position = viewHolder.getAdapterPosition();
                adapter.deleteChatRoom(position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                if (Math.abs(dX) > 0) {
                    // 빨간색 배경 그리기
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint);

                    // 흰색으로 "삭제" 텍스트 그리기
                    Paint textPaint = new Paint();
                    textPaint.setColor(Color.WHITE);
                    textPaint.setTextSize(55);
                    textPaint.setFakeBoldText(true);
                    String text = "삭제";
                    float textWidth = textPaint.measureText(text);
                    float x = itemView.getRight() - textWidth - 70; // 여유 공간을 주고 오른쪽에 표시
                    float y = itemView.getTop() + ((itemView.getBottom() - itemView.getTop()) / 2) + 20; // 수직 중앙에 표시
                    c.drawText(text, x, y, textPaint);
                }
            }
        };

        // Attach ItemTouchHelper to RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // 채팅 버튼 클릭 시 이벤트 처리
        FloatingActionButton floatingActionButton = view.findViewById(R.id.chatfragment_floatingButton2); // 수정된 부분
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), SelectFriendActivity.class));
            }
        });
        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<ChatModel> chatModels = new ArrayList<>();
        private List<String> keys = new ArrayList<>();
        private String uid;
        private ArrayList<String> destinationUsers = new ArrayList<>();
        private Map<String, Integer> unreadCountMap = new HashMap<>();

        public ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + uid)
                    .equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            chatModels.clear();
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                chatModels.add(item.getValue(ChatModel.class));
                                keys.add(item.getKey());

                                // 읽지 않은 메시지 수 계산 및 업데이트
                                calculateUnreadMessages(item.getKey());
                            }
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {

            final CustomViewHolder customViewHolder = (CustomViewHolder) holder;
            String destinationUid = null;

            for (String user : chatModels.get(position).users.keySet()) {
                if (!user.equals(uid)) {
                    destinationUid = user;
                    destinationUsers.add(destinationUid);
                }
            }

            if (destinationUid != null) {
                FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        UserModel userModel = dataSnapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            Glide.with(customViewHolder.itemView.getContext())
                                    .load(userModel.profileImageUrl)
                                    .apply(new RequestOptions().circleCrop())
                                    .into(customViewHolder.imageView);

                            customViewHolder.textView_title.setText(userModel.userName);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
            // 읽지 않은 메시지가 있는지 확인
            if (unreadCountMap.containsKey(keys.get(position))) {
                int unreadCount = unreadCountMap.get(keys.get(position));
                customViewHolder.textView_unread_count.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                customViewHolder.textView_unread_count.setText(String.valueOf(unreadCount));
            } else {
                customViewHolder.textView_unread_count.setVisibility(View.GONE);
            }

            Map<String, ChatModel.Comment> commentMap = new TreeMap<>(Collections.reverseOrder());
            commentMap.putAll(chatModels.get(position).comments);
            if (commentMap.keySet().toArray().length > 0) {
                String lastMessageKey = (String) commentMap.keySet().toArray()[0];
                customViewHolder.textView_last_message.setText(chatModels.get(position).comments.get(lastMessageKey).message);

                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                long unixTime = (long) chatModels.get(position).comments.get(lastMessageKey).timestamp;
                Date date = new Date(unixTime);
                customViewHolder.textView_timestamp.setText(simpleDateFormat.format(date));
            }

            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = null;
                    int adapterPosition = customViewHolder.getAdapterPosition();

                    if (chatModels.get(adapterPosition).users.size() > 2) {
                        intent = new Intent(view.getContext(), GroupMessageActivity.class);
                        intent.putExtra("destinationRoom", keys.get(position));
                    } else {
                        intent = new Intent(view.getContext(), MessageActivity.class);
                        intent.putExtra("destinationUid", destinationUsers.get(adapterPosition));
                        intent.putExtra("destinationUid", destinationUsers.get(position));
                    }

                    ActivityOptions activityOptions = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright, R.anim.toleft);
                        startActivity(intent, activityOptions.toBundle());
                    }
                }
            });
        }

        public void deleteChatRoom(int position) {
            String key = keys.get(position);

            // Show confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("삭제 확인");
            builder.setMessage("정말로 채팅방을 삭제하시겠습니까?");
            builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // User clicked "Yes," proceed with deletion
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(key).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                // Successfully deleted from Firebase, now update the local data
                                chatModels.remove(position);
                                keys.remove(position);
                                notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                // Handle failure (e.g., logging or displaying a message)
                                Log.e("ChatFragment", "파이어베이스에서 채팅방 삭제 실패 : " + e.getMessage());
                            });
                }
            });
            builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // User clicked "No," dismiss the dialog
                    notifyDataSetChanged(); // Refresh the RecyclerView to revert the swipe
                }
            });
            builder.show();
        }

        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView_title;
            public TextView textView_last_message;
            public TextView textView_timestamp;
            public TextView textView_unread_count;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.chatitem_imageview);
                textView_title = view.findViewById(R.id.chatitem_textview_title);
                textView_last_message = view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timestamp = view.findViewById(R.id.chatitem_textview_timestamp);
                textView_unread_count = view.findViewById(R.id.chatitem_textview_unread_count);
            }


        }
        private void calculateUnreadMessages(String chatroomKey) {
            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatroomKey).child("comments")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            int unreadCount = 0;
                            for (DataSnapshot commentSnapshot : dataSnapshot.getChildren()) {
                                ChatModel.Comment comment = commentSnapshot.getValue(ChatModel.Comment.class);
                                if (comment != null && !comment.readUsers.containsKey(uid)) {
                                    unreadCount++;
                                }
                            }

                            unreadCountMap.put(chatroomKey, unreadCount);
                            notifyDataSetChanged(); // 업데이트 후 RecyclerView 갱신
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // 에러 처리
                        }
                    });
        }
    }
}
