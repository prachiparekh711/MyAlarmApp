package clock.alarm.stopwatch.fragments


import android.app.AlarmManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.data.PreferenceData
import clock.alarm.stopwatch.data.SoundData
import clock.alarm.stopwatch.data.TimerData
import clock.alarm.stopwatch.databinding.FragmentTimerBinding
import clock.alarm.stopwatch.utils.FormatUtils
import java.util.concurrent.TimeUnit

class TimerFragment : BasePagerFragment() {
    var binding: FragmentTimerBinding? = null
    var mContext: Context? = null
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private val isRunning = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_timer, container, false)

        mContext = requireContext()

        binding?.mFirstView?.isVisible = true
        binding?.mSecondView?.isVisible = false
        binding?.mPlay?.setOnClickListener {
            getMillis()
            binding?.mFirstView?.isVisible = false
            binding?.mSecondView?.isVisible = true
            val timer: TimerData = alarmio!!.newTimer()
            timer.setDuration(getMillis(), alarmio)
            timer.setVibrate(mContext, false)
            timer.setSound(
                mContext,
                SoundData.fromString(PreferenceData.DEFAULT_TIMER_RINGTONE.getValue(context, ""))
            )
            timer.set(
                alarmio,
                mContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            )
            alarmio!!.onTimerStarted()
            startTimer(timer)
        }

        binding?.mPause?.setOnClickListener {
            onPauseTimer()
        }
        return binding?.root
    }

    fun onPauseTimer() {
        binding?.mFirstView?.isVisible = true
        binding?.mSecondView?.isVisible = false
        binding?.scrollHmsPicker?.hours = 0
        binding?.scrollHmsPicker?.minutes = 0
        binding?.scrollHmsPicker?.seconds = 0
    }

    fun startTimer(timer: TimerData) {
        binding?.timeProgrss?.setMaxProgress(timer.duration)

        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    if (timer.isSet) {
                        val remainingMillis: Long = timer.remainingMillis
                        binding?.timeProgrss?.setText(FormatUtils.formatMillis1(remainingMillis))
                        binding?.timeProgrss?.setProgress(timer.duration - remainingMillis)
                        handler!!.postDelayed(this, 10)
                    } else {
                        try {
                            onPauseTimer()
                            val manager = fragmentManager
                            manager?.popBackStack()
                        } catch (e: IllegalStateException) {
                            handler!!.postDelayed(this, 100)
                        }
                    }
                }
            }
        }
        handler!!.post(runnable as Runnable)
    }

    private fun getMillis(): Long {
        var millis: Long = 0
        val hours: Int? = binding?.scrollHmsPicker?.hours
        val minutes: Int? = binding?.scrollHmsPicker?.minutes
        val seconds: Int? = binding?.scrollHmsPicker?.seconds
        if (hours != null) {
            millis = millis.plus(TimeUnit.HOURS.toMillis(hours.toLong()))
        }
        if (minutes != null) {
            millis = millis.plus(TimeUnit.MINUTES.toMillis(minutes.toLong()))
        }
        if (seconds != null) {
            millis = millis.plus(TimeUnit.SECONDS.toMillis(seconds.toLong()))
        }
        return millis
    }

    override fun getTitle(context: Context?): String {
        return requireContext().getString(R.string.timer)
    }

}
