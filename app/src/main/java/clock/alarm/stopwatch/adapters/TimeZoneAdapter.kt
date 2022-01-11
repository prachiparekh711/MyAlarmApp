package clock.alarm.stopwatch.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.RecyclerView
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.data.PreferenceData
import java.util.*
import java.util.concurrent.TimeUnit


class TimeZoneAdapter(context1: Context?) :
    RecyclerView.Adapter<TimeZoneAdapter.MyClassView2>(), Filterable {
    var context: Context? = null
    var mainArrayList = ArrayList<String>()

    companion object {
        var arrayFilterList = ArrayList<String>()
    }

    init {
        this.context = context1
        mainArrayList.clear()
        arrayFilterList.clear()
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context1)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyClassView2 {
        val itemView = inflater.inflate(R.layout.item_timezone, parent, false)

        return MyClassView2(itemView)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onBindViewHolder(holder: MyClassView2, position: Int) {
        val timeZone: TimeZone = TimeZone.getTimeZone(arrayFilterList.get(position))

        val offsetMillis = timeZone.rawOffset
        holder.time!!.text = String.format(
            Locale.getDefault(),
            "GMT%s%02d:%02d",
            if (offsetMillis >= 0) "+" else "",
            TimeUnit.MILLISECONDS.toHours(offsetMillis.toLong()),
            TimeUnit.MILLISECONDS.toMinutes(
                Math.abs(offsetMillis).toLong()
            ) % TimeUnit.HOURS.toMinutes(1)
        )

        holder.title!!.text = timeZone.getDisplayName(Locale.getDefault())

        holder.itemView.setOnClickListener { v: View? -> holder.checkBox!!.toggle() }

        holder.checkBox!!.setOnCheckedChangeListener(null)
        holder.checkBox!!.isChecked =
            PreferenceData.TIME_ZONE_ENABLED.getSpecificValue(
                holder.itemView.context,
                timeZone.id
            ) as Boolean
        holder.checkBox!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            val timeZone1: TimeZone =
                TimeZone.getTimeZone(arrayFilterList.get(holder.adapterPosition))
            PreferenceData.TIME_ZONE_ENABLED.setValue(
                holder.itemView.context,
                isChecked,
                timeZone1.id
            )
        }
    }

    override fun getItemCount(): Int {
        return arrayFilterList.size
    }

    class MyClassView2(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var time: TextView? = null
        var title: TextView? = null
        var checkBox: AppCompatCheckBox? = null

        init {
            time = itemView.findViewById(R.id.time)
            title = itemView.findViewById(R.id.title)
            checkBox = itemView.findViewById(R.id.checkbox)
        }
    }


    fun add(model: String?) {
        if (model != null) {
            mainArrayList.add(model)
        }
        notifyDataSetChanged()
    }

    fun newArray() {
        mainArrayList = ArrayList<String>()
        mainArrayList.clear()
        arrayFilterList = ArrayList<String>()
        arrayFilterList.clear()
        notifyDataSetChanged()
    }


    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.equals("")) {
                    arrayFilterList = mainArrayList
                } else {
                    val filteredList1: ArrayList<String> = ArrayList<String>()
                    for (row in mainArrayList) {
                        val timeZone: TimeZone = TimeZone.getTimeZone(row)
                        val matchStr = timeZone.getDisplayName(Locale.getDefault())
                        if (matchStr.lowercase()
                                .contains(charString.lowercase())
                        ) {
                            filteredList1.add(row)
                        }
                    }
                    arrayFilterList = filteredList1
                }
                val filterResults = FilterResults()
                filterResults.values = arrayFilterList
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                arrayFilterList = filterResults.values as ArrayList<String>
                notifyDataSetChanged()
            }
        }
    }


}