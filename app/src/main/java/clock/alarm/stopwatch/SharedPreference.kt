package com.daily.motivational.quotes2.dataClass

import android.content.Context

class SharedPreference {

    companion object {
        val MyPREFERENCES = "SPData"
        var CLOCK_STYLE = "CLOCK_STYLE"
        var DISPLAY_SECOND = "DISPLAY_SECOND"
        var SilenceMin = "SilenceMin"
        var SnoozeMin = "SnoozeMin"
        var VOLUME = "VOLUME"

        fun getStyle(c1: Context): String {
            val sharedpreferences = c1.getSharedPreferences(
                MyPREFERENCES,
                Context.MODE_PRIVATE
            )
            return sharedpreferences.getString(CLOCK_STYLE, "D").toString()
        }

        fun setStyle(c1: Context, value: String) {
            val sharedpreferences =
                c1.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE)
            val editor = sharedpreferences.edit()
            editor.putString(CLOCK_STYLE, value)
            editor.apply()
        }

        fun getSeconds(c1: Context): Boolean {
            val sharedpreferences = c1.getSharedPreferences(
                MyPREFERENCES,
                Context.MODE_PRIVATE
            )
            return sharedpreferences.getBoolean(DISPLAY_SECOND, false)
        }

        fun setSeconds(c1: Context, value: Boolean) {
            val sharedpreferences =
                c1.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE)
            val editor = sharedpreferences.edit()
            editor.putBoolean(DISPLAY_SECOND, value)
            editor.apply()
        }

        fun getSilenceMin(c1: Context): Int {
            val sharedpreferences = c1.getSharedPreferences(
                MyPREFERENCES,
                Context.MODE_PRIVATE
            )
            return sharedpreferences.getInt(SilenceMin, 10)
        }

        fun setSilenceMin(c1: Context, value: Int) {
            val sharedpreferences =
                c1.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE)
            val editor = sharedpreferences.edit()
            editor.putInt(SilenceMin, value)
            editor.apply()
        }

        fun getSnoozeMin(c1: Context): Int {
            val sharedpreferences = c1.getSharedPreferences(
                MyPREFERENCES,
                Context.MODE_PRIVATE
            )
            return sharedpreferences.getInt(SnoozeMin, 2)
        }

        fun setSnoozeMin(c1: Context, value: Int) {
            val sharedpreferences =
                c1.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE)
            val editor = sharedpreferences.edit()
            editor.putInt(SnoozeMin, value)
            editor.apply()
        }

        fun getVolume(c1: Context): Int {
            val sharedpreferences = c1.getSharedPreferences(
                MyPREFERENCES,
                Context.MODE_PRIVATE
            )
            return sharedpreferences.getInt(VOLUME, 10)
        }

        fun setVolume(c1: Context, value: Int) {
            val sharedpreferences =
                c1.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE)
            val editor = sharedpreferences.edit()
            editor.putInt(VOLUME, value)
            editor.apply()
        }


    }
}