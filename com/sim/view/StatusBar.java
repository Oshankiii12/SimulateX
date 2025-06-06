package com.sim.view;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel
{

    public StatusBar()
    {

        class AngledLinesWindowsCornerIcon implements Icon
        {
            private static final int WIDTH = 13;

            private static final int HEIGHT = 13;

            public int getIconHeight()
            {
                return WIDTH;
            }

            public int getIconWidth()
            {
                return HEIGHT;
            }

            public void paintIcon(Component c, Graphics g, int x, int y)
            {

                g.setColor(new Color(255, 255, 255));
                g.drawLine(0, 12, 12, 0);
                g.drawLine(5, 12, 12, 5);
                g.drawLine(10, 12, 12, 10);

                g.setColor(new Color(172, 168, 153));
                g.drawLine(1, 12, 12, 1);
                g.drawLine(2, 12, 12, 2);
                g.drawLine(3, 12, 12, 3);

                g.drawLine(6, 12, 12, 6);
                g.drawLine(7, 12, 12, 7);
                g.drawLine(8, 12, 12, 8);

                g.drawLine(11, 12, 12, 11);
                g.drawLine(12, 12, 12, 12);

            }
        }

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(10, 23));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel(new AngledLinesWindowsCornerIcon()), BorderLayout.SOUTH);
        rightPanel.setOpaque(false);

        add(rightPanel, BorderLayout.EAST);
        setBackground(SystemColor.control);
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        int y = 0;
        g.setColor(new Color(156, 154, 140));
        g.drawLine(0, y, getWidth(), y);
        y++;
        g.setColor(new Color(196, 194, 183));
        g.drawLine(0, y, getWidth(), y);
        y++;
        g.setColor(new Color(218, 215, 201));
        g.drawLine(0, y, getWidth(), y);
        y++;
        g.setColor(new Color(233, 231, 217));
        g.drawLine(0, y, getWidth(), y);

        y = getHeight() - 3;
        g.setColor(new Color(233, 232, 218));
        g.drawLine(0, y, getWidth(), y);
        y++;
        g.setColor(new Color(233, 231, 216));
        g.drawLine(0, y, getWidth(), y);
        y = getHeight() - 1;
        g.setColor(new Color(221, 221, 220));
        g.drawLine(0, y, getWidth(), y);

    }

}
