package com.sim.element;

import com.sim.CirSim;
import com.sim.view.edit.EditInfo;
import com.sim.view.edit.Editable;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public abstract class CircuitElm implements Editable
{
    public static String NAME = "none";

    public static double voltageRange = 5;
    public static final int colorScaleCount = 32;
    public static Color colorScale[];
    public static double currentMult, powerMult;
    public static Point ps1, ps2;
    public static CirSim sim;
    public static Color whiteColor, selectColor, lightGrayColor, Blue;
    public static Font unitsFont;

    public static NumberFormat showFormat, shortFormat, noCommaFormat;
    public static final double pi = 3.14159265358979323846;

    public int x, y, x2, y2, flags, nodes[], voltSource;
    public int dx, dy, dsign;
    public double dn, dpx1, dpy1;
    public Point point1, point2, lead1, lead2;
    public double volts[];
    public double current, curcount;
    public Rectangle boundingBox;
    public boolean noDiagonal;
    public boolean selected;

    /**
     * Returns the dump type for the circuit element.
     * The dump type is a unique identifier that signifies the type 
     * of the element when serializing or dumping the circuit state.
     * 
     * @return the integer constant DUMP_TYPE, representing the type of this element
     */
    public int getDumpType()
    {
        return DUMP_TYPE;
    }

    public static final int DUMP_TYPE = 0;


    public Class getDumpClass()
    {
        return getClass();
    }

    /**
     * Returns the default flags for this element.
     * The flags are a bitwise combination of values that signify certain
     * properties of the element. The default flags are used when creating
     * a new element of this type.
     * 
     * @return the default flags for this element
     */
    public int getDefaultFlags()
    {
        // By default, all elements have no special properties, so
        // we return 0.
        return 0;
    }

    /**
     * Initializes the CircuitElm class by setting up the color scale used
     * for drawing the circuit elements. The color scale is a gradient that
     * ranges from blue to red and is used to represent the voltage levels
     * of the elements.
     * 
     * @param s the CirSim instance that created this class
     */
    public static void initClass(CirSim s)
    {
        // Set up the font used for displaying units (e.g. "V" for voltage).
        unitsFont = new Font("Calgary", 0, 10);

        // Save the CirSim instance in a static variable so that we can access
        // it later.
        sim = s;

        // Set up the color scale. The color scale is an array of colors that
        // represents the voltage levels of the elements. The colors range from
        // blue to red, with blue representing negative voltages and red
        // representing positive voltages.
        colorScale = new Color[colorScaleCount];
        for (int i = 0; i != colorScaleCount; i++)
        {
            // Calculate the voltage level represented by this color.
            double v = i * 2. / colorScaleCount - 1;

            // If the voltage is negative, create a color that is a shade of blue.
            // The shade of blue will get lighter as the voltage approaches zero.
            if (v < 0)
            {
                int n1 = (int) (128 * -v) + 127;
                int n2 = (int) (127 * (1 + v));
                colorScale[i] = new Color(n1, n2, n2);
            }
            // If the voltage is positive, create a color that is a shade of red.
            // The shade of red will get lighter as the voltage approaches zero.
            else
            {
                int n1 = (int) (128 * v) + 127;
                int n2 = (int) (127 * (1 - v));
                colorScale[i] = new Color(n2, n1, n2);
            }
        }

        // Set up two points that are used for drawing the circuit elements.
        // The first point is used to draw the element's bounding box, and
        // the second point is used to draw the element's pins.
        ps1 = new Point();
        ps2 = new Point();

        // Set up the format used to display numbers in the circuit.
        showFormat = DecimalFormat.getInstance();
        showFormat.setMaximumFractionDigits(2);
        shortFormat = DecimalFormat.getInstance();
        shortFormat.setMaximumFractionDigits(1);
        noCommaFormat = DecimalFormat.getInstance();
        noCommaFormat.setMaximumFractionDigits(10);
        noCommaFormat.setGroupingUsed(false);
    }

    public CircuitElm(int xx, int yy)
    {
        x = x2 = xx;
        y = y2 = yy;
        flags = getDefaultFlags();
        allocNodes();
        initBoundingBox();
    }

    public CircuitElm(int xa, int ya, int xb, int yb, int f)
    {
        x = xa;
        y = ya;
        x2 = xb;
        y2 = yb;
        flags = f;
        allocNodes();
        initBoundingBox();
    }

    /**
     * Initializes the bounding box for this element.
     * The bounding box is a rectangle that represents the area
     * occupied by the element. It is used to determine whether
     * the element is visible on the screen and to calculate the
     * position of the element's pins.
     */
    public void initBoundingBox()
    {
        // Create a new rectangle and set its bounds to the minimum
        // of the x and y coordinates, and the absolute difference
        // between the x and y coordinates, plus one. This will
        // ensure that the bounding box is always at least one pixel
        // in size.
        boundingBox = new Rectangle();
        boundingBox.setBounds(
            Math.min(x, x2),
            Math.min(y, y2),
            Math.abs(x2 - x) + 1,
            Math.abs(y2 - y) + 1
        );
    }

    /**
     * Allocates the arrays used to store the nodes and voltage values of
     * this element.
     * 
     * The nodes array stores the node numbers of the element's pins and
     * internal nodes. The volts array stores the voltage values of the
     * element's pins and internal nodes.
     * 
     * The size of the arrays is determined by the getPostCount() and
     * getInternalNodeCount() methods, which return the number of pins and
     * internal nodes, respectively.
     */
    public void allocNodes()
    {
        // Allocate the nodes array, which stores the node numbers of the
        // element's pins and internal nodes.
        nodes = new int[getPostCount() + getInternalNodeCount()];

        // Allocate the volts array, which stores the voltage values of the
        // element's pins and internal nodes.
        volts = new double[getPostCount() + getInternalNodeCount()];
    }

    /**
     * Used for import\export.
     * Saves element ID
     * 
     * The element ID is stored as either a single character or an integer
     * value. If the element ID is less than 127, it is stored as a single
     * character.
     * 
     * The element ID is followed by the element's coordinates (x, y, x2, y2)
     * and its flags.
     */
    public String dump()
    {
        int type = getDumpType();

        // If the element ID is less than 127, store it as a character
        String dump = (type < 127 ? ((char) type) + " " : type + " ");

        // Append the element's coordinates
        dump += x + " " + y + " " + x2 + " " + y2 + " ";

        // Append the element's flags
        dump += flags;

        return dump;
    }

    public void reset()
    {
        // Initialize a loop variable
        int i;
        
        // Iterate over all the nodes, including both post and internal nodes
        for (i = 0; i != getPostCount() + getInternalNodeCount(); i++)
            // Set the voltage of each node to 0
            volts[i] = 0;
        
        // Reset the current count to 0
        curcount = 0;
    }

    public void draw(Graphics g)
    {
    }

    /**
     * Set the current of the circuit element. This method is used to set the
     * current of the circuit element to a specific value.
     * 
     * @param x unused parameter
     * @param c the current to set
     */
    public void setCurrent(int x, double c)
    {
        // Set the current of the circuit element
        current = c;
    }

    /**
     * Gets the current of the circuit element.
     * 
     * This method returns the current of the circuit element in amperes.
     * 
     * @return the current of the circuit element in amperes
     */
    public double getCurrent()
    {
        // Return the current of the circuit element
        return current;
    }

    public void doStep()
    {
    }

    public void delete()
    {
    }

    public void startIteration()
    {
    }

    public double getPostVoltage(int x)
    {
        return volts[x];
    }

    public void setNodeVoltage(int n, double c)
    {
        volts[n] = c;
        calculateCurrent();
    }

    public void calculateCurrent()
    {
    }

    public void setPoints()
    {
        // Calculate the difference in x and y between the two points
        dx = x2 - x;
        dy = y2 - y;

        // Calculate the distance between the two points
        dn = Math.sqrt(dx * dx + dy * dy);

        // Calculate the x and y components of the unit vector pointing from
        // point 1 to point 2
        dpx1 = dy / dn;
        dpy1 = -dx / dn;

        // Calculate the sign of the y component of the unit vector
        // (i.e. is the y component positive or negative?)
        dsign = (dy == 0) ? sign(dx) : sign(dy);

        // Create the two points from the original coordinates
        point1 = new Point(x, y);
        point2 = new Point(x2, y2);
    }

    /**
     * Calculate the lead points for the circuit element.
     * 
     * This method takes one parameter, an integer len. The parameter len
     * specifies the length of the leads in pixels.
     * 
     * The method first checks if the distance between the two points of the
     * circuit element (i.e. the distance between point1 and point2) is less than
     * len. If it is, then the method sets the lead points to point1 and point2
     * respectively, and then returns.
     * 
     * If the distance is not less than len, then the method calculates the
     * lead points. The lead points are calculated by finding the point 1/2 of
     * the way between point1 and point2 that is len pixels away from point1,
     * and the point 1/2 of the way between point1 and point2 that is len pixels
     * away from point2. These points are then set as the lead points, and the
     * method returns.
     * 
     * @param len the length of the leads in pixels
     */
    public void calcLeads(int len)
    {
        if (dn < len || len == 0)
        {
            // If the distance between the two points is less than len, or if
            // len is 0, then we can just set the lead points to point1 and
            // point2, and return.
            lead1 = point1;
            lead2 = point2;
            return;
        }
        // Calculate the lead points
        // The lead points are calculated by finding the point 1/2 of the way
        // between point1 and point2 that is len pixels away from point1, and
        // the point 1/2 of the way between point1 and point2 that is len pixels
        // away from point2.
        // First, find the point 1/2 of the way between point1 and point2 that
        // is len pixels away from point1
        lead1 = interpPoint(point1, point2, (dn - len) / (2 * dn));
        // Then, find the point 1/2 of the way between point1 and point2 that
        // is len pixels away from point2
        lead2 = interpPoint(point1, point2, (dn + len) / (2 * dn));
    }

    /**
     * Interpolates a point between two given points based on a factor.
     * 
     * This method calculates a point that lies between two points 'a' and 'b'
     * based on a specified interpolation factor 'f'. The factor 'f' determines
     * the relative position of the interpolated point along the line segment
     * connecting points 'a' and 'b'.
     * 
     * @param a the starting point for interpolation
     * @param b the ending point for interpolation
     * @param f the interpolation factor, where 0 <= f <= 1
     *          - If f = 0, the interpolated point is at 'a'
     *          - If f = 1, the interpolated point is at 'b'
     *          - If 0 < f < 1, the interpolated point is somewhere between 'a' and 'b'
     * @return the interpolated point
     */
    public Point interpPoint(Point a, Point b, double f)
    {
        // Create a new point to hold the result of the interpolation
        Point p = new Point();
        
        // Perform the interpolation by invoking another method that calculates
        // the interpolated coordinates and assigns them to the point 'p'
        interpPoint(a, b, p, f);
        
        // Return the newly calculated interpolated point
        return p;
    }

    public void interpPoint(Point a, Point b, Point c, double f)
    {
        int xpd = b.x - a.x;
        int ypd = b.y - a.y;        /*
         * double q = (a.x*(1-f)+b.x*f+.48); System.out.println(q + " " + (int)
		 * q);
		 */
        c.x = (int) Math.floor(a.x * (1 - f) + b.x * f + .48);
        c.y = (int) Math.floor(a.y * (1 - f) + b.y * f + .48);
    }

    public void interpPoint(Point a, Point b, Point c, double f, double g)
    {
        int xpd = b.x - a.x;
        int ypd = b.y - a.y;
        int gx = b.y - a.y;
        int gy = a.x - b.x;
        g /= Math.sqrt(gx * gx + gy * gy);
        c.x = (int) Math.floor(a.x * (1 - f) + b.x * f + g * gx + .48);
        c.y = (int) Math.floor(a.y * (1 - f) + b.y * f + g * gy + .48);
    }

    public Point interpPoint(Point a, Point b, double f, double g)
    {
        Point p = new Point();
        interpPoint(a, b, p, f, g);
        return p;
    }

    public void interpPoint2(Point a, Point b, Point c, Point d, double f, double g)
    {
        int xpd = b.x - a.x;
        int ypd = b.y - a.y;
        int gx = b.y - a.y;
        int gy = a.x - b.x;
        g /= Math.sqrt(gx * gx + gy * gy);
        c.x = (int) Math.floor(a.x * (1 - f) + b.x * f + g * gx + .48);
        c.y = (int) Math.floor(a.y * (1 - f) + b.y * f + g * gy + .48);
        d.x = (int) Math.floor(a.x * (1 - f) + b.x * f - g * gx + .48);
        d.y = (int) Math.floor(a.y * (1 - f) + b.y * f - g * gy + .48);
    }

    public void draw2Leads(Graphics g)
    {
        // draw first lead
        setVoltageColor(g, volts[0]);
        drawThickLine(g, point1, lead1);

        // draw second lead
        setVoltageColor(g, volts[1]);
        drawThickLine(g, lead2, point2);
    }

    public Point[] newPointArray(int n)
    {
        Point a[] = new Point[n];
        while (n > 0)
            a[--n] = new Point();
        return a;
    }

    public void drawDots(Graphics g, Point pa, Point pb, double pos)
    {
        if ( pos == 0 || !sim.dotsCheckItem.getState())
            return;
        int dx = pb.x - pa.x;
        int dy = pb.y - pa.y;
        double dn = Math.sqrt(dx * dx + dy * dy);
        g.setColor(Color.orange);
        int ds = 16;
        pos %= ds;
        if (pos < 0)
            pos += ds;
        double di = 0;
        for (di = pos; di < dn; di += ds)
        {
            int x0 = (int) (pa.x + di * dx / dn);
            int y0 = (int) (pa.y + di * dy / dn);
            g.fillRect(x0 - 1, y0 - 1, 4, 4);
        }
    }

    public Polygon calcArrow(Point a, Point b, double al, double aw)
    {
        Polygon poly = new Polygon();
        Point p1 = new Point();
        Point p2 = new Point();
        int adx = b.x - a.x;
        int ady = b.y - a.y;
        double l = Math.sqrt(adx * adx + ady * ady);
        poly.addPoint(b.x, b.y);
        interpPoint2(a, b, p1, p2, 1 - al / l, aw);
        poly.addPoint(p1.x, p1.y);
        poly.addPoint(p2.x, p2.y);
        return poly;
    }

    public Polygon createPolygon(Point a, Point b, Point c)
    {
        Polygon p = new Polygon();
        p.addPoint(a.x, a.y);
        p.addPoint(b.x, b.y);
        p.addPoint(c.x, c.y);
        return p;
    }

    public Polygon createPolygon(Point a, Point b, Point c, Point d)
    {
        Polygon p = new Polygon();
        p.addPoint(a.x, a.y);
        p.addPoint(b.x, b.y);
        p.addPoint(c.x, c.y);
        p.addPoint(d.x, d.y);
        return p;
    }

    public Polygon createPolygon(Point a[])
    {
        Polygon p = new Polygon();
        int i;
        for (i = 0; i != a.length; i++)
            p.addPoint(a[i].x, a[i].y);
        return p;
    }

    public void drag(int xx, int yy)
    {
        xx = sim.snapGrid(xx);
        yy = sim.snapGrid(yy);
        if (noDiagonal)
        {
            if (Math.abs(x - xx) < Math.abs(y - yy))
            {
                xx = x;
            } else
            {
                yy = y;
            }
        }
        x2 = xx;
        y2 = yy;
        setPoints();
    }

    public void move(int dx, int dy)
    {
        x += dx;
        y += dy;
        x2 += dx;
        y2 += dy;
        boundingBox.move(dx, dy);
        setPoints();
    }

    // determine if moving this element by (dx,dy) will put it on top of another
    // element
    public boolean allowMove(int dx, int dy)
    {
        int nx = x + dx;
        int ny = y + dy;
        int nx2 = x2 + dx;
        int ny2 = y2 + dy;
        int i;
        for (i = 0; i != sim.elmList.size(); i++)
        {
            CircuitElm ce = sim.getElm(i);
            if (ce.x == nx && ce.y == ny && ce.x2 == nx2 && ce.y2 == ny2)
                return false;
            if (ce.x == nx2 && ce.y == ny2 && ce.x2 == nx && ce.y2 == ny)
                return false;
        }
        return true;
    }

    public void movePoint(int n, int dx, int dy)
    {
        if (n == 0)
        {
            x += dx;
            y += dy;
        } else
        {
            x2 += dx;
            y2 += dy;
        }
        setPoints();
    }

    public void drawPosts(Graphics g)
    {
        int i;
        for (i = 0; i != getPostCount(); i++)
        {
            Point p = getPost(i);
            drawPost(g, p.x, p.y, nodes[i]);
        }
    }

    public void stamp()
    {
    }

    public int getVoltageSourceCount()
    {
        return 0;
    }

    public int getInternalNodeCount()
    {
        return 0;
    }

    public void setNode(int p, int n)
    {
        nodes[p] = n;
    }

    public void setVoltageSource(int n, int v)
    {
        voltSource = v;
    }

    public int getVoltageSource()
    {
        return voltSource;
    }

    public double getVoltageDiff()
    {
        return volts[0] - volts[1];
    }

    public boolean nonLinear()
    {
        return false;
    }

    public int getPostCount()
    {
        return 2;
    }

    public int getNode(int n)
    {
        return nodes[n];
    }

    public Point getPost(int n)
    {
        return (n == 0) ? point1 : (n == 1) ? point2 : null;
    }

    public void drawPost(Graphics g, int x0, int y0, int n)
    {
        if (sim.dragElm == null && !needsHighlight() && sim.getCircuitNode(n).links.size() == 2)
            return;
        if (sim.mouseMode == CirSim.MODE_DRAG_ROW || sim.mouseMode == CirSim.MODE_DRAG_COLUMN)
            return;
        drawPost(g, x0, y0);
    }

    public void drawPost(Graphics g, int x0, int y0)
    {
        g.setColor(whiteColor);
        g.fillOval(x0 - 3, y0 - 3, 7, 7);
    }

    public void setBbox(int x1, int y1, int x2, int y2)
    {
        if (x1 > x2)
        {
            int q = x1;
            x1 = x2;
            x2 = q;
        }
        if (y1 > y2)
        {
            int q = y1;
            y1 = y2;
            y2 = q;
        }
        boundingBox.setBounds(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    void drawArrow(Graphics g, Point tail, Point head, int w, int h) {
        drawThickLine(g, tail, head);
        double dx = head.x - tail.x;
        double dy = head.y - tail.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist == 0)
            return;
        double ux = dx / dist, uy = dy / dist;
        Point p1 = new Point((int)(head.x - w*ux - h*uy), (int)(head.y - w*uy + h*ux));
        Point p2 = new Point((int)(head.x - w*ux + h*uy), (int)(head.y - w*uy - h*ux));
        int[] xpts = { head.x, p1.x, p2.x };
        int[] ypts = { head.y, p1.y, p2.y };
        g.fillPolygon(xpts, ypts, 3);
    }

    public void setBbox(Point p1, Point p2, double w)
    {
        setBbox(p1.x, p1.y, p2.x, p2.y);
        int gx = p2.y - p1.y;
        int gy = p1.x - p2.x;
        int dpx = (int) (dpx1 * w);
        int dpy = (int) (dpy1 * w);
        adjustBbox(p1.x + dpx, p1.y + dpy, p1.x - dpx, p1.y - dpy);
    }

    public void adjustBbox(int x1, int y1, int x2, int y2)
    {
        if (x1 > x2)
        {
            int q = x1;
            x1 = x2;
            x2 = q;
        }
        if (y1 > y2)
        {
            int q = y1;
            y1 = y2;
            y2 = q;
        }
        x1 = min(boundingBox.x, x1);
        y1 = min(boundingBox.y, y1);
        x2 = max(boundingBox.x + boundingBox.width - 1, x2);
        y2 = max(boundingBox.y + boundingBox.height - 1, y2);
        boundingBox.setBounds(x1, y1, x2 - x1, y2 - y1);
    }

    public void adjustBbox(Point p1, Point p2)
    {
        adjustBbox(p1.x, p1.y, p2.x, p2.y);
    }

    public boolean isCenteredText()
    {
        return false;
    }

    public void drawCenteredText(Graphics g, String s, int x, int y, boolean cx)
    {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        if (cx)
            x -= w / 2;
        g.drawString(s, x, y + fm.getAscent() / 2);
        adjustBbox(x, y - fm.getAscent() / 2, x + w, y + fm.getAscent() / 2 + fm.getDescent());
    }

    public void drawValues(Graphics g, String s, double hs)
    {
        if (s == null)
            return;
        g.setFont(unitsFont);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        g.setColor(whiteColor);
        int ya = fm.getAscent() / 2;
        int xc, yc;
        // if (this instanceof RailElm || this instanceof SweepElm)
        // {
        //     xc = x2;
        //     yc = y2;
        // } else
        // {
            xc = (x2 + x) / 2;
            yc = (y2 + y) / 2;
        // }
        int dpx = (int) (dpx1 * hs);
        int dpy = (int) (dpy1 * hs);
        if (dpx == 0)
        {
            g.drawString(s, xc - w / 2, yc - abs(dpy) - 2);
        } else
        {
            int xx = xc + abs(dpx) + 2;
            if (this instanceof VoltageElm || (x < x2 && y > y2))
                xx = xc - (w + abs(dpx) + 2);
            g.drawString(s, xx, yc + dpy + ya);
        }
    }

    public void drawCoil(Graphics g, int hs, Point p1, Point p2, double v1, double v2)
    {
        double len = distance(p1, p2);
        int segments = 30; // 10*(int) (len/10);
        int i;
        double segf = 1. / segments;

        ps1.setLocation(p1);
        for (i = 0; i != segments; i++)
        {
            double cx = (((i + 1) * 6. * segf) % 2) - 1;
            double hsx = Math.sqrt(1 - cx * cx);
            if (hsx < 0)
                hsx = -hsx;
            interpPoint(p1, p2, ps2, i * segf, hsx * hs);
            double v = v1 + (v2 - v1) * i / segments;
            setVoltageColor(g, v);
            drawThickLine(g, ps1, ps2);
            ps1.setLocation(ps2);
        }
    }

    public static void drawThickLine(Graphics g, int x, int y, int x2, int y2)
    {
        g.drawLine(x, y, x2, y2);
        g.drawLine(x + 1, y, x2 + 1, y2);
        g.drawLine(x, y + 1, x2, y2 + 1);
        g.drawLine(x + 1, y + 1, x2 + 1, y2 + 1);
    }

    public static void drawThickLine(Graphics g, Point pa, Point pb)
    {
        g.drawLine(pa.x, pa.y, pb.x, pb.y);
        g.drawLine(pa.x + 1, pa.y, pb.x + 1, pb.y);
        g.drawLine(pa.x, pa.y + 1, pb.x, pb.y + 1);
        g.drawLine(pa.x + 1, pa.y + 1, pb.x + 1, pb.y + 1);
    }

    public static void drawThickPolygon(Graphics g, int xs[], int ys[], int c)
    {
        int i;
        for (i = 0; i != c - 1; i++)
            drawThickLine(g, xs[i], ys[i], xs[i + 1], ys[i + 1]);
        drawThickLine(g, xs[i], ys[i], xs[0], ys[0]);
    }

    public static void drawThickPolygon(Graphics g, Polygon p)
    {
        drawThickPolygon(g, p.xpoints, p.ypoints, p.npoints);
    }

    public static void drawThickCircle(Graphics g, int cx, int cy, int ri)
    {
        int a;
        double m = pi / 180;
        double r = ri * .98;
        for (a = 0; a != 360; a += 20)
        {
            double ax = Math.cos(a * m) * r + cx;
            double ay = Math.sin(a * m) * r + cy;
            double bx = Math.cos((a + 20) * m) * r + cx;
            double by = Math.sin((a + 20) * m) * r + cy;
            drawThickLine(g, (int) ax, (int) ay, (int) bx, (int) by);
        }
    }

    public static String getVoltageDText(double v)
    {
        return getUnitText(Math.abs(v), "V");
    }

    public static String getVoltageText(double v)
    {
        return getUnitText(v, "V");
    }

    public static String getUnitText(double v, String u)
    {
        double va = Math.abs(v);
        if (va < 1e-14)
            return "0 " + u;
        if (va < 1e-9)
            return showFormat.format(v * 1e12) + " p" + u;
        if (va < 1e-6)
            return showFormat.format(v * 1e9) + " n" + u;
        if (va < 1e-3)
            return showFormat.format(v * 1e6) + " " + CirSim.muString + u;
        if (va < 1)
            return showFormat.format(v * 1e3) + " m" + u;
        if (va < 1e3)
            return showFormat.format(v) + " " + u;
        if (va < 1e6)
            return showFormat.format(v * 1e-3) + " k" + u;
        if (va < 1e9)
            return showFormat.format(v * 1e-6) + " M" + u;
        return showFormat.format(v * 1e-9) + " G" + u;
    }

    public static String getShortUnitText(double v, String u)
    {
        double va = Math.abs(v);
        if (va < 1e-13)
            return null;
        if (va < 1e-9)
            return shortFormat.format(v * 1e12) + "p" + u;
        if (va < 1e-6)
            return shortFormat.format(v * 1e9) + "n" + u;
        if (va < 1e-3)
            return shortFormat.format(v * 1e6) + CirSim.muString + u;
        if (va < 1)
            return shortFormat.format(v * 1e3) + "m" + u;
        if (va < 1e3)
            return shortFormat.format(v) + u;
        if (va < 1e6)
            return shortFormat.format(v * 1e-3) + "k" + u;
        if (va < 1e9)
            return shortFormat.format(v * 1e-6) + "M" + u;
        return shortFormat.format(v * 1e-9) + "G" + u;
    }

    public static String getCurrentText(double i)
    {
        return getUnitText(i, "A");
    }

    public static String getCurrentDText(double i)
    {
        return getUnitText(Math.abs(i), "A");
    }

    public void updateDotCount()
    {
        curcount = updateDotCount(current, curcount);
    }

    public double updateDotCount(double cur, double cc)
    {
        double cadd = cur * currentMult;        /*
         * if (cur != 0 && cadd <= .05 && cadd >= -.05) cadd = (cadd < 0) ? -.05
		 * : .05;
		 */
        cadd %= 8;        /*
		 * if (cadd > 8) cadd = 8; if (cadd < -8) cadd = -8;
		 */
        return cc + cadd;
    }

    public void doDots(Graphics g)
    {
        updateDotCount();
        if (sim.dragElm != this)
            drawDots(g, point1, point2, curcount);
    }

    public void doAdjust()
    {
    }

    public void setupAdjust()
    {
    }

    public void getInfo(String arr[])
    {
    }

    public int getBasicInfo(String arr[])
    {
        arr[1] = "I = " + getCurrentDText(getCurrent());
        arr[2] = "Vd = " + getVoltageDText(getVoltageDiff());
        return 3;
    }

    public void setVoltageColor(Graphics g, double volts)
    {
        if (needsHighlight())
        {
            g.setColor(Color.BLUE);
            return;
        }
        if (!sim.voltsCheckItem.getState())
        {
            if (!sim.powerCheckItem.getState()) // &&
                // !conductanceCheckItem.getState())
                g.setColor(whiteColor);
            return;
        }
        int c = (int) ((volts + voltageRange) * (colorScaleCount - 1) / (voltageRange * 2));
        if (c < 0)
            c = 0;
        if (c >= colorScaleCount)
            c = colorScaleCount - 1;
        g.setColor(colorScale[c]);
    }

    public void setPowerColor(Graphics g, boolean yellow)
    {
		/*
		 * if (conductanceCheckItem.getState()) { setConductanceColor(g,
		 * current/getVoltageDiff()); return; }
		 */
        if (!sim.powerCheckItem.getState())
            return;
        setPowerColor(g, getPower());
    }

    public void setPowerColor(Graphics g, double w0)
    {
        w0 *= powerMult;
        // System.out.println(w);
        double w = (w0 < 0) ? -w0 : w0;
        if (w > 1)
            w = 1;
        int rg = 128 + (int) (w * 127);
        int b = (int) (128 * (1 - w));
		/*
		 * if (yellow) g.setColor(new Color(rg, rg, b)); else
		 */
        if (w0 > 0)
            g.setColor(new Color(rg, b, b));
        else
            g.setColor(new Color(b, rg, b));
    }

    public void setConductanceColor(Graphics g, double w0)
    {
        w0 *= powerMult;
        // System.out.println(w);
        double w = (w0 < 0) ? -w0 : w0;
        if (w > 1)
            w = 1;
        int rg = (int) (w * 255);
        g.setColor(new Color(rg, rg, rg));
    }

    public double getPower()
    {
        return getVoltageDiff() * current;
    }

    public double getScopeValue(int x)
    {
        return (x == 1) ? getPower() : getVoltageDiff();
    }

    public String getScopeUnits(int x)
    {
        return (x == 1) ? "W" : "V";
    }

    public EditInfo getEditInfo(int n)
    {
        return null;
    }

    public void setEditValue(int n, EditInfo ei)
    {
    }

    public boolean getConnection(int n1, int n2)
    {
        return true;
    }

    public boolean hasGroundConnection(int n1)
    {
        return false;
    }

    public boolean isWire()
    {
        return false;
    }

    public boolean canViewInScope()
    {
        return getPostCount() <= 2;
    }

    public boolean comparePair(int x1, int x2, int y1, int y2)
    {
        return ((x1 == y1 && x2 == y2) || (x1 == y2 && x2 == y1));
    }

    public boolean needsHighlight()
    {
        return sim.mouseElm == this || selected;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean x)
    {
        selected = x;
    }

    public void selectRect(Rectangle r)
    {
        selected = r.intersects(boundingBox);
    }

    public static int abs(int x)
    {
        return x < 0 ? -x : x;
    }

    public static int sign(int x)
    {
        return (x < 0) ? -1 : (x == 0) ? 0 : 1;
    }

    public static int min(int a, int b)
    {
        return (a < b) ? a : b;
    }

    public static int max(int a, int b)
    {
        return (a > b) ? a : b;
    }

    public static double distance(Point p1, Point p2)
    {
        double x = p1.x - p2.x;
        double y = p1.y - p2.y;
        return Math.sqrt(x * x + y * y);
    }

    public Rectangle getBoundingBox()
    {
        return boundingBox;
    }

    public boolean hasHotkey()
    {
        return false;
    }
}
