package com.simiacryptus.skyenet

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.texttospeech.v1.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * The mouth is the interface to the Google Text-to-Speech API for the SkyeNet system
 */
@Suppress("MemberVisibilityCanBePrivate")
open class Mouth(
    val keyfile: String
) {

    fun speak(text: String) {
        runBlocking {
            synthesizeAndPlay("""<speak><break time="1s"/>$text</speak>""")
        }
    }

    protected open val client: TextToSpeechClient by lazy {
        val credentials =
            GoogleCredentials.fromStream(FileInputStream(keyfile))
        TextToSpeechClient.create(TextToSpeechSettings.newBuilder().setCredentialsProvider { credentials }.build())
    }

    suspend fun synthesizeAndPlay(ssml: String) {
        playAudio(synthesize(ssml).toByteArray())
    }

    suspend fun synthesize(ssml: String): ByteString {
        val input = SynthesisInput.newBuilder().setSsml(ssml).build()
        val voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode("en-US")
            .setSsmlGender(SsmlVoiceGender.FEMALE)
            .build()
        val audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.LINEAR16)
            .build()
        val audioContent = withContext(Dispatchers.IO) {
            client.synthesizeSpeech(input, voice, audioConfig)
        }.audioContent
        return audioContent
    }

    fun playAudio(audioData: ByteArray) {
        val audioFormat = AudioFormat(22050F, 16, 1, true, false)
        val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.use { sourceDataLine ->
            sourceDataLine.open(audioFormat)
            sourceDataLine.start()
            val wavHeaderSize = 44 // The size of a standard WAV header is 44 bytes
            sourceDataLine.write(audioData, wavHeaderSize, audioData.size - wavHeaderSize)
            sourceDataLine.drain()
            sourceDataLine.stop()
        }
    }
}