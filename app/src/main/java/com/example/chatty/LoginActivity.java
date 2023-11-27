package com.example.chatty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth; //파이어베이스 인증
    private EditText Id_edit,Pass_edit; // 이메일과 비밀번호를 입력받는것
    private Button signButton,loginButton;
    private FirebaseAuth.AuthStateListener authStateListener; // 인증 상태 리스너

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mFirebaseAuth = FirebaseAuth.getInstance(); // FirebaseAuth 객체 초기화
        mFirebaseAuth.signOut(); // 혹시 로그인 상태면 로그아웃

        Id_edit = findViewById(R.id.Id_edit);
        Pass_edit = findViewById(R.id.Pass_edit);
        loginButton = findViewById(R.id.login_btn);
        signButton = findViewById(R.id.sign_btn);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginEvent();
            }
        });

        signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 회원가입 화면으로 이동
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivity(intent);
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //로그인
                    Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    //로그아웃
                }

            }
        };
    }
    // 로그인 이벤트 처리 메서드
    void loginEvent() {
        // FirebaseAuth를 이용하여 이메일과 비밀번호로 로그인 시도
        mFirebaseAuth.signInWithEmailAndPassword(Id_edit.getText().toString(), Pass_edit.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {
                            //로그인 실패한부분
                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }


                    }
                });

    }
    @Override
    protected void onStart() {
        super.onStart();
        // FirebaseAuth의 상태 리스너 등록
        mFirebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // FirebaseAuth의 상태 리스너 해제
        mFirebaseAuth.removeAuthStateListener(authStateListener);
    }
}