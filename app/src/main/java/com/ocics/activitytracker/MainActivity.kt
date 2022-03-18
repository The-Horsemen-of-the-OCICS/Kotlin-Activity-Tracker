package com.ocics.activitytracker

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.ocics.activitytracker.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = "MainActivity"

    private var lastActivity = ""
    private var lastTransitionTime = System.currentTimeMillis()
    private val mediaPlayer = MediaPlayer()
    private val playlist = ArrayList<String>()
    private var songIndex = 0

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var db: DBHelper

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mPlacesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create view binding
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        db = DBHelper(this)

        // Init map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Places.initialize(this, BuildConfig.MAPS_API_KEY)
        mPlacesClient = Places.createClient(this)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkPermission())
            startServiceAndTasks()

        mediaPlayer.setOnCompletionListener {
            mediaPlayer.reset()
            playMusic()
        }
        playlist.addAll(getPlaylist())
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying){
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (!mediaPlayer.isPlaying && lastActivity == "Running"){
            mediaPlayer.start()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        stopService(Intent(this, ActivityRecognitionService::class.java))
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra("type")) {
            val type = intent.getIntExtra("type", -1)
            onActivityChanged(type)
        }
    }

    private fun checkPermission(): Boolean {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) + ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )+ ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1000
            )
            return false
        } else {
            return true
        }
    }

    private fun startServiceAndTasks() {
        Log.d(TAG, "startServiceAndTasks")
        stopServiceAndTasks()

        startService(Intent(this, ActivityRecognitionService::class.java))
        val pendingIntent = PendingIntent.getBroadcast(this, 2001,  Intent(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val task = ActivityRecognitionClient(this).requestActivityTransitionUpdates(getActivityTransitionRequest(),
            pendingIntent)

        task.run {
            addOnSuccessListener {
                Log.d(TAG, "requestActivityTransitionUpdates added")
            }
            addOnFailureListener {
                Log.d(TAG, "requestActivityTransitionUpdates failed")
            }
        }

    }

    private fun stopServiceAndTasks() {
        stopService(Intent(this, ActivityRecognitionService::class.java))
        val pendingIntent = PendingIntent.getBroadcast(this, 2001,  Intent(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val task = ActivityRecognitionClient(this).removeActivityTransitionUpdates(
            pendingIntent)

        task.run {
            addOnSuccessListener {
                Log.d(TAG, "removeActivityTransitionUpdates added")
            }
            addOnFailureListener {
                Log.d(TAG, "removeActivityTransitionUpdates failed")
            }
        }
    }


    private fun getActivityTransitionRequest(): ActivityTransitionRequest {
        val transitions = mutableListOf<ActivityTransition>()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        return ActivityTransitionRequest(transitions)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED) {
                        updateLocationUI()
                        getDeviceLocation()
                    }
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACTIVITY_RECOGNITION) ==
                        PackageManager.PERMISSION_GRANTED){
                        startServiceAndTasks()
                    }
                } else {
                    mBinding.activityText.text = getString(R.string.no_permission)
                }
            }
        }
    }

    private fun onActivityChanged(activityType: Int) {
        Log.d(TAG, "onActivityChanged: $activityType")
        val curActivity: String
        var curDrawable = AppCompatResources.getDrawable(this, R.drawable.unknown_icon)
        when (activityType) {
            DetectedActivity.STILL -> {
                curActivity = "Standing Still"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.still_icon)
            }
            DetectedActivity.WALKING -> {
                curActivity = "Walking"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.walk_icon)
            }
            DetectedActivity.RUNNING -> {
                curActivity = "Running"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.run_icon)
            }
            DetectedActivity.IN_VEHICLE -> {
                curActivity = "In Vehicle"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.vehicle_icon)
            }
            else -> {
                curActivity = "Unknown"
            }
        }
        // Activity changed
        if (lastActivity != curActivity) {
            if (curActivity == "Running") {
                playMusic()
            }
            saveActivity(curActivity)
            if (lastActivity != "") {
                val tempTime = System.currentTimeMillis()
                val millDiff = tempTime - lastTransitionTime
                val differenceInMinutes = millDiff / (60 * 1000) % 60
                val differenceInSeconds = millDiff / 1000 % 60
                Toast.makeText(
                    this,
                    "You were $lastActivity for $differenceInMinutes minutes $differenceInSeconds seconds",
                    Toast.LENGTH_LONG
                ).show()
                lastTransitionTime = tempTime
            }
            lastActivity = curActivity
            mBinding.activityText.text = getString(R.string.current_activity, curActivity)
            mBinding.activityImage.setImageDrawable(curDrawable)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateLocationUI()
        getDeviceLocation()
    }

    private fun updateLocationUI() {
        try {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        try {
            val locationResult = mFusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    Log.d(TAG, "Current location: ${task.result}")
                    val cameraPosition = CameraPosition.Builder().target(LatLng(task.result.latitude, task.result.longitude)).zoom(12f).build()
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                } else {
                    Log.d(TAG, "Current location is null.")
                }
            }
        } catch (e: Exception) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    // save data to SQLite
    private fun saveActivity(type: String) {
        val activity = Activity(type, SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).format(Calendar.getInstance().time))
        db.insertData(activity)
        Log.d(TAG, "Data: " + db.getAllData().joinToString("\n"))
    }

    // search music files in /music, support mp3 and flac files
    private fun getPlaylist(): Collection<String> {
        val list = ArrayList<String>()
        val args = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATA
        )
        val membersUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val songCursor: Cursor? = contentResolver.query(membersUri, args, null, null, null)
        val songIndex = 0 // PLAYING FROM THE FIRST SONG
        if (songCursor != null) {
            songCursor.moveToPosition(songIndex)
            while (!songCursor.isAfterLast) {
                if (songCursor.getString(3) == "audio/mpeg" || songCursor.getString(3) == "audio/flac")
                    list.add(songCursor.getString(4))

                songCursor.moveToNext()
            }

            songCursor.close()
        }

        Log.d("MusicDetected", playlist.count().toString())

        return list
    }

    // play local music files
    private fun playMusic() {
        if (playlist.isEmpty()) return
        val dataStream = playlist[songIndex]

        // Move index to next song
        songIndex += 1
        if (songIndex >= playlist.count())
            songIndex = 0

        try {
            mediaPlayer.setDataSource(dataStream)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

}