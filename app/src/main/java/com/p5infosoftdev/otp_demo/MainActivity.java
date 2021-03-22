package com.p5infosoftdev.otp_demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    Button btnGenerateOTP, btnSignIn;
    EditText etPhoneNumber, etOTP;
    String phoneNumber, otp;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback;
    FirebaseAuth auth;
    private String verificationCode;
    FirebaseAuth.AuthStateListener mAuthListener;
    SmsBroadcastReceiver  smsBroadcastReceiver;
    private MyReceiver receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        StartFirebaseLogin();
        requestSMSPermission();

        receiver = new MyReceiver(new Handler()); // Create the receiver
        registerReceiver(receiver, new IntentFilter("some.action")); // Register receiver

        btnGenerateOTP.setOnClickListener(v -> {
            phoneNumber = "+91" + etPhoneNumber.getText().toString();

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,                     // Phone number to verify
                    60,                           // Timeout duration
                    TimeUnit.SECONDS,                // Unit of timeout
                    MainActivity.this,        // Activity (for callback binding)
                    mCallback);                      // OnVerificationStateChangedCallbacks
        });

        btnSignIn.setOnClickListener(v -> {
            otp = etOTP.getText().toString();
            if (otp.trim().equals("") || otp.trim().equals(" ")) {
            } else {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode, otp);
                SigninWithPhone(credential);
            }
        });


    }

    private void requestSMSPermission() {
        String permission = Manifest.permission.RECEIVE_SMS;

        int grant = ContextCompat.checkSelfPermission(this, permission);
        if (grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[1];
            permission_list[0] = permission;

            ActivityCompat.requestPermissions(this, permission_list, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if ((resultCode == RESULT_OK) && (data != null)) {
                //That gives all message to us.
                // We need to get the code from inside with regex
                String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                etOTP.setText(
                        String.format("%s - %s", "", message));
                smsBroadcastReceiver.setEditText(etOTP);
                getOtpFromMessage(message);
            }
        }
    }

    private void getOtpFromMessage(String message) {
        // This will match any 6 digit number in the message
        Pattern pattern = Pattern.compile("(|^)\\d{6}");
        Matcher matcher = pattern.matcher(message);
        smsBroadcastReceiver.setEditText(etOTP);
        if (matcher.find()) {
            etOTP.setText(matcher.group(0));
        }
    }

    private void SigninWithPhone(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent i = new Intent(MainActivity.this, SignInActivity.class);
                        startActivity(i);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Incorrect OTP", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void StartFirebaseLogin() {

        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // No user is signed in
        } else {
            Intent i = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(i);
            finish();
        }

        mCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                Toast.makeText(MainActivity.this, "verification completed", Toast.LENGTH_SHORT).show();
                final String code = phoneAuthCredential.getSmsCode();
                if (code != null) {
                    verifyCode(code);
                }
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                verificationCode = s;
                Toast.makeText(MainActivity.this, "Code sent", Toast.LENGTH_SHORT).show();
            }
        };
        //______________________

    }

    public class MyReceiver extends BroadcastReceiver {

        private final Handler handler; // Handler used to execute code on the UI thread

        public MyReceiver(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            handler.post(() -> {
                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

                for (SmsMessage sms : messages)
                {
                    String message = sms.getMessageBody();
                    String otp = message.split(" ")[0];
                    etOTP.setText(otp);
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    // below method is use to verify code from Firebase.
    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode, code);
        SigninWithPhone(credential);
    }

    private void findViews() {
        btnGenerateOTP = findViewById(R.id.btn_generate_otp);
        btnSignIn = findViewById(R.id.btn_sign_in);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        etOTP = findViewById(R.id.et_otp);
    }

}