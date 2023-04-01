package com.simiacryptus.skyenet;

import org.junit.jupiter.api.Test;

import javax.swing.*;

class UnitTests {

    @Test
    void testFace() throws InterruptedException {
        Face face = new Face();
        JFrame frame = new JFrame("SkyeNet - A Helpful Pup");
        frame.setContentPane(face.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        if (!frame.isVisible()) throw new AssertionError();
        while (frame.isVisible()) {
            Thread.sleep(100);
        }
    }

}