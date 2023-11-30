package com.example.chatty;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatty.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;


public class SignupActivity extends AppCompatActivity {

    private static final int PICK_FROM_ALBUM = 10;
    private EditText et_email, et_pass,et_name;
    private Button signup_btn;
    private ImageView profile;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        et_email = findViewById(R.id.et_email);
        et_pass = findViewById(R.id.et_pass);
        et_name = findViewById(R.id.et_name);

        signup_btn = findViewById(R.id.signup_btn);

        profile = findViewById(R.id.signupActivity_imageview_profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 앨범에서 이미지 선택
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent,PICK_FROM_ALBUM);
            }
        });
        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 입력된 이메일, 비밀번호, 이름이 null이 아니고 이미지가 선택되었을 때
                if (et_email.getText().toString() == null || et_name.getText().toString() == null|| et_pass.getText().toString() == null||imageUri==null){
                    return;
                }

                // Firebase에 사용자 등록 요청
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(et_email.getText().toString(), et_pass.getText().toString())
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // 사용자 등록 성공
                                    String uid = task.getResult().getUser().getUid();
                                    UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(et_name.getText().toString()).build();
                                    task.getResult().getUser().updateProfile(userProfileChangeRequest);

                                    // Firebase Storage에 프로필 이미지 업로드
                                    FirebaseStorage.getInstance().getReference().child("userImages").child(uid).putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            @SuppressWarnings("VisibleForTests")
                                            Task<Uri> uriTask = task.getResult().getStorage().getDownloadUrl();
                                            while (!uriTask.isSuccessful()) ;
                                            Uri downloadUrl = uriTask.getResult();
                                            String imageUrl = String.valueOf(downloadUrl);

                                            // 사용자 정보를 담고 있는 모델 객체 생성
                                            UserModel userModel = new UserModel();
                                            userModel.userName = et_name.getText().toString();
                                            userModel.profileImageUrl = imageUrl;
                                            userModel.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                            // Firebase Database에 사용자 정보 등록
                                            FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    // 회원가입 성공 시 현재 액티비티 종료
                                                    SignupActivity.this.finish();
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    // Handle the error if user creation fails
                                }
                            }
                        });

            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FROM_ALBUM && resultCode == RESULT_OK) {
            // 선택한 이미지를 이미지뷰에 표시하고 이미지 경로 저장
            profile.setImageURI(data.getData()); // 가운데 뷰를 바꿈
            imageUri = data.getData();// 이미지 경로 원본
        }
    }
}
