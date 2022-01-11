package clock.alarm.stopwatch.fragments


import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.adapters.ClockAdapter
import clock.alarm.stopwatch.adapters.TimeZoneAdapter
import clock.alarm.stopwatch.data.PreferenceData
import clock.alarm.stopwatch.databinding.FragmentClockBinding
import com.daily.motivational.quotes2.dataClass.SharedPreference
import java.text.SimpleDateFormat
import java.util.*


class ClockFragment : BasePagerFragment() {

    var binding: FragmentClockBinding? = null
    var adapter: ClockAdapter? = null
    var adapter1: TimeZoneAdapter? = null
    private val excludedIds = arrayOfNulls<String>(0)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_clock,
            container,
            false
        )

        binding?.date?.text = getDate(TimeZone.getDefault().id)

        if (SharedPreference.getStyle(requireContext()).equals("A")) {
            val calendar = Calendar.getInstance()
            binding?.clock!!.setCalendar(calendar)
                .setDiameterInDp(200.0f)
                .setOpacity(1.0f)
                .setShowSeconds(true)
                .color = context?.resources?.getColor(R.color.white)!!

            binding?.clock?.isVisible = true
            binding?.timeView?.isVisible = false
        } else {
            binding?.timeView?.setTimezone(TimeZone.getDefault().id)
            binding?.timeView?.setAlignCenter()
            binding?.clock?.isVisible = false
            binding?.timeView?.isVisible = true
        }

//      data loader
        binding?.clockRv?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = ClockAdapter(requireContext())
        binding?.clockRv?.adapter = adapter
        loadAllTimes()

//        Timezone Loader
        binding?.timezoneRv?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter1 = TimeZoneAdapter(requireContext())
        binding?.timezoneRv?.adapter = adapter1

        adapter1!!.newArray()
        val timeZones: MutableList<String> = ArrayList()
        for (id1 in TimeZone.getAvailableIDs()) {
            var isFine = true
            for (id2 in timeZones) {
                if (TimeZone.getTimeZone(id1).rawOffset == TimeZone.getTimeZone(id2).rawOffset) {
                    isFine = false
                    break
                }
            }
            for (id2 in excludedIds) {
                if (TimeZone.getTimeZone(id1).rawOffset == TimeZone.getTimeZone(id2).rawOffset) {
                    isFine = false
                    break
                }
            }
            if (isFine) timeZones.add(id1)
        }

        timeZones.sortWith(Comparator { id1: String?, id2: String? ->
            TimeZone.getTimeZone(
                id1
            ).rawOffset - TimeZone.getTimeZone(id2).rawOffset
        })

        for (id in timeZones) {
            adapter1!!.add(id)
        }
        adapter1?.filter?.filter("")

//        visibility handling
        binding?.firstView?.isVisible = true
        binding?.secondView?.isVisible = false
        binding?.addClockFab?.setOnClickListener {
            binding?.searchRL?.let { it1 -> expand(it1) }
            binding?.timezoneRv?.let { it1 -> expand(it1) }
            binding?.firstView?.isVisible = false
            binding?.secondView?.isVisible = true
        }

        binding?.mClose?.setOnClickListener {
            collapse(binding?.searchRL!!)
            binding?.firstView?.isVisible = true
            binding?.secondView?.isVisible = false
            hideKeyboard()
            loadAllTimes()
        }

        binding?.searchBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                adapter1?.filter?.filter(s)

            }

            override fun afterTextChanged(s: Editable) {
            }
        })
        return binding?.root
    }

    fun getDate(timezone: String?): String? {
        val c = Calendar.getInstance()
        val date = c.time //current date and time in UTC
        val df = SimpleDateFormat("E, MMM dd")
        df.timeZone = TimeZone.getTimeZone(timezone) //format in given timezone
        return df.format(date)
    }


    fun loadAllTimes() {
        adapter!!.newArray()
        for (id in TimeZone.getAvailableIDs()) {
            if (PreferenceData.TIME_ZONE_ENABLED.getSpecificValue(context, id))
                adapter!!.add(id)
        }
    }


    fun hideKeyboard() {
        if (binding?.searchBar != null) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding?.searchBar?.windowToken, 0)
        }
    }

    fun expand(v: View) {
        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = v.measuredHeight

// Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) LinearLayout.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

// Expansion speed of 1dp/ms
        a.duration = 500
        v.startAnimation(a)
    }

    fun collapse(v: View) {
        val initialHeight = v.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

// Collapse speed of 1dp/ms
        a.duration = 500
        v.startAnimation(a)
    }

    override fun getTitle(context: Context?): String {
        return requireContext().getString(R.string.clock)
    }


}
