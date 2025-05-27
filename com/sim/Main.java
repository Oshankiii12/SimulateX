package com.sim;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;

public class Main
{
    public static void main(String[] args) {
    try {
        // Iterate over all installed LookAndFeels in the system
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            // Check if the LookAndFeel name is "Nimbus"
            if ("Nimbus".equals(info.getName())) {
                // Set the LookAndFeel to "Nimbus"
                UIManager.setLookAndFeel(info.getClassName());
                break; // Exit the loop once Nimbus LookAndFeel is set
            }
        }
    } catch (Exception e) {
        // Handle any exceptions that occur while setting the LookAndFeel
        e.printStackTrace();
    }

    // Create a new JWindow to act as a splash screen
    JWindow splash = new JWindow();

    // Load the splash image from a file
    ImageIcon splashImage = new ImageIcon("spash.png"); // Ensure the path is correct
    JLabel imageLabel = new JLabel(splashImage);

    // Add the splash image to the center of the splash window
    splash.getContentPane().add(imageLabel, BorderLayout.CENTER);

    // Set the size of the splash window to match the size of the image
    splash.setSize(splashImage.getIconWidth(), splashImage.getIconHeight());

    // Get the size of the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    // Calculate the x and y coordinates to center the splash window on the screen
    int x = (screenSize.width - splash.getWidth()) / 2;
    int y = (screenSize.height - splash.getHeight()) / 2;

    // Set the location of the splash window to the calculated coordinates
    splash.setLocation(x, y);

    // Make the splash window visible on the screen
    splash.setVisible(true);

    // Simulate loading time by pausing the execution for 5 seconds
    try {
        Thread.sleep(5000); // Display the splash screen for 5 seconds
    } catch (InterruptedException e) {
        // Handle any interruptions during the sleep
        e.printStackTrace();
    }

    // Hide the splash screen and release its resources
    splash.setVisible(false);
    splash.dispose();

    // Initialize and display the main application window
    SwingUtilities.invokeLater(() -> {
        // Create an instance of the CirSim application
        CirSim myFrame = new CirSim();

        // Initialize the CirSim application
        myFrame.init();

        // Maximize the application window
        myFrame.setExtendedState(Frame.MAXIMIZED_BOTH);

        // Set the default close operation for the window
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Make the application window visible
        myFrame.setVisible(true);
    });
}
}