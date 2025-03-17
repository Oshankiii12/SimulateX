package com.sim;

import java.util.Vector;

public class CircuitNode
{
    public int x, y;
    public final Vector links;
    public boolean internal;

    public CircuitNode()
    {
        links = new Vector();
    }
}
