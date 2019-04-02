package com.aooen.pleaselookaftersleep;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by User on 2016-07-05.
 */
public class LockService extends Service {
    private SharedPreferences mPref;
    private SharedPreferences.Editor mPrefEdit;
    private AudioManager audioManager;
    private ActivityManager activityManager;
    private int audioModeBefore = -1;
    private Intent intent;
    private boolean threadOn = false;
    private Thread lockThread;
    public boolean inSleep = false;

    @Override
    public void onCreate () {
        super.onCreate();

        threadOn = true;
        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefEdit = mPref.edit();
    }

    @Override
    public void onDestroy () {
        super.onDestroy();

        threadOn = false;
        if (audioModeBefore != -1) {
            audioManager.setRingerMode(audioModeBefore);
            audioModeBefore = -1;
        }
    }

    @Override
    public int onStartCommand (Intent _intent, int flags, int startId) {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        intent = new Intent(this, LockScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        lockThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (threadOn) {
                    //Log.d("수면을 부탁해", "서비스 실행중");

                    if (SettingsActivity.inSleep(mPref)) {
                        inSleep = true;

                        //현재 앱이 최상위가 아닐경우 intent 실행
                        if (!getApplicationContext().getPackageName().equals(activityManager.getRunningTasks(1).get(0).topActivity.getPackageName()))
                            startActivity(intent);

                        if (audioModeBefore == -1) { //수면시 무음모드
                            audioModeBefore = audioManager.getRingerMode();
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        }
                    } else {
                        if (inSleep) { //기상 후 처음 할 일
                            LockScreen.lockScreen.finish(); //잠금화면 끄기

                            if (mPref.getBoolean("useCorrent", false)) {
                                int day = mPref.getInt("afterDay", 0) + 1;

                                if (day >= 11) {
                                    mPrefEdit.putString("sleepTime", mPref.getString("afterTime", "23:00"));
                                    mPrefEdit.putBoolean("useCorrent", false);
                                    mPrefEdit.commit();
                                } else {
                                    double smooth = SettingsActivity.smoothTime(day);
                                    Date afterDate = SettingsActivity.val2date(mPref.getString("afterTime", "23:00"));
                                    Date beforeDate = SettingsActivity.val2date(mPref.getString("beforeTime", "00:00"));

                                    long diff = beforeDate.getTime() - afterDate.getTime() + 1800000;
                                    long next = SettingsActivity.val2date(mPref.getString("sleepTime", "00:00")).getTime();
                                    if (smooth >= 0) //smooth가 양수일때는 8~10일을 나타냄.
                                        next += smooth * 60000;
                                    else
                                        next += diff * smooth;

                                    Date nextDate = new Date(next);
                                    SimpleDateFormat nextFormat = new SimpleDateFormat("HH:mm");
                                    mPrefEdit.putString("sleepTime", nextFormat.format(nextDate));
                                    mPrefEdit.putInt("afterDay", day);
                                    mPrefEdit.commit();
                                }
                            }

                            inSleep = false;
                        }

                        if (audioModeBefore != -1) { // 기상시 이전상태로
                            audioManager.setRingerMode(audioModeBefore);
                            audioModeBefore = -1;
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        if (mPref.getBoolean("useLock", false)) {
            lockThread.start();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }
}