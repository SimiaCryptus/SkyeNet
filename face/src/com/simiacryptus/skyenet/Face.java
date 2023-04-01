package com.simiacryptus.skyenet;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Face {
    public JPanel panel1;
    public JTextArea commandText;
    public JButton dictationButton;
    public JCheckBox autorunCheckbox;
    public JButton executeCodeButton;
    public JButton submitCommandButton;
    public JLabel label1;
    public JTextArea scriptedCommand;
    public JTextArea scriptingResult;

    public Face() {
        submitCommandButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        executeCodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        dictationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }
}
