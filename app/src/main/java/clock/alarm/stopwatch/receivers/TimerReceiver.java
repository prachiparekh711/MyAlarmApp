package clock.alarm.stopwatch.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import clock.alarm.stopwatch.AlarmApp;
import clock.alarm.stopwatch.activities.AlarmActivity;
import clock.alarm.stopwatch.data.TimerData;


public class TimerReceiver extends BroadcastReceiver {

    public static final String EXTRA_TIMER_ID = "james.alarmio.EXTRA_TIMER_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmApp alarmio = (AlarmApp) context.getApplicationContext();
        TimerData timer = alarmio.getTimers().get(intent.getIntExtra(EXTRA_TIMER_ID, 0));
        alarmio.removeTimer(timer);

        Intent ringer = new Intent(context, AlarmActivity.class);
        ringer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ringer.putExtra(AlarmActivity.EXTRA_TIMER, timer);
        context.startActivity(ringer);
    }
}
