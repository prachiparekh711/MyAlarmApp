package clock.alarm.stopwatch.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.adapters.LibraryPagerAdapter
import clock.alarm.stopwatch.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class HomeFragment : Fragment(), BottomNavigationView.OnNavigationItemSelectedListener {

    var binding: FragmentHomeBinding? = null
    var mViewPagerAdapter: LibraryPagerAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding?.bottomNavigationView?.setOnNavigationItemSelectedListener(this)
        mViewPagerAdapter = LibraryPagerAdapter(childFragmentManager)
        binding?.viewPager?.adapter = mViewPagerAdapter
        binding?.viewPager?.offscreenPageLimit = 2

        binding?.viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
            override fun onPageSelected(i: Int) {
                binding?.bottomNavigationView?.menu?.getItem(i)?.isChecked = true
                val intent = Intent("TITLE")
                when (i) {
                    0 -> intent.putExtra("message", "Alarm")
                    1 -> intent.putExtra("message", "Clock")
                    2 -> intent.putExtra("message", "Timer")
                    3 -> intent.putExtra("message", "Stopwatch")
                }
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
            }

            override fun onPageScrollStateChanged(i: Int) {}
        })
        binding?.viewPager?.let { setupMainViewPager(it) }
        return binding?.root
    }

    private fun setupMainViewPager(viewPager: ViewPager) {
        val adapter = LibraryPagerAdapter(childFragmentManager)
        val alarmFragment = AlarmFragment()
        val clockFragment = ClockFragment()
        val timerFragment = TimerFragment()
        val stopwatchFragment = StopwatchFragment()
        adapter.addFragment(alarmFragment)
        adapter.addFragment(clockFragment)
        adapter.addFragment(timerFragment)
        adapter.addFragment(stopwatchFragment)
        viewPager.adapter = adapter
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val intent = Intent("TITLE")
        when (item.itemId) {
            R.id.alarm -> {
                binding?.viewPager?.currentItem = 0
                intent.putExtra("message", "Alarm")
            }
            R.id.clock -> {
                binding?.viewPager?.currentItem = 1
                intent.putExtra("message", "Clock")
            }
            R.id.timer -> {
                binding?.viewPager?.currentItem = 2
                intent.putExtra("message", "Timer")
            }
            R.id.stopwatch -> {
                binding?.viewPager?.currentItem = 3
                intent.putExtra("message", "Stopwatch")
            }
        }
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
        return false
    }

}