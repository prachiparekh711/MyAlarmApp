package clock.alarm.stopwatch.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import clock.alarm.stopwatch.fragments.BasePagerFragment
import java.util.*

class LibraryPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {

    val mFragmentList: MutableList<Fragment> = ArrayList()

    override fun getItem(position: Int): Fragment {
        return mFragmentList[position]
    }

    override fun getCount(): Int {
        return mFragmentList.size
    }

    fun addFragment(fragment: BasePagerFragment) {
        mFragmentList.add(fragment)
    }
}