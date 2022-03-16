package com.example.hovi_android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.hovi_android.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'hovi_android' library on application startup.
    static {
        System.loadLibrary("hovi_android");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
//        TextView tv = binding.sampleText;
//        tv.setText(stringFromJNI());

        Button check_btn = binding.checkBtn;

        check_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EyeActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * A native method that is implemented by the 'hovi_android' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}