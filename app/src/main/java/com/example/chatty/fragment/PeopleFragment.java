package com.example.chatty.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chatty.AddFriendActivity;
import com.example.chatty.R;
import com.example.chatty.chat.GroupMessageActivity;
import com.example.chatty.model.UserModel;
import com.example.chatty.chat.MessageActivity;
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
    private TextView friendCountTextView; // 추가된 부분

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragement_people, container, false);

        // 프로필 섹션을 위한 뷰 초기화
        profileImageView = view.findViewById(R.id.profile_imageview);
        profileUsernameTextView = view.findViewById(R.id.profile_username);
        profileStatusTextView = view.findViewById(R.id.profile_status);

        // 친구 수 표시 텍스트뷰 초기화
        friendCountTextView = view.findViewById(R.id.friend_count_textview);

        // 리사이클러뷰 설정 및 어댑터 지정
        RecyclerView recyclerView = view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new PeopleFragmentRecyclerViewAdapter());

        // 채팅 버튼 클릭 시 이벤트 처리
        FloatingActionButton floatingActionButton = view.findViewById(R.id.peoplefragment_floatingButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), AddFriendActivity.class));
            }
        });

        // 현재 사용자의 프로필 정보 로드 및 표시
        loadCurrentUserProfile();

        return view;
    }

    private void loadCurrentUserProfile() {
        // 현재 사용자의 프로필 정보를 가져와서 표시
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(myUid);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserModel currentUser = snapshot.getValue(UserModel.class);
                    if (currentUser != null) {
                        // Glide 또는 선호하는 이미지 로딩 라이브러리를 사용하여 프로필 이미지 로드
                        Glide.with(requireContext())
                                .load(currentUser.profileImageUrl)
                                .apply(new RequestOptions().circleCrop())
                                .into(profileImageView);

                        // 사용자명과 상태메시지 표시
                        profileUsernameTextView.setText(currentUser.userName);
                        profileStatusTextView.setText(currentUser.comment);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // 오류 처리
                Log.e("PeopleFragment", "현재 사용자 프로필 로딩 중 오류: " + error.getMessage());
            }
        });
    }

    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<UserModel> userModels;

        public PeopleFragmentRecyclerViewAdapter() {
            userModels = new ArrayList<>();
            final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // 현재 사용자의 친구 목록을 가져와서 데이터베이스에서 해당 사용자들의 정보만 필터링하여 가져오기
            DatabaseReference currentUserFriendsRef = FirebaseDatabase.getInstance().getReference().child("users").child(myUid).child("friends");
            currentUserFriendsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userModels.clear();

                    for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                        String friendUid = friendSnapshot.getValue(String.class);

                        // 해당 친구의 사용자 정보를 가져와서 리스트에 추가
                        DatabaseReference friendUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(friendUid);
                        friendUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    UserModel friendUser = snapshot.getValue(UserModel.class);
                                    if (friendUser != null) {
                                        userModels.add(friendUser);
                                    }

                                    // 업데이트
                                    updateFriendCount(userModels.size());
                                    notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // 오류 처리
                                Log.e("PeopleFragment", "친구 목록 로딩 중 오류: " + databaseError.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // 오류 처리
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