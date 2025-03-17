package com.sim.element;

import com.sim.view.edit.EditInfo;

import java.awt.*;
import java.util.StringTokenizer;

public class WireElm extends CircuitElm
{
    public static final String NAME = "Wire";

    public WireElm(int xx, int yy)
    {
        super(xx, yy);
    }

    public WireElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st)
    {
        super(xa, ya, xb, yb, f);
    }

    public static final int FLAG_SHOWCURRENT = 1;
    public static final int FLAG_SHOWVOLTAGE = 2;

    public void draw(Graphics g)
    {
        setVoltageColor(g, volts[0]);
        drawThickLine(g, point1, point2);
        doDots(g);
        setBbox(point1, point2, 3);
        if (mustShowCurrent())
        {
            String s = getShortUnitText(Math.abs(getCurrent()), "A");
            drawValues(g, s, 4);
        } else if (mustShowVoltage())
        {
            String s = getShortUnitText(volts[0], "V");
            drawValues(g, s, 4);
        }
        drawPosts(g);
    }

    public void stamp()
    {
        sim.stampVoltageSource(nodes[0], nodes[1], voltSource, 0);
    }

    public boolean mustShowCurrent()
    {
        return (flags & FLAG_SHOWCURRENT) != 0;
    }

    public boolean mustShowVoltage()
    {
        return (flags & FLAG_SHOWVOLTAGE) != 0;
    }

    public int getVoltageSourceCount()
    {
        return 1;
    }

    public void getInfo(String arr[])
    {
        arr[0] = "wire";
        arr[1] = "I = " + getCurrentDText(getCurrent());
        arr[2] = "V = " + getVoltageText(volts[0]);
    }

    public int getDumpType()
    {
        return DUMP_TYPE;
    }

    public static final int DUMP_TYPE = 'w';


    public double getPower()
    {
        return 0;
    }

    public double getVoltageDiff()
    {
        return volts[0];
    }

    public boolean isWire()
    {
        return true;
    }

    public EditInfo getEditInfo(int n)
    {
        if (n == 0)
        {
            EditInfo ei = new EditInfo("", 0, -1, -1);
            ei.checkbox = new Checkbox("Show Current", mustShowCurrent());
            return ei;
        }
        if (n == 1)
        {
            EditInfo ei = new EditInfo("", 0, -1, -1);
            ei.checkbox = new Checkbox("Show Voltage", mustShowVoltage());
            return ei;
        }
        return null;
    }

    public void setEditValue(int n, EditInfo ei)
    {
        if (n == 0)
        {
            if (ei.checkbox.getState())
                flags = FLAG_SHOWCURRENT;
            else
                flags &= ~FLAG_SHOWCURRENT;
        }
        if (n == 1)
        {
            if (ei.checkbox.getState())
                flags = FLAG_SHOWVOLTAGE;
            else
                flags &= ~FLAG_SHOWVOLTAGE;
        }
    }

    public boolean hasHotkey()
    {
        return true;
    }
}
