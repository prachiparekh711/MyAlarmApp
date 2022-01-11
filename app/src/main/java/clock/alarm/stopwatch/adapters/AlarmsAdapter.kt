package clock.alarm.stopwatch.adapters

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import clock.alarm.stopwatch.AlarmApp
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.data.AlarmData
import clock.alarm.stopwatch.data.TimerData
import clock.alarm.stopwatch.dialog.SoundChooserDialog
import clock.alarm.stopwatch.utils.FormatUtils
import clock.alarm.stopwatch.views.DaySwitch
import com.afollestad.aesthetic.Aesthetic
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.android.synthetic.main.item_alarm.view.*
import me.jfenn.alarmio.views.ProgressLineView
import me.jfenn.androidutils.DimenUtils
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * View adapter for the "alarms" list; displays all timers and
 * alarms currently stored in the application.
 */
class AlarmsAdapter(
    context1: Context,
    private val alarmio: AlarmApp,
    private val recycler: RecyclerView,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val alarmManager: AlarmManager? =
        alarmio.getSystemService(Context.ALARM_SERVICE) as? AlarmManager?
    private val timers: ArrayList<TimerData>? = alarmio.timers
    private val alarms: ArrayList<AlarmData>? = alarmio.alarms

    private var expandedPosition = -1
    var context: Context? = null

    init {
        this.context = context1
    }

    var colorAccent = Color.WHITE
        set(colorAccent) {
            field = colorAccent
            recycler.post { notifyDataSetChanged() }
        }

    var colorForeground = Color.TRANSPARENT
        set(colorForeground) {
            field = colorForeground
            if (expandedPosition > 0)
                recycler.post { notifyItemChanged(expandedPosition) }
        }

    var textColorPrimary = Color.WHITE
        set(textColorPrimary) {
            field = textColorPrimary
            recycler.post { notifyDataSetChanged() }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0)
            TimerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_timer, parent, false)
            )
        else
            AlarmViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false),
                alarmio
            )
    }

    private fun onBindTimerViewHolder(holder: TimerViewHolder, position: Int) {
        holder.runnable = object : Runnable {
            override fun run() {
                try {
                    getTimer(holder.adapterPosition)?.let { timer ->
                        val text = FormatUtils.formatMillis(timer.remainingMillis)
                        holder.time.text = text.substring(0, text.length - 3)
                        holder.progress.update(1 - timer.remainingMillis.toFloat() / timer.duration)
                    }

                    holder.handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        holder.stop.setColorFilter(textColorPrimary)
        holder.stop.setOnClickListener {
            getTimer(holder.adapterPosition)?.let { timer ->
                alarmio.removeTimer(timer)
            }
        }
    }

    private fun onBindAlarmViewHolderRepeat(holder: AlarmViewHolder, alarm: AlarmData) {

        val listener: DaySwitch.OnCheckedChangeListener =
            object : DaySwitch.OnCheckedChangeListener {
                override fun onCheckedChanged(daySwitch: DaySwitch, b: Boolean) {
                    alarm.days[holder.days.indexOfChild(daySwitch)] = b
                    alarm.setDays(alarmio, alarm.days)

                    if (!alarm.isRepeat) {
                        notifyItemChanged(holder.adapterPosition)
                    } else {
                        // if the view isn't going to change size in the recycler,
                        //   then I can just do this (prevents the background flickering as
                        //   the recyclerview attempts to smooth the transition)
                        onBindAlarmViewHolder(holder, holder.adapterPosition)
                    }
                }
            }
        var newString = ""
        for (i in 0..6) {
            val daySwitch = holder.days.getChildAt(i) as DaySwitch
            daySwitch.background = context?.resources?.getDrawable(R.drawable.togglebutton_off)
            daySwitch.isChecked = alarm.days[i]
            if (daySwitch.isChecked) {
                newString = newString + daySwitch.tag + ", "
            }
            daySwitch.onCheckedChangeListener = listener

            when (i) {
                0 -> daySwitch.setText(daySwitch.context.getString(R.string.day_sunday_abbr))
                1 -> daySwitch.setText(daySwitch.context.getString(R.string.day_monday_abbr))
                2 -> daySwitch.setText(daySwitch.context.getString(R.string.day_tuesday_abbr))
                3 -> daySwitch.setText(daySwitch.context.getString(R.string.day_wednesday_abbr))
                4 -> daySwitch.setText(daySwitch.context.getString(R.string.day_thursday_abbr))
                5 -> daySwitch.setText(daySwitch.context.getString(R.string.day_friday_abbr))
                6 -> daySwitch.setText(daySwitch.context.getString(R.string.day_saturday_abbr))
            }
        }

        if (newString != "") {
            newString = StringUtils.substring(newString, 0, newString.length - 2)
            holder.itemView.alarm_days.text = newString
        } else {
            holder.itemView.alarm_days.text = "Once"
        }
    }


    private fun onBindAlarmViewHolderToggles(holder: AlarmViewHolder, alarm: AlarmData) {
        holder.ringtone.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ring, 0, 0, 0)
        holder.ringtone.text =
            if (alarm.hasSound()) alarm.getSound()?.name else alarmio.getString(R.string.title_sound_none)
        holder.ringtone.setOnClickListener { view ->
            val dialog = SoundChooserDialog()
            dialog.setListener { sound ->
                alarm.setSound(alarmio, sound)
                onBindAlarmViewHolderToggles(holder, alarm)
            }
            dialog.show(fragmentManager, null)
        }

        if (alarm.isVibrate)
            holder.vibrateToggle.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_checked))
        else
            holder.vibrateToggle.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_unchecked))

        holder.vibrate.setOnClickListener { view ->
            alarm.setVibrate(alarmio, !alarm.isVibrate)
            if (alarm.isVibrate) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                holder.vibrateToggle.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_checked))
            } else {
                holder.vibrateToggle.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_unchecked))
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun onBindAlarmViewHolderExpansion(holder: AlarmViewHolder, position: Int) {
        val isExpanded = position == expandedPosition
        val visibility = if (isExpanded) View.VISIBLE else View.GONE

        if (visibility != holder.itemView.hiddenRL.visibility) {
            holder.itemView.hiddenRL.visibility = visibility
            Aesthetic.get()
                .colorPrimary()
                .take(1)
                .subscribe { integer ->
                    ValueAnimator.ofObject(
                        ArgbEvaluator(),
                        if (isExpanded) integer else colorForeground,
                        if (isExpanded) colorForeground else integer
                    ).apply {
                        addUpdateListener { animation ->
                            (animation.animatedValue as? Int)?.let { color ->
                            }
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}

                            override fun onAnimationEnd(animation: Animator) {
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                        start()
                    }
                }

            ValueAnimator.ofFloat(
                if (isExpanded) 0f else DimenUtils.dpToPx(2f).toFloat(),
                if (isExpanded) DimenUtils.dpToPx(2f).toFloat() else 0f
            ).apply {
                addUpdateListener { animation ->
                    (animation.animatedValue as? Float)?.let { elevation ->
                        ViewCompat.setElevation(holder.itemView, elevation)
                    }
                }
                start()
            }
        } else {
            ViewCompat.setElevation(
                holder.itemView,
                (if (isExpanded) DimenUtils.dpToPx(2f) else 0).toFloat()
            )
        }

        holder.itemView.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else holder.adapterPosition

            val transition = AutoTransition()
            transition.duration = 250
            TransitionManager.beginDelayedTransition(recycler, transition)

            recycler.post { notifyDataSetChanged() }
        }
    }

    private fun onBindAlarmViewHolder(holder: AlarmViewHolder, position: Int) {
        val isExpanded = position == expandedPosition

        val alarm = getAlarm(position) ?: return

        holder.name.isFocusableInTouchMode = isExpanded
        holder.name.isCursorVisible = false
        holder.name.clearFocus()

        holder.name.text = alarm.getName(alarmio)

        holder.name.setOnClickListener {
            launchLabelDialog(alarm, holder)
        }

        holder.enable.setOnCheckedChangeListener(null)
        holder.enable.isChecked = alarm.isEnabled
        holder.itemView.blankRL.isVisible = !alarm.isEnabled
        holder.enable.setOnCheckedChangeListener { _, b ->
            alarm.setEnabled(alarmio, alarmManager, b)
            holder.itemView.blankRL.isVisible = !alarm.isEnabled
            val transition = AutoTransition()
            transition.duration = 200
            TransitionManager.beginDelayedTransition(recycler, transition)

            recycler.post { notifyDataSetChanged() }
        }

        holder.time.text = FormatUtils.formatShort1(alarmio, alarm.time.time)
        holder.time1.text = FormatUtils.formatShort2(alarmio, alarm.time.time)
        holder.time.setOnClickListener { view ->
            showEditAlarmFragment(alarm, holder)
        }

        val nextAlarm = alarm.next
        if (alarm.isEnabled && nextAlarm != null) {
            // minutes in a week: 10080
            // maximum value of an integer: 2147483647
            // we do not need to check this int cast
            val minutes =
                TimeUnit.MILLISECONDS.toMinutes(nextAlarm.timeInMillis - Calendar.getInstance().timeInMillis)
                    .toInt()

        }

        onBindAlarmViewHolderRepeat(holder, alarm)
        if (isExpanded) {
            onBindAlarmViewHolderToggles(holder, alarm)
        }

        holder.expandImage.animate().rotationX((if (isExpanded) 180 else 0).toFloat()).start()
        holder.delete.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.delete.setOnClickListener { view ->
            val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
            builder.setTitle(
                context?.getString(
                    R.string.msg_delete_confirmation,
                    alarm.getName(alarmio)
                )
            )
            builder.setCancelable(false)
            builder
                .setPositiveButton(
                    "Ok"
                ) { _, _ -> alarmio.removeAlarm(alarm) }
            builder
                .setNegativeButton(
                    "Cancel"
                ) { dialog, _ -> dialog.cancel() }
            val alertDialog: android.app.AlertDialog? = builder.create()
            alertDialog?.show()
        }

        onBindAlarmViewHolderExpansion(holder, position)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == 0 && holder is TimerViewHolder) {
            onBindTimerViewHolder(holder, position)
        } else if (holder is AlarmViewHolder) {
            onBindAlarmViewHolder(holder, position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < timers?.size!!) 0 else 1
    }

    override fun getItemCount(): Int {
        if (timers != null) {
            if (alarms != null) {
                return timers.size + alarms.size
            } else {
                return timers.size
            }
        } else {
            return 0
        }
    }

    /**
     * Returns the timer that should be bound to the
     * specified position in the list - null if there
     * is no timer to be bound.
     */
    private fun getTimer(position: Int): TimerData? {
        return if (position in (0 until timers?.size!!))
            timers[position]
        else null
    }

    /**
     * Returns the alarm that should be bound to
     * the specified position in the list - null if
     * there is no alarm to be bound.
     */
    private fun getAlarm(position: Int): AlarmData? {
        val alarmPosition = position - timers?.size!!
        return if (alarmPosition in (0 until alarms?.size!!))
            alarms[alarmPosition]
        else null
    }

    /**
     * ViewHolder for timer items.
     */
    class TimerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val handler = Handler()
        var runnable: Runnable? = null
            set(runnable) {
                if (field != null)
                    handler.removeCallbacks(field!!)

                field = runnable
                field?.let { handler.post(it) }
            }

        val time: TextView = itemView.findViewById(R.id.time)
        val stop: ImageView = itemView.findViewById(R.id.stop)
        val progress: ProgressLineView = itemView.findViewById(R.id.progress)
    }

    /**
     * ViewHolder for alarm items.
     */
    class AlarmViewHolder(v: View, val alarmio: AlarmApp) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.label_alarm_tv)
        val enable: SwitchCompat = v.findViewById(R.id.alarm_switch)
        val time: TextView = v.findViewById(R.id.alarm_tv)
        val time1: TextView = v.findViewById(R.id.alarm_tv1)
        val days: LinearLayout = v.findViewById(R.id.days)
        val ringtone: TextView = v.findViewById(R.id.ring_alarm_tv)
        val vibrate: TextView = v.findViewById(R.id.vibrant_alarm_tv)
        val expandImage: ImageView = v.findViewById(R.id.arrow)
        val vibrateToggle: ImageView = v.findViewById(R.id.vibrateToggle)
        val delete: TextView = v.findViewById(R.id.delete_alarm_tv)

        val alarms: MutableList<AlarmData>? = alarmio.alarms

        init {
            name.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                    // ignore
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    // ignore
                }

                override fun afterTextChanged(editable: Editable) {
//                    alarms?.get(adapterPosition)?.setName(alarmio, editable.toString())

                }
            })
        }
    }

    private fun showEditAlarmFragment(alarm: AlarmData? = null, holder: AlarmViewHolder) {
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
                .setTitleText(context?.resources?.getString(R.string.select_time))
                .setHour(hour)
                .setMinute(min)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .build()


        materialTimePicker.show(
            (holder.itemView.context as FragmentActivity).supportFragmentManager,
            "MainActivity"
        )

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
                alarm.time.set(Calendar.HOUR_OF_DAY, pickedHour)
                alarm.time.set(Calendar.MINUTE, pickedMinute)
                alarm.setTime(alarmio, alarmManager, alarm.time.timeInMillis)
                alarm.setEnabled(alarmio, alarmManager, true)
                notifyItemChanged(holder.adapterPosition)
            }
        }
    }

    private fun launchLabelDialog(alarm: AlarmData, holder: AlarmViewHolder) {
        val dialog = context?.let { Dialog(it) }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCancelable(false)
        dialog?.setContentView(R.layout.label_layout)
        val nameTextField = dialog?.findViewById<EditText>(R.id.mLabelET)
        val ok = dialog?.findViewById<TextView>(R.id.ok)
        val cancel = dialog?.findViewById<TextView>(R.id.cancel)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        ok?.setOnClickListener {
            val name = nameTextField?.text.toString()
            alarm.setName(alarmio, name)
            alarm.setEnabled(alarmio, alarmManager, true)
            notifyItemChanged(holder.adapterPosition)
            dialog.dismiss()
        }

        cancel?.setOnClickListener {
            dialog.dismiss()
        }
        dialog?.show()


    }
}
