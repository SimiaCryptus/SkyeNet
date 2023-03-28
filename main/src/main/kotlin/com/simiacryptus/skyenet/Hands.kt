package com.simiacryptus.skyenet

import AutoScript
import java.lang.reflect.Method
import javax.swing.JOptionPane


object Hands {
    val utilityObjClass = Fingers.javaClass
    private val Method.toDesc: String
        get() {
            return "${this.name}(${
                this.parameters.joinToString(",") { 
                it.type.name + " " + it.name 
            }}): ${this.returnType.name}"
        }

    fun command(command: String) {
        val staticMethods = Class.forName(utilityObjClass.name).methods.filter { it.modifiers and java.lang.reflect.Modifier.STATIC != 0 }
        val predef = arrayOf("import ${utilityObjClass.name}._")
        val autoScript = AutoScript(predef)
        val symbolsDefined = autoScript.globalSymbols.toList() + staticMethods.map { it.toDesc }
        Brain.brain.create().commandToCode(Brain.AssistantCommand(command, symbolsDefined)).let {
            println(it.scala)
            val result = JOptionPane.showConfirmDialog(
                null,
                "Command: $command\nScala code: ${it.scala}",
                "Human Required...For Now",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
            )
            if (result == JOptionPane.OK_OPTION) {
                autoScript.run(it.scala)
            } else {
                println("Execution cancelled.")
            }
        }
    }


}

