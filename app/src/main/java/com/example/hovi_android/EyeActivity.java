package com.example.hovi_android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//
//import com.example.hovi_android.databinding.ActivityMainBinding;

import com.example.hovi_android.ui.CameraSourcePreview;
import com.example.hovi_android.ui.FaceTracker;
import com.example.hovi_android.ui.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EyeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
//    private TextView input_text;

    //비전
    private static final String TAG = "GooglyEyes";

    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private boolean mIsFrontFacing = true;

//    private ProgressBar progressBar;
//    private Timer timeCall;
//    private int nCnt;

    RetrofitAPI retrofitAPI;
    String action1, action2;
    TextView txt_action1, txt_action2;

    public static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye);
        mContext = this;

        //todo 눈 감기 시 타이머 작동
//        progressBar = findViewById(R.id.progressBar);
//        progressBar.setIndeterminate(false);
//
//        nCnt = 0;
//
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                work();
//                progressBar.setProgress(nCnt);
//            }
//        };
//
//        timeCall = new Timer();
//        timeCall.schedule(timerTask,0,1000);


        tts = new TextToSpeech(this, this);

        txt_action1 = findViewById(R.id.action1);
        txt_action2 = findViewById(R.id.action2);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://15.164.218.200:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitAPI = retrofit.create(RetrofitAPI.class);

        Intent intent = getIntent();
//        action1 = intent.getStringExtra("action1");
//        action2 = intent.getStringExtra("action2");
        SharedPreferences actionSP = getSharedPreferences("action", MODE_PRIVATE);
        txt_action1.setText(actionSP.getString("action1",""));
        txt_action2.setText(actionSP.getString("action2",""));

        if(action1 != null && action2 != null && action1 != "" && action2 != "") {
            txt_action1.setText(action1);
            txt_action2.setText(action2);
            Log.d("인덴트업데이드", action1 + "&" + action2);
        }else{
            SharedPreferences sharedPreferences= getSharedPreferences("user", MODE_PRIVATE);
            String user_id = sharedPreferences.getString("userId","");
            getAction(user_id);
            Log.d("..업데이드", action1 + "&" + action2);
        }


        //비전
        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.faceOverlay);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(this, MainActivity.class); //지금 액티비티에서 다른 액티비티로 이동하는 인텐트 설정
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //인텐트 플래그 설정 -> 더 알아보기
        startActivity(intent);  //인텐트 이동
        finish();   //현재 액티비티 종료
    }

//    private void work(){
//        Log.d("타이머", nCnt+"work");
//        if(nCnt >= 5){
//            timeCall.cancel();
//        }
//        nCnt++;
//    }

    Handler handler = new Handler(Looper.getMainLooper());

    public void dorightAction(){
        Log.e("오른쪽눈","액션");
        rspeakOut();

        handler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                Toast.makeText(EyeActivity.this, txt_action1.getText(), Toast.LENGTH_SHORT).show();
            }
        }, 0);
    }

    public void doleftAction(){
        Log.e("왼른쪽눈","액션");
        lspeakOut();

        handler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                Toast.makeText(EyeActivity.this, txt_action2.getText(), Toast.LENGTH_SHORT).show();
            }
        }, 0);
    }


    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void rspeakOut(){
        CharSequence text = txt_action1.getText();
        tts.setPitch((float)0.6); // 음성 톤 높이 지정
        tts.setSpeechRate((float)0.8); // 음성 속도 지정

        // 첫 번째 매개변수: 음성 출력을 할 텍스트
        // 두 번째 매개변수: 1. TextToSpeech.QUEUE_FLUSH - 진행중인 음성 출력을 끊고 이번 TTS의 음성 출력
        //                 2. TextToSpeech.QUEUE_ADD - 진행중인 음성 출력이 끝난 후에 이번 TTS의 음성 출력
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void lspeakOut(){
        CharSequence text = txt_action2.getText();
        tts.setPitch((float)0.6); // 음성 톤 높이 지정
        tts.setSpeechRate((float)0.8); // 음성 속도 지정

        // 첫 번째 매개변수: 음성 출력을 할 텍스트
        // 두 번째 매개변수: 1. TextToSpeech.QUEUE_FLUSH - 진행중인 음성 출력을 끊고 이번 TTS의 음성 출력
        //                 2. TextToSpeech.QUEUE_ADD - 진행중인 음성 출력이 끝난 후에 이번 TTS의 음성 출력
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1");
    }

    @Override
    public void onDestroy() {
        if(tts!=null){ // 사용한 TTS객체 제거
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
        //비전
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onInit(int status) { // OnInitListener를 통해서 TTS 초기화
        if(status == TextToSpeech.SUCCESS){
            int result = tts.setLanguage(Locale.KOREA); // TTS언어 한국어로 설정

            if(result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA){
                Log.e("TTS", "This Language is not supported");
            }else{
                txt_action1.setEnabled(true);
                txt_action2.setEnabled(true);
            }
        }else{
            Log.e("TTS", "Initialization Failed!");
        }
    }

    private void getAction(String user_id){

        retrofitAPI.getActions(user_id).enqueue(new Callback<Action>(){

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
                        Log.d("액션들",action1);
                    } else{
                        Log.d("액션없음", message.toString());
                    }

                }
            }

            @Override
            public void onFailure(Call<Action> call, Throwable t) {
                Log.d("액션리스폰실패", t.toString());
            }
        });
    }

    //비전

    /**
     * Handles the requesting of the camera permission.  This includes showing a "Snack bar" message
     * of why the permission is needed then sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, "Access to the camera is needed for detection",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("확인.", listener)
                .show(); // OK를 클릭해야 사라지는 스낵바
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage("This application cannot run because it does not have the camera permission.  The application will now exit.")
                .setPositiveButton("OK.", listener)
                .show();
    }

    /**
     * Saves the camera facing mode, so that it can be restored after the device is rotated.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("IsFrontFacing", mIsFrontFacing);
    }

    /**
     * Toggles between front-facing and rear-facing modes.
     */
    private View.OnClickListener mFlipButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            mIsFrontFacing = !mIsFrontFacing;

            if (mCameraSource != null) {
                mCameraSource.release();
                mCameraSource = null;
            }

            createCameraSource();
            startCameraSource();
        }
    };

    //==============================================================================================
    // Detector
    //==============================================================================================

    /**
     * Creates the face detector and associated processing pipeline to support either front facing
     * mode or rear facing mode.  Checks if the detector is ready to use, and displays a low storage
     * warning if it was not possible to download the face library.
     */
    @NonNull
    private FaceDetector createFaceDetector(Context context) {

        FaceDetector detector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(mIsFrontFacing)
                .setMinFaceSize(mIsFrontFacing ? 0.35f : 0.15f)
                .build();

        Detector.Processor<Face> processor;
        if (mIsFrontFacing) {
            // For front facing mode

            Tracker<Face> tracker = new FaceTracker(mGraphicOverlay);
//            Tracker<Face> tracker1 = new doAction()

            processor = new LargestFaceFocusingProcessor.Builder(detector, tracker).build();
        } else {
            // For rear facing mode, a factory is used to create per-face tracker instances.

            MultiProcessor.Factory<Face> factory = new MultiProcessor.Factory<Face>() {
                @Override
                public Tracker<Face> create(Face face) {
                    return new FaceTracker(mGraphicOverlay);
                }
            };
            processor = new MultiProcessor.Builder<>(factory).build();
        }

        detector.setProcessor(processor);

        if (!detector.isOperational()) {

            // isOperational() can be used to check if the required native library is currently available.  .
            Log.w(TAG, "Face detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "Face detector dependencies cannot be downloaded due to low device storage", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Face detector dependencies cannot be downloaded due to low device storage");
            }
        }
        return detector;
    }


    /**
     * Creates the face detector and the camera.
     */
    private void createCameraSource() {
        Context context = getApplicationContext();
        FaceDetector detector = createFaceDetector(context);

        int facing = CameraSource.CAMERA_FACING_FRONT;

        if (!mIsFrontFacing) {
            facing = CameraSource.CAMERA_FACING_BACK;
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setFacing(facing)
                .setRequestedPreviewSize(320, 240)
                .setRequestedFps(10.0f)
                .setAutoFocusEnabled(true)
                .build();
    }


    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());

        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }


        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


}