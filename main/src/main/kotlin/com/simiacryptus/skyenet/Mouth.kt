package com.simiacryptus.skyenet

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.texttospeech.v1.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

@Suppress("MemberVisibilityCanBePrivate")
object Mouth {
    val keyfile: String = "C:\\Users\\andre\\code\\aicoder\\SkyeNet\\google_speech_api.key.json"

    private val client: TextToSpeechClient by lazy {
        val credentials =
            GoogleCredentials.fromStream(FileInputStream(keyfile))
        TextToSpeechClient.create(TextToSpeechSettings.newBuilder().setCredentialsProvider { credentials }.build())
    }

    suspend fun synthesizeAndPlay(text: String) {
        playAudio(synthesize(text).toByteArray())
    }

    suspend fun synthesize(text: String): ByteString {
        val input = SynthesisInput.newBuilder().setText(text).build()
        val voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode("en-US")
            .setSsmlGender(SsmlVoiceGender.FEMALE)
            .build()
        val audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.LINEAR16)
            .build()
        return withContext(Dispatchers.IO) {
            client.synthesizeSpeech(input, voice, audioConfig)
        }.audioContent
    }

    fun playAudio(audioData: ByteArray) {
        val audioFormat = AudioFormat(16000F, 16, 1, true, false)
        val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.use { sourceDataLine ->
            sourceDataLine.open(audioFormat)
            sourceDataLine.start()
            sourceDataLine.write(audioData, 0, audioData.size)
            sourceDataLine.drain()
            sourceDataLine.stop()
        }
    }
}