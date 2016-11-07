package com.example.owner.supportapp.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.owner.supportapp.DBHelper;
import com.example.owner.supportapp.R;
import com.example.owner.supportapp.consts.CommonConstants;
import com.example.owner.supportapp.main.MainActivity;
import com.google.android.gms.gcm.GcmListenerService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Owner on 7/18/2016.
 */
public class NotificationsListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        String title = data.getString("contentTitle");
        String port = data.getString("node");
        String status = data.getString("status");

        String node = "Node 1";

        if(port.equals("5555")){
            node = "Node 1";
        }else if(port.equals("5556")) {
            node = "Node 2";
        }else if(port.equals("5557")) {
            node = "Node 3";
        }

        NotificationManager notificationManager= (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        Notification n  = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true).build();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        n.flags |= Notification.FLAG_AUTO_CANCEL;
        n.contentIntent = intent;
        notificationManager.notify(0, n);

        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String formattedDate = df.format(c.getTime());

        DBHelper dbHelper = new DBHelper(getApplicationContext());
        dbHelper.insertContact(message, formattedDate, node, status);
    }
}
