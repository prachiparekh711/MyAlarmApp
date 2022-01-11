package clock.alarm.stopwatch.fragments.sound;

import android.content.Context;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import clock.alarm.stopwatch.data.SoundData;
import clock.alarm.stopwatch.fragments.BasePagerFragment;
import clock.alarm.stopwatch.interfaces.ContextFragmentInstantiator;
import clock.alarm.stopwatch.interfaces.SoundChooserListener;


public abstract class BaseSoundChooserFragment extends BasePagerFragment implements SoundChooserListener {

    private SoundChooserListener listener;

    public void setListener(SoundChooserListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSoundChosen(SoundData sound) {
        if (listener != null)
            listener.onSoundChosen(sound);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener = null;
    }

    abstract static class Instantiator extends ContextFragmentInstantiator {

        private final WeakReference<SoundChooserListener> listener;

        public Instantiator(Context context, SoundChooserListener listener) {
            super(context);
            this.listener = new WeakReference<>(listener);
        }

        @Nullable
        @Override
        public BasePagerFragment newInstance(int position) {
            SoundChooserListener listener = this.listener.get();
            if (listener != null)
                return newInstance(position, listener);
            else return null;
        }

        abstract BasePagerFragment newInstance(int position, SoundChooserListener listener);
    }

}
