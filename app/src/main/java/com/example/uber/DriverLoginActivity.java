package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

public class DriverLoginActivity extends AppCompatActivity {

    //private EditText mEmail, mPassword;
    private TextInputLayout mEmail, mPassword;
    private Button mLogin, mRegistration;
    private TextView mForgotPassword;

    private String userId;

    private static final String TAG = "DriverLoginActivity";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth=FirebaseAuth.getInstance();

        /**
         * Obiect responsabil de starea inregistrarii
         */
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    getUserAccType();
                }
            }
        };

        mEmail=(TextInputLayout)findViewById(R.id.email);
        mPassword=(TextInputLayout)findViewById(R.id.password);
        mLogin = (Button)findViewById(R.id.login);
        mRegistration=(Button)findViewById(R.id.registration);
        mForgotPassword = findViewById(R.id.forgotPassword);

        /**
         * Crearea conturilor utilizatorilor
         */
        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateEmail() | validatePassword() && (validateEmail() && validatePassword())) {
                    final String email = mEmail.getEditText().getText().toString();
                    final String password = mPassword.getEditText().getText().toString();
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(DriverLoginActivity.this, "Error: This email is already used", Toast.LENGTH_SHORT).show();
                            } else {
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("name");
                                current_user_db.setValue(email);
                            }
                        }
                    });
                }
            }
        });
        /**
         * Logarea in aplciatie
         */
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateEmail() | validatePassword() && (validateEmail() && validatePassword())) {
                    final String email = mEmail.getEditText().getText().toString();
                    final String password = mPassword.getEditText().getText().toString();
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(DriverLoginActivity.this, "Sign in error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        /**
         * Resetarea parolei
         */
        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DriverLoginActivity.this, ForgotPassword.class));
            }
        });

    }

    /**
     * Determinarea tipului de utilizator astfel incat pasagerii sa nu se poata autentifica in sectiunea soferilor si invers
     */
    public void getUserAccType() {
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference requestDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);
        requestDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Intent intent = new Intent(getApplication(), DriverMapActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);/**/
                    finish();
                    startActivity(intent);
                    return;
                }else{
                    Toast.makeText(DriverLoginActivity.this, "We dont find this account in Drivers.Maybe you want to login in Customers?", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /**
     * validarea email-ului
     */
    private boolean validateEmail() {
        final String emailInput = mEmail.getEditText().getText().toString().trim();
        if (emailInput.isEmpty()) {
            mEmail.setError("The email field can't be empty");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            mEmail.setError("Please enter a valid email address");
            return false;
        } else {
            mEmail.setError(null);
            return true;
        }
    }

    /**
     * Validam Parola
     */
    private boolean validatePassword() {
        final String passwordInput = mPassword.getEditText().getText().toString().trim();
        if (passwordInput.isEmpty()) {
            mPassword.setError("The password field can't be empty");
            return false;
        } else if (!PayPalConfig.PASSWORD_PATTERN.matcher(passwordInput).matches()) {
            mPassword.setError("The password is to week");
            return false;
        } else {
            mPassword.setError(null);
            return true;
        }
    }



    /**
     * Activam AuthStateListener-ul, ce este responsabil de starea logarii, in momentul in care activitatea este pregatita de a fi
     * afisata pe dispozitiv
     */
    @Override
    protected void onStart(){
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    /**
     * Cand activitatea este in background sau aplicatia este scoasa din memorie, sa dezactivam AuthStateListener-ul
     */
    @Override
    protected void onStop(){
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
