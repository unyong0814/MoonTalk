package com.moonstudio.voltex.moontalk.chat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.moonstudio.voltex.moontalk.R;
import com.moonstudio.voltex.moontalk.model.ChatModel;
import com.moonstudio.voltex.moontalk.model.NotificationModel;
import com.moonstudio.voltex.moontalk.model.UserModel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {
    private String destinationUid;
    private Button mButton;
    private EditText mEditText;

    private String uid;
    private String chatRoomUid;

    private RecyclerView mRecyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    int peopleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //채팅을 요구하는 아이디, 즉 단말기에 로그인된 UID

        destinationUid = getIntent().getStringExtra("destinationUid"); //채팅을 당하는 아이디
        mButton = (Button)findViewById(R.id.messageActivity_button);
        mEditText = findViewById(R.id.messageActivity_editText);
        mRecyclerView = (RecyclerView)findViewById(R.id.messageActivity_recyclerview);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //버튼 누르면 대화방 만들기
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid, true);
                chatModel.users.put(destinationUid, true);

                if(chatRoomUid == null) {


                    mButton.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference()
                            .child("chatrooms")
                            .push()
                            .setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom();
                        }
                    });

                } else {
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = mEditText.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP;

                    FirebaseDatabase.getInstance().getReference()
                            .child("chatrooms")
                            .child(chatRoomUid)
                            .child("comments")
                            .push()
                            .setValue(comment)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    sendGcm();
                                    mEditText.setText("");
                                }
                            });
                }

            }
        });
        checkChatRoom();


    }

    void sendGcm() {
        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = destinationUserModel.pushToken;
        notificationModel.notification.title = userName;
        notificationModel.notification.text = mEditText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = mEditText.getText().toString();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8"), gson.toJson(notificationModel));
        Request request = new Request.Builder()
                .header("Content-Type", "Application/json")
                .addHeader("Authorization", "key=AIzaSyAwQWx9D5_4ZzpNn2Yp7THZ7SEHm_Hg-lU")
                .url("https://gcm-http.googleapis.com/gcm/send")
                .post(requestBody)
                .build();


        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

    }


    void checkChatRoom() {
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for( DataSnapshot item : dataSnapshot.getChildren()) {
                            ChatModel chatModel = item.getValue(ChatModel.class);

                            if(chatModel.users.containsKey(destinationUid)) {
                                chatRoomUid = item.getKey(); //Child의 chatroom key값을 가져옴
                                mButton.setEnabled(true);
                                mRecyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                                mRecyclerView.setAdapter(new RecyclerViewAdapter());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    //리사이클러 뷰 어댑터

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<ChatModel.Comment> comments;

        public RecyclerViewAdapter() {
            //comment를 담아주는 코드

            comments = new ArrayList<>();


            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    destinationUserModel = dataSnapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("chatrooms")
                    .child(chatRoomUid)
                    .child("comments");

            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            comments.clear();
                            Map<String, Object> readUsersMap = new HashMap<>();

                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                String key = item.getKey();
                                ChatModel.Comment comment_origin = item.getValue(ChatModel.Comment.class);
                                ChatModel.Comment comment_modify = item.getValue(ChatModel.Comment.class);
                                comment_modify.readUsers.put(uid, true);

                                //읽었다는 태그 달기.
                                readUsersMap.put(key, comment_modify);
                                comments.add(comment_origin);


                            }

                            if(!comments.get(comments.size() - 1).readUsers.containsKey(uid)) {
                                //comments를 읽은 사람중 내가 포함되어있나? 없으면, 서버에 보고.
                                FirebaseDatabase.getInstance()
                                        .getReference()
                                        .child("chatrooms")
                                        .child(chatRoomUid)
                                        .child("comments")
                                        .updateChildren(readUsersMap)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                //메시지가 갱신
                                                notifyDataSetChanged();

                                                mRecyclerView.scrollToPosition(comments.size() - 1); //맨마지막 포지션
                                            }
                                        });

                            } else {
                                //comment를 읽은 사람 중 내가 포함되어있으면, 데이터 갱신.
                                notifyDataSetChanged();

                                mRecyclerView.scrollToPosition(comments.size() - 1); //맨마지막 포지션
                            }


                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder = ((MessageViewHolder)holder);

            //내가보낸 메시지
            if(comments.get(position).uid.equals(uid)) {
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubbble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
                setReadCounter(position, messageViewHolder.textView_readCounter_left);
            }
            //상대방이 보낸 메시지
            else {
                Glide.with(holder.itemView.getContext())
                        .load(destinationUserModel.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageview_profile);
                messageViewHolder.textView_name.setText(destinationUserModel.userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);
                setReadCounter(position, messageViewHolder.textView_readCounter_right);
            }
            long unixTime = (long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);
        }


        void setReadCounter(final int position, final TextView textView) {
            if (peopleCount == 0) {

                //총인원 몇명인지?
                FirebaseDatabase.getInstance().getReference()
                        .child("chatrooms")
                        .child(chatRoomUid)
                        .child("users")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();
                                //전체인원 - 메시지 읽은 인원.
                                peopleCount = users.size();
                                int count = peopleCount - comments.get(position).readUsers.size();
                                if (count > 0 ) {
                                    textView.setVisibility(View.VISIBLE);
                                    textView.setText(String.valueOf(count));
                                } else
                                {
                                    textView.setVisibility(View.INVISIBLE);
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            } else {

                int count = peopleCount - comments.get(position).readUsers.size();
                if (count > 0 ) {
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(String.valueOf(count));
                } else
                {
                    textView.setVisibility(View.INVISIBLE);
                }
            }

        }


        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textView_name;
            public ImageView imageview_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public MessageViewHolder(View view) {
                super(view);
                textView_message = (TextView)view.findViewById(R.id.messageItem_textView_message);
                textView_name = (TextView)view.findViewById(R.id.messageItem_textview_name);
                imageview_profile = (ImageView)view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = (LinearLayout) view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayout_main = (LinearLayout)view.findViewById(R.id.messageItem_linearlayout_main);
                textView_timestamp = (TextView)view.findViewById(R.id.messageItem_textView_timestamp);
                textView_readCounter_left = (TextView) view.findViewById(R.id.messageItem_textView_readCounter_left);
                textView_readCounter_right = (TextView) view.findViewById(R.id.messageItem_textView_readCounter_right);
            }
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        databaseReference.removeEventListener(valueEventListener); //back키 누를 떄 와칭 꺼짐.
        finish();
        overridePendingTransition(R.anim.fromleft, R.anim.toright);
    }
}
