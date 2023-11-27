package com.example.chatty;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.chatty.fragment.peopleFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fragment를 관리하기 위한 FragmentManager 객체 생성
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Fragment 트랜잭션 시작
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // activity_main.xml 파일에 정의된 FrameLayout에 peopleFragment를 추가 또는 교체
        fragmentTransaction.replace(R.id.mainactivity_framelayout, new peopleFragment());
        // 트랜잭션 완료
        fragmentTransaction.commit();
    }
}
