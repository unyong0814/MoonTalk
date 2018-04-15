package com.moonstudio.voltex.moontalk.chat;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.moonstudio.voltex.moontalk.R;
import com.moonstudio.voltex.moontalk.model.ChatModel;
import com.moonstudio.voltex.moontalk.model.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupMessageActivity extends AppCompatActivity {
    Map<String,UserModel> users = new HashMap<>();
    String destinationRoom;
    String uid;
    EditText editText;

    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;

    private RecyclerView mRecyclerView;

    List<ChatModel.Comment> comments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);
        destinationRoom = getIntent().getStringExtra("destinationRoom");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        editText = (EditText) findViewById(R.id.groupMessageActivity_editText);


        FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                users = (Map<String, UserModel>) dataSnapshot.getValue();
                //System.out.println(users.size());
                init();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRecyclerView = (RecyclerView)findViewById(R.id.groupMessageActivity_recyclerview);
        mRecyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    void init() {
        Button button = (Button) findViewById(R.id.groupMessageActivity_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatModel.Comment comment = new ChatModel.Comment();
                comment.uid = uid;
                comment.message = editText.getText().toString();
                comment.timestamp = ServerValue.TIMESTAMP;
                FirebaseDatabase.getInstance().getReference()
                        .child("chatrooms")
                        .child(destinationRoom)
                        .child("comments")
                        .push().setValue(comment)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                editText.setText("");
                            }
                        });

            }
        });
    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public GroupMessageRecyclerViewAdapter() {
            getMessageList();
        }


        void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("chatrooms")
                    .child(destinationRoom)
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

                    if (!comments.get(comments.size() - 1).readUsers.containsKey(uid)) {
                        //comments를 읽은 사람중 내가 포함되어있나? 없으면, 서버에 보고.
                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("chatrooms")
                                .child(destinationRoom)
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
            //View그려줌.
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message,parent,false);
            return new GroupMessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class GroupMessageViewHolder extends RecyclerView.ViewHolder {
            public GroupMessageViewHolder(View view) {
                super(view);
            }
        }
    }
}
