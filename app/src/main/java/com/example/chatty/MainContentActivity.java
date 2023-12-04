package com.example.chatty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainContentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_content);

        Button startChatButton = findViewById(R.id.start_chat);
        startChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 버튼을 눌렀을 때 MainActivity를 실행하는 코드
                startActivity(new Intent(MainContentActivity.this, MainActivity.class));
                finish(); // 현재 액티비티를 종료해서 스택에서 제거
            }
        });
    }
}
