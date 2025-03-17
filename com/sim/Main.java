package com.sim;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;

public class Main
{
    public static void main(String[] args)
    {
        try
        {
            // for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            // {
            //     if ("Nimbus".equals(info.getName()))
            //     {
            //         UIManager.setLookAndFeel(info.getClassName());
            //         break;
            //     }
            // }
        } catch (Exception e)
        {}
        JWindow splash = new JWindow();
        // Load the splash image
        ImageIcon splashImage = new ImageIcon("spash.png"); // Ensure the path is correct
        JLabel imageLabel = new JLabel(splashImage);
        splash.getContentPane().add(imageLabel, BorderLayout.CENTER);

        // Set window size to match the image
        splash.setSize(splashImage.getIconWidth(),splashImage.getIconHeight());

        // Center the splash screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - splash.getWidth()) / 2;
        int y = (screenSize.height - splash.getHeight()) / 2;
        splash.setLocation(x, y);

        // Show splash screen
        splash.setVisible(true);

        // Simulate loading time
        try {
            Thread.sleep(5000); // Display for 3 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Close splash screen
        splash.setVisible(false);
        splash.dispose();

        // Launch main application
        System.out.println(splashImage.getIconWidth());
       SwingUtilities.invokeLater(() -> {
        CirSim myFrame = new CirSim();
        myFrame.init();
        myFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myFrame.setVisible(true);
       });
       
    }
}