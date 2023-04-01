package com.simiacryptus.skyenet

import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame

/**
 * The head is the primary interface to the user. It is responsible for
 * 1. Capturing audio via the Ears
 * 2. Interacting with the user via the Face
 * 3. Relaying commands to the Body
 */
class Head(
    val brain: Brain,
    val ears: Ears = Ears(brain),
    val body : Body = Body(brain),
    val face : Face = Face()
) {
    fun start() : JFrame {
        val frame = JFrame("SkyeNet - A Helpful Pup")
        val starting = AtomicReference(true)
        try {
            face.dictationButton.addActionListener {
                Thread {
                    face.dictationButton.isEnabled = false
                    val audioCaptureOn = AtomicBoolean(true)
                    try {
                        val buffer = ears.startAudioCapture { audioCaptureOn.get() }
                        buffer.clear()
                        ears.listenForCommand(rawBuffer=buffer) {
                            face.commandText.text = it
                            face.submitCommandButton.doClick()
                        }
                    } finally {
                        audioCaptureOn.set(false)
                        face.dictationButton.isEnabled = true
                    }
                }.start()
            }
            face.submitCommandButton.addActionListener {
                Thread {
                    face.submitCommandButton.isEnabled = false
                    try {
                        val commandToCode = body.commandToCode(
                            face.commandText.text
                        )
                        face.scriptedCommand.text = commandToCode.javascript
                        if(face.autorunCheckbox.isSelected) {
                            try {
                                val value = body.heart.run(
                                    commandToCode.javascript
                                )
                                face.scriptingResult.text = value.toString()
                            } catch (e: Exception) {
                                // print exception to string output stream
                                val byteArrayOutputStream = ByteArrayOutputStream()
                                val printStream = PrintStream(byteArrayOutputStream)
                                e.printStackTrace(printStream)
                                face.scriptingResult.text = byteArrayOutputStream.toString()
                            }
                        }
                    } finally {
                        face.submitCommandButton.isEnabled = true
                    }
                }.start()
            }
            face.executeCodeButton.addActionListener {
                Thread {
                    face.executeCodeButton.isEnabled = false
                    try {
                        body.heart.run(
                            face.scriptedCommand.text
                        )
                    } finally {
                        face.executeCodeButton.isEnabled = true
                    }
                }.start()
            }
            frame.contentPane = face.panel1
            frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE
            frame.pack()
            frame.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    face.panel1.revalidate()
                    face.panel1.repaint()
                }
            })
            frame.isVisible = true
            return frame
        } finally {
            starting.set(false)
        }
    }
}