package com.aooen.pleaselookaftersleep;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by User on 2016-06-26.
 */
public class LockScreen extends AppCompatActivity {
    private SharedPreferences mPref;
    static Activity lockScreen;
    private Timer timer;
    private Handler handler;
    private TextView nowTime;
    private SimpleDateFormat formetTime;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_screen);

        lockScreen = this;
        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (SettingsActivity.val2sec(mPref.getString("getupTime", "06:00")) < SettingsActivity.val2sec(mPref.getString("sleepTime", "00:00")))
            LockScreen.lockScreen.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        /* 배경화면 불러오기 */
        ImageView imageview = (ImageView) findViewById(R.id.imageView);
        imageview.setImageDrawable(WallpaperManager.getInstance(this).getDrawable());
        imageview.setMaxHeight(100);
        imageview.setMaxWidth(120);

        /* 시계 */
        ((TextView) findViewById(R.id.txtGetUpTime)).setText(mPref.getString("getupTime", "06:00"));
        nowTime = (TextView) findViewById(R.id.txtNowTime);
        formetTime = new SimpleDateFormat("HH:mm");
        handler = new Handler() {
            public void handleMessage(Message msg) {
                nowTime.setText(formetTime.format(new Date(System.currentTimeMillis())));
                handler.sendEmptyMessageDelayed(0, 1000);
            }
        };
        handler.sendEmptyMessage(0);


        findViewById(R.id.btnEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LockScreen.this, SettingsActivity.class));
            }
        });
    }
}