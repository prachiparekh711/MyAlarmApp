package clock.alarm.stopwatch.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import clock.alarm.stopwatch.R
import clock.alarm.stopwatch.databinding.FragmentPolicyBinding

class PolicyFragment : Fragment() {
    var binding: FragmentPolicyBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_policy, container, false)
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )


        binding?.mWebView?.webViewClient = MyWebViewClient()
        openURL()
        return binding?.root
    }

    private fun openURL() {
        binding?.mWebView?.loadUrl(requireActivity().resources.getString(R.string.policy))
        binding?.mWebView?.requestFocus()
    }

    private class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }
    }

}