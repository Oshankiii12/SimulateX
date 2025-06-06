package com.sim.view.edit;

import java.awt.*;

public class EditInfo
{
    public EditInfo(String n, double val, double mn, double mx)
    {
        name = n;
        value = val;
        if (mn == 0 && mx == 0 && val > 0)
        {
            minval = 1e10;
            while (minval > val / 100)
                minval /= 10.;
            maxval = minval * 1000;
        } else
        {
            minval = mn;
            maxval = mx;
        }
        forceLargeM = name.indexOf("(ohms)") > 0 || name.indexOf("(Hz)") > 0;
        dimensionless = false;
    }

    public EditInfo setDimensionless()
    {
        dimensionless = true;
        return this;
    }

    public final String name;
    public String text;
    public double value;
    public double minval;
    public final double maxval;
    public TextField textf;
    public Scrollbar bar;
    public Choice choice;
    public Checkbox checkbox;
    public boolean newDialog;
    public final boolean forceLargeM;
    public boolean dimensionless;
}
