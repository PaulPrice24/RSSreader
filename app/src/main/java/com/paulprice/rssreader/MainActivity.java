package com.paulprice.rssreader;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void Login(View view) {
        Intent i = new Intent(getApplicationContext(), Login.class);
        startActivity(i);
    }

    public void Register(View view) {
        Intent i = new Intent(getApplicationContext(), Register.class);
        startActivity(i);
    }
}