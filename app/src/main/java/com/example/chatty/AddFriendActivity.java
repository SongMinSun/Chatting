package com.example.chatty;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddFriendActivity extends AppCompatActivity {

    private EditText etFriendEmail;
    private Button btnAddFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        etFriendEmail = findViewById(R.id.editFriendEmail);
        btnAddFriend = findViewById(R.id.addFriend);

        btnAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFriend();
            }
        });
    }

    private void addFriend() {
        final String friendEmail = etFriendEmail.getText().toString().trim();

        if (!friendEmail.isEmpty()) {
            // 현재 사용자의 UID 가져오기
            final String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // 사용자 목록에서 입력받은 이메일로 UID 가져오기
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
            usersRef.orderByChild("email").equalTo(friendEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        // 찾은 사용자의 UID
                        String friendUid = userSnapshot.getKey();

                        // 현재 사용자의 친구 목록에 추가
                        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserUid);
                        currentUserRef.child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                List<String> friends = new ArrayList<>();
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                                        friends.add(friendSnapshot.getValue(String.class));
                                    }
                                }

                                // 이미 친구 목록에 있는지 확인
                                if (!friends.contains(friendUid)) {
                                    friends.add(friendUid);
                                    currentUserRef.child("friends").setValue(friends);
                                    // TODO: 친구 추가 성공 처리

                                    // 친구 추가 후 화면 종료
                                    finish();
                                } else {
                                    // TODO: 이미 친구인 경우 처리
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // TODO: 에러 처리
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // TODO: 에러 처리
                }
            });
        }
    }

}
