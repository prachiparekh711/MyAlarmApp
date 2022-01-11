package clock.alarm.stopwatch.fragments


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.databinding.FragmentStopwatchBinding
import clock.alarm.stopwatch.services.StopwatchService
import clock.alarm.stopwatch.utils.FormatUtils
import com.afollestad.aesthetic.Aesthetic.Companion.get
import io.reactivex.disposables.Disposable

class StopwatchFragment : BasePagerFragment(), StopwatchService.Listener, ServiceConnection {

    var binding: FragmentStopwatchBinding? = null
    var service: StopwatchService? = null
    var textColorPrimarySubscription: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_stopwatch, container, false)

        binding?.reset?.setOnClickListener(View.OnClickListener { v: View? -> service?.reset() })
        binding?.reset?.isClickable = false

        binding?.toggle?.setOnClickListener(View.OnClickListener { v: View? ->
            service?.toggle()
        })

        binding?.lap?.setOnClickListener(View.OnClickListener { v: View? -> service?.lap() })

        textColorPrimarySubscription = get()
            .textColorPrimary()
            .subscribe { integer: Int ->

                for (i in 0 until binding?.laps?.childCount!!) {
                    val layout =
                        binding?.laps?.getChildAt(i) as LinearLayout
                    for (i2 in 0 until layout.childCount) {
                        (layout.getChildAt(i2) as TextView).setTextColor(Color.WHITE)
                    }
                }
            }
        val intent = Intent(context, StopwatchService::class.java)
        requireContext().startService(intent)
        requireContext().bindService(intent, this, Context.BIND_AUTO_CREATE)
        service?.toggle()

        return binding?.root
    }


    override fun onDestroyView() {
        textColorPrimarySubscription!!.dispose()
        binding?.timeView?.unsubscribe()
        if (service != null) {
            service!!.setListener(null)
            val isRunning = service!!.isRunning
            requireContext().unbindService(this)
            if (!isRunning) requireContext().stopService(
                Intent(
                    context,
                    StopwatchService::class.java
                )
            )
        }
        super.onDestroyView()
    }

    override fun getTitle(context: Context?): String {
        return requireContext().getString(R.string.stopwatch)
    }

    override fun onStateChanged(isRunning: Boolean) {
        if (isRunning) {
            binding?.reset?.isClickable = false
            binding?.reset?.animate()?.alpha(0f)?.start()
            binding?.lap?.visibility = View.VISIBLE
            binding?.toggle?.setImageResource(R.drawable.ic_pause)
        } else {
            if (service!!.elapsedTime > 0) {
                binding?.reset?.isClickable = true
                binding?.reset?.animate()?.alpha(1f)?.start()
            }
            binding?.lap?.visibility = View.INVISIBLE
            binding?.toggle?.setImageResource(R.drawable.ic_play)
        }
    }

    override fun onReset() {
        binding?.laps?.removeAllViews()
        binding?.timeView?.setMaxProgress(0)
        binding?.timeView?.setReferenceProgress(0)
        binding?.reset?.isClickable = false
        binding?.reset?.alpha = 0f
        binding?.lap?.visibility = View.INVISIBLE
    }

    override fun onTick(currentTime: Long, text: String?) {
        if (service != null) {
            if (text != null) {
                binding?.timeView?.setText(text)
            }
            binding?.timeView?.setProgress(currentTime.minus(if (service!!.lastLapTime.toInt() == 0) currentTime else service!!.lastLapTime))
        }
    }

    override fun onLap(lapNum: Int, lapTime: Long, lastLapTime: Long, lapDiff: Long) {
        if (lastLapTime == 0L) binding?.timeView?.setMaxProgress(lapDiff) else binding?.timeView?.setReferenceProgress(
            lapDiff
        )

        val layout = LinearLayout(context)

        val number = TextView(context)
        number.text = getString(R.string.title_lap_number, lapNum)
        context?.resources?.getColor(R.color.white)?.let { number.setTextColor(it) }
        layout.addView(number)

        val layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.weight = 1f

        val lap = TextView(context)
        lap.layoutParams = layoutParams
        lap.gravity = GravityCompat.END
        lap.text = getString(R.string.title_lap_time, FormatUtils.formatMillis(lapDiff))
        context?.resources?.getColor(R.color.white)?.let { lap.setTextColor(it) }
        layout.addView(lap)

        val total = TextView(context)
        total.layoutParams = layoutParams
        total.gravity = GravityCompat.END
        total.text = getString(R.string.title_total_time, FormatUtils.formatMillis(lapTime))
        context?.resources?.getColor(R.color.white)?.let { total.setTextColor(it) }
        layout.addView(total)

        binding?.laps?.addView(layout, 0)
    }

    override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
        if (iBinder != null && iBinder is StopwatchService.LocalBinder) {
            service = iBinder.service
            onStateChanged(service!!.isRunning)
            onTick(0, "00 : 00")
            service!!.setListener(this)
        }
    }


    override fun onServiceDisconnected(componentName: ComponentName?) {
        service = null
    }

}
