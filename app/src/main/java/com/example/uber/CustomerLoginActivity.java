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

public class CustomerLoginActivity extends AppCompatActivity {


    private TextInputLayout mEmail, mPassword;
    private Button mLogin, mRegistration;
    private TextView mForgotPassword;

    private static final String TAG = "CustomerLoginActivity";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    getUserAccType();
                }
            }
        };

        mEmail = (TextInputLayout) findViewById(R.id.email);
        mPassword = (TextInputLayout) findViewById(R.id.password);
        mLogin = (Button) findViewById(R.id.login);
        mRegistration = (Button) findViewById(R.id.registration);
        mForgotPassword = findViewById(R.id.forgotPassword);

        /**
         * Iregistrarea conturilor utilizatorilor(crearea conturilor)
         */
        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateEmail() | validatePassword() && (validateEmail() && validatePassword())) {
                    final String email = mEmail.getEditText().getText().toString();
                    final String password = mPassword.getEditText().getText().toString();
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(CustomerLoginActivity.this, "Error: This email is already used", Toast.LENGTH_LONG).show();
                            } else {
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);
                                current_user_db.setValue(true);
                            }
                        }
                    });
                }
            }
        });

        /**
         * logarea in aplicatie a utilizatorilor
         */
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateEmail() | validatePassword() && (validateEmail() && validatePassword())) {
                    final String email = mEmail.getEditText().getText().toString();
                    final String password = mPassword.getEditText().getText().toString();
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(CustomerLoginActivity.this, "Sign in error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        /**
         * resetarea parolei. Redirectionarea catre o noua activitate unde se efectueaza trimiterea unui email pentru a se reseta parola
         */
        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerLoginActivity.this, ForgotPassword.class));
            }
        });
    }

    /**
     * Se determina tipul utilizatorului pentru, astfel incat soferii sa nu se poata loga in cadrul sectiunii destinate pasagerilor
     */
    public void getUserAccType() {
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference requestDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        requestDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Intent intent = new Intent(getApplication(), CustomerMapActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);/**/
                    finish();
                    startActivity(intent);
                    return;
                }else{
                    Toast.makeText(CustomerLoginActivity.this, "We dont find this account in customers. Maybe you want to login in Drivers?", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * metoda ce valideaza email-ul
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
     * validarea parolei
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
     * activarea serviciului Firebase de autentificare
     */
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    /**
     * Oprirea serviciului Firebase de autentificare
     */
    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
