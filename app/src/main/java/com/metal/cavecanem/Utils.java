package com.metal.cavecanem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class Utils {

    public static final String INTENT_ACTION = "com.metal.cavecanem.intent";
    public static final int ACTION_GENERATE = 0;
    public static final int ACTION_SEND = 1;

    public static String searchText(ViewGroup parent){
        StringBuilder builder = new StringBuilder();
        recursiveTextView(parent, builder);
        return builder.toString();
    }
    private static void recursiveTextView(ViewGroup parent, StringBuilder builder) {
        String result = "";
        for (int i = 0; i < parent.getChildCount(); i++) {
            final View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                recursiveTextView((ViewGroup) child, builder);
                // DO SOMETHING WITH VIEWGROUP, AFTER CHILDREN HAS BEEN LOOPED
            } else {
                if (child != null && child instanceof TextView) {
                    String text = ((TextView) child).getText().toString().replaceAll("[^A-Za-z0-9 ]", "");
                    if (!text.isEmpty()){
                        if (i>0){
                            result = result+", ";
                        }
                        result = result+text;
                    }
                }
            }
        }
        if (!result.isEmpty()){
            builder.append(result);
            builder.append(". ");

        }
    }

    public static void setupChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Suggestions";
            String description = "Helps you respond to messages from the notifications area.";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("CANEM_SUGGESTION", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    public static Notification notifyReceivedNotification(Context context, StatusBarNotification sbn) {

        setupChannel(context);

        Intent intentAction = new Intent("com.metal.cavecanem.LISTENER");
        intentAction.putExtra("id", sbn.getId());
        intentAction.putExtra(INTENT_ACTION, ACTION_GENERATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sbn.getId(), intentAction, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String respondTo = sbn.getNotification().extras.getString(NotificationCompat.EXTRA_PEOPLE_LIST);
        if (respondTo == null) {
            respondTo = "message";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CANEM_SUGGESTION")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Respond to " + respondTo)
                .setContentText("Generating a response to your message: " + sbn.getNotification().tickerText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Generating a response to your message: " + sbn.getNotification().tickerText))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .addAction(R.drawable.ic_launcher_background, "Generate", pendingIntent)
                .addAction(R.drawable.ic_launcher_background, "Add context", pendingIntent);
        return builder.build();


    }

    public static Notification pendingSendNotification(Context context, StatusBarNotification sbn, String result){
        setupChannel(context);

        Intent intentSend = new Intent("com.metal.cavecanem.LISTENER");
        intentSend.putExtra("id", sbn.getId());
        intentSend.putExtra(INTENT_ACTION, ACTION_SEND);
        intentSend.putExtra("message", result);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sbn.getId(), intentSend, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String respondTo = sbn.getNotification().extras.getString(NotificationCompat.EXTRA_PEOPLE_LIST);
        if (respondTo == null) {
            respondTo = "message";
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CANEM_SUGGESTION")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Respond to " + respondTo)
                .setContentText(result)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(result))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .addAction(R.drawable.ic_launcher_background, "Send", pendingIntent)
                .addAction(R.drawable.ic_launcher_background, "Add context", pendingIntent);
        return builder.build();
    }
}
