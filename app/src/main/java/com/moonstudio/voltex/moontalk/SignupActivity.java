package com.moonstudio.voltex.moontalk;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.moonstudio.voltex.moontalk.model.UserModel;

public class SignupActivity extends AppCompatActivity {
    public static final String TAG = "KUY";
    private static final int PICK_FROM_ALBUM = 10;
    private EditText email;
    private EditText name;
    private EditText password;
    private Button signup;
    private String splash_background;
    private ImageView profile;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);


        //status bar
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        splash_background = mFirebaseRemoteConfig.getString(getString(R.string.rc_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //최신 버전만 적용됨 //status bar
            getWindow().setStatusBarColor(Color.parseColor(splash_background));
        }

        //이미지뷰

        profile = (ImageView)findViewById(R.id.signupActivity_imageview_profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK); //사진 가져오기
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                Log.i(TAG, "사진가져오기호출");
                startActivityForResult(intent, PICK_FROM_ALBUM);
            }
        });

//        에디트텍스트
        email = (EditText)findViewById(R.id.signupActivity_edittext_email);
        name = (EditText)findViewById(R.id.signupActivity_edittext_name);
        password = (EditText)findViewById(R.id.signupActivity_edittext_password);

//        버튼
        signup = (Button)findViewById(R.id.signupActivity_button_signup);
        signup.setBackgroundColor(Color.parseColor(splash_background));


        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(email.getText().toString() == null || password.getText().toString() == null) {
                    Toast.makeText(SignupActivity.this, "이메일 또는 패스워드를 입력하세요.", Toast.LENGTH_LONG).show();

                    return;
                }

                if(imageUri != null) {
                    //이미지 선택 하였을 때 처리...
                    Log.i(TAG, "Firebase에 Authentificationd에 이메일 / 패스워드 등록됨");


                    FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                            .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    final String uid = task.getResult().getUser().getUid();


                                    //회원가입할 때 자신의 이름을 넣는 코드.
                                    UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest
                                            .Builder()
                                            .setDisplayName(name.getText().toString())
                                            .build();

                                    task.getResult().getUser().updateProfile(userProfileChangeRequest);


                                    //사진올리고,
                                    FirebaseStorage.getInstance().getReference().child("userImages").child(uid).putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            //파일 저장된 경로를 다시 보내줌
                                            Log.i(TAG, "파일 저장된 경로를 다시 보내줌");

                                            String imageUrl = task.getResult().getDownloadUrl().toString();

                                            UserModel userModel = new UserModel();
                                            userModel.userName = name.getText().toString();
                                            userModel.profileImageUrl = imageUrl;
                                            userModel.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                            FirebaseDatabase.getInstance()
                                                    .getReference()
                                                    .child("users")
                                                    .child(uid)
                                                    .setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            SignupActivity.this.finish();
                                                        }
                                            });


                                        }
                                    });



                                }
                            });
                } else {
                    Toast.makeText(SignupActivity.this, "이미지를 선택하지 않으셨습니다. 이미지를 선택하세요.", Toast.LENGTH_LONG).show();
                    return;
                }

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "requestCode : " + requestCode + ", resultCode :" + resultCode);
        if(requestCode == PICK_FROM_ALBUM && resultCode == RESULT_OK) {
            Log.i(TAG, "onActivityResult호출");

            profile.setImageURI(data.getData()); //가운데 뷰를 바꿈
            imageUri = data.getData(); //이미지 경로 원본

        }
    }
}
