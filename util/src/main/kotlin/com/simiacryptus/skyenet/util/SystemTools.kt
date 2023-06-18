package com.simiacryptus.skyenet.util

import com.simiacryptus.openai.HttpClientManager
import com.simiacryptus.util.describe.Description
import com.simiacryptus.skyenet.Mouth
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import java.io.File
import java.nio.charset.StandardCharsets
import javax.swing.*

class SystemTools(
    googleSpeechKey: String = File(File(System.getProperty("user.home")),"google_speech_api.key.json").absolutePath,
) {
    private val mouth = Mouth(googleSpeechKey)

    fun print(text: String) = println(text)

    @Description("Use text-to-speech for audio output")
    fun speak(text: String) = mouth.speak(text)

    @Description("Fetch a URL and return the contents as a string")
    fun get(url: String): String = _get(url)

    @Description("Prompt the user for input")
    fun prompt(text: String) = _prompt(text)

    companion object {
        fun _prompt(text: String) {
            val jFrame = JFrame()
            // Add a label, a single text field, and an OK button. Enter also triggers the OK button.
            val okButton = JButton("OK")
            val panel = JPanel()
            jFrame.contentPane = panel

            panel.add(JLabel(text))
            val textField = JTextField(20)
            panel.add(textField)
            panel.add(okButton)

            // Set up the action listener for the OK button and the Enter key.
            okButton.addActionListener {
                jFrame.dispose()
            }
            textField.addActionListener {
                okButton.doClick()
            }

            // Set up the JFrame and display it.
            jFrame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            jFrame.pack()
            jFrame.setLocationRelativeTo(null)
            jFrame.isVisible = true
        }

        fun _get(url: String): String {
            // Use Apache HTTP to fetch the URL
            val httpClient = HttpClientManager().getClient()
            val httpGet = HttpGet(url)
            val httpResponse = httpClient.execute(httpGet)
            val entity = httpResponse.entity
            val content = EntityUtils.toString(entity, StandardCharsets.UTF_8)
            return content
        }
    }

}