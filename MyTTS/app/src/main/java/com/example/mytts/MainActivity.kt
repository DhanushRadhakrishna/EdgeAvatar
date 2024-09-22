package com.example.mytts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mytts.databinding.ActivityMainBinding
import java.util.Locale

private lateinit var mainBinding : ActivityMainBinding
class MainActivity : AppCompatActivity() {
    private var myTTS : TextToSpeech? = null
    private lateinit var textToSpeak : String

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
        textToSpeak = mainBinding.editText.text.toString()

        val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if(it.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
            {
                myTTS = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
                    if(status == TextToSpeech.SUCCESS)
                    {
                        Log.d("MainActivity","TextToSpeechSuccess")
                    }
                    else{
                        Log.d("MainActivity","TTSFailture")
                    }
                })
            }
            else{
                Log.d("MainActivity","NeedToInstallData")
            }
        }
        val intent = Intent()
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)

        getResult.launch(intent)
    }
    fun onSpeak(view : View)
    {
        if(view.id == R.id.speakButton){
        textToSpeak = mainBinding.editText.text.toString()
            if(textToSpeak=="") {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show()
            }
            else{
                myTTS?.setLanguage(Locale.ENGLISH)
                myTTS?.speak(textToSpeak,TextToSpeech.QUEUE_FLUSH,null)
            }
        }
    }
}