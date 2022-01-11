package clock.alarm.stopwatch.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import clock.alarm.stopwatch.AlarmApp;


public abstract class BaseFragment extends Fragment implements AlarmApp.AlarmioListener {

    private AlarmApp alarmio;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alarmio = (AlarmApp) getContext().getApplicationContext();
        alarmio.addListener(this);
    }

    @Override
    public void onDestroy() {
        alarmio.removeListener(this);
        alarmio = null;
        super.onDestroy();
    }

    @Nullable
    protected AlarmApp getAlarmio() {
        return alarmio;
    }

    public void notifyDataSetChanged() {
        // Update the info displayed in the fragment.
    }

    @Override
    public void onAlarmsChanged() {
        // Update any alarm-dependent data.
    }

    @Override
    public void onTimersChanged() {
        // Update any timer-dependent data.
    }
}
