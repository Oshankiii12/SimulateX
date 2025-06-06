package com.sim.element;

import com.sim.view.edit.EditInfo;

import java.awt.*;
import java.util.StringTokenizer;

public class SwitchElm extends CircuitElm
{
    public static final String NAME = "Switch";

    public boolean momentary;
    // position 0 == closed, position 1 == open
    public int position, posCount;

    public SwitchElm(int xx, int yy)
    {
        super(xx, yy);
        momentary = false;
        position = 0;
        posCount = 2;
    }

    public SwitchElm(int xx, int yy, boolean mm)
    {
        super(xx, yy);
        position = (mm) ? 1 : 0;
        momentary = mm;
        posCount = 2;
    }

    public SwitchElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st)
    {
        super(xa, ya, xb, yb, f);
        String str = st.nextToken();
        if (str.compareTo("true") == 0)
            position = 1;
        else if (str.compareTo("false") == 0)
            position = 0;
        else
            position = Integer.parseInt(str);
        momentary = Boolean.parseBoolean(st.nextToken());
        posCount = 2;
    }

    public int getDumpType()
    {
        return DUMP_TYPE;
    }

    public static final int DUMP_TYPE = 's';


    public String dump()
    {
        return super.dump() + " " + position + " " + momentary;
    }

    public Point ps, ps2;

    public void setPoints()
    {
        super.setPoints();
        calcLeads(32);
        ps = new Point();
        ps2 = new Point();
    }

    public void draw(Graphics g)
    {
        int openhs = 16;
        int hs1 = (position == 1) ? 0 : 2;
        int hs2 = (position == 1) ? openhs : 2;
        setBbox(point1, point2, openhs);

        draw2Leads(g);

        if (position == 0)
            doDots(g);

        if (!needsHighlight())
            g.setColor(whiteColor);
        interpPoint(lead1, lead2, ps, 0, hs1);
        interpPoint(lead1, lead2, ps2, 1, hs2);

        drawThickLine(g, ps, ps2);
        drawPosts(g);
    }

    public void calculateCurrent()
    {
        if (position == 1)
            current = 0;
    }

    public void stamp()
    {
        if (position == 0)
            sim.stampVoltageSource(nodes[0], nodes[1], voltSource, 0);
    }

    public int getVoltageSourceCount()
    {
        return (position == 1) ? 0 : 1;
    }

    public void mouseUp()
    {
        if (momentary)
            toggle();
    }

    public void toggle()
    {
        position++;
        if (position >= posCount)
            position = 0;
    }

    public void getInfo(String arr[])
    {
        arr[0] = (momentary) ? "push switch (SPST)" : "switch (SPST)";
        if (position == 1)
        {
            arr[1] = "open";
            arr[2] = "Vd = " + getVoltageDText(getVoltageDiff());
        } else
        {
            arr[1] = "closed";
            arr[2] = "V = " + getVoltageText(volts[0]);
            arr[3] = "I = " + getCurrentDText(getCurrent());
        }
    }

    public boolean getConnection(int n1, int n2)
    {
        return position == 0;
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
            ei.checkbox = new Checkbox("Momentary Switch", momentary);
            return ei;
        }
        return null;
    }

    public void setEditValue(int n, EditInfo ei)
    {
        if (n == 0)
            momentary = ei.checkbox.getState();
    }
}
