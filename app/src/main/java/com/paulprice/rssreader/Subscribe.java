package com.paulprice.rssreader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class Subscribe extends AppCompatActivity {

    Button send_button;
    EditText send_text;
    EditText item_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        send_button = findViewById(R.id.send_button_id);
        send_text = findViewById(R.id.send_text_id);
        item_text = findViewById(R.id.item_text_id);

        send_button.setOnClickListener(v -> {

            FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser() ;
            String str = send_text.getText().toString();
            String numberString = item_text.getText().toString();
            int number = Integer.parseInt(numberString);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference(currentFirebaseUser.getUid());

            myRef.child("Link").setValue(str);

            myRef.child("Items").setValue(number);

            Intent intent = new Intent(getApplicationContext(), RssReader.class);
            intent.putExtra("message_key", str);
            startActivity(intent);
        });
    }
}