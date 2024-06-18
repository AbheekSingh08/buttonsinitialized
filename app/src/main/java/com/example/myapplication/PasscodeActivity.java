package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PasscodeActivity extends AppCompatActivity { //irrelevant now

    private static final String CORRECT_PASSCODE = "123456"; // Replace with your desired passcode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passcode);

        EditText passcodeEditText = findViewById(R.id.passcode_edit_text);
        Button submitButton = findViewById(R.id.submit_button);

        submitButton.setOnClickListener(v -> {
            String enteredPasscode = passcodeEditText.getText().toString();
            if (CORRECT_PASSCODE.equals(enteredPasscode)) {
                Intent intent = new Intent(PasscodeActivity.this, SavedMediaActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(PasscodeActivity.this, "Incorrect passcode", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
