package com.sim;

public class RowInfo
{
    public static final int ROW_NORMAL = 0; 
    public static final int ROW_CONST = 1;  
    public static final int ROW_EQUAL = 2;
    public int nodeEq, type, mapCol, mapRow;
    public double value;
    public boolean rsChanges; 
    public boolean lsChanges;
    public boolean dropRow; 

    public RowInfo()
    {
        type = ROW_NORMAL;
    }
}
