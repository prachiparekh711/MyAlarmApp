package clock.alarm.stopwatch.activities

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import clock.alarm.stopwatch.AlarmApp
import clock.alarm.stopwatch.BuildConfig
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.databinding.ActivityMainBinding
import clock.alarm.stopwatch.fragments.HomeFragment
import clock.alarm.stopwatch.fragments.PolicyFragment
import clock.alarm.stopwatch.fragments.SettingFragment
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.AestheticKeyProvider
import com.airbnb.lottie.LottieAnimationView
import com.shrikanthravi.customnavigationdrawer2.widget.SNavigationDrawer
import java.util.*

class MainActivity : AppCompatActivity(),
    AlarmApp.ActivityListener, AestheticKeyProvider {

    companion object {
        const val EXTRA_FRAGMENT = "MainActivity.EXTRA_FRAGMENT"
        const val FRAGMENT_TIMER = 0
        const val FRAGMENT_STOPWATCH = 2
    }

    var binding: ActivityMainBinding? = null
    var fragment: Fragment? = null
    var menuItem: Int = 0
    var homeFragment: HomeFragment? = null
    var settingFragment: SettingFragment? = null
    var policyFragment: PolicyFragment? = null
    private var alarmio: AlarmApp? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)
        Aesthetic.attach(baseContext)

        alarmio = applicationContext as AlarmApp
        alarmio?.setListener(this)
        setNavigationItems()
        homeFragment = HomeFragment()
        settingFragment = SettingFragment()
        policyFragment = PolicyFragment()
    }

    override fun onPause() {
        super.onPause()
        Aesthetic.pause(this)
        alarmio?.stopCurrentSound()
    }

    override fun onResume() {
        super.onResume()
        Aesthetic.resume(this)
    }

    fun setNavigationItems() {

        val menuItems: MutableList<com.shrikanthravi.customnavigationdrawer2.data.MenuItem> =
            ArrayList()

        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Home",
                R.drawable.ic_rectangle,
                R.drawable.ic_home
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Settings",
                R.drawable.ic_rectangle,
                R.drawable.ic_setting
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Rate this app",
                R.drawable.ic_rectangle,
                R.drawable.ic_star
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Share with friend",
                R.drawable.ic_rectangle,
                R.drawable.ic_share
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Contact us",
                R.drawable.ic_rectangle,
                R.drawable.ic_contact
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Privacy policy",
                R.drawable.ic_rectangle,
                R.drawable.ic_policy
            )
        )
        binding?.navigationDrawer?.menuItemList = menuItems

        try {
            fragment = HomeFragment()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        val fragmentManager: FragmentManager = supportFragmentManager
        fragment?.let {
            fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(
                    R.id.container,
                    it
                ).commit()
        }

        binding?.navigationDrawer?.onMenuItemClickListener =
            SNavigationDrawer.OnMenuItemClickListener { position ->
                menuItem = position
                when (position) {
                    0 -> {
                        fragment = HomeFragment()
                    }
                    1 -> {
                        fragment = SettingFragment()
                    }
                    2 -> {
                        showRateDialog()
                    }
                    3 -> {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            shareIntent.putExtra(
                                Intent.EXTRA_SUBJECT,
                                resources.getString(R.string.app_name)
                            )
                            var shareMessage = "\nLet me recommend you this application\n\n"
                            shareMessage = """
                                ${shareMessage}https://play.google.com/store/apps/details?id=
                                ${BuildConfig.APPLICATION_ID} """.trimIndent()
                            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                            startActivity(Intent.createChooser(shareIntent, "Share via"))
                        } catch (e: Exception) {
                            //e.toString();
                        }

                    }
                    4 -> {
                        sendFeedback()
                    }
                    5 -> {
                        fragment = PolicyFragment()
                    }
                }

                binding?.navigationDrawer?.drawerListener =
                    object : SNavigationDrawer.DrawerListener {
                        override fun onDrawerOpened() {

                        }

                        override fun onDrawerOpening() {

                        }

                        override fun onDrawerClosing() {
                            if (fragment != null) {
                                val fragmentManager = supportFragmentManager
                                fragmentManager.beginTransaction().setCustomAnimations(
                                    android.R.animator.fade_in,
                                    android.R.animator.fade_out
                                ).replace(
                                    R.id.container,
                                    fragment!!
                                ).commit()
                            }
                        }

                        override fun onDrawerClosed() {

                        }

                        override fun onDrawerStateChanged(newState: Int) {

                        }
                    }
            }
    }

    private fun showRateDialog() {
        val dialog = Dialog(this@MainActivity, android.R.style.Theme_Black_NoTitleBar)
        dialog.requestWindowFeature(Window.FEATURE_ACTION_BAR)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialige_rate_us)
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))
        dialog.setCanceledOnTouchOutside(true)
        val animationView = dialog.findViewById(R.id.animationView) as LottieAnimationView
        animationView.animate()
        val yesBtn = dialog.findViewById(R.id.yes) as TextView
        val noBtn = dialog.findViewById(R.id.no) as TextView
        yesBtn.setOnClickListener {
            val appPackageName = packageName

            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$appPackageName")
                    )
                )
            } catch (anfe: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                    )
                )
            }
            dialog.dismiss()
        }
        noBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()

    }

    fun sendFeedback() {
        val emailIntent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:" + resources?.getString(R.string.feedback_email))
        )
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback")
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email via..."))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                "There are no email clients installed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun requestPermissions(vararg permissions: String?) {
        ActivityCompat.requestPermissions(this, permissions, 0)
    }

    override fun gettFragmentManager(): FragmentManager? {
        return supportFragmentManager
    }

    override fun key(): String? {
        return null
    }


}