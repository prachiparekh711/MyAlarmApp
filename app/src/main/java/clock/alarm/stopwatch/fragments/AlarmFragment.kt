package clock.alarm.stopwatch.fragments

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.adapters.AlarmsAdapter
import clock.alarm.stopwatch.data.AlarmData
import clock.alarm.stopwatch.databinding.FragmentAlarmBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

class AlarmFragment : BasePagerFragment() {

    var binding: FragmentAlarmBinding? = null
    private var alarmsAdapter: AlarmsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_alarm,
            container,
            false
        )
        binding?.recycler?.layoutManager = GridLayoutManager(context, 1)
        alarmsAdapter = binding?.recycler?.let {
            fragmentManager?.let { it1 ->
                alarmio?.let { it2 ->
                    AlarmsAdapter(
                        requireContext(),
                        it2, it,
                        it1
                    )
                }
            }
        }
        binding?.recycler?.adapter = alarmsAdapter

        binding?.addAlarmFab?.setOnClickListener(fabListener)
        onChanged()
        return binding?.root
    }

    override fun onAlarmsChanged() {
        if (binding?.recycler != null && alarmsAdapter != null) {
            binding?.recycler?.post(Runnable { alarmsAdapter!!.notifyDataSetChanged() })
            onChanged()
        }
    }

    override fun onTimersChanged() {
        if (binding?.recycler != null && alarmsAdapter != null) {
            binding?.recycler?.post(Runnable { alarmsAdapter!!.notifyDataSetChanged() })
            onChanged()
        }
    }

    private fun onChanged() {
        if (binding?.empty != null && alarmsAdapter != null) binding?.empty?.visibility =
            if (alarmsAdapter!!.itemCount > 0) View.GONE else View.VISIBLE
    }

    override fun getTitle(context: Context?): String {
        return requireContext().getString(R.string.title_alarms)
    }

    val fabListener = View.OnClickListener {
        showEditAlarmFragment()
    }

    private fun showEditAlarmFragment(alarm: AlarmData? = null) {
        var hour = 0
        var min = 0
        if (alarm != null) {
            hour = alarm.time.get(Calendar.HOUR_OF_DAY)
            min = alarm.time.get(Calendar.MINUTE)
        } else {
            val cal = Calendar.getInstance()
            hour = cal.get(Calendar.HOUR_OF_DAY)
            min = cal.get(Calendar.MINUTE)
        }

        val materialTimePicker: MaterialTimePicker =
            MaterialTimePicker.Builder()
                .setTitleText(requireContext().resources.getString(R.string.select_time))
                .setHour(hour)
                .setMinute(min)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .build()


        materialTimePicker.show(childFragmentManager, "MainActivity")

        materialTimePicker.addOnPositiveButtonClickListener {

            val pickedHour: Int = materialTimePicker.hour
            val pickedMinute: Int = materialTimePicker.minute

            val formattedTime: String = when {
                pickedHour > 12 -> {
                    if (pickedMinute < 10) {
                        "${materialTimePicker.hour - 12}:0${materialTimePicker.minute} pm"
                    } else {
                        "${materialTimePicker.hour - 12}:${materialTimePicker.minute} pm"
                    }
                }
                pickedHour == 12 -> {
                    if (pickedMinute < 10) {
                        "${materialTimePicker.hour}:0${materialTimePicker.minute} pm"
                    } else {
                        "${materialTimePicker.hour}:${materialTimePicker.minute} pm"
                    }
                }
                pickedHour == 0 -> {
                    if (pickedMinute < 10) {
                        "${materialTimePicker.hour + 12}:0${materialTimePicker.minute} am"
                    } else {
                        "${materialTimePicker.hour + 12}:${materialTimePicker.minute} am"
                    }
                }
                else -> {
                    if (pickedMinute < 10) {
                        "${materialTimePicker.hour}:0${materialTimePicker.minute} am"
                    } else {
                        "${materialTimePicker.hour}:${materialTimePicker.minute} am"
                    }
                }
            }

            Log.e("formattedTime:", formattedTime)
            if (alarm != null) {
                //update
            } else {
                //new
                launchLabelDialog(pickedHour, pickedMinute)
            }
        }
    }

    private fun launchLabelDialog(hour: Int = 0, min: Int = 0) {
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.label_layout, null)
        val nameTextField = dialogView.findViewById<EditText>(R.id.mLabelET)
        val ok = dialogView.findViewById<TextView>(R.id.ok)
        val cancel = dialogView.findViewById<TextView>(R.id.cancel)
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setView(dialogView)
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        ok.setOnClickListener {
            val name = nameTextField.text.toString()
            addNewAlarm(hour, min, name)
            alertDialog?.dismiss()
        }

        cancel.setOnClickListener {
            addNewAlarm(hour, min, "")
            alertDialog?.dismiss()
        }
    }

    fun addNewAlarm(hour: Int = 0, min: Int = 0, label: String?) {
        val manager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = alarmio!!.newAlarm()
        alarm.name = label
        alarm.time[Calendar.HOUR_OF_DAY] = hour
        alarm.time[Calendar.MINUTE] = min
        alarm.setTime(alarmio, manager, alarm.time.timeInMillis)
        alarm.setEnabled(context, manager, true)

        alarmio!!.onAlarmsChanged()
    }


}