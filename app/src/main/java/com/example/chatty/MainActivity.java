package com.example.chatty;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.chatty.fragment.AccountFragment;
import com.example.chatty.fragment.ChatFragment;
import com.example.chatty.fragment.PeopleFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 로그인 여부 확인
        if (isUserLoggedIn()) {
            // 이미 로그인되어 있다면 채팅 화면으로 전환
            replaceFragment(new PeopleFragment());
        } else {
            // 로그인되어 있지 않다면 로그인 화면으로 전환
            // (여기에 로그인 화면으로 전환하는 코드를 추가하세요)
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish(); // 로그인 화면으로 전환 후 현재 액티비티를 종료
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.mainactivity_bottomnavigationview);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_people:
                        replaceFragment(new PeopleFragment());
                        return true;
                    case R.id.action_chat:
                        replaceFragment(new ChatFragment());
                        return true;
                    case R.id.action_account:
                        replaceFragment(new AccountFragment());
                        return true;
                }

                return false;
            }
        });
    }

    private boolean isUserLoggedIn() {
        // Firebase Authentication을 사용하는 경우 현재 로그인된 사용자가 있는지 확인
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainactivity_framelayout, fragment);
        fragmentTransaction.commit();
    }
}
