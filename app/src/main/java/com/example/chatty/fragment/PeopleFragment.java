package com.example.chatty.fragment;

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
import com.example.chatty.AddFriendActivity;
import com.example.chatty.R;
import com.example.chatty.model.UserModel;
import com.example.chatty.chat.MessageActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PeopleFragment extends Fragment {

    private ImageView profileImageView;
    private TextView profileUsernameTextView;
    private TextView profileStatusTextView;
    private TextView friendCountTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragement_people, container, false);

        profileImageView = view.findViewById(R.id.profile_imageview);
        profileUsernameTextView = view.findViewById(R.id.profile_username);
        profileStatusTextView = view.findViewById(R.id.profile_status);
        friendCountTextView = view.findViewById(R.id.friend_count_textview);

        RecyclerView recyclerView = view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        PeopleFragmentRecyclerViewAdapter adapter = new PeopleFragmentRecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        // ItemTouchHelper를 사용하여 스와이프 동작 처리
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 스와이프된 아이템의 위치(position)을 가져옴
                int position = viewHolder.getAdapterPosition();
                // 해당 위치의 아이템을 삭제
                adapter.deleteUser(position);
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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        FloatingActionButton floatingActionButton = view.findViewById(R.id.peoplefragment_floatingButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), AddFriendActivity.class));
            }
        });

        loadCurrentUserProfile();

        return view;
    }

    private void loadCurrentUserProfile() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(myUid);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserModel currentUser = snapshot.getValue(UserModel.class);
                    if (currentUser != null) {
                        Glide.with(requireContext())
                                .load(currentUser.profileImageUrl)
                                .apply(new RequestOptions().circleCrop())
                                .into(profileImageView);

                        profileUsernameTextView.setText(currentUser.userName);
                        profileStatusTextView.setText(currentUser.comment);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("PeopleFragment", "현재 사용자 프로필 로딩 중 오류: " + error.getMessage());
            }
        });
    }

    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<UserModel> userModels;

        public PeopleFragmentRecyclerViewAdapter() {
            userModels = new ArrayList<>();
            final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference currentUserFriendsRef = FirebaseDatabase.getInstance().getReference().child("users").child(myUid).child("friends");
            currentUserFriendsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userModels.clear();

                    for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                        String friendUid = friendSnapshot.getValue(String.class);

                        DatabaseReference friendUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(friendUid);
                        friendUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    UserModel friendUser = snapshot.getValue(UserModel.class);
                                    if (friendUser != null) {
                                        userModels.add(friendUser);
                                    }

                                    updateFriendCount(userModels.size());
                                    notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e("PeopleFragment", "친구 목록 로딩 중 오류: " + databaseError.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("PeopleFragment", "현재 사용자의 친구 목록 로딩 중 오류: " + databaseError.getMessage());
                }
            });
        }


        private void updateFriendCount(int count) {
            friendCountTextView.setText("친구 수 " + count + "명");
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            return new CustomViewHolder(view);
        }

        private void deleteUser(int position) {
            String targetUid = userModels.get(position).uid;

            // 다이얼로그 표시
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("삭제 확인");
            builder.setMessage("정말로 친구를 삭제하시겠습니까?");
            builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // 사용자가 '예'를 선택한 경우
                    // Update local data
                    userModels.remove(position);
                    updateFriendCount(userModels.size());
                    notifyItemRemoved(position);

                    // Update friends list in the current user's data locally
                    String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(myUid);
                    currentUserRef.child("friends").child(targetUid).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Remove the item from Firebase Realtime Database
                                    DatabaseReference targetUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(targetUid);

                                    // Remove the current user from the target user's friends list
                                    targetUserRef.child("friends").child(myUid).removeValue()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    // Handle success (e.g., logging or displaying a message)
                                                    Log.d("PeopleFragment", "친구 삭제 성공");

                                                    // Remove the target user from the current user's friends list
                                                    removeFriendFromCurrentUser(targetUid);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Handle failure (logging or displaying a message)
                                                    Log.e("PeopleFragment", "타겟 사용자의 friends 업데이트 실패: " + e.getMessage());
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Handle failure (logging or displaying a message)
                                    Log.e("PeopleFragment", "로컬 데이터 삭제 실패: " + e.getMessage());
                                }
                            });
                }
            });
            builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    notifyDataSetChanged();
                }
            });
            builder.show();
        }


        private void removeFriendFromCurrentUser(String targetUid) {
            // Remove the target user from the current user's friends list
            String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(myUid).child("friends");

            currentUserRef.orderByValue().equalTo(targetUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshot.getRef().removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Handle success (e.g., logging or displaying a message)
                                        Log.d("PeopleFragment", "타겟 사용자를 현재 사용자의 friends에서 삭제 성공");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Handle failure (logging or displaying a message)
                                        Log.e("PeopleFragment", "현재 사용자의 friends 업데이트 실패: " + e.getMessage());
                                    }
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                    Log.e("PeopleFragment", "현재 사용자의 friends 업데이트 중 오류: " + databaseError.getMessage());
                }
            });
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            Glide.with(holder.itemView.getContext())
                    .load(userModels.get(holder.getAdapterPosition()).profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(((CustomViewHolder) holder).imageView);
            ((CustomViewHolder) holder).textView.setText(userModels.get(holder.getAdapterPosition()).userName);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), MessageActivity.class);
                    intent.putExtra("destinationUid", userModels.get(holder.getAdapterPosition()).uid);
                    ActivityOptions activityOptions = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright, R.anim.toleft);
                        startActivity(intent, activityOptions.toBundle());
                    }
                }
            });

            if (userModels.get(position).comment != null) {
                ((CustomViewHolder) holder).textView_comment.setText(userModels.get(position).comment);
            }
        }

        @Override
        public int getItemCount() {
            return userModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_comment;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.frienditem_imageview);
                textView = view.findViewById(R.id.frienditem_textview);
                textView_comment = view.findViewById(R.id.frienditem_textview_comment);
            }
        }
    }
}
