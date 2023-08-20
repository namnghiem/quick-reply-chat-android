package com.metal.cavecanem;

import static com.robj.notificationhelperlibrary.utils.NotificationUtils.getQuickReplyAction;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.robj.notificationhelperlibrary.utils.NotificationUtils;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import models.Action;
import services.BaseNotificationListener;

public class ChatService extends BaseNotificationListener {

    private ChatServiceBroadcastReceiver chatServiceBroadcastReceiver;
    private int NOTIFICATION_TAG = 23915;
    NotificationManagerCompat notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        chatServiceBroadcastReceiver = new ChatServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.metal.cavecanem.LISTENER");
        registerReceiver(chatServiceBroadcastReceiver, intentFilter);
        notificationManager = NotificationManagerCompat.from(this);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        StatusBarNotification[] activeNotifications = this.getActiveNotifications();

        if (activeNotifications != null && activeNotifications.length > 0) {
            for (int i = 0; i < activeNotifications.length; i++) {
                Intent intent = new Intent("com.metal.cavecanem.LISTENER");
                intent.putExtra("action", "removed");
                intent.putExtra("id", sbn.getId());
                sendBroadcast(intent);
                break;

            }

        }
    }

    @Override
    protected boolean shouldAppBeAnnounced(StatusBarNotification statusBarNotification) {
        return true;
    }

    @Override
    protected void onNotificationPosted(StatusBarNotification sbn, String s) {
        Action action = getQuickReplyAction(sbn.getNotification(), sbn.getPackageName());

        if (action != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_TAG + sbn.getId(), Utils.notifyReceivedNotification(this, sbn));
            }
            //TODO: toast or request notification
        }

    }

    private class ChatServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            StatusBarNotification currentNotification = null;

            if (intent.getIntExtra("id", -1) != -1) {
                StatusBarNotification[] activeNotifications = getActiveNotifications();
                for (int i = 0; i < activeNotifications.length; i++) {
                    currentNotification = activeNotifications[i];
                    currentNotification.getNotification().actions = new Notification.Action[0];
                    break;
                }

                if(currentNotification!=null) {
                    if(intent.getIntExtra(Utils.INTENT_ACTION, -1)==Utils.ACTION_GENERATE){

                        String array = currentNotification.getNotification().extras.getString("android.text");
                        if (array != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                RemoteViews views = Notification.Builder.recoverBuilder(ChatService.this, currentNotification.getNotification()).createBigContentView();
                                FrameLayout layout = new FrameLayout(ChatService.this);
                                View view = views.apply(ChatService.this, layout);
                                String query = Utils.searchText((ViewGroup) view);

                                ArrayList<ChatMessage> messages = new ArrayList<>();
                                messages.add(new ChatMessage("system", "Briefly continue the following conversation."));
                                messages.add(new ChatMessage("user", query));

                                OpenAiService service = new OpenAiService("");
                                ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                                        .model("gpt-3.5-turbo")
                                        .messages(messages)
                                        .build();

                                StatusBarNotification finalCurrentNotification = currentNotification;

                                Observable.fromCallable(new Callable<String>() {
                                            @Override
                                            public String call() throws Exception {
                                                String text = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage().getContent();
                                                return text;
                                            }
                                        })
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Observer<String>() {
                                            @Override
                                            public void onSubscribe(@NonNull Disposable d) {
                                            }

                                            @Override
                                            public void onNext(@NonNull String s) {
                                                if (ActivityCompat.checkSelfPermission(ChatService.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                                    notificationManager.notify(NOTIFICATION_TAG + finalCurrentNotification.getId(), Utils.pendingSendNotification(ChatService.this, finalCurrentNotification, s));
                                                    return;
                                                }
                                            }

                                            @Override
                                            public void onError(@NonNull Throwable e) {
                                                e.printStackTrace();
                                            }

                                            @Override
                                            public void onComplete() {

                                            }
                                        });
                            }
                        }
                    }else if(intent.getIntExtra(Utils.INTENT_ACTION, -1)==Utils.ACTION_SEND){
                        Action reply = NotificationUtils.getQuickReplyAction(currentNotification.getNotification(), currentNotification.getPackageName());
                        String message = intent.getStringExtra("message");
                        if(message!=null){
                            try {
                                reply.sendReply(ChatService.this, message);
                            } catch (PendingIntent.CanceledException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }

                }
            }

        }
    }
}