package clock.alarm.stopwatch.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.databinding.FragmentSettingBinding
import clock.alarm.stopwatch.utils.FormatUtils
import com.daily.motivational.quotes2.dataClass.SharedPreference
import es.dmoral.toasty.Toasty
import java.util.*

class SettingFragment : Fragment() {
    var binding: FragmentSettingBinding? = null
    var mgr: AudioManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setting, container, false)


        if (SharedPreference.getStyle(requireContext()) == "A") {
            binding?.mStyleTV?.text = context?.resources?.getString(R.string.analog)
        } else {
            binding?.mStyleTV?.text = context?.resources?.getString(R.string.analog)
        }
        binding?.mStyle?.setOnClickListener {
            if (SharedPreference.getStyle(requireContext()) == "A") {
                SharedPreference.setStyle(requireContext(), "D")
                binding?.mStyleTV?.text = context?.resources?.getString(R.string.digital)
            } else {
                SharedPreference.setStyle(requireContext(), "A")
                binding?.mStyleTV?.text = context?.resources?.getString(R.string.analog)
            }
            context?.let { it1 ->
                Toasty.info(
                    it1, "${binding?.mStyleTV?.text} clock is set", Toast.LENGTH_LONG
                ).show()
            }
        }

        binding?.sSwitch?.isChecked = SharedPreference.getSeconds(requireContext())

        binding?.sSwitch?.setOnCheckedChangeListener { _, b ->
            if (!b) {
                binding?.sSwitch?.isChecked = false
                SharedPreference.setSeconds(requireContext(), false)
            } else {
                binding?.sSwitch?.isChecked = true
                SharedPreference.setSeconds(requireContext(), true)
            }
        }

        binding?.mChange?.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_DATE_SETTINGS))
        }

        binding?.mMinutes?.text = "${SharedPreference.getSilenceMin(requireContext())} Minutes"
        binding?.mSilence?.setOnClickListener {
            ShowMinuteDialog(1)
        }

        binding?.mMinutes1?.text = "${SharedPreference.getSnoozeMin(requireContext())} Minutes"
        binding?.mSnooz?.setOnClickListener {
            ShowMinuteDialog(2)
        }

        mgr = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mgr?.getStreamMaxVolume(AudioManager.STREAM_ALARM)?.let { binding?.mSeekbar?.setMax(it) }
        context?.let { SharedPreference.getVolume(it) }
            ?.let { binding?.mSeekbar?.setProgress(it) }
        Log.e("sound_max_s", binding?.mSeekbar?.max.toString())

        binding?.mSeekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                bar: SeekBar?, progress: Int,
                fromUser: Boolean
            ) {
                Log.e("sound_progress", progress.toString())
                SharedPreference.setVolume(requireContext(), progress)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {
                // no-op
            }

            override fun onStopTrackingTouch(bar: SeekBar?) {
                // no-op
            }
        })

        binding?.mHome?.setOnClickListener {
            val tz: TimeZone = TimeZone.getDefault()
            Toasty.info(
                requireContext(), "Home TimeZone is " + tz.getDisplayName(false, TimeZone.SHORT)
                    .toString(), Toast.LENGTH_LONG
            ).show()
        }

        return binding?.root
    }

    fun ShowMinuteDialog(value: Int) {
        var minutes: IntArray? = null
        if (value == 1) {
            minutes = intArrayOf(1, 2, 3, 4, 5)
        } else {
            minutes = intArrayOf(2, 5, 10, 20, 30, 60)
        }
        val names = arrayOfNulls<CharSequence>(minutes.size)
        for (i in minutes.indices) {
            names[i] = FormatUtils.formatUnit(requireContext(), minutes[i])
        }

        AlertDialog.Builder(
            requireContext(),
            R.style.Theme_AppCompat_Light_Dialog_Alert
        )
            .setItems(
                names
            ) { dialog: DialogInterface?, which: Int ->
                if (value == 1) {
                    SharedPreference.setSilenceMin(requireContext(), minutes[which])
                    binding?.mMinutes?.text = "${minutes.get(which)} Minutes"
                } else {
                    SharedPreference.setSnoozeMin(requireContext(), minutes[which])
                    binding?.mMinutes1?.text = "${minutes.get(which)} Minutes"
                }

            }
            .setNegativeButton(
                android.R.string.cancel
            ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            .show()
    }
}