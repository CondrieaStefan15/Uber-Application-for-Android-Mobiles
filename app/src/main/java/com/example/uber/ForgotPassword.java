package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;


public class ForgotPassword extends AppCompatActivity {

    Toolbar toolbar;
    ProgressBar progressBar;
    TextInputLayout userEmail;
    Button sendEmail;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        userEmail = findViewById(R.id.EmailToSend);
        sendEmail = findViewById(R.id.SendPassword);

        firebaseAuth=FirebaseAuth.getInstance();

        /**
         * trimiterea mail-ului de resetare al parolei
         */
        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                firebaseAuth.sendPasswordResetEmail(userEmail.getEditText().getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        if(task.isSuccessful()){
                            Toast.makeText(ForgotPassword.this, "The link to reset your password was sended", Toast.LENGTH_LONG).show();
                        }else
                        {
                            Toast.makeText(ForgotPassword.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();

                        }
                    }
                });
            }
        });
    }
}
