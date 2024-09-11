package com.example.mystt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mystt.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File

private lateinit var mainBinding: ActivityMainBinding
private var permissionGranted = false
class MainActivity : AppCompatActivity() {

    var wavObject : Wav? = null
    var tempWavFile: String = "final_record.wav"
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startRecording()
            }
            else{
                Snackbar.make(mainBinding.root,"Please give permission to record audio",Snackbar.LENGTH_SHORT).show()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        wavObject = Wav(this,application.cacheDir.path)
        mainBinding.startButton.setOnClickListener{
            checkForPermission()
            startRecording()
        }
        mainBinding.stopButton.setOnClickListener {
            stopRecording()
        }
        mainBinding.transcribeButton.setOnClickListener {
            try{
                val cacheFile = File(this.cacheDir,tempWavFile)

                val text = convertSpeechToText(this,cacheFile.path)
                mainBinding.textTV.text = text
                Log.i("MainActivity",text)
            }
            catch (e : Exception){
                Log.e("ConvertSpeechToText",e.message!!)
            }

        }
    }
    fun startRecording(){
        wavObject?.startRecording()
    }
    fun stopRecording(){
        wavObject?.stopRecording()
    }

    fun checkForPermission(){
        //check if permission already granted
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
        {
            return
        }
        else{
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

    }
}