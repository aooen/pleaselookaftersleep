package com.aooen.pleaselookaftersleep;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by User on 2016-07-02.
 */
public class SettingsActivity extends PreferenceActivity {
    private SharedPreferences mPref;
    private SharedPreferences.Editor mPrefEdit;
    private Preference prefSleepTime, prefGetupTime, prefAfterTime, prefBeforeTime, prefAfterDay;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings);
        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefEdit = mPref.edit();
        serviceIntent = new Intent(this, LockService.class);

        prefSleepTime = findPreference("sleepTime");
        prefSleepTime.setSummary(mPref.getString("sleepTime", "00:00"));
        prefGetupTime = findPreference("getupTime");
        prefGetupTime.setSummary(mPref.getString("getupTime", "06:00"));
        prefAfterTime = findPreference("afterTime");
        prefAfterTime.setSummary(mPref.getString("afterTime", "23:00"));
        prefAfterTime.setEnabled(!mPref.getBoolean("useCorrent", false));
        prefBeforeTime = findPreference("beforeTime");
        prefAfterDay = findPreference("afterDay");
        if (mPref.getBoolean("useCorrent", false)) {
            prefBeforeTime.setSummary(mPref.getString("beforeTime", "00:00"));
            prefAfterDay.setSummary(Integer.toString(mPref.getInt("afterDay", 0)) + " / 10 일");
        }

        findPreference("useLock").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                if (inSleep(mPref)) {
                    Toast.makeText(getApplicationContext(), "수면중에는 변경할 수 없습니다.\n기상 시간에 한해, 최소 3분 후로 조정 가능합니다.", Toast.LENGTH_LONG).show();
                    return false;
                }

                if ((boolean)value) {
                    startService(serviceIntent);
                } else {
                    stopService(serviceIntent);
                }
                return true;
            }
        });

        findPreference("useCorrent").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                prefAfterTime.setEnabled(!(boolean)value);
                if ((boolean)value) {
                    prefBeforeTime.setSummary(mPref.getString("sleepTime", "00:00"));
                    mPrefEdit.putString("beforeTime", mPref.getString("sleepTime", "00:00"));
                    prefAfterDay.setSummary("1 / 10 일");
                    mPrefEdit.putInt("afterDay", 1);
                    mPrefEdit.commit();
                } else {
                    prefBeforeTime.setSummary("");
                    prefAfterDay.setSummary("");
                }
                return true;
            }
        });

        /* 시간을 TimePicker를 이용해 입력받기 */
        prefSleepTime.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mPref.getBoolean("useCorrent", false)) {
                    Toast.makeText(getApplicationContext(), "취침 시간 교정중에는 변경할 수 없습니다.", Toast.LENGTH_LONG).show();
                    return false;
                }
                pickTime(preference, "sleepTime");
                return true;
            }
        });

        prefGetupTime.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                pickTime(preference, "getupTime");
                return true;
            }
        });

        prefAfterTime.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                pickTime(preference, "afterTime");
                return true;
            }
        });
    }

    private void pickTime (final Preference preferece, final String key) {
        Dialog dialog = new TimePickerDialog(SettingsActivity.this, new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hour, int minute) {
                String time = String.format("%02d", hour) + ":" + String.format("%02d", minute);

                if (inSleep(mPref) && (key != "getupTime" || val2sec(time) < 180000 || val2sec(time) >= val2sec(mPref.getString("sleepTime", "00:00")))) { //수면중일때 기상 시간에 한해 3분 미만이거나 수면 시간 이후일 경우 (현재 시간보다 뒤로 돌리는 경우)
                    Toast.makeText(getApplicationContext(), "수면중에는 변경할 수 없습니다.\n기상 시간에 한해, 최소 3분 후로 조정 가능합니다.", Toast.LENGTH_LONG).show();
                    return;
                }

                preferece.setSummary(time);
                mPrefEdit.putString(key, time);
                mPrefEdit.commit();
            }
        }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), true);
        dialog.show();
    }

    /* 시간을 Date 객체로 변환시켜주는 함수. */
    static public Date val2date (String time) {
        String[] arrTime = new String(time).split(":");
        int hour = Integer.parseInt(arrTime[0]);
        int minute = Integer.parseInt(arrTime[1]);

        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(new Date());
        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(new Date());
        timeCal.set(Calendar.HOUR_OF_DAY, hour);
        timeCal.set(Calendar.MINUTE, minute);
        timeCal.set(Calendar.SECOND, 0);
        timeCal.set(Calendar.MILLISECOND, 0);

        if (timeCal.before(nowCal)) //현재 시간보다 빠르면
            timeCal.add(Calendar.DATE, 1);

        return timeCal.getTime();
    }

    /* 시간을 몇 밀리초 남았는지 알려주는 함수. */
    static public long val2sec (String time) {
        return val2date(time).getTime() - new Date().getTime();
    }

    /* 수면 시간일 경우 true를 반환하는 함수 */
    static public boolean inSleep (SharedPreferences pref) {
        return SettingsActivity.val2sec(pref.getString("getupTime", "06:00")) < SettingsActivity.val2sec(pref.getString("sleepTime", "00:00"));
    }

    static public double smoothTime (int day) {
        switch (day) {
            case 2:
            case 6:
                return -0.1;
            case 3:
                return -0.2;
            case 4:
            case 5:
                return -0.3;
            case 8:
                return 15;
            case 9:
                return 10;
            case 10:
                return 5;
        }
        return 0;
    }
}