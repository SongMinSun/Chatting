package com.example.chatty.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.chatty.R;
import com.example.chatty.model.UserModel;
import com.example.chatty.chat.MessageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class PeopleFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragement_people, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new PeopleFragmentRecyclerViewAdapter());

        return view;
    }

    // RecyclerView에 표시되는 친구 목록을 관리하는 어댑터 클래스
    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<UserModel> userModels; // 사용자 모델 목록을 저장하는 리스트

        // 어댑터 생성자
        public PeopleFragmentRecyclerViewAdapter() {
            userModels = new ArrayList<>();
            String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // 데이터베이스에서 모든 사용자 정보를 가져오기
            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userModels.clear();// 기존 목록을 비움
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);

                        if(userModel.uid.equals(myUid)){
                            continue; // 자신은 목록에 추가하지 않음
                        }
                        userModels.add(userModel); // 사용자 모델을 목록에 추가

                    }
                    notifyDataSetChanged(); // 어댑터에 데이터 변경 알림
                }

                @Override
                public void onCancelled(DatabaseError error) {

                }
            });
        }

        // ViewHolder를 생성하고 레이아웃 연결
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            return new CustomViewHolder(view);
        }

        // ViewHolder에 데이터를 바인딩
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // Glide를 사용하여 프로필 이미지 로드
            Glide.with(holder.itemView.getContext())
                    .load(userModels.get(position).profileImageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("Glide", "Image load failed", e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(((CustomViewHolder) holder).imageView);
            // 사용자 이름 설정
            ((CustomViewHolder) holder).textView.setText(userModels.get(position).userName);

            // 아이템 클릭 이벤트 처리
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        // 메시지 화면으로 전환하는 인텐트 생성
                        Intent intent = new Intent(view.getContext(), MessageActivity.class);
                        intent.putExtra("destinationUid", userModels.get(adapterPosition).uid);
                        // 화면 전환 애니메이션 설정
                        ActivityOptions activityOptions = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright, R.anim.toleft);
                            startActivity(intent, activityOptions.toBundle());
                        }
                    }
                }
            });
            if(userModels.get(position).comment != null){
                ((CustomViewHolder) holder).textView_comment.setText(userModels.get(position).comment);
            }
        }

        // 목록의 아이템 개수 반환
        @Override
        public int getItemCount() {
            return userModels.size();
        }

        // ViewHolder 클래스 정의
        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_comment;

            public CustomViewHolder(View view) {
                super(view);
                imageView = (ImageView) view.findViewById(R.id.frienditem_imageview);
                textView = (TextView) view.findViewById(R.id.frienditem_textview);
                textView_comment = (TextView)view.findViewById(R.id.frienditem_textview_comment);
            }
        }
    }
}

