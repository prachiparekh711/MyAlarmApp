package clock.alarm.stopwatch.activities;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;
import com.daily.motivational.quotes2.dataClass.SharedPreference;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import clock.alarm.stopwatch.AlarmApp;
import clock.alarm.stopwatch.R;
import clock.alarm.stopwatch.data.AlarmData;
import clock.alarm.stopwatch.data.PreferenceData;
import clock.alarm.stopwatch.data.SoundData;
import clock.alarm.stopwatch.data.TimerData;
import clock.alarm.stopwatch.services.SleepReminderService;
import clock.alarm.stopwatch.utils.FormatUtils;
import clock.alarm.stopwatch.utils.ImageUtils;
import io.reactivex.disposables.Disposable;
import me.jfenn.slideactionview.SlideActionListener;
import me.jfenn.slideactionview.SlideActionView;

public class AlarmActivity extends AestheticActivity implements SlideActionListener {

    public static final String EXTRA_ALARM = "james.alarmio.AlarmActivity.EXTRA_ALARM";
    public static final String EXTRA_TIMER = "james.alarmio.AlarmActivity.EXTRA_TIMER";

    private View overlay;
    private TextView date;
    private TextView time;
    private TextView label;
    private SlideActionView actionView;

    private AlarmApp alarmio;
    private Vibrator vibrator;
    private AudioManager audioManager;

    private boolean isAlarm;
    private long triggerMillis;
    private AlarmData alarm;
    private TimerData timer;
    private SoundData sound;
    private boolean isVibrate;

    private boolean isSlowWake;
    private long slowWakeMillis;

    private int currentVolume;
    private int minVolume;
    private int originalVolume;
    private int volumeRange;

    private Handler handler;
    private Runnable runnable;
    private boolean isWoken;
    private PowerManager.WakeLock wakeLock;

    private Disposable textColorPrimaryInverseSubscription;
    private Disposable isDarkSubscription;

    private boolean isDark;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        alarmio = (AlarmApp) getApplicationContext();

        overlay = findViewById(R.id.overlay);
        date = findViewById(R.id.date);
        label = findViewById(R.id.mLabel);
        time = findViewById(R.id.time);
        actionView = findViewById(R.id.slideView);

        // Lock orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        textColorPrimaryInverseSubscription = Aesthetic.Companion.get()
                .textColorPrimaryInverse()
                .subscribe(integer -> overlay.setBackgroundColor(integer));

        isDarkSubscription = Aesthetic.Companion.get()
                .isDark()
                .subscribe(aBoolean -> isDark = aBoolean);

        actionView.setLeftIcon(VectorDrawableCompat.create(getResources(), R.drawable.ic_snooze, getTheme()));
        actionView.setRightIcon(VectorDrawableCompat.create(getResources(), R.drawable.ic_close, getTheme()));
        actionView.setListener(this);

        isSlowWake = PreferenceData.SLOW_WAKE_UP.getValue(this);
        slowWakeMillis = PreferenceData.SLOW_WAKE_UP_TIME.getValue(this);

        isAlarm = getIntent().hasExtra(EXTRA_ALARM);
        if (isAlarm) {
            alarm = getIntent().getParcelableExtra(EXTRA_ALARM);
            isVibrate = alarm.isVibrate;
            if (alarm.hasSound())
                sound = alarm.getSound();
            if (alarm.name != null) {
                label.setText(" " + alarm.name + " ");
            }

        } else if (getIntent().hasExtra(EXTRA_TIMER)) {
            timer = getIntent().getParcelableExtra(EXTRA_TIMER);
            isVibrate = timer.isVibrate;
            if (timer.hasSound())
                sound = timer.getSound();
            label.setText("Timer");
        } else finish();

        date.setText(FormatUtils.format(new Date(), FormatUtils.FORMAT_DATE));
        time.setText(FormatUtils.format(new Date(), FormatUtils.getShortFormat(this)));
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = SharedPreference.Companion.getVolume(getBaseContext());
//        Log.e("sound_volume", String.valueOf(volume));
//        Log.e("sound_max", String.valueOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)));
        audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM, volume,
                0
        );
        if (sound != null && !sound.isSetVolumeSupported()) {
            // Use the backup method if it is not supported


            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            if (isSlowWake) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM);
                } else {
                    minVolume = 0;
                }
                volumeRange = originalVolume - minVolume;
                currentVolume = minVolume;

//                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, minVolume, 0);
            }
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        triggerMillis = System.currentTimeMillis();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - triggerMillis;
                String text = FormatUtils.formatMillis(elapsedMillis);
//                time.setText(String.format("-%s", text.substring(0, text.length() - 3)));

                if (isVibrate) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    else vibrator.vibrate(500);
                }

                int silentafter = SharedPreference.Companion.getSilenceMin(getBaseContext());
                Date date = new Date();
                date.setMinutes(silentafter);
                long timeInMillis = date.getTime();
                if (sound != null) {
//                    Log.e("elapsedMillis", String.valueOf(elapsedMillis));
                    if (alarm != null && sound.isPlaying(alarmio)) {
//                        Log.e("isPlaying", String.valueOf(sound.isPlaying(alarmio)));
                        if (elapsedMillis > timeInMillis) {
//                            Log.e("elapsedMillis >", String.valueOf(elapsedMillis));
                            sound.stop(alarmio);
                        }
                    }
                }

                if (sound != null && !sound.isPlaying(alarmio) && elapsedMillis < timeInMillis) {
//                    Log.e("elapsedMillis <", String.valueOf(elapsedMillis));
                    sound.play(alarmio);

                }

                if (alarm != null && isSlowWake) {
                    float slowWakeProgress = (float) elapsedMillis / slowWakeMillis;

                    WindowManager.LayoutParams params = getWindow().getAttributes();
//                    params.screenBrightness = Math.max(0.01f, Math.min(1f, slowWakeProgress));
                    getWindow().setAttributes(params);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAGS_CHANGED);

                    if (sound != null && sound.isSetVolumeSupported()) {
                        float newVolume = Math.min(1f, slowWakeProgress);

                        sound.setVolume(alarmio, newVolume);
                    } else if (currentVolume < originalVolume) {
                        // Backup volume setting behavior
                        int newVolume = minVolume + (int) Math.min(originalVolume, slowWakeProgress * volumeRange);
                        if (newVolume != currentVolume) {
//                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0);
                            currentVolume = newVolume;
                        }
                    }
                }

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        if (sound != null) {
            sound.play(alarmio);

        }

        SleepReminderService.refreshSleepTime(alarmio);

        if (PreferenceData.RINGING_BACKGROUND_IMAGE.getValue(this))
            ImageUtils.getBackgroundImage((ImageView) findViewById(R.id.background));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textColorPrimaryInverseSubscription != null && isDarkSubscription != null) {
            textColorPrimaryInverseSubscription.dispose();
            isDarkSubscription.dispose();
        }

        stopAnnoyingness();
    }

    private void stopAnnoyingness() {
        if (handler != null)
            handler.removeCallbacks(runnable);

        if (sound != null && sound.isPlaying(alarmio)) {
            sound.stop(alarmio);

            if (isSlowWake && !sound.isSetVolumeSupported()) {
//                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
        startActivity(new Intent(intent));
    }

    @Override
    public void onSlideLeft() {
        final int[] minutes = new int[]{2, 5, 10, 20, 30, 60};
        CharSequence[] names = new CharSequence[minutes.length + 1];
        for (int i = 0; i < minutes.length; i++) {
            names[i] = FormatUtils.formatUnit(AlarmActivity.this, minutes[i]);
        }

        names[minutes.length] = getString(R.string.title_snooze_custom);

        stopAnnoyingness();
        TimerData timer = alarmio.newTimer();
        timer.setDuration(TimeUnit.MINUTES.toMillis(SharedPreference.Companion.getSnoozeMin(getBaseContext())), alarmio);
        timer.setVibrate(AlarmActivity.this, isVibrate);
        timer.setSound(AlarmActivity.this, sound);
        timer.set(alarmio, ((AlarmManager) AlarmActivity.this.getSystemService(Context.ALARM_SERVICE)));
        alarmio.onTimerStarted();

        finish();
//        new AlertDialog.Builder(AlarmActivity.this, isDark ? R.style.Theme_AppCompat_Dialog_Alert : R.style.Theme_AppCompat_Light_Dialog_Alert)
//                .setItems(names, (dialog, which) -> {
//                    if (which < minutes.length) {
//                        TimerData timer = alarmio.newTimer();
//                        timer.setDuration(TimeUnit.MINUTES.toMillis(minutes[which]), alarmio);
//                        timer.setVibrate(AlarmActivity.this, isVibrate);
//                        timer.setSound(AlarmActivity.this, sound);
//                        timer.set(alarmio, ((AlarmManager) AlarmActivity.this.getSystemService(Context.ALARM_SERVICE)));
//                        alarmio.onTimerStarted();
//
//                        finish();
//                    } else {
//                        TimeChooserDialog timerDialog = new TimeChooserDialog(AlarmActivity.this);
//                        timerDialog.setListener((hours, minutes1, seconds) -> {
//                            TimerData timer = alarmio.newTimer();
//                            timer.setVibrate(AlarmActivity.this, isVibrate);
//                            timer.setSound(AlarmActivity.this, sound);
//                            timer.setDuration(TimeUnit.HOURS.toMillis(hours)
//                                            + TimeUnit.MINUTES.toMillis(minutes1)
//                                            + TimeUnit.SECONDS.toMillis(seconds),
//                                    alarmio);
//
//                            timer.set(alarmio, ((AlarmManager) getSystemService(Context.ALARM_SERVICE)));
//                            alarmio.onTimerStarted();
//                            finish();
//                        });
//                        timerDialog.show();
//                    }
//                })
//                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
//                .show();

        overlay.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    @Override
    public void onSlideRight() {
        overlay.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        finish();
    }
}
