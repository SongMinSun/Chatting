package com.example.chatty.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chatty.MainActivity;
import com.example.chatty.R;
import com.example.chatty.fragment.ChatFragment;
import com.example.chatty.model.ChatModel;
import com.example.chatty.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class GroupMessageActivity extends AppCompatActivity {
    Map<String, UserModel> users = new HashMap<>();
    String destinationRoom;
    String uid;
    EditText editText;
    private ImageView back_chat;

    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private RecyclerView recyclerView;
    List<ChatModel.Comment> comments = new ArrayList<>();

    int peopleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);
        destinationRoom = getIntent().getStringExtra("destinationRoom");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        editText = findViewById(R.id.groupMessageActivity_editText);
        back_chat = findViewById(R.id.back_chat);
        back_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // back_chat를 클릭했을 때의 이벤트를 처리합니다.
                Intent intent = new Intent(GroupMessageActivity.this, MainActivity.class);
                intent.putExtra("openChatFragment", true);
                startActivity(intent);

                // 현재 액티비티를 종료하여 이전 액티비티로 돌아갈 수 있습니다.
                finish();
            }
        });

        FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    users.put(item.getKey(), item.getValue(UserModel.class));
                }

                init();
                recyclerView = findViewById(R.id.groupMessageActivity_recyclerview);
                recyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
                recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    void init() {
        Button button = findViewById(R.id.groupMessageActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatModel.Comment comment = new ChatModel.Comment();
                comment.uid = uid;
                comment.message = editText.getText().toString();
                comment.timestamp = ServerValue.TIMESTAMP;
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Map<String, Boolean> map = (Map<String, Boolean>) dataSnapshot.getValue();

                                for (String item : map.keySet()) {
                                    if (item.equals(uid)) {
                                        continue;
                                    }
                                }
                                editText.setText("");

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });

            }
        });
    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public GroupMessageRecyclerViewAdapter() {
            getMessageList();
        }

        void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments");
            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    comments.clear();
                    Map<String, Object> readUsersMap = new HashMap<>();
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment_origin = item.getValue(ChatModel.Comment.class);
                        ChatModel.Comment comment_modify = item.getValue(ChatModel.Comment.class);
                        comment_modify.readUsers.put(uid, true);

                        readUsersMap.put(key, comment_modify);
                        comments.add(comment_origin);
                    }

                    if (!comments.isEmpty() && !comments.get(comments.size() - 1).readUsers.containsKey(uid)) {
                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments")
                                .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        notifyDataSetChanged();
                                        recyclerView.scrollToPosition(comments.size() - 1);
                                    }
                                });
                    } else {
                        notifyDataSetChanged();
                        recyclerView.scrollToPosition(comments.size() - 1);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new GroupMessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            GroupMessageViewHolder messageViewHolder = ((GroupMessageViewHolder) holder);

            if (comments.get(position).uid.equals(uid)) {
                messageViewHolder.textViewMessage.setText(comments.get(position).message);
                messageViewHolder.textViewMessage.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.linearLayoutDestination.setVisibility(View.INVISIBLE);
                messageViewHolder.textViewMessage.setTextSize(25);
                messageViewHolder.linearLayoutMain.setGravity(Gravity.RIGHT);
                setReadCounter(position, messageViewHolder.textViewReadCounterLeft);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(users.get(comments.get(position).uid).profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageViewProfile);
                messageViewHolder.textViewName.setText(users.get(comments.get(position).uid).userName);
                messageViewHolder.linearLayoutDestination.setVisibility(View.VISIBLE);
                messageViewHolder.textViewMessage.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textViewMessage.setText(comments.get(position).message);
                messageViewHolder.textViewMessage.setTextSize(25);
                messageViewHolder.linearLayoutMain.setGravity(Gravity.LEFT);
                setReadCounter(position, messageViewHolder.textViewReadCounterRight);
            }

            long unixTime = (long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textViewTimestamp.setText(time);
        }

        void setReadCounter(final int position, final TextView textView) {
            if (comments.isEmpty()) {
                textView.setVisibility(View.INVISIBLE);
                return;
            }

            if (peopleCount == 0) {
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();
                        peopleCount = users.size();

                        if (position < comments.size()) {
                            int count = peopleCount - comments.get(position).readUsers.size();
                            if (count > 0) {
                                textView.setVisibility(View.VISIBLE);
                                textView.setText(String.valueOf(count));
                            } else {
                                textView.setVisibility(View.INVISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else {
                if (position < comments.size()) {
                    int count = peopleCount - comments.get(position).readUsers.size();
                    if (count > 0) {
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(String.valueOf(count));
                    } else {
                        textView.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class GroupMessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textViewMessage;
            public TextView textViewName;
            public ImageView imageViewProfile;
            public LinearLayout linearLayoutDestination;
            public LinearLayout linearLayoutMain;
            public TextView textViewTimestamp;
            public TextView textViewReadCounterLeft;
            public TextView textViewReadCounterRight;

            public GroupMessageViewHolder(View view) {
                super(view);
                textViewMessage = view.findViewById(R.id.messageItem_textView_message);
                textViewName = view.findViewById(R.id.messageItem_textview_name);
                imageViewProfile = view.findViewById(R.id.messageItem_imageview_profile);
                linearLayoutDestination = view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayoutMain = view.findViewById(R.id.messageItem_linearlayout_main);
                textViewTimestamp = view.findViewById(R.id.messageItem_textview_timestamp);
                textViewReadCounterLeft = view.findViewById(R.id.messageItem_textview_readCounter_left);
                textViewReadCounterRight = view.findViewById(R.id.messageItem_textview_readCounter_right);
            }
        }
    }
}
