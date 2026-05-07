package com.krishna.crimedetection.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.krishna.crimedetection.databinding.ActivityRegisterBinding;
import com.krishna.crimedetection.utils.PreferenceUtils;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etUsername.getText().toString();
            String email = binding.etEmail.getText().toString();
            String familyNum = binding.etFamilyNum.getText().toString();
            String password = binding.etPassword.getText().toString();

            if (name.isEmpty() || email.isEmpty() || familyNum.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            PreferenceUtils.saveUser(this, name, email, familyNum);
            PreferenceUtils.setLoggedIn(this, true);
            startActivity(new Intent(this, com.krishna.crimedetection.activities.MainActivity.class));
            finishAffinity();
        });

        binding.tvLogin.setOnClickListener(v -> finish());
    }
}
