package com.example.hovi_android;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hovi_android.databinding.ActivityMainBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    //todo 서버와 데이터 삭제 후 재로딩 로직

    // Used to load the 'hovi_android' library on application startup.
    static {
        System.loadLibrary("hovi_android");
    }

    private ActivityMainBinding binding;

    Retrofit retrofit;
    RetrofitAPI retrofitAPI;
    String save_user;
    TextView txt_action1, txt_action2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        //앱을 실행해서 처음에 유저 정보를 확인한다.
        // 유저가 새로운 유저라면 showDialog 호출 후 유저 정보를 서버로 보낸 뒤에 startApp(유저정보), 기존 유저라면 startApp(유저정보)을 호출한다.
        // startApp을 하면, 유저정보를 사용해서 getAction을 호출하여 서버에 저장된 액션들을 반환한다.


        Button setting_btn = binding.btnSetting;

        Button check_btn = binding.checkBtn;
        Button revise_btn = binding.reviseBtn;
        txt_action1 = binding.txtAction1;
        txt_action2 = binding.txtAction2;

        retrofit = new Retrofit.Builder()
                .baseUrl("http://15.164.218.200:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitAPI = retrofit.create(RetrofitAPI.class);

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        save_user = sharedPreferences.getString("userId", "");

        SharedPreferences actionSP = getSharedPreferences("action", MODE_PRIVATE);
        txt_action1.setText(actionSP.getString("action1","Water Please"));
        txt_action2.setText(actionSP.getString("action2","Hungry"));

        if (save_user == null || save_user == "") { //처음 접속한 유저

            String user_id = getuniqueid(); //생성
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userId", user_id);
            editor.commit();
            Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
            showDiaglog(user_id);

        } else { //기존 유저
            getAction(save_user);
        }

        //Click SETTING button
        setting_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        //Click CHECK button
        check_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EyeActivity.class);
                startActivity(intent);
            }
        });

        //Click REVISE button
        revise_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ReviseActivity.class);
                startActivity(intent);
            }
        });

    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        getAction(save_user);
//    }

    private void showDiaglog(String uid) {

        //Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Welcome").setMessage("If you want to change expressions, click REVISE button");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                startJoin(uid);
                getAction(uid);
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private String getuniqueid() {
        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        return android_id;
    }

    private void startJoin(String user_id) {
        retrofitAPI.checkData(user_id).enqueue(new Callback<Check>() {
            @Override
            public void onResponse(Call<Check> call, Response<Check> response) {
                if (response.isSuccessful()) {
                    Check check = response.body();
                    if (check.getdata() == true) {
                        Log.d("데이터있음", "성공");
                    } else {
                        Log.d("데이터없음", user_id);
                        setUserInfo(user_id);
                    }

                }
            }

            @Override
            public void onFailure(Call<Check> call, Throwable t) {
                Log.d("데이터", "실실패" + t);
            }
        });
    }

    private void setUserInfo(String user_id) {
        retrofitAPI.setUser(user_id).enqueue(new Callback<User>() {

            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                Log.e("post", String.valueOf(response));
                if (response.isSuccessful()) {
                    User user = response.body();
                    Integer status = user.getStatus();
                    Log.d("set사용자", user.toString());
                    Log.d("set사용자1", status.toString());
                }
                Log.d("set사용자 실패", response.body().toString());
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.d("set사용자", "리스폰실패");
                t.printStackTrace();
            }
        });
    }

    private void getAction(String user_id) {
        retrofitAPI.getActions(user_id).enqueue(new Callback<Action>() {
            @Override
            public void onResponse(Call<Action> call, Response<Action> response) {
                if (response.isSuccessful()) {
                    Action message = response.body();
                    if (message.getMes() != null) {
                        Log.d("액션", message.getMes());
                        List<ActionList> actions = message.getActions();
                        String action1 = actions.get(0).getActionBody();
                        String action2 = actions.get(1).getActionBody();
                        txt_action1.setText(action1);
                        txt_action2.setText(action2);
                        Log.d("액션들", action1);
                    } else {
                        Log.d("액션없음", message.toString());
                    }
                }
            }

            @Override
            public void onFailure(Call<Action> call, Throwable t) {
                Log.d("액션리스폰실패", t.toString());
            }
        });

        SharedPreferences actionSP = getSharedPreferences("action", MODE_PRIVATE);
        SharedPreferences.Editor editor = actionSP.edit();
        editor.putString("action1", (String) txt_action1.getText());
        editor.putString("action2", (String) txt_action2.getText());
        editor.commit();
    }
}