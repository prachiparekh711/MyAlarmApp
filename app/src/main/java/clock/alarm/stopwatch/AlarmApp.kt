package clock.alarm.stopwatch

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.media.Ringtone
import android.net.Uri
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.multidex.MultiDexApplication
import clock.alarm.stopwatch.data.AlarmData
import clock.alarm.stopwatch.data.PreferenceData
import clock.alarm.stopwatch.data.SoundData
import clock.alarm.stopwatch.data.TimerData
import clock.alarm.stopwatch.services.SleepReminderService
import clock.alarm.stopwatch.services.TimerService
import clock.alarm.stopwatch.utils.DebugUtils
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import java.util.*


class AlarmApp : MultiDexApplication(), Player.EventListener {
    /**
     * Get an instance of SharedPreferences.
     *
     * @return          The instance of SharedPreferences being used by the application.
     * @see [android.content.SharedPreferences Documentation]
     */
    var prefs: SharedPreferences? = null
        private set

    /**
     * @return the current SunriseSunsetCalculator object, or null if it cannot
     * be instantiated.
     * @see [SunriseSunsetLib Repo]
     */
    private var sunsetCalculator: SunriseSunsetCalculator? = null
        private get() {
            if (field == null && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                    val location = locationManager.getLastKnownLocation(
                        locationManager.getBestProvider(Criteria(), false)!!
                    )
                    field = SunriseSunsetCalculator(
                        Location(location!!.latitude, location.longitude),
                        TimeZone.getDefault().id
                    )
                } catch (ignored: NullPointerException) {
                }
            }
            return field
        }

    /**
     * Get the currently playing ringtone.
     *
     * @return          The currently playing ringtone, or null.
     */
    var currentRingtone: Ringtone? = null
        private set
    var alarms: ArrayList<AlarmData>? = null
    var timers: ArrayList<TimerData>? = null

    private var listeners: MutableList<AlarmioListener>? = null
    private var listener: ActivityListener? = null
    private var player: SimpleExoPlayer? = null
    private var hlsMediaSourceFactory: HlsMediaSource.Factory? = null
    private var progressiveMediaSourceFactory: ProgressiveMediaSource.Factory? = null
    private var currentStream: String? = null
    override fun onCreate() {
        super.onCreate()
        DebugUtils.setup(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        listeners = ArrayList()
        alarms = ArrayList<AlarmData>()
        timers = ArrayList<TimerData>()
        player = SimpleExoPlayer.Builder(this).build()
        player!!.addListener(this)
        val dataSourceFactory =
            DefaultDataSourceFactory(this, Util.getUserAgent(this, "exoplayer2example"), null)
        hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
        progressiveMediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val alarmLength: Int = PreferenceData.ALARM_LENGTH.getValue(this)
        for (id in 0 until alarmLength) {
            alarms!!.add(AlarmData(id, this))
        }
        val timerLength: Int = PreferenceData.TIMER_LENGTH.getValue(this)
        for (id in 0 until timerLength) {
            val timer = TimerData(id, this)
            if (timer.isSet) timers!!.add(timer)
        }
        if (timerLength > 0) startService(Intent(this, TimerService::class.java))
        SleepReminderService.refreshSleepTime(this)
    }


    /**
     * Create a new alarm, assigning it an unused preference id.
     *
     * @return          The newly instantiated [AlarmData](./data/AlarmData).
     */
    fun newAlarm(): AlarmData {
        val alarm = AlarmData(alarms!!.size, Calendar.getInstance())
        alarm.sound = SoundData.fromString(PreferenceData.DEFAULT_ALARM_RINGTONE.getValue(this, ""))
        alarms!!.add(alarm)
        onAlarmCountChanged()
        return alarm
    }

    /**
     * Remove an alarm and all of its its preferences.
     *
     * @param alarm     The alarm to be removed.
     */
    fun removeAlarm(alarm: AlarmData) {
        alarm.onRemoved(this)
        val index = alarms!!.indexOf(alarm)
        alarms!!.removeAt(index)
        for (i in index until alarms!!.size) {
            alarms!![i].onIdChanged(i, this)
        }
        onAlarmCountChanged()
        onAlarmsChanged()
    }

    /**
     * Update preferences to show that the alarm count has been changed.
     */
    fun onAlarmCountChanged() {
        PreferenceData.ALARM_LENGTH.setValue(this, alarms!!.size)
    }

    /**
     * Notify the application of changes to the current alarms.
     */
    fun onAlarmsChanged() {
        for (listener in listeners!!) {

            listener.onAlarmsChanged()
        }
    }

    /**
     * Create a new timer, assigning it an unused preference id.
     *
     * @return          The newly instantiated [TimerData](./data/TimerData).
     */
    fun newTimer(): TimerData {
        val timer = TimerData(timers!!.size)
        timers!!.add(timer)
        onTimerCountChanged()
        return timer
    }

    /**
     * Remove a timer and all of its preferences.
     *
     * @param timer     The timer to be removed.
     */
    fun removeTimer(timer: TimerData) {
        timer.onRemoved(this)
        val index = timers!!.indexOf(timer)
        timers!!.removeAt(index)
        for (i in index until timers!!.size) {
            timers!![i].onIdChanged(i, this)
        }
        onTimerCountChanged()
        onTimersChanged()
    }

    /**
     * Update the preferences to show that the timer count has been changed.
     */
    fun onTimerCountChanged() {
        PreferenceData.TIMER_LENGTH.setValue(this, timers!!.size)
    }

    /**
     * Notify the application of changes to the current timers.
     */
    fun onTimersChanged() {
        for (listener in listeners!!) {
            listener.onTimersChanged()
        }
    }

    /**
     * Starts the timer service after a timer has been set.
     */
    fun onTimerStarted() {
        startService(Intent(this, TimerService::class.java))
    }


    /**
     * Determine if the sunrise/sunset stuff should occur automatically.
     *
     * @return          True if the day/night stuff is automated.
     */
    val isDayAuto: Boolean
        get() = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && PreferenceData.DAY_AUTO.getValue(this)

    /**
     * @return the hour of the start of the day (24h), as specified by the user
     */
    val dayStart: Int
        get() = if (isDayAuto && sunsetCalculator != null) sunsetCalculator!!.getOfficialSunriseCalendarForDate(
            Calendar.getInstance()
        ).get(Calendar.HOUR_OF_DAY) else PreferenceData.DAY_START.getValue(this)

    /**
     * @return the hour of the end of the day (24h), as specified by the user
     */
    val dayEnd: Int
        get() = if (isDayAuto && sunsetCalculator != null) sunsetCalculator!!.getOfficialSunsetCalendarForDate(
            Calendar.getInstance()
        ).get(Calendar.HOUR_OF_DAY) else PreferenceData.DAY_END.getValue(this)

    /**
     * @return the hour of the calculated sunrise time, or null.
     */
    val sunrise: Int?
        get() = sunsetCalculator?.getOfficialSunsetCalendarForDate(
            Calendar.getInstance()
        )?.get(Calendar.HOUR_OF_DAY)

    /**
     * @return the hour of the calculated sunset time, or null.
     */
    val sunset: Int?
        get() = sunsetCalculator?.getOfficialSunsetCalendarForDate(
            Calendar.getInstance()
        )?.get(Calendar.HOUR_OF_DAY)

    /**
     * Determine if a ringtone is currently playing.
     *
     * @return          True if a ringtone is currently playing.
     */
    val isRingtonePlaying: Boolean
        get() = currentRingtone != null && currentRingtone!!.isPlaying

    fun playRingtone(ringtone: Ringtone) {
        if (!ringtone.isPlaying) {
            stopCurrentSound()
            ringtone.play()
        }
        currentRingtone = ringtone
    }

    /**
     * Play a stream ringtone.
     *
     * @param url       The URL of the stream to be passed to ExoPlayer.
     * @see [ExoPlayer Repo]
     */
    private fun playStream(url: String, type: String, factory: MediaSourceFactory) {
        stopCurrentSound()

        // Error handling, including when this is a progressive stream
        // rather than a HLS stream, is in onPlayerError
        player?.prepare(factory.createMediaSource(Uri.parse(url)))
        player?.playWhenReady = true
        currentStream = url
    }

    /**
     * Play a stream ringtone.
     *
     * @param url       The URL of the stream to be passed to ExoPlayer.
     * @see [ExoPlayer Repo]
     */
    fun playStream(url: String?, type: String?) {
        if (url != null) {
            if (type != null) {
                hlsMediaSourceFactory?.let { playStream(url, type, it) }
            }
        }
    }

    /**
     * Play a stream ringtone.
     *
     * @param url           The URL of the stream to be passed to ExoPlayer.
     * @param attributes    The attributes to play the stream with.
     * @see [ExoPlayer Repo]
     */
    fun playStream(url: String?, type: String?, attributes: AudioAttributes?) {
        player?.stop()
        if (attributes != null) {
            player?.audioAttributes = attributes
        }
        playStream(url, type)
    }

    /**
     * Stop the currently playing stream.
     */
    fun stopStream() {
        player?.stop()
        currentStream = null
    }

    /**
     * Sets the player volume to the given float.
     *
     * @param volume            The volume between 0 and 1
     */
    fun setStreamVolume(volume: Float) {
        player?.volume = volume
    }

    /**
     * Determine if the passed url matches the stream that is currently playing.
     *
     * @param url           The URL to match the current stream to.
     * @return              True if the URL matches that of the currently playing
     * stream.
     */
    fun isPlayingStream(url: String): Boolean {
        return currentStream != null && currentStream == url
    }

    /**
     * Stop the currently playing sound, regardless of whether it is a ringtone
     * or a stream.
     */
    fun stopCurrentSound() {
        if (isRingtonePlaying) currentRingtone!!.stop()
        stopStream()
    }

    fun addListener(listener: AlarmioListener) {
        listeners!!.add(listener)
    }

    fun removeListener(listener: AlarmioListener) {
        listeners!!.remove(listener)
    }

    fun setListener(listener: ActivityListener?) {
        this.listener = listener

    }


    override fun onLoadingChanged(isLoading: Boolean) {}
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING, Player.STATE_READY, Player.STATE_IDLE -> {}
            else -> currentStream = null
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {}
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
    override fun onPlayerError(error: ExoPlaybackException) {
        val lastStream = currentStream
        currentStream = null
        val exception: Exception
        exception = when (error.type) {
            ExoPlaybackException.TYPE_RENDERER -> error.rendererException
            ExoPlaybackException.TYPE_SOURCE -> {
                if (lastStream != null && error.sourceException.message!!.contains("does not start with the #EXTM3U header")) {
                    playStream(lastStream, SoundData.TYPE_RADIO, progressiveMediaSourceFactory!!)
                    return
                }
                error.sourceException
            }
            ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException
            else -> return
        }
        exception.printStackTrace()
        Toast.makeText(
            this,
            exception.javaClass.name + ": " + exception.message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onPositionDiscontinuity(reason: Int) {}
    override fun onSeekProcessed() {}
    fun requestPermissions(vararg permissions: String?) {
        if (listener != null) listener!!.requestPermissions(*permissions)
    }

    val fragmentManager: FragmentManager?
        get() = if (listener != null) listener!!.gettFragmentManager() else null

    interface AlarmioListener {
        fun onAlarmsChanged()
        fun onTimersChanged()
    }

    interface ActivityListener {
        fun requestPermissions(vararg permissions: String?)
        fun gettFragmentManager(): FragmentManager? //help
    }

    companion object {

        const val NOTIFICATION_CHANNEL_STOPWATCH = "stopwatch"
        const val NOTIFICATION_CHANNEL_TIMERS = "timers"
    }
}
