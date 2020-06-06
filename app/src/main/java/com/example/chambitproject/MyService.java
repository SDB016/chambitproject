package com.example.chambitproject;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MyService extends Service {
    public static final String MESSAGE_KEY = "jisub1";
    public static final String MESSAGE_LIST = "jisub2";
    public static final String NOTIFICATION_CHANNEL_ID = "10001";

    Handler handler;
    LocationManager lm;
    Location location_temp_gps;
    NotificationManager notificationManager;
    TextToSpeech tts;
    StationInfo stationInfo;
    StationItem stationItem;
    ArrayList<String> station_list;

    int count = 0;
    double latitude;
    double longitude;
    private IBinder mBinder;
    boolean thread_run = false;

    final LocationListener mLL = new LocationListener() {
        public void onLocationChanged(Location location) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new LocalBinder();
        handler = new Handler();
        stationItem = new StationItem();
        stationInfo = new StationInfo();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                if (status == 0) {
                    tts.setLanguage(Locale.KOREAN);
                    tts.setPitch(1.0f);
                    tts.setSpeechRate(1.0f);
                }
            }
        });
    }

    public void get_location() {
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            location_temp_gps = location;
            stationItem = stationInfo.get_next_station(station_list.get(count),latitude, longitude);
        }
        else
            Log.e("lm", "location is null");
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, mLL);
    }

    public StationItem get_station() {
        return stationItem;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        station_list = intent.getStringArrayListExtra(MESSAGE_LIST);

        boolean message = intent.getExtras().getBoolean(MESSAGE_KEY);
        if(message){
            Intent mIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("참빛설계")
                    .setContentIntent(pendingIntent)
                    .setContentText("도착지까지 안내해 드릴게요")
                    .setOngoing(true);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"채널이름", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("오레오 이상");
                assert notificationManager != null;
                notificationManager.createNotificationChannel(channel);
            }
            notificationManager.notify(1234, builder.build());
            thread_run=true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(thread_run){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                get_location();

                                /*if(count==3){
                                    String text = "짐을 챙겨 주세요";
                                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text);
                                }*/
                            }
                        });
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        else {
            thread_run=false;
            notificationManager.cancel(1234);
        }
        return START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public MyService getService() {
            return MyService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread_run = false;
        lm.removeUpdates(mLL);
    }
}