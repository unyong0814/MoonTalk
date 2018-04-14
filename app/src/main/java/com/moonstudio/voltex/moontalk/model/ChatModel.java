package com.moonstudio.voltex.moontalk.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by unyongkim on 2018. 3. 24..
 */

public class ChatModel {
    public Map<String,Boolean> users = new HashMap<>(); //채팅방의 유저들
    public Map<String, Comment> comments = new HashMap<>(); //채팅방의 내용


    public static class Comment { // 내부클래스(Inner Class)
        public String uid;
        public String message;
        public Object timestamp;
        public Map<String, Object> readUsers = new HashMap<>();
    }
}
