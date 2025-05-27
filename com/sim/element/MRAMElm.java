package com.sim.element;

import com.sim.view.edit.EditInfo;
import java.awt.*;
import java.util.StringTokenizer;

public class MRAMElm extends CircuitElm {
    public static final String NAME = "MRAM";
    public static final int DUMP_TYPE = 'l';

    public double r_parallel = 100;         // Low resistance state
    public double r_antiparallel = 500;     // High resistance state
    public double switchingCurrent = 0.001; // 1 mA threshold
    private double targetResistance;        // Current target resistance
    public double resistance;               // Actual instantaneous resistance
    public boolean isParallel;              // Current magnetic state

    public MRAMElm(int xx, int yy) {
        super(xx, yy);
        resistance = r_parallel;
        targetResistance = r_parallel;
        isParallel = true;
    }

    public MRAMElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
        super(xa, ya, xb, yb, f);
        r_parallel = Double.parseDouble(st.nextToken());
        r_antiparallel = Double.parseDouble(st.nextToken());
        switchingCurrent = Double.parseDouble(st.nextToken());
        isParallel = Boolean.parseBoolean(st.nextToken());
        resistance = isParallel ? r_parallel : r_antiparallel;
        targetResistance = resistance;
    }

    public String dump() {
        return super.dump() + " " + r_parallel + " " + r_antiparallel + " " 
            + switchingCurrent + " " + isParallel;
    }

    public void draw(Graphics g) {
        draw2Leads(g);
        setBbox(point1, point2, 20);
        
        // Draw MRAM symbol
        int hs = 12;
        Point center = interpPoint(point1, point2, 0.5);
        Point boxTopLeft = new Point(center.x - hs, center.y - hs);
        
        // Draw box
        boolean highlighted = needsHighlight();
        g.setColor(highlighted ? selectColor : Color.GRAY);
        g.fillRect(boxTopLeft.x, boxTopLeft.y, hs*2, hs*2);
        g.setColor(Color.BLACK);
        g.drawRect(boxTopLeft.x, boxTopLeft.y, hs*2, hs*2);
        
        // Draw state indicator (arrow direction)
        int arrSize = 8;
        if (isParallel) {
            drawArrow(g, center.x - arrSize/2, center.y, center.x + arrSize/2, center.y);
        } else {
            drawArrow(g, center.x, center.y - arrSize/2, center.x, center.y + arrSize/2);
        }
        
     // Draw data bit (0 for parallel, 1 for anti-parallel)
        String bit = isParallel ? "0" : "1";
        Color textColor = highlighted ? Color.BLACK : Color.WHITE;
        g.setColor(textColor);
        Font oldFont = g.getFont();
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(bit);
        int textHeight = fm.getAscent();
        // Center the text in the box
        int x = center.x - textWidth / 2;
        int y = center.y - textHeight / 2 + fm.getAscent();
        g.drawString(bit, x, y);
        g.setFont(oldFont); // Restore original font
        
        drawPosts(g);
        doDots(g);
    }
    
    private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
        // Add arrowhead
        Polygon p = new Polygon();
        p.addPoint(x2, y2);
        if (x1 == x2) { // Vertical arrow
            p.addPoint(x2-2, y2 - 3);
            p.addPoint(x2+2, y2 - 3);
        } else { // Horizontal arrow
            p.addPoint(x2 - 3, y2-2);
            p.addPoint(x2 - 3, y2+2);
        }
        g.fillPolygon(p);
    }
    
    public void setPoints() {
        super.setPoints();
        calcLeads(32); // standard lead length
        setBbox(point1, point2, 20);
    }

    public void calculateCurrent() {
        current = (volts[0] - volts[1]) / resistance;
    }

    public void startIteration() {
        double i = getCurrent();
        
        // Determine target resistance based on current direction and threshold
        if (i > switchingCurrent) {
            targetResistance = r_antiparallel;
        } else if (i < -switchingCurrent) {
            targetResistance = r_parallel;
        }
        
        // Smooth resistance transition towards target
        double blend = 0.05;
        resistance += (targetResistance - resistance) * blend;
        
        // Update state when close to target
        if (Math.abs(resistance - targetResistance) < 1e-6) {
            isParallel = (targetResistance == r_parallel);
        }
    }

    public void updateResistance() {
        resistance = isParallel ? r_parallel : r_antiparallel;

    	System.out.println(dump());
    }

    public void stamp() {
        sim.stampNonLinear(nodes[0]);
        sim.stampNonLinear(nodes[1]);
    }

    public void doStep() {
        sim.stampResistor(nodes[0], nodes[1], resistance);
    }

    public boolean nonLinear() {
        return true;
    }

    public void getInfo(String[] arr) {
        arr[0] = "MRAM Cell";
        getBasicInfo(arr);
        arr[3] = "R = " + getUnitText(resistance, sim.ohmString);
        arr[4] = "State: " + (isParallel ? "Parallel" : "Anti-parallel");
        arr[5] = "Ith = " + getUnitText(switchingCurrent, "A");
    }

    public double getScopeValue(int x) {
        return (x == 2) ? resistance : (x == 1) ? getPower() : getVoltageDiff();
    }

    public String getScopeUnits(int x) {
        return (x == 2) ? sim.ohmString : (x == 1) ? "W" : "V";
    }

    public EditInfo getEditInfo(int n) {
        if (n == 0) return new EditInfo("R (Parallel)", r_parallel, 0, 0);
        if (n == 1) return new EditInfo("R (Anti-parallel)", r_antiparallel, 0, 0);
        if (n == 2) return new EditInfo("Switching Current (A)", switchingCurrent, 0, 0);
        return null;
    }

    public void setEditValue(int n, EditInfo ei) {
        if (n == 0) r_parallel = ei.value;
        if (n == 1) r_antiparallel = ei.value;
        if (n == 2) switchingCurrent = ei.value;
        updateResistance();
    }

    public int getDumpType() {
        return DUMP_TYPE;
    }
}
