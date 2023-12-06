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
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.CountDownLatch;


public class Login extends AppCompatActivity {

    Button send_button;
    EditText textInputEmail;
    EditText textInputPassword;

    private String value;

    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        send_button = findViewById(R.id.send_button_id);

        textInputEmail = findViewById(R.id.text_input_email);
        textInputPassword = findViewById(R.id.text_input_password);

        send_button.setOnClickListener(v -> {
            if (!validateEmail() | !validatePassword()) {
                return;
            }else {
                loginUserAccount();
            }
        });
    }

    private boolean validateEmail() {
        String emailInput = textInputEmail.getText().toString().trim();

        if (emailInput.isEmpty()) {
            textInputEmail.setError("Email can't be empty");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            textInputEmail.setError("Please enter a valid email address");
            return false;
        } else {
            textInputEmail.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String passwordInput = textInputPassword.getText().toString().trim();

        if (passwordInput.isEmpty()) {
            textInputPassword.setError("Password can't be empty");
            return false;
        } else {
            textInputPassword.setError(null);
            return true;
        }
    }

    private void loginUserAccount()
    {

        // Take the value of two edit texts in Strings
        String email, password;
        email = textInputEmail.getText().toString();
        password = textInputPassword.getText().toString();

        // signin existing user
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(
                                    @NonNull Task<AuthResult> task)
                            {

                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(),
                                                    "Welcome",
                                                    Toast.LENGTH_LONG)
                                            .show();

                                    FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                                    DatabaseReference myRef = database.getReference(currentFirebaseUser.getUid());

                                    // Add ValueEventListener to retrieve the value
                                    myRef.child("Link").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            // This method is called once with the initial value and again whenever
                                            // data at this location is updated.
                                            value = dataSnapshot.getValue(String.class);
                                            if (value != null) {
                                                    // if value is an empty string
                                                    Intent intent = new Intent(Login.this, RssReader.class);
                                                    startActivity(intent);
                                            }else{
                                                Intent intent = new Intent(Login.this, Subscribe.class);
                                                startActivity(intent);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            // Handle any errors that may occur.
                                            Toast.makeText(Login.this, "Error retrieving value", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                else {
                                    // sign-in failed
                                    Toast.makeText(getApplicationContext(),
                                                    "Invalid Login",
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            }
                        });
    }
}