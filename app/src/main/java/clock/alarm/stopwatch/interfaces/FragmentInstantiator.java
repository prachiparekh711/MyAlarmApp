package clock.alarm.stopwatch.interfaces;

import androidx.annotation.Nullable;

import clock.alarm.stopwatch.fragments.BasePagerFragment;


public interface FragmentInstantiator {
    @Nullable
    BasePagerFragment newInstance(int position);

    @Nullable
    String getTitle(int position);
}
