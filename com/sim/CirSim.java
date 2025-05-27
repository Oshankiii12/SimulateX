package com.sim;
import com.sim.element.*;
import com.sim.ElementBuilder;
import com.sim.model.ElementDumpTypesRegistry;
import com.sim.view.*;
import com.sim.view.edit.EditDialog;
import com.sim.view.edit.EditOptions;
import com.sim.view.edit.Editable;
import com.sim.view.menu.RMBMenuBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

public class CirSim extends JFrame implements ComponentListener, ActionListener, AdjustmentListener, MouseMotionListener, MouseListener, ItemListener, KeyListener
{
    private ElementDumpTypesRegistry elementDumpTypesRegistry;
    public Thread engine = null;

    public Dimension winSize;
    public Image dbimage;

    public Random random;
    public static final int sourceRadius = 7;
    public static final double freqMult = 3.14159265 * 2 * 4;

    /**
     * Get the information about this applet.
     * @return information about this applet
     */
    public String getAppletInfo()
    {
        return "Circuit by Lavish Meena & Oshanki Priya";
    }

    private RMBMenuBuilder menuBuilder;
    public static Container main;
    public Label titleLabel;
    public Button resetButton;
    public Button dumpMatrixButton;
    public MenuItem exportItem, exportLinkItem, importItem, exitItem, selectAllItem, optionsItem;
    public Menu optionsMenu;
    public CheckboxMenuItem dotsCheckItem;
    public CheckboxMenuItem voltsCheckItem;
    public CheckboxMenuItem powerCheckItem;
    public CheckboxMenuItem smallGridCheckItem;
    public CheckboxMenuItem showValuesCheckItem;
    public CheckboxMenuItem conductanceCheckItem;
    public CheckboxMenuItem conventionCheckItem;
    public Scrollbar speedBar;
    public Scrollbar currentBar;
    public Label powerLabel;
    public Scrollbar powerBar;
    public PopupMenu elmMenu;
    public MenuItem elmEditMenuItem;
    public MenuItem elmDeleteMenuItem;
    public MenuItem elmScopeMenuItem;
    public PopupMenu scopeMenu;
    public PopupMenu popupMenu;
    public CheckboxMenuItem scopeVIMenuItem;
    public Class addingClass;
    public int mouseMode = MODE_SELECT;
    public int tempMouseMode = MODE_SELECT;
    public String mouseModeStr = "Select";
    public static final double pi = 3.14159265358979323846;
    public static final int MODE_ADD_ELM = 0;
    public static final int MODE_DRAG_ALL = 1;
    public static final int MODE_DRAG_ROW = 2;
    public static final int MODE_DRAG_COLUMN = 3;
    public static final int MODE_DRAG_SELECTED = 4;
    public static final int MODE_DRAG_POST = 5;
    public static final int MODE_SELECT = 6;
    public static final int infoWidth = 8;
    public int dragX, dragY, initDragX, initDragY;
    public int selectedSource;
    public Rectangle selectedArea;
    public int gridSize, gridMask, gridRound;
    public boolean dragging;
    public boolean analyzeFlag;
    public boolean dumpMatrix;
    public boolean useBufferedImage;
    public double t;
    public final int pause = 10;
    public int scopeSelected = -1;
    public int menuScope = -1;
    public int hintType = -1, hintItem1, hintItem2;
    public String stopMessage;
    public double timeStep;
    public Vector elmList;
    public Vector setupList;
    public CircuitElm dragElm, menuElm, mouseElm, stopElm;
    public int mousePost = -1;
    public CircuitElm plotXElm, plotYElm;
    public int draggingPost;
    public SwitchElm heldSwitchElm;
    public double circuitMatrix[][], circuitRightSide[], origRightSide[], origMatrix[][];
    public RowInfo circuitRowInfo[];
    public int circuitPermute[];
    public boolean circuitNonLinear;
    public int voltageSourceCount;
    public int circuitMatrixSize, circuitMatrixFullSize;
    public boolean circuitNeedsMap;
    public int scopeCount;
    public Scope scopes[];
    public int scopeColCount[];
    public static EditDialog editDialog;
    public static ImportDialog impDialog;
    public static String muString = "u";
    public static String ohmString = "ohm";
    public String clipboard;
    public Rectangle circuitArea;
    public int circuitBottom;
    public Vector undoStack, redoStack;
    public static final String appVersion = "Breadboard Simulator v1.0";

    /**
     * Generates a random integer within the range [0, maxValue).
     *
     * @param maxValue the upper bound (exclusive) for the random value
     * @return a non-negative random integer less than maxValue
     */
    private int getRandomInt(int maxValue) {
        // Generate a random integer
        int randomValue = random.nextInt();
        // Ensure the random value is non-negative
        if (randomValue < 0) {
            randomValue = -randomValue;
        }
        // Return the random value modulo maxValue
        return randomValue % maxValue;
    }

    public CircuitCanvas cv;

    public CirSim()
    {
        super(appVersion);
    }

    public String startCircuit = null;
    public String startLabel = null;
    public final String startCircuitText = null;
    public final String baseURL = "";

    /**
     * Initializes the circuit simulator application.
     */
    public void init() {
        boolean isConventional = true;

        // Initialize the CircuitElm class with this simulator instance
        CircuitElm.initClass(this);
        main = this;

        // Check Java version for compatibility
        String javaVersion = System.getProperty("java.class.version");
        double javaVersionFloat = Double.parseDouble(javaVersion);
        if (javaVersionFloat >= 48) {
            muString = "\u03bc"; // Unicode for micro symbol
            ohmString = "\u03a9"; // Unicode for ohm symbol
            useBufferedImage = true;
        }

        // Set layout and initialize canvas
        main.setLayout(new CircuitLayout());
        cv = new CircuitCanvas(this);
        cv.addComponentListener(this);
        cv.addMouseMotionListener(this);
        cv.addMouseListener(this);
        cv.addKeyListener(this);

        // Add canvas to main container
        main.add(cv);
        cv.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Initialize element dump registry
        elementDumpTypesRegistry = new ElementDumpTypesRegistry();

        // Setup menu bar and file menu
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        menuBar.add(fileMenu);

        // Add file menu items
        fileMenu.add(importItem = getMenuItem("Import"));
        fileMenu.add(exportItem = getMenuItem("Export"));
        fileMenu.addSeparator();
        fileMenu.add(exitItem = getMenuItem("Exit"));

        // Setup options menu
        optionsMenu = new Menu("Options");
        menuBar.add(optionsMenu);

        // Add options menu items
        optionsMenu.add(dotsCheckItem = getCheckItem("Show Current"));
        dotsCheckItem.setState(false);
        optionsMenu.add(voltsCheckItem = getCheckItem("Show Voltage"));
        voltsCheckItem.setState(false);
        optionsMenu.add(powerCheckItem = getCheckItem("Show Power"));
        optionsMenu.add(showValuesCheckItem = getCheckItem("Show Values"));
        showValuesCheckItem.setState(true);
        optionsMenu.add(smallGridCheckItem = getCheckItem("Small Grid"));
        optionsMenu.add(conventionCheckItem = getCheckItem("Conventional Current Motion"));
        conventionCheckItem.setState(isConventional);

        // Setup circuits menu and popup menu
        Menu circuitsMenu = new Menu("Circuits");
        popupMenu = new RMBMenuBuilder(this).build();
        main.add(popupMenu);

        // Initialize control buttons and scrollbars
        dumpMatrixButton = new Button("Dump Matrix");
        speedBar = new Scrollbar(Scrollbar.HORIZONTAL, 3, 1, 0, 260);
        speedBar.addAdjustmentListener(this);
        currentBar = new Scrollbar(Scrollbar.HORIZONTAL, 50, 1, 1, 100);
        currentBar.addAdjustmentListener(this);
        powerBar = new Scrollbar(Scrollbar.HORIZONTAL, 50, 1, 1, 100);
        powerBar.addAdjustmentListener(this);
        powerBar.disable();

        // Initialize grid and element lists
        setGrid();
        elmList = new Vector<>();
        setupList = new Vector<>();
        undoStack = new Vector<>();
        redoStack = new Vector<>();

        // Initialize scopes
        scopes = new Scope[20];
        scopeColCount = new int[20];
        scopeCount = 0;

        // Initialize random number generator and canvas colors
        random = new Random();
        cv.setBackground(Color.BLACK);
        cv.setForeground(Color.BLACK);

        // Setup element menu
        elmMenu = new PopupMenu();
        elmMenu.add(elmEditMenuItem = getMenuItem("Edit"));
        elmMenu.add(elmScopeMenuItem = getMenuItem("View in Scope"));
        elmMenu.add(elmDeleteMenuItem = getMenuItem("Delete"));
        main.add(elmMenu);

        // Build scope menu
        scopeMenu = buildScopeMenu();

        // Populate setup list and set menu bar
        getSetupList(circuitsMenu, false);
        setMenuBar(menuBar);

        // Read initial setup if available
        if (startCircuitText != null) {
            readSetup(startCircuitText);
        } else if (stopMessage == null && startCircuit != null) {
            readSetupFile(startCircuit, startLabel);
        }

        // Set window size and position
        Dimension screenSize = getToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height);
        handleResize();

        Dimension windowSize = getSize();
        setLocation((screenSize.width - windowSize.width) / 2, (screenSize.height - windowSize.height) / 2);

        // Create double buffering image
        dbimage = main.createImage(winSize.width, winSize.height);
        setVisible(true);
        main.requestFocus();
    }

    public boolean shown = false;

    /**
     * Triggers the visibility of the window.
     * If the window is not already visible, it sets the window to visible.
     */
    public void triggerShow()
    {
        // Check if the window is currently not visible
        if (!isVisible()) {
            // Set the window to visible
            setVisible(true);
        }
    }

    /**
     * Builds and returns a popup menu for scope options. The menu items in the
     * scope menu are as follows:
     * <ul>
     * <li>Remove: removes the scope from the list of scopes</li>
     * <li>Speed 2x: doubles the speed of the scope</li>
     * <li>Speed 1/2x: halves the speed of the scope</li>
     * <li>Scale 2x: doubles the scale of the scope</li>
     * <li>Max Scale: sets the scale of the scope to the maximum</li>
     * <li>Reset: resets the scope to its original settings</li>
     * <li>Show V vs I: shows the voltage vs current graph in the scope</li>
     * </ul>
     *
     * @return the constructed PopupMenu
     */
    private PopupMenu buildScopeMenu() {
        PopupMenu menu = new PopupMenu();
        menu.add(getMenuItem("Remove", "remove"));
        menu.add(getMenuItem("Speed 2x", "speed2"));
        menu.add(getMenuItem("Speed 1/2x", "speed1/2"));
        
        menu.add(getMenuItem("Scale 2x", "scale"));
        menu.add(getMenuItem("Max Scale", "maxscale"));
        menu.add(getMenuItem("Reset", "reset"));
        // menu.add(getCheckItem("Show V vs I"));
        menu.add(scopeVIMenuItem = getCheckItem("Show V vs I"));
        add(menu);
        return menu;
    }

    /**
     * Creates and returns a menu item with the specified label.
     * The menu item will have an action listener attached to it.
     *
     * @param label the label of the menu item
     * @return the constructed MenuItem with the specified label
     */
    public MenuItem getMenuItem(String label) {
        // Create a new MenuItem with the given label
        MenuItem menuItem = new MenuItem(label);
        
        // Add an action listener to the menu item
        menuItem.addActionListener(this);
        
        // Return the created MenuItem
        return menuItem;
    }

    /**
     * Creates and returns a menu item with the specified label and action command.
     * The menu item will have an action listener attached to it.
     *
     * @param label the label of the menu item
     * @param actionCommand the action command of the menu item
     * @return the constructed MenuItem with the specified label and action command
     */
    public MenuItem getMenuItem(String label, String actionCommand) {
        // Create a new MenuItem with the given label and action command
        MenuItem menuItem = new MenuItem(label);
        menuItem.setActionCommand(actionCommand);
        // Add an action listener to the menu item
        menuItem.addActionListener(this);
        // Return the created MenuItem
        return menuItem;
    }

    /**
     * Creates and returns a new CheckboxMenuItem with the specified label.
     * The item will have no action command, and the item state will be changed
     * when the item is clicked.
     * @param label the label of the menu item
     * @return the created CheckboxMenuItem
     */
    public CheckboxMenuItem getCheckItem(String label) {
        CheckboxMenuItem menuItem = new CheckboxMenuItem(label);
        menuItem.addItemListener(this);
        menuItem.setActionCommand("");
        return menuItem;
    }

    /**
     * Creates and returns a new CheckboxMenuItem with the specified label and action command.
     * The item will have its state changed when clicked, and will trigger the associated action.
     *
     * @param label the label of the menu item
     * @param actionCommand the action command of the menu item
     * @return the created CheckboxMenuItem
     */
    public CheckboxMenuItem getCheckItem(String label, String actionCommand) {
        // Create a new CheckboxMenuItem with the given label
        CheckboxMenuItem menuItem = new CheckboxMenuItem(label);
        
        // Add an item listener to respond to state changes
        menuItem.addItemListener(this);
        
        // Set the action command to be triggered upon activation
        menuItem.setActionCommand(actionCommand);
        
        // Return the constructed CheckboxMenuItem
        return menuItem;
    }

    /**
     * Handles the resizing of the circuit view.
     * This method adjusts the size of the canvas and centers the circuit elements within the view.
     * It also updates the circuit area dimensions and ensures that the circuit elements are positioned correctly.
     */
    public void handleResize() {
        // Set the canvas size to match the current window size
        cv.setSize(getSize());
        final Dimension newSize = cv.getSize();
        winSize = newSize;
        
        // Return if the new width is zero
        if (newSize.width == 0)
            return;
        
        // Create a new image for double buffering
        dbimage = main.createImage(newSize.width, newSize.height);
        
        // Define the area of the circuit
        final int h = newSize.height / 4;
        circuitArea = new Rectangle(0, 0, newSize.width, newSize.height - h - 10);
        
        // Initialize boundary variables for circuit elements
        int minLeft = Integer.MAX_VALUE;
        int maxRight = Integer.MIN_VALUE;
        int minTop = Integer.MAX_VALUE;
        int maxBottom = Integer.MIN_VALUE;
        
        // Calculate the bounding box for the circuit elements
        for (int i = 0; i < elmList.size(); i++) {
            final CircuitElm ce = getElm(i);
            
            // Special case for non-centered text elements
            if (!ce.isCenteredText()) {
                minLeft = min(ce.x, min(ce.x2, minLeft));
                maxRight = max(ce.x, max(ce.x2, maxRight));
            }
            minTop = min(ce.y, min(ce.y2, minTop));
            maxBottom = max(ce.y, max(ce.y2, maxBottom));
        }
        
        // Calculate the displacement to center the circuit
        int dx = gridMask & ((circuitArea.width - (maxRight - minLeft)) / 2 - minLeft);
        int dy = gridMask & ((circuitArea.height - (maxBottom - minTop)) / 2 - minTop);
        
        // Ensure the circuit does not move out of bounds
        if (dx + minLeft < 0)
            dx = gridMask & (-minLeft);
        if (dy + minTop < 0)
            dy = gridMask & (-minTop);
        
        // Move the circuit elements by the calculated displacement
        for (int i = 0; i < elmList.size(); i++) {
            final CircuitElm ce = getElm(i);
            ce.move(dx, dy);
        }
        
        // Mark the circuit for analysis after moving elements
        needAnalyze();
        
        // Reset the circuit bottom position
        circuitBottom = 0;
    }

    /**
     * Destroys the current frame, disposing of any resources.
     */
    public void destroyFrame() {
        dispose();
    }

    /**
     * Handles events, specifically focusing on the destruction of the window.
     * If the event signifies a window destroy action, the frame is destroyed.
     * 
     * @param event The event to handle.
     * @return true if the event was handled (window destroy event), false otherwise.
     */
    @Override
    public boolean handleEvent(Event event) {
        // Check if the event is a window destroy event
        if (event.id == Event.WINDOW_DESTROY) {
            // Destroy the current frame
            destroyFrame();
            return true;
        }
        // Delegate handling to the superclass if not a window destroy event
        return super.handleEvent(event);
    }

    /**
     * Paints the current graphics context.
     * This method is a callback for the paint event of the circuit canvas.
     * It delegates the painting to the CircuitCanvas object.
     * @param g The graphics context to paint.
     */
    public void paint(Graphics g)
    {
        // Repaint the circuit canvas
        cv.repaint();
    }

    public static final int resct = 6;
    public long lastTime = 0, lastFrameTime, lastIterTime, secTime = 0;
    public int frames = 0;
    public int steps = 0;
    public int framerate = 0, steprate = 0;
    /**
     * Updates the circuit graphics and runs the circuit simulation.
     * This method acts as the main loop of the circuit simulator.
     * Tasks handled include analyzing the circuit, running the simulation,
     * updating graphics, and managing frame rates.
     * It handles the following tasks:
     * 1. Checks if the circuit needs to be analyzed after moving elements.
     * 2. Runs the circuit simulation (see the runCircuit() method).
     * 3. Updates the graphics of the circuit elements.
     * 4. Updates the scope graphics.
     * 5. Updates the power meter graphics.
     * 6. Updates the framerate and steprate (for debugging purposes).
     * 7. Updates the circuit bottom position.
     * 8. Draws a grid on the circuit area.
     * 9. Draws the circuit elements.
     * 10. Draws the scope graphics.
     * 11. Draws the power meter graphics.
     * 12. Draws the current and voltage labels.
     * 13. Draws the bad connections.
     * 14. Draws the hint message.
     * 15. Draws the selected area.
     * 16. Updates the circuit matrix.
     * 17. Updates the last frame time.
     * @param realg The graphics context to update.
     */
    public void updateCircuit(Graphics realg) {
        CircuitElm realMouseElm;
        
        // Return if window size is not initialized or width is zero
        if (winSize == null || winSize.width == 0) return;
        
        // Analyze the circuit if flagged
        if (analyzeFlag) {
            analyzeCircuit();
            analyzeFlag = false;
        }
        
        // Set the mouse element if it is a CircuitElm
        if (editDialog != null && editDialog.elm instanceof CircuitElm) {
            mouseElm = (CircuitElm) (editDialog.elm);
        }
        
        realMouseElm = mouseElm;
        if (mouseElm == null) mouseElm = stopElm;
        
        // Setup scopes for the simulation
        setupScopes();
        
        // Initialize graphics for double buffering
        Graphics g = dbimage.getGraphics();
        CircuitElm.selectColor = Color.blue;
        CircuitElm.whiteColor = Color.black;
        CircuitElm.lightGrayColor = Color.black;
        
        // Set background color and fill the canvas
        g.setColor(new Color(214, 243, 243));
        g.fillRect(0, 0, winSize.width, winSize.height);

        // Draw grid lines on the circuit area
        int gridSpacing = 20;
        Color gridColor = new Color(255, 255, 255, 255);
        g.setColor(gridColor);
        for (int x = 0; x < winSize.width; x += gridSpacing) {
            g.drawLine(x, 0, x, winSize.height);
        }
        for (int y = 0; y < winSize.height; y += gridSpacing) {
            g.drawLine(0, y, winSize.width, y);
        }

        // Run the circuit simulation
        runCircuit();

        // Calculate time since last update
        long sysTime = System.currentTimeMillis();
        if (lastTime != 0) {
            int inc = (int) (sysTime - lastTime);
            double c = currentBar.getValue();
            c = java.lang.Math.exp(c / 3.5 - 14.2);
            CircuitElm.currentMult = 1.7 * inc * c;
            if (!conventionCheckItem.getState())
                CircuitElm.currentMult = -CircuitElm.currentMult;
        }
        
        // Update frame rate every second
        if (sysTime - secTime >= 1000) {
            framerate = frames;
            steprate = steps;
            frames = 0;
            steps = 0;
            secTime = sysTime;
        }
        lastTime = sysTime;

        // Update power multiplier
        CircuitElm.powerMult = Math.exp(powerBar.getValue() / 4.762 - 7);

        Font oldfont = g.getFont();
        
        // Draw each circuit element
        for (int i = 0; i != elmList.size(); i++) {
            if (powerCheckItem.getState())
                g.setColor(Color.gray);
            getElm(i).draw(g);
        }

        // Draw post positions if dragging
        if (tempMouseMode == MODE_DRAG_ROW || tempMouseMode == MODE_DRAG_COLUMN || 
            tempMouseMode == MODE_DRAG_POST || tempMouseMode == MODE_DRAG_SELECTED) {
            for (int i = 0; i != elmList.size(); i++) {
                CircuitElm ce = getElm(i);
                ce.drawPost(g, ce.x, ce.y);
                ce.drawPost(g, ce.x2, ce.y2);
            }
        }

        int badnodes = 0;

        // Identify bad connections
        for (int i = 0; i != nodeList.size(); i++) {
            CircuitNode cn = getCircuitNode(i);
            if (!cn.internal && cn.links.size() == 1) {
                int bb = 0;
                CircuitNodeLink cnl = (CircuitNodeLink) cn.links.elementAt(0);
                for (int j = 0; j != elmList.size(); j++)
                    if (cnl.elm != getElm(j) && getElm(j).boundingBox.contains(cn.x, cn.y))
                        bb++;
                if (bb > 0) {
                    g.setColor(Color.red);
                    g.fillOval(cn.x - 3, cn.y - 3, 7, 7);
                    badnodes++;
                }
            }
        }

        // Draw dragging elements
        if (dragElm != null && (dragElm.x != dragElm.x2 || dragElm.y != dragElm.y2))
            dragElm.draw(g);

        g.setFont(oldfont);
        int ct = scopeCount;
        if (stopMessage != null) ct = 0;

        // Draw scopes
        for (int i = 0; i != ct; i++)
            scopes[i].draw(g);

        g.setColor(CircuitElm.whiteColor);
        
        // Display stop message if it exists
        if (stopMessage != null) {
            g.drawString(stopMessage, 10, circuitArea.height);
        } else {
            if (circuitBottom == 0) calcCircuitBottom();

            String info[] = new String[10];
            if (mouseElm != null) {
                if (mousePost == -1)
                    mouseElm.getInfo(info);
                else
                    info[0] = "V = " + CircuitElm.getUnitText(mouseElm.getPostVoltage(mousePost), "V");
            } else {
                CircuitElm.showFormat.setMinimumFractionDigits(2);
                CircuitElm.showFormat.setMinimumFractionDigits(0);
            }

            int x = 0;
            if (ct != 0)
                x = scopes[ct - 1].rightEdge() + 20;
            x = Math.max(x, winSize.width * 10 / 11);

            // Count lines of data
            int i;
            for (i = 0; info[i] != null; i++);
            if (badnodes > 0)
                info[i++] = badnodes + ((badnodes == 1) ? " bad connection" : " bad connections");

            // Determine where to display data
            int ybase = 10;
            for (i = 0; info[i] != null; i++)
                g.drawString(info[i], x, ybase + 15 * (i + 1));
        }

        // Draw selected area
        if (selectedArea != null) {
            g.setColor(CircuitElm.selectColor);
            g.drawRect(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
        }

        mouseElm = realMouseElm;
        frames++;

        // Draw the double buffered image
        realg.drawImage(dbimage, 0, 0, this);

        // Limit to 50 fps
        if (circuitMatrix != null) {
            long delay = 1000 / 50 - (System.currentTimeMillis() - lastFrameTime);
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    // Handle exception
                }
            }
            cv.repaint(0);
        }
        lastFrameTime = lastTime;
    }

    /**
     * Setup the scopes based on the elements in the circuit.
     * This function checks that the elements still exist and removes
     * any unused scopes/columns. It also positions the scopes correctly
     * and resets the graph if necessary.
     */
    public void setupScopes()
    {
        int i;

        // Check scopes to make sure the elements still exist
        // and remove unused scopes/columns
        int pos = -1;
        for (i = 0; i < scopeCount; i++)
        {
            if (locateElm(scopes[i].elm) < 0)
                scopes[i].setElm(null);
            if (scopes[i].elm == null)
            {
                // Remove scope from list
                int j;
                for (j = i; j != scopeCount; j++)
                    scopes[j] = scopes[j + 1];
                scopeCount--;
                i--;
                continue;
            }
            if (scopes[i].position > pos + 1)
                scopes[i].position = pos + 1;
            pos = scopes[i].position;
        }
        // Remove any unused scopes from the end of the list
        while (scopeCount > 0 && scopes[scopeCount - 1].elm == null)
            scopeCount--;
        int h = winSize.height - circuitArea.height;
        pos = 0;
        for (i = 0; i != scopeCount; i++)
            scopeColCount[i] = 0;
        // Count the number of scopes in each column
        for (i = 0; i != scopeCount; i++)
        {
            pos = max(scopes[i].position, pos);
            scopeColCount[scopes[i].position]++;
        }
        int colct = pos + 1;
        int iw = infoWidth;
        if (colct <= 2)
            // If there is only one or two columns, make the info window
            // bigger
            iw = iw * 3 / 2;
        int w = (winSize.width - iw) / colct;
        int marg = 10;
        if (w < marg * 2)
            // If the scope is too narrow, make it wider
            w = marg * 2;
        pos = -1;
        int colh = 0;
        int row = 0;
        int speed = 0;
        for (i = 0; i != scopeCount; i++)
        {
            Scope s = scopes[i];
            if (s.position > pos)
            {
                pos = s.position;
                // Calculate the height of the column
                colh = h / scopeColCount[pos];
                row = 0;
                // Set the speed of the scope
                speed = s.speed;
            }
            if (s.speed != speed)
            {
                // If the speed of the scope changes, reset the graph
                s.speed = speed;
                s.resetGraph();
            }
            // Set the position of the scope
            Rectangle r = new Rectangle(pos * w + 4, winSize.height - h - 60, w - marg, colh);
            row++;
            if (!r.equals(s.rect))
                s.setRect(r);
        }
    }

    /**
     * Toggle a switch by decrementing the switch count and toggling the
     * switch if the count reaches zero.
     * @param n the switch count
     */
    public void toggleSwitch(int n)
    {
        int i;
        // Loop through all the elements in the list
        for (i = 0; i != elmList.size(); i++)
        {
            // Get the element
            CircuitElm ce = getElm(i);
            // If the element is a switch
            if (ce instanceof SwitchElm)
            {
                // Decrement the switch count
                n--;
                // If the switch count reaches zero
                if (n == 0)
                {
                    // Toggle the switch
                    ((SwitchElm) ce).toggle();
                    // Set the flag to analyze the circuit
                    analyzeFlag = true;
                    // Repaint the circuit view
                    cv.repaint();
                    // Return because we've toggled the switch
                    return;
                }
            }
        }
    }

    /**
     * Sets the flag to analyze the circuit and repaints the circuit view.
     * This method is used to request an analysis of the circuit when the
     * circuit has changed.
     */
    public void needAnalyze()
    {
        // Set the flag to analyze the circuit
        // This flag is used to determine if the circuit needs to be analyzed
        // on the next call to updateCircuit.
        analyzeFlag = true;
        // Repaint the circuit view
        // This will cause the circuit view to be updated with the new circuit
        // layout and graphics.
        cv.repaint();
    }

    public Vector nodeList;
    public CircuitElm voltageSources[];

    /**
     * Returns the CircuitNode at the given index in the node list.
     * The node list is a list of all the nodes in the circuit.
     * @param n the index of the node to get
     * @return the CircuitNode at the given index
     */
    public CircuitNode getCircuitNode(int n)
    {
        if (n >= nodeList.size())
            return null;
        return (CircuitNode) nodeList.elementAt(n);
    }

    /**
     * Returns the CircuitElm at the given index in the element list.
     * The element list is a list of all the elements in the circuit.
     * @param n the index of the element to get
     * @return the CircuitElm at the given index
     */
    public CircuitElm getElm(int n)
    {
        // If the index passed in is greater than the number of elements
        // in the list, return null
        if (n >= elmList.size())
            return null;
        // Otherwise, get the element at the given index
        // and return it as a CircuitElm
        return (CircuitElm) elmList.elementAt(n);
    }

    public void analyzeCircuit() {
        // Calculate the bottom position of the circuit elements
        calcCircuitBottom();

        // Return if there are no elements in the circuit
        if (elmList.isEmpty())
            return;

        // Clear any previous stop message and stop element
        stopMessage = null;
        stopElm = null;

        int i, j;
        int vscount = 0; // Counter for voltage sources
        nodeList = new Vector(); // List to store circuit nodes
        boolean gotGround = false; // Flag to check if ground is present
        boolean gotRail = false; // Flag to check if rail is present
        CircuitElm volt = null; // Variable to store voltage element

        // Look for voltage or ground element
        for (i = 0; i != elmList.size(); i++) {
            CircuitElm ce = getElm(i);
            if (ce instanceof GroundElm) {
                gotGround = true;
                break;
            }
            if (volt == null && ce instanceof VoltageElm)
                volt = ce;
        }

        // If no ground, treat the first terminal of the voltage element as ground
        if (!gotGround && volt != null && !gotRail) {
            CircuitNode cn = new CircuitNode();
            Point pt = volt.getPost(0);
            cn.x = pt.x;
            cn.y = pt.y;
            nodeList.addElement(cn);
        } else {
            // Allocate an extra node for ground otherwise
            CircuitNode cn = new CircuitNode();
            cn.x = cn.y = -1;
            nodeList.addElement(cn);
        }

        // Allocate nodes and voltage sources for each circuit element
        for (i = 0; i != elmList.size(); i++) {
            CircuitElm ce = getElm(i);
            int inodes = ce.getInternalNodeCount(); // Get internal node count
            int ivs = ce.getVoltageSourceCount(); // Get voltage source count
            int posts = ce.getPostCount(); // Get post count

            // Allocate a node for each post and match posts to nodes
            for (j = 0; j != posts; j++) {
                Point pt = ce.getPost(j);
                int k;
                for (k = 0; k != nodeList.size(); k++) {
                    CircuitNode cn = getCircuitNode(k);
                    if (pt.x == cn.x && pt.y == cn.y)
                        break;
                }
                if (k == nodeList.size()) {
                    // If node does not exist, create a new node
                    CircuitNode cn = new CircuitNode();
                    cn.x = pt.x;
                    cn.y = pt.y;
                    CircuitNodeLink cnl = new CircuitNodeLink();
                    cnl.num = j;
                    cnl.elm = ce;
                    cn.links.addElement(cnl);
                    ce.setNode(j, nodeList.size());
                    nodeList.addElement(cn);
                } else {
                    // If node exists, link the element to the node
                    CircuitNodeLink cnl = new CircuitNodeLink();
                    cnl.num = j;
                    cnl.elm = ce;
                    getCircuitNode(k).links.addElement(cnl);
                    ce.setNode(j, k);

                    // Set voltage of ground node to 0
                    if (k == 0)
                        ce.setNodeVoltage(j, 0);
                }
            }

            // Allocate internal nodes
            for (j = 0; j != inodes; j++) {
                CircuitNode cn = new CircuitNode();
                cn.x = cn.y = -1;
                cn.internal = true;
                CircuitNodeLink cnl = new CircuitNodeLink();
                cnl.num = j + posts;
                cnl.elm = ce;
                cn.links.addElement(cnl);
                ce.setNode(cnl.num, nodeList.size());
                nodeList.addElement(cn);
            }
            vscount += ivs; // Increment voltage source count
        }

        // Initialize voltage sources array
        voltageSources = new CircuitElm[vscount];
        vscount = 0;
        circuitNonLinear = false; // Flag to determine if circuit is nonlinear

        // Determine if circuit is nonlinear and set voltage sources
        for (i = 0; i != elmList.size(); i++) {
            CircuitElm ce = getElm(i);
            if (ce.nonLinear())
                circuitNonLinear = true;
            int ivs = ce.getVoltageSourceCount();
            for (j = 0; j != ivs; j++) {
                voltageSources[vscount] = ce;
                ce.setVoltageSource(j, vscount++);
            }
        }
        voltageSourceCount = vscount;

        // Setup matrix size and initialize matrices
        int matrixSize = nodeList.size() - 1 + vscount;
        circuitMatrix = new double[matrixSize][matrixSize];
        circuitRightSide = new double[matrixSize];
        origMatrix = new double[matrixSize][matrixSize];
        origRightSide = new double[matrixSize];
        circuitMatrixSize = circuitMatrixFullSize = matrixSize;
        circuitRowInfo = new RowInfo[matrixSize];
        circuitPermute = new int[matrixSize];

        // Initialize row information and mapping
        for (i = 0; i != matrixSize; i++)
            circuitRowInfo[i] = new RowInfo();
        circuitNeedsMap = false;

        // Stamp linear circuit elements
        for (i = 0; i != elmList.size(); i++) {
            CircuitElm ce = getElm(i);
            ce.stamp();
        }

        // Determine unconnected nodes and connect them
        boolean closure[] = new boolean[nodeList.size()];
        boolean tempclosure[] = new boolean[nodeList.size()];
        boolean changed = true;
        closure[0] = true; // Ground node is always connected
        while (changed) {
            changed = false;
            for (i = 0; i != elmList.size(); i++) {
                CircuitElm ce = getElm(i);
                for (j = 0; j < ce.getPostCount(); j++) {
                    if (!closure[ce.getNode(j)]) {
                        if (ce.hasGroundConnection(j))
                            closure[ce.getNode(j)] = changed = true;
                        continue;
                    }
                    int k;
                    for (k = 0; k != ce.getPostCount(); k++) {
                        if (j == k)
                            continue;
                        int kn = ce.getNode(k);
                        if (ce.getConnection(j, k) && !closure[kn]) {
                            closure[kn] = true;
                            changed = true;
                        }
                    }
                }
            }
            if (changed)
                continue;

            // Connect unconnected nodes by stamping a high resistance
            for (i = 0; i != nodeList.size(); i++)
                if (!closure[i] && !getCircuitNode(i).internal) {
                    System.out.println("node " + i + " unconnected");
                    stampResistor(0, i, 1e8);
                    closure[i] = true;
                    changed = true;
                    break;
                }
        }

        // Look for voltage source loops
        for (i = 0; i != elmList.size(); i++) {
            CircuitElm ce = getElm(i);
            if ((ce instanceof VoltageElm && ce.getPostCount() == 2) || ce instanceof WireElm) {
                FindPathInfo fpi = new FindPathInfo(FindPathInfo.VOLTAGE, ce, ce.getNode(1));
                if (fpi.findPath(ce.getNode(0))) {
                    stop("Voltage source/wire loop with no resistance!", ce);
                    return;
                }
            }
        }

        // Simplify the matrix to speed up computations
        for (i = 0; i != matrixSize; i++) {
            int qm = -1, qp = -1;
            double qv = 0;
            RowInfo re = circuitRowInfo[i];
            if (re.lsChanges || re.dropRow || re.rsChanges)
                continue;
            double rsadd = 0;

            // Look for rows that can be removed
            for (j = 0; j != matrixSize; j++) {
                double q = circuitMatrix[i][j];
                if (circuitRowInfo[j].type == RowInfo.ROW_CONST) {
                    rsadd -= circuitRowInfo[j].value * q;
                    continue;
                }
                if (q == 0)
                    continue;
                if (qp == -1) {
                    qp = j;
                    qv = q;
                    continue;
                }
                if (qm == -1 && q == -qv) {
                    qm = j;
                    continue;
                }
                break;
            }
            if (j == matrixSize) {
                if (qp == -1) {
                    stop("Matrix error", null);
                    return;
                }
                RowInfo elt = circuitRowInfo[qp];
                if (qm == -1) {
                    int k;
                    for (k = 0; elt.type == RowInfo.ROW_EQUAL && k < 100; k++) {
                        qp = elt.nodeEq;
                        elt = circuitRowInfo[qp];
                    }
                    if (elt.type == RowInfo.ROW_EQUAL) {
                        elt.type = RowInfo.ROW_NORMAL;
                        continue;
                    }
                    if (elt.type != RowInfo.ROW_NORMAL) {
                        System.out.println("type already " + elt.type + " for " + qp + "!");
                        continue;
                    }
                    elt.type = RowInfo.ROW_CONST;
                    elt.value = (circuitRightSide[i] + rsadd) / qv;
                    circuitRowInfo[i].dropRow = true;
                    i = -1; // Start over from scratch
                } else if (circuitRightSide[i] + rsadd == 0) {
                    if (elt.type != RowInfo.ROW_NORMAL) {
                        int qq = qm;
                        qm = qp;
                        qp = qq;
                        elt = circuitRowInfo[qp];
                        if (elt.type != RowInfo.ROW_NORMAL) {
                            System.out.println("swap failed");
                            continue;
                        }
                    }
                    elt.type = RowInfo.ROW_EQUAL;
                    elt.nodeEq = qm;
                    circuitRowInfo[i].dropRow = true;
                }
            }
        }

        // Find size of new simplified matrix
        int nn = 0;
        for (i = 0; i != matrixSize; i++) {
            RowInfo elt = circuitRowInfo[i];
            if (elt.type == RowInfo.ROW_NORMAL) {
                elt.mapCol = nn++;
                continue;
            }
            if (elt.type == RowInfo.ROW_EQUAL) {
                RowInfo e2 = null;
                for (j = 0; j != 100; j++) {
                    e2 = circuitRowInfo[elt.nodeEq];
                    if (e2.type != RowInfo.ROW_EQUAL)
                        break;
                    if (i == e2.nodeEq)
                        break;
                    elt.nodeEq = e2.nodeEq;
                }
            }
            if (elt.type == RowInfo.ROW_CONST)
                elt.mapCol = -1;
        }
        for (i = 0; i != matrixSize; i++) {
            RowInfo elt = circuitRowInfo[i];
            if (elt.type == RowInfo.ROW_EQUAL) {
                RowInfo e2 = circuitRowInfo[elt.nodeEq];
                if (e2.type == RowInfo.ROW_CONST) {
                    elt.type = e2.type;
                    elt.value = e2.value;
                    elt.mapCol = -1;
                } else {
                    elt.mapCol = e2.mapCol;
                }
            }
        }

        // Create the new simplified matrix
        int newsize = nn;
        double newmatx[][] = new double[newsize][newsize];
        double newrs[] = new double[newsize];
        int ii = 0;
        for (i = 0; i != matrixSize; i++) {
            RowInfo rri = circuitRowInfo[i];
            if (rri.dropRow) {
                rri.mapRow = -1;
                continue;
            }
            newrs[ii] = circuitRightSide[i];
            rri.mapRow = ii;
            for (j = 0; j != matrixSize; j++) {
                RowInfo ri = circuitRowInfo[j];
                if (ri.type == RowInfo.ROW_CONST)
                    newrs[ii] -= ri.value * circuitMatrix[i][j];
                else
                    newmatx[ii][ri.mapCol] += circuitMatrix[i][j];
            }
            ii++;
        }

        // Update matrices with the new simplified values
        circuitMatrix = newmatx;
        circuitRightSide = newrs;
        matrixSize = circuitMatrixSize = newsize;
        for (i = 0; i != matrixSize; i++)
            origRightSide[i] = circuitRightSide[i];
        for (i = 0; i != matrixSize; i++)
            for (j = 0; j != matrixSize; j++)
                origMatrix[i][j] = circuitMatrix[i][j];
        circuitNeedsMap = true;

        // Perform LU factorization if the circuit is linear
        if (!circuitNonLinear) {
            if (!lu_factor(circuitMatrix, circuitMatrixSize, circuitPermute)) {
                stop("Singular matrix!", null);
                return;
            }
        }
    }

    public void calcCircuitBottom() {
        int i;
        circuitBottom = 0;

        // Calculate the lowest point in the circuit layout
        // This is done by iterating over all elements in the circuit
        // and keeping track of the lowest point seen so far
        //
        // For each element, we get its bounding box
        // and check the y-coordinate of the bottom of the box
        // against the current lowest point seen
        // If the element's y-coordinate is lower than the current lowest point,
        // then update the lowest point
        for (i = 0; i != elmList.size(); i++) {
            Rectangle rect = getElm(i).boundingBox;
            int bottom = rect.height + rect.y;
            if (bottom > circuitBottom)
                circuitBottom = bottom;
        }
    }

    public class FindPathInfo 
    {
        static final int INDUCT = 1;
        static final int VOLTAGE = 2;
        static final int SHORT = 3;
        static final int CAP_V = 4;
        final boolean[] used;
        final int dest;
        final CircuitElm firstElm;
        final int type;

        FindPathInfo(int t, CircuitElm e, int d)
        {
            dest = d;
            type = t;
            firstElm = e;
            used = new boolean[nodeList.size()];
        }

        /**
         * Recursively finds a path to a destination node in the circuit.
         * This is a wrapper method that initiates the pathfinding with unlimited depth.
         *
         * @param n1 The starting node for the pathfinding attempt.
         * @return true if a path to the destination node is found, false otherwise.
         */
        boolean findPath(int n1) {
            // Call the overloaded findPath method with the starting node n1
            // and depth set to -1, indicating no depth limit for the search.
            return findPath(n1, -1);
        }

        boolean findPath(int n1, int depth) {
            // Check if the current node is the destination node
            if (n1 == dest)
                return true;

            // If depth limit reached, stop searching
            if (depth-- == 0)
                return false;

            // If this node has already been visited, return false
            if (used[n1]) {
                return false;
            }

            // Mark the current node as used
            used[n1] = true;

            int i;
            // Iterate over all circuit elements
            for (i = 0; i != elmList.size(); i++) {
                CircuitElm ce = getElm(i);

                // Skip the first element
                if (ce == firstElm)
                    continue;

                // Check if the current element matches the path type
                if (type == VOLTAGE) {
                    if (!(ce.isWire() || ce instanceof VoltageElm))
                        continue;
                }

                // If path type is SHORT, ensure element is a wire
                if (type == SHORT && !ce.isWire())
                    continue;

                // If path type is CAP_V, ensure element is a wire or voltage source
                if (type == CAP_V) {
                    if (!(ce.isWire() || ce instanceof VoltageElm))
                        continue;
                }

                // Special case: If starting from node 0, look for ground connections
                if (n1 == 0) {
                    int j;
                    for (j = 0; j != ce.getPostCount(); j++)
                        if (ce.hasGroundConnection(j) && findPath(ce.getNode(j), depth)) {
                            used[n1] = false;
                            return true;
                        }
                }

                int j;
                // Find the post that matches the current node
                for (j = 0; j != ce.getPostCount(); j++) {
                    if (ce.getNode(j) == n1)
                        break;
                }

                // If no matching post found, continue to the next element
                if (j == ce.getPostCount())
                    continue;

                // Check ground connection and search from node 0
                if (ce.hasGroundConnection(j) && findPath(0, depth)) {
                    used[n1] = false;
                    return true;
                }

                int k;
                // Iterate over all posts again for connections
                for (k = 0; k != ce.getPostCount(); k++) {
                    if (j == k)
                        continue;

                    // If a connection is found, continue searching from the connected node
                    if (ce.getConnection(j, k) && findPath(ce.getNode(k), depth)) {
                        used[n1] = false;
                        return true;
                    }
                }
            }

            // Unmark the current node and return false as no path was found
            used[n1] = false;
            return false;
        }
    }

    /**
     * Stops the circuit simulation with a given message and circuit element.
     *
     * @param s  The stop message to be displayed or logged.
     * @param ce The circuit element associated with the stop condition.
     */
    public void stop(String s, CircuitElm ce)
    {
        // Set the stop message to the provided string
        stopMessage = s;
        
        // Nullify the circuit matrix to stop further simulation calculations
        circuitMatrix = null;
        
        // Store the circuit element that caused the stop condition
        stopElm = ce;
        
        // Reset the analyze flag as the simulation is stopped
        analyzeFlag = false;
        
        // Request a repaint of the circuit view to reflect the stopped state
        cv.repaint();
    }
    // Stamp independent voltage source #vs, from n1 to n2, with a voltage of v
    //
    // This function is used to stamp an independent voltage source in the circuit matrix.
    // It takes the following parameters:
    // - n1: The first node of the voltage source
    // - n2: The second node of the voltage source
    // - vs: The number of the voltage source
    // - v: The voltage of the voltage source
    //
    // The function first calculates the node number of the voltage source (vn) by adding the
    // voltage source number to the number of nodes in the circuit.
    // It then stamps the voltage source in the circuit matrix:
    // - The voltage source is connected to the first node (n1) with a negative one
    // - The voltage source is connected to the second node (n2) with a positive one
    // - The right side of the voltage source is set to the voltage of the voltage source
    // - The voltage source is connected to the first node with a positive one
    // - The voltage source is connected to the second node with a negative one
    public void stampVoltageSource(int n1, int n2, int vs, double v)
    {
        int vn = nodeList.size() + vs;
        stampMatrix(vn, n1, -1);
        stampMatrix(vn, n2, 1);
        stampRightSide(vn, v);
        stampMatrix(n1, vn, 1);
        stampMatrix(n2, vn, -1);
    }
    
    // Stamps an independent voltage source into the circuit matrix.
    // This method is used when the voltage amount will be updated in a subsequent step (e.g., in doStep()).
    //
    // Parameters:
    // - n1: The first node of the voltage source.
    // - n2: The second node of the voltage source.
    // - vs: The index/number of the voltage source.
    //
    // The method proceeds by calculating the node number of the voltage source (vn).
    // The voltage source is then stamped into the circuit matrix as follows:
    // - Connect the voltage source to the first node (n1) with a coefficient of -1.
    // - Connect the voltage source to the second node (n2) with a coefficient of +1.
    // - Set the right side of the voltage source to zero, as the actual voltage will be updated later.
    // - Connect the first node to the voltage source with a coefficient of +1.
    // - Connect the second node to the voltage source with a coefficient of -1.
    public void stampVoltageSource(int n1, int n2, int vs)
    {
        int vn = nodeList.size() + vs; // Calculate the voltage source node number
        stampMatrix(vn, n1, -1); // Connect vn to n1 with -1
        stampMatrix(vn, n2, 1);  // Connect vn to n2 with +1
        stampRightSide(vn);      // Initialize right side of vn to zero
        stampMatrix(n1, vn, 1);  // Connect n1 to vn with +1
        stampMatrix(n2, vn, -1); // Connect n2 to vn with -1
    }

    /**
     * Updates the voltage source in the simulation matrix.
     * This method is responsible for setting the voltage of a specific voltage source.
     *
     * Parameters:
     * - n1: The first node of the voltage source.
     * - n2: The second node of the voltage source (not directly used here).
     * - vs: The index/number of the voltage source.
     * - v: The voltage to be set for this voltage source.
     */
    public void updateVoltageSource(int n1, int n2, int vs, double v)
    {
        // Calculate the node number for the voltage source node.
        // This is based on the index of the voltage source and the current size of the node list.
        int vn = nodeList.size() + vs;

        // Set the right side of the voltage source's node in the circuit matrix to the specified voltage.
        // This effectively updates the voltage value for the voltage source in the simulation.
        stampRightSide(vn, v);
    }

    public void stampResistor(int n1, int n2, double r)
    {
        // Calculate the inverse of the resistance (i.e., the conductance)
        double r0 = 1 / r;
        
        // Sanity check to ensure that the conductance is a valid number
        if (Double.isNaN(r0) || Double.isInfinite(r0))
        {
            System.out.print("Encountered an invalid resistance value of " + r + ", which maps to " + r0 + "\n");
            // This is a dummy line to prevent the compiler from complaining about the if statement
            // being useless. It's not actually used anywhere, but it's a nice way to check the
            // sanity of the resistance value.
            int a = 0;
            a /= a;
        }
        
        // Stamp the resistor connections into the circuit matrix
        // The stampMatrix method takes the following parameters:
        // - The row number of the node to be stamped
        // - The column number of the node to be stamped
        // - The value to be stamped
        // In this case, we're stamping the resistor connections between nodes n1 and n2
        // The first call stamps the connection from n1 to n1 with a value of r0
        // The second call stamps the connection from n2 to n2 with a value of r0
        // The third call stamps the connection from n1 to n2 with a value of -r0
        // The fourth call stamps the connection from n2 to n1 with a value of -r0
        stampMatrix(n1, n1, r0);
        stampMatrix(n2, n2, r0);
        stampMatrix(n1, n2, -r0);
        stampMatrix(n2, n1, -r0);
    }

    /**
     * Stamps the conductance connections for a resistor in the circuit matrix.
     * 
     * This method takes in the node numbers of the two nodes the resistor is connected to,
     * as well as the conductance value (i.e., the inverse of the resistance).
     * 
     * It then stamps the following connections into the circuit matrix:
     * - A connection from node n1 to node n1 with a value of r0
     * - A connection from node n2 to node n2 with a value of r0
     * - A connection from node n1 to node n2 with a value of -r0
     * - A connection from node n2 to node n1 with a value of -r0
     * 
     * This is equivalent to adding the following circuit elements to the circuit matrix:
     * - A conductance of r0 from node n1 to ground
     * - A conductance of r0 from node n2 to ground
     * - A conductance of r0 from node n1 to node n2
     * - A conductance of r0 from node n2 to node n1
     * 
     * Note that the conductance values are negative for the connections from node n1 to node n2
     * and from node n2 to node n1. This is because we're using a nodal analysis approach to construct
     * the circuit matrix, which means that we're solving for the voltage at each node in the circuit.
     * When we add a conductance from node n1 to node n2, we're effectively adding a current source
     * to node n1 that is proportional to the voltage difference between node n1 and node n2. This
     * current source has a negative sign because it's a current flowing out of node n1 and into node n2.
     * 
     * @param n1 The node number of the first node the resistor is connected to.
     * @param n2 The node number of the second node the resistor is connected to.
     * @param r0 The conductance value of the resistor.
     */
    public void stampConductance(int n1, int n2, double r0)
    {
        stampMatrix(n1, n1, r0);
        stampMatrix(n2, n2, r0);
        stampMatrix(n1, n2, -r0);
        stampMatrix(n2, n1, -r0);
    }

    /**
     * Stamps a current source into the circuit matrix.
     * 
     * When we stamp a current source, we're effectively adding a current source
     * to the circuit that is connected between two nodes. This current source
     * supplies a constant current to the circuit, regardless of the voltage
     * difference between the two nodes.
     * 
     * The way we stamp the current source is by adding the following circuit
     * elements to the circuit matrix:
     * - A current source of i amperes connected from node n1 to ground
     * - A current source of -i amperes connected from node n2 to ground
     * 
     * This is equivalent to adding -i amperes to the right side of the equation
     * for node n1, and adding i amperes to the right side of the equation for
     * node n2.
     * 
     * @param n1 The node number of the first node the current source is connected to.
     * @param n2 The node number of the second node the current source is connected to.
     * @param i The current value of the current source in amperes.
     */
    public void stampCurrentSource(int n1, int n2, double i)
    {
        // Add -i amperes to the right side of the equation for node n1
        stampRightSide(n1, -i);

        // Add i amperes to the right side of the equation for node n2
        stampRightSide(n2, i);
    }

    // stamp value x in row i, column j, meaning that a voltage change
    // of dv in node j will increase the current into node i by x dv.
    // (Unless i or j is a voltage source node.)
    // 
    // This is the core of the nodal analysis algorithm. It adds a
    // connection with a value of x between nodes i and j in the
    // circuit matrix. This is equivalent to saying that a change in
    // voltage of dv in node j will cause a change in current of x dv
    // into node i.
    // 
    // If node i or node j is a voltage source node, then this is a
    // bit more complicated. Voltage source nodes are handled
    // specially in the circuit matrix. The voltage source node is
    // represented by a constant current source, which is
    // represented by a special kind of circuit element in the
    // circuit matrix. When we add a connection between a node and a
    // voltage source node, we need to subtract the voltage source's
    // current from the right side of the equation for the node.
    // 
    // We also need to handle the case where node i or node j is a
    // constant current source node. Constant current source nodes
    // are also represented by a special kind of circuit element in
    // the circuit matrix. When we add a connection between a node and
    // a constant current source node, we need to subtract the
    // constant current source's current from the right side of the
    // equation for the node.
    // 
    // Finally, we need to handle the case where node i or node j is
    // a node that has a voltage-controlled current source (VCCS)
    // connected to it. VCCS nodes are represented by a special kind
    // of circuit element in the circuit matrix. When we add a
    // connection between a node and a VCCS node, we need to add the
    // VCCS's current to the right side of the equation for the node.
    // 
    // The overall strategy is to figure out which special kind of
    // node we're dealing with, and then handle it accordingly. We
    // first check if the node is a voltage source node. If it is,
    // then we subtract the voltage source's current from the right
    // side of the equation for the node. If it's not a voltage source
    // node, then we check if it's a constant current source node. If
    // it is, then we subtract the constant current source's current
    // from the right side of the equation for the node. If it's not a
    // constant current source node, then we check if it's a VCCS
    // node. If it is, then we add the VCCS's current to the right side
    // of the equation for the node.
    // 
    // If none of the above applies, then we just add the connection
    // to the circuit matrix as usual.
    public void stampMatrix(int i, int j, double x)
    {
        if (i > 0 && j > 0)
        {
            if (circuitNeedsMap)
            {
                i = circuitRowInfo[i - 1].mapRow;
                RowInfo ri = circuitRowInfo[j - 1];
                if (ri.type == RowInfo.ROW_CONST)
                {
                    circuitRightSide[i] -= x * ri.value;
                    return;
                }
                j = ri.mapCol;
            } else
            {
                i--;
                j--;
            }
            circuitMatrix[i][j] += x;
        }
    }

    /**
     * Stamps the specified value on the right side of row i, representing an
     * independent current source flowing into node i.
     * 
     * This method updates the circuit's right-side vector, which represents
     * the net current flowing into each node. The value x is added to the
     * right side of the specified row i in the circuit matrix. This effectively
     * models an independent current source that injects current into the 
     * corresponding node.
     * 
     * If the circuit requires a mapping, we use the mapped row index from 
     * the circuitRowInfo array. Otherwise, we directly decrement the row 
     * index by one to access the correct position in the matrix.
     * 
     * @param i The row index corresponding to the node where the current 
     *          source is connected.
     * @param x The value of the current source to be added to the right 
     *          side of the equation.
     */
    public void stampRightSide(int i, double x)
    {
        // Ensure the row index is greater than zero
        if (i > 0)
        {
            // Check if the circuit requires mapping of the row index
            if (circuitNeedsMap)
            {
                // Map the row index using circuitRowInfo
                i = circuitRowInfo[i - 1].mapRow;
                // For debugging: print the row index and value being stamped
                // System.out.println("stamping " + i + " " + x);
            } 
            else
            {
                // Adjust the row index for zero-based indexing
                i--;
            }
            // Add the current source value to the right side of the row in the circuit matrix
            circuitRightSide[i] += x;
        }
    }

    // Indicate that the value on the right side of row i changes in doStep()
    // This function is used to mark rows in the circuit matrix as having
    // changed, so that we know to re-compute them in the next iteration.
    // The right side of the equation is the part that contains the
    // current source values, so if the current source value changes,
    // we need to re-compute the right side of the equation.
    // 
    // The reason we need to do this is because the circuit matrix is
    // sparse, meaning that most of the elements are zero. When we
    // change the value of a current source, we need to update the
    // corresponding row in the circuit matrix, but we only need to
    // update the non-zero elements of the row. This is more efficient
    // than re-computing the entire row.
    // 
    // The way we do this is by marking the rows that have changed
    // with a boolean flag, and then when we go to compute the
    // solution, we only update the rows that have changed.
    // 
    // This function takes a row index as an argument, and marks the
    // corresponding row in the circuit matrix as having changed.
    public void stampRightSide(int i)
    {
        // System.out.println("rschanges true " + (i-1));
        if (i > 0)
        {
            // Mark the row as having changed
            circuitRowInfo[i - 1].rsChanges = true;
        }
    }

    // indicate that the values on the left side of row i change in doStep()
    public void stampNonLinear(int i)
    {
        // Mark the row as having changed
        // The reason we need to do this is because the left side of the equation
        // contains the non-linear elements of the circuit, like diodes and
        // transistors. When we change the value of one of these elements, we
        // need to update the corresponding row in the circuit matrix, but we
        // only need to update the non-zero elements of the row. This is more
        // efficient than re-computing the entire row.
        // 
        // The way we do this is by marking the rows that have changed
        // with a boolean flag, and then when we go to compute the
        // solution, we only update the rows that have changed.
        if (i > 0)
            circuitRowInfo[i - 1].lsChanges = true;
    }

    // Gets the iteration count for the current simulation
    // 
    // This function takes no arguments and returns a double value
    // representing the number of iterations that should be run in the
    // circuit simulation. The number of iterations is determined by the
    // value of the speedBar, which is a JSlider that allows the user
    // to control the speed of the simulation. The value of the speedBar
    // is mapped to an iteration count using an exponential function.
    // 
    // The mapping is as follows: the value of the speedBar ranges from
    // 0 to 100, and the iteration count ranges from 0 to infinity. The
    // mapping is exponential, so that as the user moves the speedBar
    // from 0 to 100, the iteration count increases rapidly. This allows
    // the user to quickly speed up or slow down the simulation.
    // 
    // The function first checks if the speedBar is at its minimum value
    // (i.e. 0). If it is, then the function returns 0, indicating that
    // the simulation should not run at all. Otherwise, the function
    // computes the iteration count using the following formula:
    // 
    // iterCount = 0.1 * exp((speedBarValue - 61) / 24)
    // 
    // This formula is an exponential function that maps the speedBar
    // value to an iteration count. The constant 0.1 is used to scale
    // the iteration count so that it is reasonable for most circuits.
    // The constant 61 is used to shift the exponential function so
    // that the iteration count is reasonable for most circuits. The
    // constant 24 is used to scale the exponential function so that
    // it maps the speedBar value to a reasonable iteration count.
    // 
    // The function returns the computed iteration count, which is then
    // used to control the number of iterations that are run in the
    // circuit simulation.
    public double getIterCount()
    {
        if (speedBar.getValue() == 0)
            return 0;
        // return (Math.exp((speedBar.getValue()-1)/24.) + .5);
        return .1 * Math.exp((speedBar.getValue() - 61) / 24.);
    }

    public boolean converged;
    public int subIterations;


    // The main circuit simulation loop. This method is called repeatedly
    // to update the circuit. It is responsible for running the circuit
    // simulation and updating the graphics.
    // 
    // The simulation is run in a loop, with the number of iterations
    // determined by the value of the speedBar. The value of the speedBar
    // is used to determine the number of iterations to run in the
    // circuit simulation.
    // 
    // The simulation is run as follows:
    // 
    // 1. The number of iterations is determined.
    // 2. The circuit matrix is solved iteratively.
    // 3. The voltage sources are updated.
    // 4. The circuit nodes are updated.
    // 5. The circuit elements are updated.
    // 6. The graphics are updated.
    // 
    public void runCircuit()
    {
        if (circuitMatrix == null || elmList.size() == 0)
        {
            circuitMatrix = null;
            return;
        }
        int iter;
        // int maxIter = getIterCount();
        boolean debugprint = dumpMatrix;
        dumpMatrix = false;
        // compute the desired iteration rate in milliseconds
        long steprate = (long) (160 * getIterCount());
        long tm = System.currentTimeMillis();
        long lit = lastIterTime;
        // skip the iteration if the desired iteration rate has not been
        // exceeded
        if (1000 >= steprate * (tm - lastIterTime))
            return;
        // iterate over the circuit matrix
        for (iter = 1; ; iter++)
        {
            int i, j, k, subiter;
            // start the iteration for each element in the circuit
            for (i = 0; i != elmList.size(); i++)
            {
                CircuitElm ce = getElm(i);
                ce.startIteration();
            }
            steps++;
            // compute the number of subiterations required
            final int subiterCount = 5000;
            // iterate over the subiterations
            for (subiter = 0; subiter != subiterCount; subiter++)
            {
                converged = true;
                subIterations = subiter;
                // copy the original right side of the matrix to the
                // current right side
                for (i = 0; i != circuitMatrixSize; i++)
                    circuitRightSide[i] = origRightSide[i];
                // copy the original matrix to the current matrix
                if (circuitNonLinear)
                {
                    for (i = 0; i != circuitMatrixSize; i++)
                        for (j = 0; j != circuitMatrixSize; j++)
                            circuitMatrix[i][j] = origMatrix[i][j];
                }
                // do a step for each element in the circuit
                for (i = 0; i != elmList.size(); i++)
                {
                    CircuitElm ce = getElm(i);
                    ce.doStep();
                }
                // check for any stop messages
                if (stopMessage != null)
                    return;
                boolean printit = debugprint;
                debugprint = false;
                // check for any nan or infinite values in the matrix
                for (j = 0; j != circuitMatrixSize; j++)
                {
                    for (i = 0; i != circuitMatrixSize; i++)
                    {
                        double x = circuitMatrix[i][j];
                        if (Double.isNaN(x) || Double.isInfinite(x))
                        {
                            stop("nan/infinite matrix!", null);
                            return;
                        }
                    }
                }
                // print the matrix
                if (printit)
                {
                    for (j = 0; j != circuitMatrixSize; j++)
                    {
                        for (i = 0; i != circuitMatrixSize; i++)
                            System.out.print(circuitMatrix[j][i] + ",");
                        System.out.print("  " + circuitRightSide[j] + "\n");
                    }
                    System.out.print("\n");
                }
                // check if the matrix is singular
                if (circuitNonLinear)
                {
                    if (converged && subiter > 0)
                        break;
                    if (!lu_factor(circuitMatrix, circuitMatrixSize, circuitPermute))
                    {
                        stop("Singular matrix!", null);
                        return;
                    }
                }
                // solve the matrix
                lu_solve(circuitMatrix, circuitMatrixSize, circuitPermute, circuitRightSide);

                // copy the results to the circuit nodes and voltage sources
                for (j = 0; j != circuitMatrixFullSize; j++)
                {
                    RowInfo ri = circuitRowInfo[j];
                    double res = 0;
                    if (ri.type == RowInfo.ROW_CONST)
                        res = ri.value;
                    else
                        res = circuitRightSide[ri.mapCol];
					/*
					 * System.out.println(j + " " + res + " " + ri.type + " " +
					 * ri.mapCol);
					 */
                    if (Double.isNaN(res))
                    {
                        converged = false;
                        // debugprint = true;
                        break;
                    }
                    if (j < nodeList.size() - 1)
                    {
                        CircuitNode cn = getCircuitNode(j + 1);
                        for (k = 0; k != cn.links.size(); k++)
                        {
                            CircuitNodeLink cnl = (CircuitNodeLink) cn.links.elementAt(k);
                            cnl.elm.setNodeVoltage(cnl.num, res);
                        }
                    } else
                    {
                        int ji = j - (nodeList.size() - 1);
                        // System.out.println("setting vsrc " + ji + " to " +
                        // res);
                        voltageSources[ji].setCurrent(ji, res);
                    }
                }
                // check if the circuit has converged
                if (!circuitNonLinear)
                    break;
            }
            // print a message if the circuit has not converged
            if (subiter > 5)
                System.out.print("converged after " + subiter + " iterations\n");
            // stop the simulation if the circuit has not converged
            if (subiter == subiterCount)
            {
                stop("Convergence failed!", null);
                break;
            }
            // update the time
            t += timeStep;
            // update the scopes
            for (i = 0; i != scopeCount; i++)
                scopes[i].timeStep();
            // update the last iteration time
            tm = System.currentTimeMillis();
            lit = tm;
            // check if the desired iteration rate has been exceeded
            if (iter * 1000 >= steprate * (tm - lastIterTime) || (tm - lastFrameTime > 500))
                break;
        }
        // update the last iteration time
        lastIterTime = lit;
        // print a message showing the iteration rate
        // System.out.println((System.currentTimeMillis()-lastFrameTime)/(double)
        // iter);
    }

    /**
     * Returns the minimum of two integers.
     * 
     * This is a simple function that takes two integers as arguments and returns
     * the minimum of the two. This function is used to limit the number of
     * iterations that the simulation runs to a reasonable value.
     * 
     * @param a The first integer to be compared.
     * @param b The second integer to be compared.
     * @return The minimum of the two integers.
     */
    public int min(int a, int b)
    {
        // If a is less than b, return a
        if (a < b)
            return a;
        // Otherwise, return b
        return b;
    }

    /**
     * Returns the maximum of two integers.
     * 
     * This function takes two integers as arguments and returns the maximum of
     * the two. The maximum is determined by comparing the two integers and
     * returning the larger of the two.
     * 
     * @param a The first integer to be compared.
     * @param b The second integer to be compared.
     * @return The maximum of the two integers.
     */
    public int max(int a, int b)
    {
        // If a is greater than b, return a
        if (a > b)
            return a;
        // Otherwise, return b
        return b;
    }

    public void actionPerformed(ActionEvent e)
    {
        String ac = e.getActionCommand();

        // When the reset button is pressed, reset all elements, and all scopes.
        // This is done by calling reset() on all elements, and resetGraph() on
        // all scopes.
        if (e.getSource() == resetButton)
        {
            int i;

            // See the comment above the declaration of dbimage for why we do
            // this.
            dbimage = main.createImage(winSize.width, winSize.height);

            // Reset all elements.
            for (i = 0; i != elmList.size(); i++)
                getElm(i).reset();

            // Reset all scopes.
            for (i = 0; i != scopeCount; i++)
                scopes[i].resetGraph();

            // Set the flag to cause the circuit to be re-analyzed.
            analyzeFlag = true;

            // Reset the time.
            t = 0;

            // Repaint the circuit view.
            cv.repaint();
        }

        // When the "Dump Matrix" button is pressed, cause the circuit matrix to
        // be dumped to the console when the circuit is analyzed.
        if (e.getSource() == dumpMatrixButton)
            dumpMatrix = true;

        // When the "Export" menu item is selected, cause the circuit to be
        // exported to a file.
        if (e.getSource() == exportItem)
            doImport(false, false);

        // When the "Options" menu item is selected, cause the EditOptions dialog
        // to be displayed.
        if (e.getSource() == optionsItem)
            doEdit(new EditOptions(this));

        // When the "Import" menu item is selected, cause the ImportDialog to be
        // displayed.
        if (e.getSource() == importItem)
            doImport(true, false);

        // When the "Export Link" menu item is selected, cause the ImportDialog to
        // be displayed, but with the "Import from URL" option selected.
        if (e.getSource() == exportLinkItem)
            doImport(false, true);

        // When the "Select All" menu item is selected, cause all elements in the
        // circuit to be selected.
        if (e.getSource() == selectAllItem)
            doSelectAll();

        // When the "Exit" menu item is selected, cause the application frame to
        // be destroyed.
        if (e.getSource() == exitItem)
        {
            destroyFrame();
            return;
        }

        // When the "Stack All" menu item is selected, cause all scopes to be
        // stacked on top of each other.
        if (ac.compareTo("stackAll") == 0)
            stackAll();

        // When the "Unstack All" menu item is selected, cause all scopes to be
        // unstacked.
        if (ac.compareTo("unstackAll") == 0)
            unstackAll();

        // When the "Edit" menu item is selected, cause the EditInfo dialog to be
        // displayed for the element that was previously selected.
        if (e.getSource() == elmEditMenuItem)
            doEdit(menuElm);

        // When the "Delete" menu item is selected, cause all selected elements
        // to be deleted.
        if (ac.compareTo("Delete") == 0)
        {
            if (e.getSource() != elmDeleteMenuItem)
                menuElm = null;
            doDelete();
        }

        // When the "Scope" menu item is selected for an element, cause the
        // element to be added to the end of the list of scopes if it is not
        // already there, or cause it to be removed from the list of scopes if
        // it is already there.
        if (e.getSource() == elmScopeMenuItem && menuElm != null)
        {
            int i;
            for (i = 0; i != scopeCount; i++)
                if (scopes[i].elm == null)
                    break;
            if (i == scopeCount)
            {
                if (scopeCount == scopes.length)
                    return;
                scopeCount++;
                scopes[i] = new Scope(this);
                scopes[i].position = i;
                handleResize();
            }
            scopes[i].setElm(menuElm);
        }

        // When any of the scope menu items are selected, cause the appropriate
        // action to be taken on the selected scope.
        if (menuScope != -1)
        {
            if (ac.compareTo("remove") == 0)
                scopes[menuScope].setElm(null);
            if (ac.compareTo("speed2") == 0)
                scopes[menuScope].speedUp();
            if (ac.compareTo("speed1/2") == 0)
                scopes[menuScope].slowDown();
            if (ac.compareTo("scale") == 0)
                scopes[menuScope].adjustScale(.5);
            if (ac.compareTo("maxscale") == 0)
                scopes[menuScope].adjustScale(1e-50);
            if (ac.compareTo("stack") == 0)
                stackScope(menuScope);
            if (ac.compareTo("unstack") == 0)
                unstackScope(menuScope);
            if (ac.compareTo("selecty") == 0)
                scopes[menuScope].selectY();
            if (ac.compareTo("reset") == 0)
                scopes[menuScope].resetGraph();
            cv.repaint();
        }

        // When any of the setup menu items are selected, cause the circuit to be
        // set up from the selected file.
        if (ac.indexOf("setup ") == 0)
        {
            // pushUndo();
            readSetupFile(ac.substring(6), ((MenuItem) e.getSource()).getLabel());
        }
    }

    /**
     * Stacks the scope at index s on top of the previous scope.
     * @param s the index of the scope to stack
     */
    public void stackScope(int s)
    {
        if (s == 0)
        {
            // If we're trying to stack the first scope, do nothing unless there are
            // multiple scopes.
            if (scopeCount < 2)
                return;
            // Otherwise, stack the second scope on top of the first.
            s = 1;
        }
        // If the scope we're trying to stack is already on top of the previous
        // scope, do nothing.
        if (scopes[s].position == scopes[s - 1].position)
            return;
        // Move the scope we're trying to stack to the position of the previous
        // scope.
        scopes[s].position = scopes[s - 1].position;
        // Move all subsequent scopes down one position.
        for (s++; s < scopeCount; s++)
            scopes[s].position--;
    }

    public void unstackScope(int s)
    {
        // If we're trying to unstack the first scope and there's only one
        // scope, do nothing.
        if (s == 0 && scopeCount < 2)
            return;
        // If we're trying to unstack the first scope and there are multiple
        // scopes, shift the second scope up to the top.
        if (s == 0)
            s = 1;
        // If the scope we're trying to unstack is not on top of the previous
        // scope, do nothing.
        if (scopes[s].position != scopes[s - 1].position)
            return;
        // Move the scope we're trying to unstack down one position, and
        // move all subsequent scopes down one position as well.
        for (; s < scopeCount; s++)
            scopes[s].position++;
    }

    public void stackAll()
    {
        // This function is called when the "Stack All" menu item is selected.
        // It stacks all of the scopes on top of the first scope.
        int i;
        for (i = 0; i != scopeCount; i++)
        {
            // Set the position of each scope to 0, which is the position of the
            // first scope.
            scopes[i].position = 0;
            // Set showMax to false so that the maximum value for the scope won't
            // be shown, and set showMin to false so that the minimum value for
            // the scope won't be shown. This is done so that the values for all
            // of the scopes except the first one won't be shown.
            scopes[i].showMax = scopes[i].showMin = false;
        }
    }

    /**
     * Unstack all of the scopes so that each scope is on a different row
     * and shows its maximum value.
     */
    public void unstackAll()
    {
        int i;
        // Loop over all of the scopes.
        for (i = 0; i != scopeCount; i++)
        {
            // Set the position of each scope to the scope's index.
            // This means that each scope will be on a different row.
            scopes[i].position = i;
            // Set the showMax flag to true for each scope, which means that
            // the maximum value for each scope will be shown.
            scopes[i].showMax = true;
        }
    }

    /**
     * This function is called when the user wants to edit the properties of
     * a component. It takes an Editable object as an argument, which is an
     * interface that is implemented by the components that can be edited.
     * The function first clears the selection, which means that it will remove
     * all of the selected components from the selection list.
     * 
     * If the editDialog is not null, it means that the edit dialog is already
     * showing, so we hide it and set it to null. This is done so that we can
     * prevent multiple edit dialogs from being opened at the same time.
     * 
     * We create a new EditDialog object and pass the Editable object and this
     * CirSim object to it. We then call the show() method on the EditDialog
     * object, which will make the dialog visible.
     * 
     */
    public void doEdit(Editable eable)
    {
        clearSelection();
        // pushUndo();
        if (editDialog != null)
        {
            requestFocus();
            editDialog.setVisible(false);
            editDialog = null;
        }
        editDialog = new EditDialog(eable, this);
        editDialog.show();
    }

    /**
     * This function is called when the user wants to import a circuit from a string.
     * 
     * It takes two boolean arguments, imp and url. The imp argument is true if the
     * user wants to import a circuit string, and false if the user wants to export
     * the current circuit to a string. The url argument is true if the user wants
     * to import a circuit string from a URL, and false if the user wants to import
     * a circuit string from a text area.
     * 
     * If the impDialog is not null, it means that the import dialog is already
     * showing, so we hide it and set it to null. This is done so that we can prevent
     * multiple import dialogs from being opened at the same time.
     * 
     * If the user wants to import a circuit string, we don't need to do anything
     * else. If the user wants to export the current circuit to a string, we get
     * the string representation of the circuit by calling the dumpCircuit() method.
     * 
     * If the user wants to import a circuit string from a URL, we append the
     * string representation of the circuit to the baseURL and URL encode it.
     * 
     * Finally, we create a new ImportDialog object and pass it the CirSim object,
     * the circuit string (or the URL encoded string), and the imp and url booleans.
     * We then show the dialog by calling the show() method on it.
     */
    public void doImport(boolean imp, boolean url)
    {
        if (impDialog != null)
        {
            requestFocus();
            impDialog.setVisible(false);
            impDialog = null;
        }
        String dump = (imp) ? "" : dumpCircuit();
        if (url)
            dump = baseURL + "#" + URLEncoder.encode(dump);
        impDialog = new ImportDialog(this, dump, url, imp);
        impDialog.show();
        // pushUndo();
    }

    public String dumpCircuit() {
        // Variable to iterate over elements and scopes
        int i;
        
        // Initialize flags based on the state of various check items
        // If dotsCheckItem is selected, set the first bit
        int f = (dotsCheckItem.getState()) ? 1 : 0;
        
        // If smallGridCheckItem is selected, set the second bit
        f |= (smallGridCheckItem.getState()) ? 2 : 0;
        
        // If voltsCheckItem is not selected, set the third bit
        f |= (voltsCheckItem.getState()) ? 0 : 4;
        
        // If powerCheckItem is selected, set the fourth bit
        f |= (powerCheckItem.getState()) ? 8 : 0;
        
        // If showValuesCheckItem is not selected, set the fifth bit
        f |= (showValuesCheckItem.getState()) ? 0 : 16;
        
        // 32 = linear scale in afilter (not used here but reserved)
        
        // Create a dump string with flags, timestep, iteration count, current 
        // bar value, voltage range, and power bar value
        String dump = "$ " + f + " " + timeStep + " " + getIterCount() + " " +
                      currentBar.getValue() + " " + CircuitElm.voltageRange + " " +
                      powerBar.getValue() + "\n";
        
        // Append dump data of each circuit element
        for (i = 0; i != elmList.size(); i++) {
            dump += getElm(i).dump() + "\n";
        }
        
        // Append dump data of each scope
        for (i = 0; i != scopeCount; i++) {
            String d = scopes[i].dump();
            if (d != null) {
                dump += d + "\n";
            }
        }
        
        // Append hint data if any hint is active
        if (hintType != -1) {
            dump += "h " + hintType + " " + hintItem1 + " " + hintItem2 + "\n";
        }
        
        // Return the complete dump string
        return dump;
    }

    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        // Get the source of the adjustment event, which is expected to be a Scrollbar
        Scrollbar sourceScrollbar = (Scrollbar) e.getSource();
        
        // Retrieve the current value of the scrollbar
        int scrollbarValue = sourceScrollbar.getValue();
        
        // Print the current value of the scrollbar followed by a newline character
        System.out.print(scrollbarValue + "\n");
    }

    /**
     * Reads data from a URL and returns it as a ByteArrayOutputStream.
     * 
     * This function takes a URL as an argument and reads its contents into a
     * ByteArrayOutputStream. It is used to read data from a URL, such as a file
     * or a web page.
     * 
     * The function is given below:
     * 
     * 1.  The function first calls the getContent() method on the URL object
     *     passed to it. This method returns an object, which is then cast to a
     *     FilterInputStream.
     * 
     * 2.  The function then creates a ByteArrayOutputStream object. This object
     *     is used to store the data read from the URL.
     * 
     * 3.  The function then reads the data from the URL by calling the read()
     *     method on the FilterInputStream object. The read() method takes a byte
     *     array as an argument and returns the number of bytes read. The
     *     function then writes the data to the ByteArrayOutputStream object by
     *     calling the write() method on it.
     * 
     * 4.  The function then returns the ByteArrayOutputStream object.
     * 
     * @param url the URL to read from
     * @return a ByteArrayOutputStream containing the data read from the URL
     * @throws IOException if the data could not be read from the URL
     */
    public ByteArrayOutputStream readUrlData(URL url) throws java.io.IOException
    {
        // Call the getContent() method on the URL object and cast the result to a FilterInputStream
        Object o = url.getContent();
        FilterInputStream fis = (FilterInputStream) o;
        
        // Create a ByteArrayOutputStream object to store the data read from the URL
        ByteArrayOutputStream ba = new ByteArrayOutputStream(fis.available());
        
        // Read the data from the URL by calling the read() method on the FilterInputStream object
        int blen = 1024;
        byte b[] = new byte[blen];
        while (true)
        {
            // Call the read() method on the FilterInputStream object and store the result in the len variable
            int len = fis.read(b);
            
            // If the read() method returns 0 or less, then there is no more data to read so break out of the loop
            if (len <= 0)
                break;
            
            // Write the data to the ByteArrayOutputStream object by calling the write() method on it
            ba.write(b, 0, len);
        }
        
        // Return the ByteArrayOutputStream object
        return ba;
    }

    /**
     * Returns the codebase for this application as a URL.
     * 
     * The codebase is the location of the data files for this application. In
     * this case, the data files are located in the "data" directory in the same
     * directory as the class file for this class.
     * 
     * The function first tries to get the canonical path of the current working
     * directory. This is done by creating a new File object with "." as the
     * argument, and then calling the getCanonicalPath() method on it. This
     * method returns the canonical path of the file, which is the path with all
     * symbolic links resolved.
     * 
     * The function then creates a new URL object by calling the URL constructor
     * with the string "file:" plus the canonical path plus "/data/" as the
     * argument. This creates a URL that points to the "data" directory in the
     * same directory as the class file for this class.
     * 
     * If any of the above operations fail, the function catches the exception,
     * prints a stack trace, and returns null.
     * 
     * @return the codebase for this application as a URL
     */
    public URL getCodeBase()
    {
        try
        {
            // Get the canonical path of the current working directory
            File f = new File(".");
            String canonicalPath = f.getCanonicalPath();

            // Create a new URL object that points to the "data" directory in the
            // same directory as the class file for this class
            URL codebase = new URL("file:" + canonicalPath + "/data/");

            // Return the URL
            return codebase;
        } catch (Exception e)
        {
            // If any of the above operations fail, catch the exception and print
            // a stack trace
            e.printStackTrace();

            // Return null
            return null;
        }
    }

    public void getSetupList(Menu menu, boolean retry)
    {
        // This function is used to build a menu of circuit simulations
        // that can be loaded. The menu is built by reading a file called
        // "setuplist.cfg" in the "cis" package. The file contains a list of
        // lines, each of which contains a title and a file name. The title
        // is used as the label for a menu item, and the file name is used
        // as the argument to the "setup" command.

        // The menu is built by creating a stack of menus, and then adding
        // the menu items to the stack. The stack is used to keep track of
        // the current menu, and the menus are used to group related menu
        // items together.

        // The function first tries to read the file. If the file can't be
        // read, it prints an error message and returns.

        Menu stack[] = new Menu[6];
        int stackptr = 0;
        stack[stackptr++] = menu;

        try
        {
            // Read the file into a byte array
            ByteArrayOutputStream ba = readUrlData(getClass().getResource("/cis/setuplist.cfg"));
            byte b[] = ba.toByteArray();
            int len = ba.size();
            int p;

            // Check if the file was read successfully
            if (len == 0 || b[0] != '#')
            {
                // If the file could not be read, print an error message and
                // try again
                System.out.println("Unable to read setuplist.cfg, trying again");
                getSetupList(menu, true);
                return;
            }

            // Loop through the file and parse each line
            for (p = 0; p < len; )
            {
                int l;
                for (l = 0; l != len - p; l++)
                    if (b[l + p] == '\n')
                    {
                        l++;
                        break;
                    }
                String line = new String(b, p, l - 1);

                // Check if the line is a comment
                if (line.charAt(0) == '#')
                    ;
                else if (line.charAt(0) == '+')
                {
                    // If the line starts with a '+', create a new menu and
                    // add it to the stack
                    Menu n = new Menu(line.substring(1));
                    menu.add(n);
                    menu = stack[stackptr++] = n;
                } else if (line.charAt(0) == '-')
                {
                    // If the line starts with a '-', pop the top menu off the
                    // stack
                    menu = stack[--stackptr - 1];
                } else
                {
                    // If the line doesn't start with a '+' or '-', then it
                    // must contain a title and a file name. Split the line
                    // into two parts and create a new menu item with the title
                    // and the file name as the argument to the "setup"
                    // command.
                    int i = line.indexOf(' ');
                    if (i > 0)
                    {
                        String title = line.substring(i + 1);
                        boolean first = false;
                        if (line.charAt(0) == '>')
                            first = true;
                        String file = line.substring(first ? 1 : 0, i);
                        menu.add(getMenuItem(title, "setup " + file));
                        if (first && startCircuit == null)
                        {
                            startCircuit = file;
                            startLabel = title;
                        }
                    }
                }
                p += l;
            }
        } catch (Exception e)
        {
            // If anything goes wrong, print an error message and stop the
            // program
            e.printStackTrace();
            stop("Can't read setuplist.txt!", null);
        }
    }

    /**
     * Reads a setup from a string.
     * This method invokes the readSetup method with the provided text
     * and a retain flag set to false, indicating that existing elements
     * in the circuit will be cleared before reading the new setup.
     *
     * @param text
     *            String containing the setup to read
     */
    public void readSetup(String text)
    {
        // Call the readSetup method with the given text and retain flag set to false
        // This means the circuit will be cleared before reading the new setup
        readSetup(text, false);
    }

    /**
     * Reads a setup from a string, which can be used for both reading the setup
     * from a file or a string entered by the user.
     *
     * @param text
     *            A string containing the setup to read. This string is expected
     *            to represent the configuration of the circuit elements.
     * @param retain
     *            A boolean flag indicating whether to retain the existing
     *            elements in the circuit. If set to true, the new elements
     *            from the setup will be added to the current elements in the
     *            circuit. If set to false, the current circuit will be cleared
     *            of all elements before adding the new elements from the setup.
     */
    public void readSetup(String text, boolean retain)
    {
        // Convert the input text string into a byte array. This is necessary
        // because the underlying readSetup method expects a byte array.
        byte[] byteArray = text.getBytes();

        // Determine the length of the input text. This length is needed
        // to correctly process the byte array in the readSetup method.
        int textLength = text.length();
        
        // Call the readSetup method with the byte array, its length, and
        // the retain flag. This processes the circuit setup based on the
        // provided text and retain option.
        readSetup(byteArray, textLength, retain);

        // Set the title label of the circuit setup to "untitled". This is
        // likely used to indicate that the current setup has not been assigned
        // a specific title or has not been saved under a particular name.
        titleLabel.setText("untitled");
    }

    public void readSetupFile(String str, String title)
    {
        // Initialize the time variable to zero, which might be used for simulation timing or reset
        t = 0;

        // Print the file name or path to the console for debugging purposes
        System.out.println(str);

        try
        {
            // Create a ByteArrayOutputStream to hold the data read from the specified setup file.
            // The file is located in the "cis" directory and the path is constructed using the provided string.
            ByteArrayOutputStream ba = readUrlData(getClass().getResource("/cis/" + str));

            // Convert the data in the ByteArrayOutputStream to a byte array and read the setup.
            // The readSetup method will process the byte array and set up the circuit elements.
            // The third parameter 'false' indicates that existing elements should not be retained.
            readSetup(ba.toByteArray(), ba.size(), false);
        } catch (Exception e)
        {
            // If any exception occurs during the file reading process, print the stack trace for debugging.
            e.printStackTrace();

            // Stop the execution and display an error message indicating the file could not be read.
            stop("Unable to read " + str + "!", null);
        }

        // Set the window title to include the application version and the provided title.
        super.setTitle(appVersion + " - " + title);
    }

    public void componentShown(ComponentEvent e)
    {
        // When the window is shown, such as when it is restored from being minimized,
        // we need to redraw the canvas to ensure that the circuit elements are visible.
        // This is necessary because the canvas is not redrawn when the window is minimized
        // and then restored. If the window is resized while it is minimized, the canvas
        // will not be resized and the circuit elements will not be redrawn until the window
        // is restored. This repaint call ensures that the circuit elements are redrawn when
        // the window is restored.
        cv.repaint();
    }

    /**
     * Handles the event where the window is resized.
     * This method is a callback invoked by the Java AWT framework when the window is resized.
     * The method is responsible for ensuring that the circuit elements are correctly
     * laid out within the window.
     * 
     * @param e
     *            An instance of ComponentEvent, which contains details about the event
     */
    public void componentResized(ComponentEvent e)
    {
        // Call the handleResize method to update the size of the canvas, center the circuit elements
        // within the window, and update the circuit area dimensions.
        handleResize();

        // Call the repaint method with a delay of 100ms to ensure that the window is updated
        // with the new size and the circuit elements are correctly positioned and drawn.
        // This is necessary because the circuit elements are not immediately updated when the window is resized.
        // The repaint method requests that the window be redrawn and provides a delay in milliseconds
        // before the window is updated. This delay is necessary to allow the window to process the resize event
        // and to prevent the window from being redrawn too quickly, which can cause the window to be slow
        // or unresponsive.
        cv.repaint(100);
    }

    public void readSetup(byte[] b, int len, boolean retain) {
        // Read the setup from a byte array. This method is used to read a setup from a file
        // or a string entered by the user.
        //
        // Parameters:
        // b - the byte array containing the setup
        // len - the length of the byte array
        // retain - a boolean flag indicating whether to retain the existing elements in the circuit
        //         If set to true, the new elements from the setup will be added to the current elements
        //         in the circuit. If set to false, the current circuit will be cleared of all elements
        //         before adding the new elements from the setup.
        int i;
        
        // If retain is false, reset the simulation environment
        if (!retain) {
            // Delete all circuit elements
            for (i = 0; i != elmList.size(); i++) {
                CircuitElm ce = getElm(i);
                ce.delete();
            }
            // Clear the element list
            elmList.removeAllElements();
            
            // Reset simulation parameters
            hintType = -1; // Disable hints
            timeStep = 5e-6; // Set default time step
            // Set default states for various UI controls
            dotsCheckItem.setState(true);
            smallGridCheckItem.setState(false);
            powerCheckItem.setState(false);
            voltsCheckItem.setState(true);
            showValuesCheckItem.setState(true);
            setGrid(); // Initialize the grid
            speedBar.setValue(117); // Set speed bar value
            currentBar.setValue(50); // Set current bar value
            powerBar.setValue(50); // Set power bar value
            CircuitElm.voltageRange = 5; // Set voltage range
            scopeCount = 0; // Reset scope count
        }
        
        // Request canvas repaint
        cv.repaint();
        
        int p;
        // Process each line in the byte array
        for (p = 0; p < len;) {
            int l;
            int linelen = 0;
            // Find the length of the current line
            for (l = 0; l != len - p; l++) {
                if (b[l + p] == '\n' || b[l + p] == '\r') {
                    linelen = l++;
                    // Handle Windows-style line endings
                    if (l + p < b.length && b[l + p] == '\n') {
                        l++;
                    }
                    break;
                }
            }
            
            // Convert the current line to a string
            String line = new String(b, p, linelen);
            // Tokenize the line
            StringTokenizer st = new StringTokenizer(line);
            // Process each token
            while (st.hasMoreTokens()) {
                // Get the first token and determine its type
                String type = st.nextToken();
                int tint = type.charAt(0);
                try {
                    // Handle scope data
                    if (tint == 'o') {
                        Scope sc = new Scope(this);
                        sc.position = scopeCount;
                        sc.undump(st);
                        scopes[scopeCount++] = sc;
                        break;
                    }
                    // Handle hint data
                    if (tint == 'h') {
                        readHint(st);
                        break;
                    }
                    // Handle options data
                    if (tint == '$') {
                        readOptions(st);
                        break;
                    }
                    // Ignore afilter-specific data
                    if (tint == '%' || tint == '?' || tint == 'B') {
                        break;
                    }
                    // Check for element dump types
                    if (tint >= '0' && tint <= '9') {
                        tint = Integer.parseInt(type);
                    }
                    // Parse element coordinates and flags
                    int x1 = Integer.parseInt(st.nextToken());
                    int y1 = Integer.parseInt(st.nextToken());
                    int x2 = Integer.parseInt(st.nextToken());
                    int y2 = Integer.parseInt(st.nextToken());
                    int f = Integer.parseInt(st.nextToken());
                    CircuitElm ce = null;
                    // Retrieve the class for the element
                    Class cls = elementDumpTypesRegistry.dumpTypes[tint];
                    if (cls == null) {
                        System.out.println("unrecognized dump type: " + type);
                        break;
                    }
                    // Define the constructor parameter types
                    Class[] carr = new Class[6];
                    carr[0] = carr[1] = carr[2] = carr[3] = carr[4] = int.class;
                    carr[5] = StringTokenizer.class;
                    Constructor cstr = null;
                    // Get the constructor for the element
                    cstr = cls.getConstructor(carr);

                    // Create an instance of the element
                    Object[] oarr = new Object[6];
                    oarr[0] = new Integer(x1);
                    oarr[1] = new Integer(y1);
                    oarr[2] = new Integer(x2);
                    oarr[3] = new Integer(y2);
                    oarr[4] = new Integer(f);
                    oarr[5] = st;
                    ce = (CircuitElm) cstr.newInstance(oarr);
                    // Set points for the element
                    ce.setPoints();
                    // Add the element to the list
                    elmList.addElement(ce);
                } catch (java.lang.reflect.InvocationTargetException ee) {
                    // Handle errors during element creation
                    ee.getTargetException().printStackTrace();
                    break;
                } catch (Exception ee) {
                    // Handle other exceptions
                    ee.printStackTrace();
                    break;
                }
                break;
            }
            // Move to the next line
            p += l;
        }
        
        // If retain is false, handle the resize
        if (!retain) {
            handleResize(); // Adjust scopes after resize
        }
        needAnalyze(); // Trigger analysis of the circuit
    }

    /**
     * Read a hint from a string tokenizer.
     *
     * @param st The string tokenizer to read from.
     */
    public void readHint(StringTokenizer st)
    {
        // Read the type of the hint
        hintType = Integer.parseInt(st.nextToken());

        // Read the first item of the hint
        hintItem1 = Integer.parseInt(st.nextToken());

        // Read the second item of the hint
        hintItem2 = Integer.parseInt(st.nextToken());
    }

    public void readOptions(StringTokenizer st)
    {
        // Parse the flags from the tokenizer
        int flags = Integer.parseInt(st.nextToken());
        
        // Update the state of check items based on the flags
        // If the first bit is set, enable the dotsCheckItem
        dotsCheckItem.setState((flags & 1) != 0);
        
        // If the second bit is set, enable the smallGridCheckItem
        smallGridCheckItem.setState((flags & 2) != 0);
        
        // If the third bit is not set, enable the voltsCheckItem
        voltsCheckItem.setState((flags & 4) == 0);
        
        // If the fourth bit is set, enable the powerCheckItem
        powerCheckItem.setState((flags & 8) == 8);
        
        // If the fifth bit is not set, enable the showValuesCheckItem
        showValuesCheckItem.setState((flags & 16) == 0);
        
        // Parse the time step value from the tokenizer
        timeStep = Double.parseDouble(st.nextToken());
        
        // Parse the speed value from the tokenizer
        double sp = Double.parseDouble(st.nextToken());
        
        // Calculate the speed bar value using a logarithmic scale
        int sp2 = (int) (Math.log(10 * sp) * 24 + 61.5);
        
        // Set the speed bar to the calculated value
        speedBar.setValue(sp2);
        
        // Set the current bar value from the tokenizer
        currentBar.setValue(Integer.parseInt(st.nextToken()));
        
        // Set the voltage range for circuit elements
        CircuitElm.voltageRange = Double.parseDouble(st.nextToken());
        
        try
        {
            // Attempt to set the power bar value from the tokenizer
            powerBar.setValue(Integer.parseInt(st.nextToken()));
        } catch (Exception e)
        {
            // Catch any exceptions that occur during parsing and ignore them
        }
        
        // Call method to set up the grid based on current settings
        setGrid();
    }

    /**
     * Adjusts the given coordinate to snap to the nearest grid point.
     * 
     * This function takes an integer x, representing a coordinate on the canvas,
     * and adjusts it to align with the nearest grid point. The snapping is 
     * achieved using the gridRound and gridMask values, which are presumably
     * set elsewhere in the code to define the granularity and alignment of the grid.
     * 
     * The operation performed here involves adding gridRound to the input x, and then
     * applying a bitwise AND operation with gridMask. This effectively rounds the 
     * coordinate x to the nearest grid point, ensuring that elements are aligned 
     * according to the specified grid layout.
     * 
     * @param x The original coordinate to be adjusted.
     * @return The adjusted coordinate, snapped to the nearest grid point.
     */
    public int snapGrid(int x)
    {
        // Add gridRound to the coordinate to prepare for alignment
        int adjustedX = x + gridRound;
        
        // Use bitwise AND with gridMask to snap to the nearest grid point
        return adjustedX & gridMask;
    }

    /**
     * Handles a switch element being toggled.
     * 
     * @param x The x-coordinate of the mouse event.
     * @param y The y-coordinate of the mouse event.
     * @return True if the switch was toggled, false otherwise.
     */
    public boolean doSwitch(int x, int y)
    {
        // If there is no current mouse element, or the current mouse element is not a switch, return false
        if (mouseElm == null || !(mouseElm instanceof SwitchElm))
            return false;
        
        // Get the switch element that was clicked
        SwitchElm se = (SwitchElm) mouseElm;
        
        // Toggle the switch element
        se.toggle();
        
        // If the switch element is momentary, i.e. it only stays on for a short time, store it in heldSwitchElm
        if (se.momentary)
            heldSwitchElm = se;
        
        // Trigger an analysis of the circuit
        needAnalyze();
        
        // Return true to indicate that the switch was toggled
        return true;
    }

    public int locateElm(CircuitElm elm)
    {
        int i;
        for (i = 0; i != elmList.size(); i++)
            if (elm == elmList.elementAt(i))
                return i;
        return -1;
    }

    public void mouseDragged(MouseEvent e) {
        // Ignore right mouse button with no modifiers (needed on PC)
        if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
            int ex = e.getModifiersEx();
            // If no modifier keys are pressed, return early
            if ((ex & (MouseEvent.META_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.ALT_DOWN_MASK)) == 0)
                return;
        }
        
        // Check if the drag is within the circuit area bounds
        if (!circuitArea.contains(e.getX(), e.getY()))
            return;

        // If there's an element currently being dragged, update its position
        if (dragElm != null)
            dragElm.drag(e.getX(), e.getY());

        boolean success = true;
        
        // Determine the action based on the current mouse mode
        switch (tempMouseMode) {
            case MODE_DRAG_ALL:
                // Drag all elements
                dragAll(snapGrid(e.getX()), snapGrid(e.getY()));
                break;
            case MODE_DRAG_ROW:
                // Drag a specific row of elements
                dragRow(snapGrid(e.getX()), snapGrid(e.getY()));
                break;
            case MODE_DRAG_COLUMN:
                // Drag a specific column of elements
                dragColumn(snapGrid(e.getX()), snapGrid(e.getY()));
                break;
            case MODE_DRAG_POST:
                // Drag a post of the current mouse element if it exists
                if (mouseElm != null)
                    dragPost(snapGrid(e.getX()), snapGrid(e.getY()));
                break;
            case MODE_SELECT:
                // Select an area or element
                if (mouseElm == null)
                    selectArea(e.getX(), e.getY());
                else {
                    // Switch to dragging selected elements
                    tempMouseMode = MODE_DRAG_SELECTED;
                    success = dragSelected(e.getX(), e.getY());
                }
                break;
            case MODE_DRAG_SELECTED:
                // Drag selected elements
                success = dragSelected(e.getX(), e.getY());
                break;
        }
        
        // Mark that dragging is in progress
        dragging = true;

        if (success) {
            // Update the drag coordinates to the snapped grid position
            dragX = snapGrid(e.getX());
            dragY = snapGrid(e.getY());
        }
        
        // Repaint the circuit area to reflect changes
        cv.repaint(pause);
    }

    /**
     * Drag all elements in the circuit by the specified amount.
     * This is called when the user is dragging all elements in the circuit.
     * It moves all elements in the circuit by the specified amount.
     * @param x the x-coordinate of the drag event
     * @param y the y-coordinate of the drag event
     */
    public void dragAll(int x, int y)
    {
        // Calculate the amount of drag
        int dx = x - dragX;
        int dy = y - dragY;

        // If the drag didn't move, don't do anything
        if (dx == 0 && dy == 0)
            // Return immediately if there's no effective movement
            // This means that the user is dragging without moving their mouse
            // at all, so we don't need to do anything.
            return;

        // Iterate through each element in the circuit
        // This loop is responsible for moving all circuit elements
        // by the calculated (dx, dy) amount, effectively dragging
        // the entire circuit to a new position.
        // This is why the user will see all elements in the circuit
        // move when they drag the background.
        int i;
        for (i = 0; i != elmList.size(); i++)
        {
            // Get the current circuit element from the list
            // This retrieves the next element in the list of elements
            // in the circuit.
            CircuitElm ce = getElm(i);

            // Move the current element by the specified dx and dy values
            // This calls the move() method on the current element and
            // moves it by the specified amount.
            ce.move(dx, dy);
        }

        // After moving elements, remove any that have zero length
        // This is important because dragging can result in some elements
        // collapsing to zero length, which can lead to inconsistencies
        // or errors in the circuit simulation.
        removeZeroLengthElements();
    }

    public void dragRow(int x, int y)
    {
        int dy = y - dragY;
        if (dy == 0)
            return;
        int i;
        for (i = 0; i != elmList.size(); i++)
        {
            CircuitElm ce = getElm(i);
            if (ce.y == dragY)
                ce.movePoint(0, 0, dy);
            if (ce.y2 == dragY)
                ce.movePoint(1, 0, dy);
        }
        removeZeroLengthElements();
    }

    public void dragColumn(int x, int y)
    {
        // Calculate the difference in x-coordinates from the original drag position
        int dx = x - dragX;
        
        // If there is no horizontal movement, exit the method early
        if (dx == 0)
            return;
        
        // Initialize a loop counter
        int i;
        
        // Iterate over each element in the circuit
        for (i = 0; i != elmList.size(); i++)
        {
            // Get the current circuit element from the list
            CircuitElm ce = getElm(i);
            
            // Check if the element's starting x-coordinate matches the original drag position
            if (ce.x == dragX)
                // Move the first point of the element by dx horizontally
                ce.movePoint(0, dx, 0);
            
            // Check if the element's ending x-coordinate matches the original drag position
            if (ce.x2 == dragX)
                // Move the second point of the element by dx horizontally
                ce.movePoint(1, dx, 0);
        }
        
        // Remove any elements that have collapsed to zero length after moving
        removeZeroLengthElements();
    }

    public boolean dragSelected(int x, int y)
    {
        // If the mouse element is selected, allow it to be dragged.
        // We set it to selected here so that it will be moved with the other
        // selected elements.
        boolean me = false;
        if (mouseElm != null && !mouseElm.isSelected())
            mouseElm.setSelected(me = true);

        // Snap the drag position to the nearest grid point. This is
        // done by calling the snapGrid() method on the x and y
        // coordinates. This ensures that the elements are moved to
        // a position that is a multiple of the grid size.
        // unless we're only dragging text elements, which can be moved off-grid
        int i;
        x = snapGrid(x);
        y = snapGrid(y);

        // Calculate the difference in x and y coordinates from the original drag position.
        // This is done by subtracting the original drag coordinates from the current
        // coordinates.
        int dx = x - dragX;
        int dy = y - dragY;

        // If there is no horizontal or vertical movement, exit the method early.
        // This is done by checking if the differences in x and y coordinates are
        // both zero.
        if (dx == 0 && dy == 0)
        {
            // Don't leave mouseElm selected if we selected it above
            if (me)
                mouseElm.setSelected(false);
            return false;
        }

        // Check if the move is allowed.
        // This is done by iterating over each element in the circuit and
        // calling the allowMove() method on each element. If any element
        // returns false, the move is not allowed.
        boolean allowed = true;
        for (i = 0; allowed && i != elmList.size(); i++)
        {
            CircuitElm ce = getElm(i);
            if (ce.isSelected() && !ce.allowMove(dx, dy))
                allowed = false;
        }

        // If the move is allowed, move the elements.
        // This is done by iterating over each element in the circuit and
        // calling the move() method on each element.
        if (allowed)
        {
            for (i = 0; i != elmList.size(); i++)
            {
                CircuitElm ce = getElm(i);
                if (ce.isSelected())
                    ce.move(dx, dy);
            }
            needAnalyze();
        }

        // Don't leave mouseElm selected if we selected it above
        if (me)
            mouseElm.setSelected(false);

        return allowed;
    }

    public void dragPost(int x, int y)
    {
        // draggingPost is the index of which end of the element is being dragged (0 or 1)
        // if it's -1, we haven't set it yet, so set it to the index of the end of the element
        // that is currently closest to the mouse position
        if (draggingPost == -1)
        {
            int dist1 = distanceSq(mouseElm.x, mouseElm.y, x, y);
            int dist2 = distanceSq(mouseElm.x2, mouseElm.y2, x, y);
            draggingPost = (dist1 > dist2) ? 1 : 0;
        }

        // calculate the difference in x and y coordinates from the original drag position
        int dx = x - dragX;
        int dy = y - dragY;

        // if there is no movement, exit the method early
        if (dx == 0 && dy == 0)
            return;

        // move the element's post to the new position
        mouseElm.movePoint(draggingPost, dx, dy);

        // mark the circuit as needing to be re-analyzed
        needAnalyze();
    }

    public void selectArea(int x, int y)
    {
        // Determine the minimum and maximum x-coordinates between the current x position and the initial drag position
        int x1 = min(x, initDragX);
        int x2 = max(x, initDragX);
        
        // Determine the minimum and maximum y-coordinates between the current y position and the initial drag position
        int y1 = min(y, initDragY);
        int y2 = max(y, initDragY);
        
        // Create a rectangle representing the selected area using the calculated coordinates
        selectedArea = new Rectangle(x1, y1, x2 - x1, y2 - y1);
        
        // Iterate over all circuit elements
        int i;
        for (i = 0; i != elmList.size(); i++)
        {
            // Get the current circuit element from the list
            CircuitElm ce = getElm(i);
            
            // Check if the element's bounding box intersects with the selected area
            // If it does, mark the element as selected
            ce.selectRect(selectedArea);
        }
    }

    /**
     * Sets the selected element in the circuit to the specified element.
     * All other elements will be deselected.
     * @param cs the element to select
     */
    public void setSelectedElm(CircuitElm cs)
    {
        // Iterate over all elements in the circuit
        int i;
        for (i = 0; i != elmList.size(); i++)
        {
            // Get the current element
            CircuitElm ce = getElm(i);
            
            // Set the element to be selected if it is the same as the specified element
            // Otherwise, set it to be deselected
            ce.setSelected(ce == cs);
        }
        
        // Set the mouse element to the selected element
        mouseElm = cs;
    }

    /**
     * Removes any elements from the circuit that have zero length.
     * This is useful when an element is dragged and it results in a zero length element.
     * Zero length elements can cause inconsistencies in the circuit simulation.
     * We iterate through all elements in the circuit and check if the element has zero length.
     * If it does, we remove it from the circuit and delete it.
     * We mark the circuit as needing to be re-analyzed after removing any elements.
     */
    public void removeZeroLengthElements()
    {
        int i;
        boolean changed = false;
        for (i = elmList.size() - 1; i >= 0; i--)
        {
            // Get the current element
            CircuitElm ce = getElm(i);
            
            // Check if the element has zero length
            // We do this by checking if the x and y coordinates of the element are the same
            if (ce.x == ce.x2 && ce.y == ce.y2)
            {
                // If the element has zero length, remove it from the circuit
                // We also delete the element so that it is no longer referenced
                elmList.removeElementAt(i);
                ce.delete();
                
                // Mark that the circuit has changed
                changed = true;
            }
        }
        
        // Mark the circuit as needing to be re-analyzed
        // This is necessary because removing elements may affect the circuit simulation
        needAnalyze();
    }

    public void mouseMoved(MouseEvent e) {
        // If the left mouse button is pressed, exit early to avoid processing mouse move events
        if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
            return;

        // Get the current mouse position
        int x = e.getX();
        int y = e.getY();

        // Snap the drag coordinates to the grid and initialize the dragging post to -1
        dragX = snapGrid(x);
        dragY = snapGrid(y);
        draggingPost = -1;

        int i;
        // Store the original mouse element to check later if the mouse element has changed
        CircuitElm origMouse = mouseElm;
        // Reset mouse element and post index
        mouseElm = null;
        mousePost = -1;
        // Reset plot elements
        plotXElm = plotYElm = null;
        // Initialize best distance and area with large values for comparison
        int bestDist = 100000;
        int bestArea = 100000;

        // Iterate over all circuit elements to find the one under the mouse pointer
        for (i = 0; i != elmList.size(); i++) {
            CircuitElm ce = getElm(i);

            // Check if the bounding box of the element contains the mouse pointer
            if (ce.boundingBox.contains(x, y)) {
                int j;
                // Calculate area of the bounding box
                int area = ce.boundingBox.width * ce.boundingBox.height;
                // Get the number of posts, limit to 2 for processing
                int jn = ce.getPostCount();
                if (jn > 2)
                    jn = 2;

                // Check each post of the element
                for (j = 0; j != jn; j++) {
                    Point pt = ce.getPost(j);
                    // Calculate distance to the post
                    int dist = distanceSq(x, y, pt.x, pt.y);

                    // Prefer elements with posts closer to the pointer and smaller bounding box area
                    if (dist <= bestDist && area <= bestArea) {
                        bestDist = dist;
                        bestArea = area;
                        mouseElm = ce;
                    }
                }

                // If the element has no posts, select it
                if (ce.getPostCount() == 0)
                    mouseElm = ce;
            }
        }

        // Reset selected scope
        scopeSelected = -1;

        // If no element was selected, check for scopes and posts
        if (mouseElm == null) {
            // Check if the mouse is over any scope
            for (i = 0; i != scopeCount; i++) {
                Scope s = scopes[i];
                if (s.rect.contains(x, y)) {
                    // Select the scope
                    s.select();
                    scopeSelected = i;
                }
            }

            // Check proximity to any post
            for (i = 0; i != elmList.size(); i++) {
                CircuitElm ce = getElm(i);
                int j;
                int jn = ce.getPostCount();

                // Check each post of the element
                for (j = 0; j != jn; j++) {
                    Point pt = ce.getPost(j);
                    int dist = distanceSq(x, y, pt.x, pt.y);

                    // If close to a post, select the element and set the post index
                    if (distanceSq(pt.x, pt.y, x, y) < 26) {
                        mouseElm = ce;
                        mousePost = j;
                        break;
                    }
                }
            }
        } else {
            mousePost = -1; // Reset mouse post index

            // Check for posts close to the mouse pointer
            for (i = 0; i != mouseElm.getPostCount(); i++) {
                Point pt = mouseElm.getPost(i);
                // Set the post index if close to the pointer
                if (distanceSq(pt.x, pt.y, x, y) < 26)
                    mousePost = i;
            }
        }

        // Repaint if the mouse element has changed since last check
        if (mouseElm != origMouse)
            cv.repaint();
    }

    // Calculate the square of the distance between two points.
    // This is an optimization to avoid the time consuming sqrt operation.
    // The distance between two points is given by the Pythagorean theorem,
    // which is sqrt(x^2 + y^2). Since we only need to know if the distance
    // is less than some threshold (in this case 26), we can compare the
    // square of the distance instead of the distance itself.
    // This is because x^2 + y^2 < 26^2 if and only if sqrt(x^2 + y^2) < 26.
    public int distanceSq(int x1, int y1, int x2, int y2) {
        // Calculate the differences in x and y
        x2 -= x1;
        y2 -= y1;
        // Calculate the square of the distance
        return x2 * x2 + y2 * y2;
    }

    public void mouseClicked(MouseEvent e) {
        // If the left mouse button is clicked, handle selection
        // This is the part of the code that handles the action of the user
        // clicking the left mouse button. The left mouse button is used for
        // making selections in the circuit.
        if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
            // Clear any existing selection
            // This is done so that the user can select a new element by
            // clicking on it. If the user clicks on an element, it will be
            // selected. If the user clicks on the background or another
            // element, the original selection will be cleared.
            if (mouseMode == MODE_SELECT || mouseMode == MODE_DRAG_SELECTED)
                clearSelection();
        }
    }

    public void mouseEntered(MouseEvent e) {
        // This empty method is a callback for the mouse entered event.
        // It is called when the mouse pointer enters the window.
        // It can be used to implement hover effects or to change
        // the state of the application when the mouse pointer is
        // inside the window.
        // We don't need to do anything here, so the method is empty.
    }

    public void mouseExited(MouseEvent e) {
        // This method is called when the mouse pointer exits the window.
        // It is used to reset the state of the application when the mouse
        // pointer is no longer inside the window.
        // The following code resets the selected scope and mouse/plot elements,
        // then repaints the canvas.

        // Reset selected scope
        scopeSelected = -1;

        // Reset mouse/plot elements
        mouseElm = null;
        plotXElm = null;
        plotYElm = null;

        // Repaint the canvas
        cv.repaint();
    }

    public void mousePressed(MouseEvent e) {
        // Retrieve the extended modifiers of the mouse event for additional checks
        int ex = e.getModifiersEx();

        // Check if a popup menu should be shown
        // This happens when neither the meta key nor the shift key is pressed,
        // and the popup trigger is active (usually a right-click on certain platforms)
        if ((ex & (MouseEvent.META_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK)) == 0 && e.isPopupTrigger()) {
            doPopupMenu(e);
            return; // Exit the method after showing the popup menu
        }

        // Check if the left mouse button is pressed
        if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
            // Handle actions related to the left mouse button press
            tempMouseMode = mouseMode; // Store the current mouse mode temporarily

            // Determine the temporary mouse mode based on additional keys pressed
            if ((ex & MouseEvent.ALT_DOWN_MASK) != 0 && (ex & MouseEvent.META_DOWN_MASK) != 0)
                tempMouseMode = MODE_DRAG_COLUMN; // Set to drag column mode if alt and meta keys are pressed
            else if ((ex & MouseEvent.ALT_DOWN_MASK) != 0 && (ex & MouseEvent.SHIFT_DOWN_MASK) != 0)
                tempMouseMode = MODE_DRAG_ROW; // Set to drag row mode if alt and shift keys are pressed
            else if ((ex & MouseEvent.SHIFT_DOWN_MASK) != 0)
                tempMouseMode = MODE_SELECT; // Set to select mode if only the shift key is pressed
            else if ((ex & MouseEvent.ALT_DOWN_MASK) != 0)
                tempMouseMode = MODE_DRAG_ALL; // Set to drag all mode if only the alt key is pressed
            else if ((ex & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.META_DOWN_MASK)) != 0)
                tempMouseMode = MODE_DRAG_POST; // Set to drag post mode if ctrl or meta key is pressed
        } else if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
            // Handle actions related to the right mouse button press
            if ((ex & MouseEvent.SHIFT_DOWN_MASK) != 0)
                tempMouseMode = MODE_DRAG_ROW; // Set to drag row mode if the shift key is pressed
            else if ((ex & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.META_DOWN_MASK)) != 0)
                tempMouseMode = MODE_DRAG_COLUMN; // Set to drag column mode if ctrl or meta key is pressed
            else
                return; // Exit if no specific mode is applicable
        }

        // If the current mode is not select or drag selected, clear the current selection
        if (tempMouseMode != MODE_SELECT && tempMouseMode != MODE_DRAG_SELECTED)
            clearSelection();

        // Attempt to toggle a switch element at the mouse position
        if (doSwitch(e.getX(), e.getY()))
            return;

        // Initialize drag variables for a new drag operation
        initDragX = e.getX(); // Store initial x-coordinate of the drag
        initDragY = e.getY(); // Store initial y-coordinate of the drag
        dragging = true; // Set dragging state to true

        // Check if a new element should be added to the circuit
        if (tempMouseMode != MODE_ADD_ELM || addingClass == null)
            return;

        // Snap the drag coordinates to the grid
        int x0 = snapGrid(e.getX());
        int y0 = snapGrid(e.getY());

        // Check if the snapped coordinates are within the circuit area
        if (!circuitArea.contains(x0, y0))
            return;

        // Build a new element at the snapped grid position
        dragElm = ElementBuilder.build(addingClass, x0, y0);
    }

    public void doPopupMenu(MouseEvent e)
    {
        // This method is called when the user requests a popup menu,
        // usually by right-clicking on the window.
        // It is used to show a context menu that depends on the state of the
        // application.

        // Store the currently selected element
        menuElm = mouseElm;
        menuScope = -1;

        // Check if a scope is selected
        if (scopeSelected != -1)
        {
            // Get the popup menu for the selected scope
            PopupMenu m = scopes[scopeSelected].getMenu();
            // Store the index of the selected scope
            menuScope = scopeSelected;
            // If the menu is not null, show it at the mouse position
            if (m != null)
                m.show(e.getComponent(), e.getX(), e.getY());
        }
        // Check if an element is selected
        else if (mouseElm != null)
        {
            // Enable or disable the edit menu item based on the element's editability
            elmEditMenuItem.setEnabled(mouseElm.getEditInfo(0) != null);
            // Enable or disable the scope menu item based on the element's ability to be viewed in a scope
            elmScopeMenuItem.setEnabled(mouseElm.canViewInScope());
            // Show the popup menu for the element at the mouse position
            elmMenu.show(e.getComponent(), e.getX(), e.getY());
        }
        // If neither a scope nor an element is selected, show the main menu
        else
        {
            // Check the state of each menu item in the main menu
            doMainMenuChecks(popupMenu);
            // Show the main menu at the mouse position
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public void doMainMenuChecks(Menu m)
    {
        // This method is used to check the state of menu items in the main menu.
        // It is called whenever the main menu is about to be displayed.
        // It recursively checks all menu items in the hierarchy.

        int i;
        if (m == optionsMenu)
            return; // We don't need to check the options menu.

        // Iterate over all menu items in the current menu.
        for (i = 0; i != m.getItemCount(); i++)
        {
            MenuItem mc = m.getItem(i); // Get the current menu item.
            if (mc instanceof Menu)
            {
                // If the menu item is a sub-menu, recursively call this method
                // to check the state of its menu items.
                doMainMenuChecks((Menu) mc);
            }
            if (mc instanceof CheckboxMenuItem)
            {
                // If the menu item is a checkbox menu item, check its state.
                // A checkbox menu item is a menu item with a checkbox.
                // The state of a checkbox menu item is determined by the
                // value of the action command of the checkbox menu item.
                // The action command of a checkbox menu item is a string that
                // represents the value of the checkbox menu item.
                // The state of a checkbox menu item is true if the action command
                // of the checkbox menu item is equal to the current mouse mode,
                // and false otherwise.
                CheckboxMenuItem cmi = (CheckboxMenuItem) mc;
                cmi.setState(mouseModeStr.compareTo(cmi.getActionCommand()) == 0);
            }
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        int ex = e.getModifiersEx();
        if ((ex & (MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.META_DOWN_MASK)) == 0 && e.isPopupTrigger())
        {
            // Show popup menu if triggered and exit method
            doPopupMenu(e);
            return;
        }
        
        // Reset temporary mouse mode to original mouse mode
        tempMouseMode = mouseMode;

        // Clear selected area and stop dragging
        selectedArea = null;
        dragging = false;

        // Flag to track if the circuit has changed
        boolean circuitChanged = false;

        // If there is a switch element being held
        if (heldSwitchElm != null) {
            // Release the switch and mark the circuit as changed
            heldSwitchElm.mouseUp();
            heldSwitchElm = null;
            circuitChanged = true;
        }

        // If there is an element currently being dragged
        if (dragElm != null) {
            // Check if the dragged element has zero size
            if (dragElm.x == dragElm.x2 && dragElm.y == dragElm.y2) {
                // If it has zero size, delete the element
                dragElm.delete();
            } else {
                // Otherwise, add the element to the circuit and mark the circuit as changed
                elmList.addElement(dragElm);
                circuitChanged = true;
            }
            // Clear the dragged element reference
            dragElm = null;
        }

        // If the circuit has changed, trigger an analysis
        if (circuitChanged)
            needAnalyze();

        // If there's still a dragged element reference, ensure it's cleared
        if (dragElm != null)
            dragElm.delete();
        dragElm = null;

        // Repaint the canvas to reflect any changes
        cv.repaint();
    }
    //     {
    //         powerBar.enable();
    //         // powerLabel.enable();
    //     } else
    //     {
    //         powerBar.disable();
    //         // powerLabel.disable();
    //     }
    //     enableUndoRedo();
    // }

    /**
     * This method is called whenever a menu item or checkbox menu item is selected
     * or deselected.
     * 
     * @param e the event that triggered this method call
     */
    public void itemStateChanged(ItemEvent e)
    {
        // Repaint the circuit view to reflect any changes
        cv.repaint(pause);

        // Get the object that triggered this method call
        Object mi = e.getItemSelectable();

        // If the small grid checkbox is selected or deselected, set the grid size
        if (mi == smallGridCheckItem)
            setGrid();

        // If the power checkbox is selected or deselected, toggle the state of the
        // volts checkbox
        if (mi == powerCheckItem)
        {
            if (powerCheckItem.getState())
                voltsCheckItem.setState(false);
            else
                voltsCheckItem.setState(true);
        }

        // If the volts checkbox is selected, toggle the state of the power checkbox
        if (mi == voltsCheckItem && voltsCheckItem.getState())
            powerCheckItem.setState(false);

        // If the menu scope is not -1, handle the menu item in the scope menu
        if (menuScope != -1)
        {
            Scope sc = scopes[menuScope];
            sc.handleMenu(e, mi);
        }

        // If the menu item is a checkbox menu item, handle it
        if (mi instanceof CheckboxMenuItem)
        {
            // Get the menu item
            MenuItem mmi = (MenuItem) mi;

            // Set the mouse mode to add an element
            mouseMode = MODE_ADD_ELM;

            // Get the action command of the menu item
            String s = mmi.getActionCommand();

            // If the action command is not empty, set the mouse mode string
            if (s.length() > 0)
                mouseModeStr = s;

            // If the action command is "DragAll", set the mouse mode to drag all elements
            if (s.compareTo("DragAll") == 0)
                mouseMode = MODE_DRAG_ALL;

            // If the action command is "DragRow", set the mouse mode to drag a row of elements
            else if (s.compareTo("DragRow") == 0)
                mouseMode = MODE_DRAG_ROW;

            // If the action command is "DragColumn", set the mouse mode to drag a column of elements
            else if (s.compareTo("DragColumn") == 0)
                mouseMode = MODE_DRAG_COLUMN;

            // If the action command is "DragSelected", set the mouse mode to drag selected elements
            else if (s.compareTo("DragSelected") == 0)
                mouseMode = MODE_DRAG_SELECTED;

            // If the action command is "DragPost", set the mouse mode to drag a post of an element
            else if (s.compareTo("DragPost") == 0)
                mouseMode = MODE_DRAG_POST;

            // If the action command is "Select", set the mouse mode to select an area or element
            else if (s.compareTo("Select") == 0)
                mouseMode = MODE_SELECT;

            // If the action command is not empty, set the class of element to add
            else if (s.length() > 0)
            {
                try
                {
                    // Set the cursor to the default cursor
                    cv.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                    // Set the class of element to add
                    addingClass = Class.forName(s);
                } catch (Exception ee)
                {
                    // Print any exceptions
                    ee.printStackTrace();
                }
            }
            // Set the temporary mouse mode to the mouse mode
            tempMouseMode = mouseMode;
        }
    }

    /**
     * Sets the grid size for dragging elements.
     */
    public void setGrid()
    {
        // Set the grid size to 8 if the small grid checkbox is selected, and 16
        // otherwise
        gridSize = (smallGridCheckItem.getState()) ? 8 : 16;

        // Set the grid mask to the inverse of the grid size minus 1
        gridMask = ~(gridSize - 1);

        // Set the grid round to the grid size divided by 2 minus 1
        gridRound = gridSize / 2 - 1;
    }

    // /**
    //  * Pushes the current circuit state onto the undo stack.
    //  */
    // public void pushUndo()
    // {
    //     redoStack.removeAllElements();
    //     String s = dumpCircuit();
    //     if (undoStack.size() > 0 && s.compareTo((String) (undoStack.lastElement())) == 0)
    //         return;
    //     undoStack.add(s);
    //     enableUndoRedo();
    // }

    // /**
    //  * Undoes the last action by popping the top state off the undo stack and
    //  * setting the circuit to that state.
    //  */
    // public void doUndo()
    // {
    //     if (undoStack.size() == 0)
    //         return;
    //     redoStack.add(dumpCircuit());
    //     String s = (String) (undoStack.remove(undoStack.size() - 1));
    //     readSetup(s);
    //     enableUndoRedo();
    // }

    // /**
    //  * Redoes the last undone action by popping the top state off the redo stack
    //  * and setting the circuit to that state.
    //  */
    // public void doRedo()
    // {
    //     if (redoStack.size() == 0)
    //         return;
    //     undoStack.add(dumpCircuit());
    //     String s = (String) (redoStack.remove(redoStack.size() - 1));
    //     readSetup(s);
    //     enableUndoRedo();
    // }

    // /**
    //  * Enables or disables the undo and redo menu items based on whether the
    //  * undo or redo stack is empty.
    //  */
    // public void enableUndoRedo()
    // {
    //     redoItem.setEnabled(redoStack.size() > 0);
    //     undoItem.setEnabled(undoStack.size() > 0);
    // }

    /**
     * Selects the element that is currently associated with the right-click popup
     * menu, and deselects all other elements.
     */
    public void setMenuSelection()
    {
        if (menuElm != null)
        {
            // If the element is already selected, don't do anything
            if (menuElm.selected)
                return;

            // Otherwise, clear the selection of all elements
            clearSelection();

            // Select the element associated with the right-click popup menu
            menuElm.setSelected(true);
        }
    }

    // public void doCut()
    // {
    //     int i;
    //     // Push the current state of the circuit onto the undo stack
    //     pushUndo();
    //     // Select the element associated with the right-click popup menu
    //     setMenuSelection();
    //     // Clear the clipboard
    //     clipboard = "";
    //     // Iterate over all elements in the circuit
    //     for (i = elmList.size() - 1; i >= 0; i--)
    //     {
    //         // Get the current element
    //         CircuitElm ce = getElm(i);
    //         // If the element is selected
    //         if (ce.isSelected())
    //         {
    //             // Append the element's dump string to the clipboard
    //             clipboard += ce.dump() + "\n";
    //             // Delete the element from the circuit
    //             ce.delete();
    //             // Remove the element from the list of elements
    //             elmList.removeElementAt(i);
    //         }
    //     }
    //     // Enable the "Paste" menu item if there is something in the clipboard
    //     enablePaste();
    //     // Mark the circuit as needing to be re-analyzed
    //     needAnalyze();
    // }

    public void doDelete()
    {
        int i;
        // Select the element associated with the right-click popup menu
        // and deselect all other elements to ensure only the intended
        // elements are affected by the delete operation.
        setMenuSelection();

        // Iterate over all elements in the circuit in reverse order.
        // We traverse the list backward to safely remove elements
        // without causing index issues.
        for (i = elmList.size() - 1; i >= 0; i--)
        {
            // Retrieve the current circuit element from the list.
            CircuitElm ce = getElm(i);

            // Check if the current element is selected.
            // Only selected elements will be deleted.
            if (ce.isSelected())
            {
                // Call the delete method on the element to perform
                // any necessary cleanup or resource deallocation.
                ce.delete();

                // Remove the element from the list to ensure it is
                // no longer part of the circuit.
                elmList.removeElementAt(i);
            }
        }

        // Mark the circuit as needing re-analysis.
        // This is necessary to update the simulation state after
        // elements have been removed.
        needAnalyze();
    }

    // public void doCopy()
    // {
    //     int i;
    //     clipboard = "";
    //     setMenuSelection();
    //     for (i = elmList.size() - 1; i >= 0; i--)
    //     {
    //         CircuitElm ce = getElm(i);
    //         if (ce.isSelected())
    //             clipboard += ce.dump() + "\n";
    //     }
    //     enablePaste();
    // }

    // public void enablePaste()
    // {
    //     pasteItem.setEnabled(clipboard.length() > 0);
    // }

    // public void doPaste()
    // {
    //     pushUndo();
    //     clearSelection();
    //     int i;
    //     Rectangle oldbb = null;
    //     for (i = 0; i != elmList.size(); i++)
    //     {
    //         CircuitElm ce = getElm(i);
    //         Rectangle bb = ce.getBoundingBox();
    //         if (oldbb != null)
    //             oldbb = oldbb.union(bb);
    //         else
    //             oldbb = bb;
    //     }
    //     int oldsz = elmList.size();
    //     readSetup(clipboard, true);

    //     // select new items
    //     Rectangle newbb = null;
    //     for (i = oldsz; i != elmList.size(); i++)
    //     {
    //         CircuitElm ce = getElm(i);
    //         ce.setSelected(true);
    //         Rectangle bb = ce.getBoundingBox();
    //         if (newbb != null)
    //             newbb = newbb.union(bb);
    //         else
    //             newbb = bb;
    //     }
    //     if (oldbb != null && newbb != null && oldbb.intersects(newbb))
    //     {
    //         // find a place for new items
    //         int dx = 0, dy = 0;
    //         int spacew = circuitArea.width - oldbb.width - newbb.width;
    //         int spaceh = circuitArea.height - oldbb.height - newbb.height;
    //         if (spacew > spaceh)
    //             dx = snapGrid(oldbb.x + oldbb.width - newbb.x + gridSize);
    //         else
    //             dy = snapGrid(oldbb.y + oldbb.height - newbb.y + gridSize);
    //         for (i = oldsz; i != elmList.size(); i++)
    //         {
    //             CircuitElm ce = getElm(i);
    //             ce.move(dx, dy);
    //         }
    //         // center circuit
    //         handleResize();
    //     }
    //     needAnalyze();
    // }

    public void clearSelection()
    {
        // This method is called when the user wants to clear the selection
        // of all elements in the circuit. This is done by iterating over
        // all elements in the circuit and calling the setSelected() method
        // on each element with the argument false.
        int i;
        for (i = 0; i != elmList.size(); i++)
        {
            // Get the current element
            CircuitElm ce = getElm(i);

            // Clear the selection of the element
            ce.setSelected(false);
        }
    }

    public void doSelectAll()
    {
        // This method is called when the user wants to select all elements
        // in the circuit. This is done by iterating over all elements in the
        // circuit and calling the setSelected() method on each element with
        // the argument true.
        int i;
        for (i = 0; i != elmList.size(); i++)
        {
            // Get the current element
            CircuitElm ce = getElm(i);

            // Set the element to be selected
            ce.setSelected(true);
        }
    }

    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
        // Check if the key character is a printable character (ASCII range: 33 to 126)
        if (e.getKeyChar() > ' ' && e.getKeyChar() < 127)
        {
            // Retrieve the class associated with the key character from the registry
            Class elmClass = elementDumpTypesRegistry.dumpTypes[e.getKeyChar()];
            
            // If the class is null or a Scope class, do nothing
            if (elmClass == null || elmClass == Scope.class)
                return;
            
            // Initialize a CircuitElm object using the class
            CircuitElm elm = null;
            elm = ElementBuilder.build(elmClass, 0, 0);
            
            // Check if the element has a hotkey and the dump class matches the element class
            if (elm == null || !(elm.hasHotkey() && elm.getDumpClass() == elmClass))
                return;
            
            // Set the mouse mode to add element
            mouseMode = MODE_ADD_ELM;
            
            // Change the cursor to default cursor
            cv.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            
            // Set the mouse mode string and the class that is being added
            mouseModeStr = elmClass.getName();
            addingClass = elmClass;
        }
        
        // If the space key is pressed
        if (e.getKeyChar() == ' ')
        {
            // Set the mouse mode to select
            mouseMode = MODE_SELECT;
            
            // Change the cursor to hand cursor
            cv.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Set the mouse mode string to "Select"
            mouseModeStr = "Select";
        }
        
        // Temporarily store the current mouse mode
        tempMouseMode = mouseMode;
    }

    // This method performs LU factorization on a matrix using Gaussian elimination.
    // It decomposes matrix 'a' into upper and lower triangular matrices.
    // The integer array 'ipvt' records the pivot indices used during the process.

    //
    // Gaussian elimination is a method for solving systems of linear
    // equations which is related to LU factorization. LU factorization is
    // a factorization of a matrix A into the product of a lower triangular
    // matrix L and an upper triangular matrix U. Gaussian elimination is
    // a method for solving the system of equations Ax=b by transforming the
    // matrix A into upper triangular form, and then solving for x by
    // back-substitution.
    //
    // The algorithm works by looping through the columns of the matrix,
    // and for each column, it calculates the upper triangular elements
    // for that column, and then calculates the lower triangular elements
    // for that column. It does this by subtracting multiples of the
    // elements above it in the column from the elements below it.
    // The elements above it in the column are the upper triangular elements
    // which have already been calculated. The elements below it in the
    // column are the lower triangular elements which have not yet been
    // calculated. The algorithm uses the elements above it in the column
    // to calculate the lower triangular elements below it in the column.
    //
    // The algorithm also does partial pivoting to avoid dividing by zero.
    // It looks for the largest element in the column below the current
    // element, and swaps the current element with that element if it is
    // larger. This is called partial pivoting because it only looks at
    // the elements below the current element, and does not look at the
    // elements above it.
    //
    // The algorithm also scales each row of the matrix by its largest
    // element, to prevent overflow. It does this by dividing each row
    // by the largest element in that row.
    public boolean lu_factor(double a[][], int n, int ipvt[]) {
        double scaleFactors[]; // Array to store scaling factors for each row.
        int i, j, k; // Loop variables.

        scaleFactors = new double[n]; // Initialize the scaling factors array.

        // Loop through each row to determine the scaling factors.
        for (i = 0; i != n; i++) {
            double largest = 0; // Variable to store the largest element in the row.

            // Find the largest element in the current row.
            for (j = 0; j != n; j++) {
                double x = Math.abs(a[i][j]); // Get the absolute value of the current element.
                if (x > largest) // Update the largest element if needed.
                    largest = x;
            }

            // If the row is all zeros, the matrix is singular and cannot be factored.
            if (largest == 0)
                return false;

            // Calculate the scaling factor as the reciprocal of the largest element.
            scaleFactors[i] = 1.0 / largest;
        }

        // Perform LU factorization using Crout's method, iterating through columns.
        for (j = 0; j != n; j++) {

            // Calculate the upper triangular elements for column j.
            for (i = 0; i != j; i++) {
                double q = a[i][j]; // Initialize q with the current matrix element.
                for (k = 0; k != i; k++)
                    q -= a[i][k] * a[k][j]; // Subtract the product of known elements.
                a[i][j] = q; // Update the matrix element with the calculated value.
            }

            // Calculate the lower triangular elements for column j.
            double largest = 0; // Variable to store the largest element for pivoting.
            int largestRow = -1; // Row index of the largest element for pivoting.
            for (i = j; i != n; i++) {
                double q = a[i][j]; // Initialize q with the current matrix element.
                for (k = 0; k != j; k++)
                    q -= a[i][k] * a[k][j]; // Subtract the product of known elements.
                a[i][j] = q; // Update the matrix element with the calculated value.

                double x = Math.abs(q); // Get the absolute value for pivoting.
                if (x >= largest) { // Check if this element is the largest so far.
                    largest = x;
                    largestRow = i; // Update the largest row index if needed.
                }
            }

            // Perform pivoting to ensure numerical stability.
            if (j != largestRow) {
                double x;
                for (k = 0; k != n; k++) {
                    x = a[largestRow][k]; // Swap the rows in the matrix.
                    a[largestRow][k] = a[j][k];
                    a[j][k] = x;
                }
                scaleFactors[largestRow] = scaleFactors[j]; // Swap the scaling factors.
            }

            // Record the row exchange in the pivot index array.
            ipvt[j] = largestRow;

            // Avoid division by zero by setting a small value if zero is encountered.
            if (a[j][j] == 0.0) {
                System.out.println("avoided zero");
                a[j][j] = 1e-18;
            }

            // Scale the lower triangular elements if not in the last column.
            if (j != n - 1) {
                double mult = 1.0 / a[j][j]; // Calculate the multiplier.
                for (i = j + 1; i != n; i++)
                    a[i][j] *= mult; // Scale the elements below the diagonal.
            }
        }
        return true; // Return true indicating successful factorization.
    }

    // Solves the set of n linear equations using a LU factorization
    // previously performed by lu_factor. On input, b[0..n-1] is the right
    // hand side of the equations, and on output, contains the solution.
    public void lu_solve(double a[][], int n, int ipvt[], double b[])
    {
        int i;

        // find first nonzero b element
        // This loop is used to find the first non-zero element in the
        // b array. This is the starting point for the forward substitution
        // process, which is used to solve the lower triangular system.
        // The loop starts at the beginning of the array and continues
        // until a non-zero element is found.
        for (i = 0; i != n; i++)
        {
            int row = ipvt[i];

            // Swap the elements of the b array so that the non-zero element
            // is in the first position.
            double swap = b[row];
            b[row] = b[i];
            b[i] = swap;
            if (swap != 0)
                break;
        }

        // forward substitution using the lower triangular matrix
        // This loop is used to perform the forward substitution using
        // the lower triangular matrix. The elements of the b array
        // are updated in-place. The loop starts at the second element
        // of the array (since the first element was already processed
        // in the previous loop) and continues until the end of the array.
        int bi = i++;
        for (; i < n; i++)
        {
            int row = ipvt[i];
            int j;
            double tot = b[row];

            // Swap the elements of the b array so that the solution to the
            // previous equation is in the correct position.
            b[row] = b[i];
            for (j = bi; j < i; j++)
                tot -= a[i][j] * b[j];
            b[i] = tot;
        }

        // back-substitution using the upper triangular matrix
        // This loop is used to perform the back-substitution using the
        // upper triangular matrix. The elements of the b array are
        // updated in-place. The loop starts at the last element of the
        // array and continues until the beginning of the array.
        for (i = n - 1; i >= 0; i--)
        {
            double tot = b[i];

            int j;
            for (j = i + 1; j != n; j++)
                tot -= a[i][j] * b[j];
            b[i] = tot / a[i][i];
        }
    }

}

