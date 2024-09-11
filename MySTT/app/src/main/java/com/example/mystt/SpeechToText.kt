package com.example.mystt

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.jlibrosa.audio.JLibrosa
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.IntBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

var sampleRate: Int = 16000
val DEFAULT_AUDIO_DURATION = -1

fun convertSpeechToText(context : Context, cacheFilePath : String): String
{
    var tfLiteModel: MappedByteBuffer? = null
    var tfLite: Interpreter? = null
    val TFLITE_FILE = "CONFORMER.tflite"

    val jLibrosa = JLibrosa()
    try {
        val audioFeatureValues: FloatArray = jLibrosa.loadAndRead(
            cacheFilePath,
            sampleRate,
            DEFAULT_AUDIO_DURATION
        )

        val inputArray = arrayOf<Any>(audioFeatureValues)
        val outputBuffer = IntBuffer.allocate(2000)

        val outputMap: MutableMap<Int, Any> = HashMap()
        outputMap[0] = outputBuffer

        tfLiteModel = loadModelFile(context.assets, TFLITE_FILE)
        val tfLiteOptions = Interpreter.Options()
        tfLite = Interpreter(tfLiteModel?:throw IllegalArgumentException(), tfLiteOptions)
        tfLite.resizeInput(0, intArrayOf(audioFeatureValues.size))

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap)

        val outputSize: Int = tfLite.getOutputTensor(0).shape().get(0)
        val outputArray = IntArray(outputSize)
        outputBuffer.rewind()
        outputBuffer[outputArray]
        val finalResult = StringBuilder()
        for (i in 0 until outputSize) {
            val c = outputArray[i].toChar()
            if (outputArray[i] != 0) {
                finalResult.append(outputArray[i].toChar())
            }
        }
//        resultTextview.setText(finalResult.toString())
        return finalResult.toString()
    } catch (e: Exception) {
        Log.e("ConvertSpeechToText", e.message!!)
        return "error"
    }
}
@Throws(IOException::class)
fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer? {
    try{
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }catch (e : Exception)
    {
        Log.e("ConvertSpeechToText", e.message!!)
        return null
    }

}