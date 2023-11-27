package com.example.chatty.chat;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chatty.R;
import com.example.chatty.model.ChatModel;
import com.example.chatty.model.UserModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageActivity extends AppCompatActivity {

    private String destinatonUid; // 대화 상대의 UID
    private Button button;
    private EditText editText;

    private String uid;  // 현재 사용자의 UID
    private String chatRoomUid;  // 채팅방의 UID
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message2);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();  //채팅을 요구 하는 아아디 즉 단말기에 로그인된 UID
        destinatonUid = getIntent().getStringExtra("destinationUid"); // 채팅을 당하는 아이디
        button = findViewById(R.id.messageActivity_button);
        editText = findViewById(R.id.messageActivity_editText);
        recyclerView = findViewById(R.id.messageActivity_recyclerview);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 대화 모델 생성
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid, true);
                chatModel.users.put(destinatonUid, true);

                // 채팅방 UID가 없는 경우
                if (chatRoomUid == null) {
                    // 데이터베이스에 새로운 채팅방 생성
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // 채팅방이 성공적으로 생성된 경우, 다시 채팅방 확인
                                    checkChatRoom();
                                }
                            });
                } else {
                    // 채팅방 UID가 있는 경우, 기존 채팅방에 새로운 댓글 추가
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = editText.getText().toString();

                    // 데이터베이스에 "comments" 필드가 없으면 생성
                    FirebaseDatabase.getInstance().getReference()
                            .child("chatrooms")
                            .child(chatRoomUid)
                            .child("comments")
                            .updateChildren(new HashMap<String, Object>());

                    // 이후에 대화 추가
                    FirebaseDatabase.getInstance().getReference()
                            .child("chatrooms")
                            .child(chatRoomUid)
                            .child("comments")
                            .push()
                            .setValue(comment)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // 성공적으로 댓글이 추가되었을 때
                                    editText.setText(""); // 댓글이 추가된 후 editText를 초기화
                                }
                            });
                }
            }
        });
        // 액티비티 생성 시 채팅방 확인
        checkChatRoom();


    }
    // 채팅방을 확인하는 메소드
    void  checkChatRoom(){

        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot item : dataSnapshot.getChildren()){
                    // 채팅방들을 순회
                    ChatModel  chatModel = item.getValue(ChatModel.class);
                    if(chatModel.users.containsKey(destinatonUid)){
                        // 대화 상대가 채팅방에 포함된 경우, 채팅방 UID 설정
                        chatRoomUid = item.getKey();
                        button.setEnabled(true);
                        // RecyclerView 및 어댑터 설정
                        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 데이터베이스 오류 처리
            }
        });
    }

    // RecyclerView를 관리하는 RecyclerViewAdapter 클래스
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{


        List<ChatModel.Comment> comments;
        UserModel userModel;

        // 어댑터의 생성자
        public RecyclerViewAdapter() {
            comments = new ArrayList<>();

            // 대화 상대의 정보를 데이터베이스에서 가져옴
            FirebaseDatabase.getInstance().getReference().child("users").child(destinatonUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userModel = dataSnapshot.getValue(UserModel.class);
                    // 채팅방의 메시지 목록을 가져옴
                    getMessageList();

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // 데이터베이스 오류 처리

                }
            });






        }
        // 채팅방의 메시지 목록을 가져오는 메소드
        void getMessageList(){

            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    comments.clear();

                    // 댓글들을 순회하며 목록에 추가
                    for(DataSnapshot item : dataSnapshot.getChildren()){
                        comments.add(item.getValue(ChatModel.Comment.class));
                    }
                    // 어댑터에 데이터 변경 알림
                    notifyDataSetChanged();
                    // RecyclerView를 마지막 위치로..
                    recyclerView.scrollToPosition(comments.size() -1);


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }

        // 새로운 ViewHolder가 필요한 경우 생성하는 메소드
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message,parent,false);


            return new MessageViewHolder(view);
        }

        // ViewHolder에 데이터를 바인딩하는 메소드
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder = ((MessageViewHolder)holder);

            // 내가보낸 메세지
            if(comments.get(position).uid.equals(uid)){
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);

            }else {
                // 상대가 보낸 메세지
                Glide.with(holder.itemView.getContext())
                        .load(userModel.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textview_name.setText(userModel.userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);


            }

        }

        // RecyclerView의 아이템 개수를 반환하는 메소드
        @Override
        public int getItemCount() {
            return comments.size();
        }

        // 메시지 아이템 뷰를 담당하는 MessageViewHolder 클래스
        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textview_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;

            // MessageViewHolder의 생성자
            public MessageViewHolder(View view) {
                super(view);
                textView_message = view.findViewById(R.id.messageItem_textView_message);
                textview_name = view.findViewById(R.id.messageItem_textview_name);
                imageView_profile = view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayout_main = (LinearLayout)view.findViewById(R.id.messageItem_linearlayout_main);
            }
        }
    }
}