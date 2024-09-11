package com.example.mystt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

public class Wav(val context : Context,val filePath : String) {

    var bufferSize = 0
    var recorder: AudioRecord? = null
    var sampleRate: Int = 16000
    var channel: Int = AudioFormat.CHANNEL_IN_STEREO
    var audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT
    var isRecording: Boolean = false
    val coroutine  = CoroutineScope(Dispatchers.IO)
    var recordingThread: Thread? = null
    val bpp: Int = 16
    var tempRawFile: String = "temp_record.raw"
    var tempWavFile: String = "final_record.wav"
    init {
        try{
            bufferSize = AudioRecord.getMinBufferSize(
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

        }catch (e : Exception)
        {
            Log.i("WavFileGenerator",e.message?:"some error in buffer size")
        }
    }

    private fun getPath(name: String): String {
        return filePath + "/" + name

    }
    public fun startRecording()
    {
        try {
            if(ActivityCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            {
                return
            }
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channel,
                audioEncoding,
                bufferSize
            )
            val status: Int = recorder!!.getState()
            if (status == 1) {
                recorder!!.startRecording()
                isRecording = true
            }
            recordingThread = Thread { this.writeRawData() }
            recordingThread!!.start()

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    private fun writeRawData() {
        try {
            if (filePath != null) {
                val data = ByteArray(bufferSize)
                val path = getPath(tempRawFile)
                val fileOutputStream = FileOutputStream(path)
                if (fileOutputStream != null) {
                    var read: Int
                    while (isRecording) {
                        read = recorder!!.read(data, 0, bufferSize)
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                val cacheFile = File(context.cacheDir,tempWavFile)
                                if(cacheFile.exists())
                                {
                                    cacheFile.delete()
                                }
                                fileOutputStream.write(data)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                fileOutputStream.close()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        try {
            if (recorder != null) {
                isRecording = false
                val status = recorder!!.state
                if (status == 1) {
                    recorder!!.stop()
                }
                recorder!!.release()
//                coroutine.cancel()
                recordingThread = null
                createWavFile(getPath(tempRawFile),getPath(tempWavFile))
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun wavHeader(fileOutputStream: FileOutputStream, totalAudioLen: Long, totalDataLen: Long, channels: Int, byteRate: Long) {
        try {
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte() // RIFF/WAVE header
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xffL).toByte()
            header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
            header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
            header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte() // 'fmt ' chunk
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 4 bytes: size of 'fmt ' chunk
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // format = 1
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate.toLong() and 0xffL).toByte()
            header[25] = ((sampleRate.toLong() shr 8) and 0xffL).toByte()
            header[26] = ((sampleRate.toLong() shr 16) and 0xffL).toByte()
            header[27] = ((sampleRate.toLong() shr 24) and 0xffL).toByte()
            header[28] = (byteRate and 0xffL).toByte()
            header[29] = ((byteRate shr 8) and 0xffL).toByte()
            header[30] = ((byteRate shr 16) and 0xffL).toByte()
            header[31] = ((byteRate shr 24) and 0xffL).toByte()
            header[32] = (2 * 16 / 8).toByte() // block align
            header[33] = 0
            header[34] = bpp.toByte() // bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xffL).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()
            fileOutputStream.write(header, 0, 44)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun createWavFile(tempPath : String, wavPath: String) {
        try{
            val fileInputStream: FileInputStream = FileInputStream(tempPath)
            val fileOutputStream: FileOutputStream = FileOutputStream(wavPath)
            val data = ByteArray(bufferSize)
            val channels = 2
            val byteRate: Long = (bpp * sampleRate * channels / 8).toLong()
            val totalAudioLen: Long = fileInputStream.getChannel().size()
            val totalDataLen = totalAudioLen + 36
            wavHeader(fileOutputStream, totalAudioLen, totalDataLen, channels, byteRate)
            while (fileInputStream.read(data) != -1) {
                fileOutputStream.write(data)
            }
            fileInputStream.close()
            fileOutputStream.close()
        } catch (e : Exception) {
            e.printStackTrace();
        }
    }
}