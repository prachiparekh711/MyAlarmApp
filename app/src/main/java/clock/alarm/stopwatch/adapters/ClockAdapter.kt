package clock.alarm.stopwatch.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.views.DigitalClockView
import java.text.SimpleDateFormat
import java.util.*


class ClockAdapter(context1: Context?) :
    RecyclerView.Adapter<ClockAdapter.MyClassView2>() {
    var context: Context? = null
    var mainArrayList = ArrayList<String>()


    init {
        this.context = context1
        mainArrayList.clear()
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context1)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyClassView2 {
        val itemView = inflater.inflate(R.layout.item_clock, parent, false)

        return MyClassView2(itemView)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onBindViewHolder(holder: MyClassView2, position: Int) {
        var clockClass = mainArrayList.get(position)

        if (clockClass != TimeZone.getDefault().id) {
            holder.timeView.setTimezone(clockClass)
            holder.timeView.setAlignCenter()
            holder.timezone.text = String.format(
                "%s %s",
                clockClass.replace("_".toRegex(), " "),
                TimeZone.getTimeZone(clockClass).displayName
            )

            holder.date.text = getDate(clockClass)
        }

    }

    fun getDate(timezone: String?): String? {
        val c = Calendar.getInstance()
        val date = c.time //current date and time in UTC
        val df = SimpleDateFormat("E, MMM dd")
        df.timeZone = TimeZone.getTimeZone(timezone) //format in given timezone
        return df.format(date)
    }


    override fun getItemCount(): Int {
        return mainArrayList.size
    }

    class MyClassView2(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var timeView: DigitalClockView
        var date: TextView
        var timezone: TextView

        init {
            timeView = itemView.findViewById(R.id.timeView)
            date = itemView.findViewById(R.id.date)
            timezone = itemView.findViewById(R.id.timezone)
        }
    }


    fun add(model: String?) {
        if (model != null) {
            mainArrayList.add(model)
        }
//        Log.e("History_size :- ", mainArrayList.size.toString())
        notifyDataSetChanged()
    }

    fun newArray() {
        mainArrayList = ArrayList<String>()
        mainArrayList.clear()
        notifyDataSetChanged()
    }

}