package com.moonstudio.voltex.moontalk.model;

/**
 * Created by kim-un-yong on 2018. 4. 3..
 */

public class NotificationModel {
    public String to;
    public Notification notification = new Notification();
    public Data data = new Data();
    public static class Notification {
        public String title;
        public String text;
    }

    public static class Data {
        public String title;
        public String text;
    }
}
