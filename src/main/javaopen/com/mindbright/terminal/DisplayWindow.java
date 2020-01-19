/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone Group AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 2.0, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.terminal;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.JButton;
import javax.swing.JScrollBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class DisplayWindow extends JPanel
    implements DisplayView, AdjustmentListener, MouseListener,
               MouseMotionListener, ComponentListener, FocusListener,
              ImageObserver, MouseWheelListener
{
    private static final long serialVersionUID = 1L;

    private boolean pendingShow = true;
    private boolean visTopChangeAllowed = true;

    final static int REPAINT_SLEEP = 70; // ms 

    final static boolean DEBUG         = false;

    final static public int MIN_ROWS = 2;
    final static public int MIN_COLS = 8;
    final static public int MAX_COLS = 512;
    final static public int MAX_ROWS = 512;

    protected boolean haveScrollbar;
    private volatile boolean updateScrollbar = false;

    protected JPanel myPanel;
    private boolean runRepaintThread;

    private boolean logoDraw;
    private Image   logoImg;
    private int     logoX;
    private int     logoY;
    private int     logoW;
    private int     logoH;
    private int     centerLogoX;
    private int     centerLogoY;

    private boolean isDirty = false;
    private int     dirtyTop;
    private int     dirtyBottom;
    private int     dirtyLeft;
    private int     dirtyRight;

    private boolean resizable = true;
    private int rows; // Number of displayed rows
    private int cols; // Number of displayed columns
    private int vpixels;
    private int hpixels;
    private int borderWidth  = 2;
    private int borderHeight = 2;
    private int xPos; // Position of display windows on screen
    private int yPos; // Position of display windows on screen

    private int charWidth;
    private int charHeight;
    private int baselineIndex;
    private int lineSpaceDelta;

    private boolean cursorHollow = false;
    private boolean hasCursor = false;
    private int curRow;
    private int curCol;

    private Color origBgColor;
    private Color origFgColor;
    private Color cursorColor;

    private int visTop; // The buffer row number of the top displayed row
    private boolean visTopChangePending = false; // Should visTop change on resize
    private int visTopChange = 0; // New visTop value

    private Image     memImage;
    private Graphics  memGraphics;
    private Dimension memImageSize;

    private Font plainFont;
    private Font boldFont;    

    private DisplayModel model;
    private DisplayController controller;

    private boolean hasSelection = false;
    private int selectionTopRow;
    private int selectionTopCol;
    private int selectionBottomRow;
    private int selectionBottomCol;


    /** This class collapses repaint requests.
     * This thread class sleeps for a couple of milli-sec, wakes up to see if
     * repainting is needed (and repaints if that is the case) and then
     * go to sleep again. The idea is that a Canvas instance is a
     * heavy-weight object and a call to it's repaint method will be
     * executed directly, and not put on an event queue.
     */
    private class Repainter extends Thread {
        protected int sleepTime;
        protected boolean repaintRequested;
        protected boolean hasSlept;

        Repainter(int sleepTime) {
            super("DisplayWindow.repainter");

            this.sleepTime = sleepTime;
            repaintRequested = false;
            hasSlept = false;

            synchronized (this) {
                start();
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        public void run() {
            synchronized (this) {
                this.notify();
            }

            while (runRepaintThread) {
                try {
                    synchronized (this) {
                        this.wait(sleepTime);
                        if (repaintRequested) {
                            doRepaint();
                            repaintRequested = false;
                            hasSlept = false;
                        } else {
                            hasSlept = true;
                        }
                        
                    }
                } catch (InterruptedException e) {
                    //System.out.println("Repainter is interrupted!");
                }
            }
        }

        synchronized void repaint(boolean force) {
            repaintRequested = true;
            if (force || hasSlept) {
                synchronized (this) {
                    this.notify();
                }
            }
        }
    }

    private Repainter repainter;

    public DisplayWindow() {
        super();

        haveScrollbar = false;
        visTop        = 0;

        isDirty = false;

        addComponentListener(this);
        addFocusListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);
        
        setAutoscrolls(true);
        setFocusTraversalKeysEnabled(false);

        runRepaintThread = true;
        repainter = new Repainter(REPAINT_SLEEP);
    }

    public void setModel(DisplayModel model) {
        this.model = model;
    }
    public DisplayModel getModel() {
        return model;
    }
    public void setController(DisplayController controller) {
        this.controller = controller;
    }

    public void setKeyListener(KeyListener keyListener) {
        addKeyListener(keyListener);
    }

    public void delKeyListener(KeyListener keyListener) {
        removeKeyListener(keyListener);
    }

    private boolean isInsideSelection(int row, int col) {
        if (!hasSelection) {
            return false;
        }
        if (row < selectionTopRow || row > selectionBottomRow) {
            return false;
        }
        if (row == selectionTopRow && col < selectionTopCol) {
            return false;
        }
        if (row == selectionBottomRow && col > selectionBottomCol) {
            return false;
        }
        return true;
    }

    public void setLogo(Image logoImg, int x, int y, int w, int h) {
        this.logoImg = logoImg;
        this.logoX   = x;
        this.logoY   = y;
        this.logoW   = w;
        this.logoH   = h;
    }

    public Image getLogo() {
        return logoImg;
    }
    public boolean showLogo() {
        logoDraw = (logoImg != null);
        centerLogoX = -1;
        centerLogoY = -1;
        makeAllDirty();
        repaint();        
        return logoDraw;
    }

    public void hideLogo() {
        logoDraw = false;
        makeAllDirty();
        repaint();
    }

    public static Color getTermRGBColor(String value)
        throws NumberFormatException {
        int r, g, b, c1, c2;
        Color c;
        c1 = value.indexOf(',');
        c2 = value.lastIndexOf(',');
        if (c1 == -1 || c2 == -1)
            throw new NumberFormatException();
        r = Integer.parseInt(value.substring(0, c1).trim());
        g = Integer.parseInt(value.substring(c1 + 1, c2).trim());
        b = Integer.parseInt(value.substring(c2 + 1).trim());
        c = new Color(r, g, b);
        return c;
    }

    public static Color getTermColor(String name)
        throws IllegalArgumentException {
        int i;
        for (i = 0; i < termColors.length; i++) {
            if (termColorNames[i].equalsIgnoreCase(name))
                break;
        }
        if (i == termColors.length)
            throw new IllegalArgumentException("Unknown color: " + name);
        return termColors[i];
    }

    public void setFont(String name, int size) {
        plainFont = new Font(name, Font.PLAIN, size);
        boldFont  = new Font(name, Font.BOLD, size);

        super.setFont(plainFont);
        calculateCharSize();

        if (isShowing()) {
            setGeometry(rows, cols);
        }
    }

    public void setLineSpaceDelta(int delta) {
        lineSpaceDelta = delta;
    }

    public void setFont(Font font) {
        setFont(font.getName(), font.getSize());
    }

    public void setVisTopChangeAllowed(boolean set) {
        visTopChangeAllowed = set;
    }
    public void setVisTopDelta(int delta) {
        setVisTopDelta(delta, visTopChangeAllowed);
    }
    public void setVisTopDelta(int delta, boolean changeAllowed) {
        setVisTop(visTop + delta, changeAllowed);
    }
    public void setVisTop(int visTop) {
        setVisTop(visTop, visTopChangeAllowed);
    }
    public void setVisTop(int visTop, boolean changeAllowed) {
        if (model == null)
            return;

        visTopChangePending = false;
        visTop = fenceVisTop(visTop);
        if (this.visTop != visTop) {
            if (changeAllowed) {
                this.visTop = visTop;
                repaint();
                updateScrollbarValues();
            }
        }
    }
    public void setPendingVisTopChange(int visTop) {
        visTopChangePending = true;
        visTopChange = visTop;
    }
    private int fenceVisTop(int visTop) {
        int min = 0;
        int max = model.getBufferRows() - rows;
        if (visTop < min) {
            visTop = min;
        }
        if (visTop > max) {
            visTop = max;
        }
        return visTop;
    }

    public void updateScrollbarValues() {
        if (model == null)
            return;
        if (haveScrollbar)
            updateScrollbar = true;
    }

    private JScrollBar scrollbar;

    protected void updateScrollbarValues(int val, int ext, int min, int max) {
        scrollbar.setValues(val, ext, min, max);
        scrollbar.setBlockIncrement(ext);
    }

    protected Dimension getScrollbarSize() {
        return scrollbar.getSize();
    }

    protected void addScrollbarToPanel(String where) {
        myPanel.add(scrollbar, where);
    }
    
    protected void removeScrollbarFromPanel() {
        myPanel.remove(scrollbar);
    }

    public Container getPanelWithScrollbar(String scrollPos) {
        if (myPanel != null)
            return myPanel;

        scrollbar = new JScrollBar(JScrollBar.VERTICAL);
        updateScrollbarValues();
        scrollbar.addAdjustmentListener(this);
        scrollbar.addMouseWheelListener(this);

        myPanel = new JPanel(new BorderLayout());
        myPanel.add(this, BorderLayout.CENTER);
        if(scrollPos.equals("left")) {
            myPanel.add(scrollbar, BorderLayout.WEST);
            haveScrollbar = true;
        } else if(scrollPos.equals("right")) {
            myPanel.add(scrollbar, BorderLayout.EAST);
            haveScrollbar = true;
        } else {
            haveScrollbar = false; // No scrollbar
        }
        return myPanel;
    }

    public void moveScrollbar(String scrollPos) {
        if (myPanel == null)
            return;
        if (scrollPos.equals("left") || scrollPos.equals("right")) {
            removeScrollbarFromPanel();
            addScrollbarToPanel(scrollPos.equals("right") ?
                                BorderLayout.EAST : BorderLayout.WEST);
            revalidate();
            requestFocus();
            haveScrollbar = true;
        } else if (scrollPos.equals("none")) {
            removeScrollbarFromPanel();
            revalidate();
            requestFocus();
            haveScrollbar = false;
        }
    }

    private synchronized final void makeAllDirty() {
        // Reset dirty area since it can be larger than display after
        // a resize
        dirtyTop = 0;
        dirtyLeft = 0;
        dirtyBottom = rows;
        dirtyRight = cols;
        isDirty = true;
    }

    private final void makeCursorDirty() {
        makeAreaDirty(curRow, curCol, curRow+1, curCol+1);
    }

    private final void makeSelectionDirty() {
        int top, left, bottom, right;
        top = selectionTopRow;
        bottom = selectionBottomRow;
        if (top != bottom) {
            left = 0;
            right = cols;
        } else {
            if (selectionTopCol < selectionBottomCol) {
                left = selectionTopCol;
                right = selectionBottomCol;
            } else {
                right = selectionTopCol;
                left = selectionBottomCol;
            }
        }
        makeAreaDirty(top, left, bottom+1, right+1);
    }

    public void updateDirtyArea(int top, int left, int bottom, int right) {
        makeAreaDirty(top, left, bottom, right);
    }

    // input is buffer coordinates, dirty is screen coordinates
    private synchronized final void makeAreaDirty(int top, int left,
            int bottom, int right) {
        if (bottom < visTop || top > (visTop + rows)) {
            // Dirt outside visible area, ignore
            return;
        }

        // Translate to screen coordinates
        top = top - visTop;
        bottom = bottom - visTop;

        if (!isDirty) {
            dirtyTop = top;
            dirtyBottom = bottom;
            dirtyLeft = left;
            dirtyRight = right;
            isDirty = true;
        } else {
            // Grow dirty area to include all dirty spots on screen
            if(top < dirtyTop) {
                dirtyTop = top;
            }
            if(bottom > dirtyBottom) {
                dirtyBottom = bottom;
            }
            if(left < dirtyLeft) {
                dirtyLeft = left;
            }
            if(right > dirtyRight) {
                dirtyRight = right;
            }
            if (dirtyTop == dirtyBottom) {
                dirtyBottom++;
            }
            if (dirtyLeft == dirtyRight) {
                dirtyRight++;
            }
        }
        // Make sure that values are sane
        dirtyTop = (dirtyTop < 0) ? 0 : dirtyTop;
        dirtyBottom = (dirtyBottom > rows) ? rows : dirtyBottom;
        dirtyLeft  = (dirtyLeft < 0) ? 0 : dirtyLeft;
        dirtyRight = (dirtyRight > cols) ? cols : dirtyRight;

        // Make sure that the dirty area is a box, so if the new
        // dirt spans many lines, the entire screen width should
        // be repainted.
        if (dirtyBottom - dirtyTop > 1) {
            dirtyLeft = 0;
            dirtyRight = cols;
        }
    }

    //
    // FocusListener, AdjustmentListener, MouseListener,
    // MouseMotionListener, ComponentListener, MouseWheelListener
    //
    public void focusGained(FocusEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        cursorHollow = false;
        makeCursorDirty();
        repaint(true);
    }
    public void focusLost(FocusEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        cursorHollow = true;
        makeCursorDirty();
        repaint(true);
    }

    public boolean isFocusable() {
        return true;
    }

    // !!! Since the realization of the window is very different on different
    // platforms (w.r.t. generated events etc.) we don't listen to
    // componentResized event until window is shown, in that instance we also
    // do the pending setGeometry.
    //
    public void componentMoved(ComponentEvent e) {
        // !!! TODO: Do we want to save absolute positions???
        if (DEBUG)
            System.out.println("componentMoved: " + e);
    }

    public synchronized void componentShown(ComponentEvent e) {
        if (DEBUG)
            System.out.println("componentShown: pending=" + pendingShow + "," + cols+"x"+rows+", " + e);
        if (pendingShow) {
            pendingShow = false;
            calculateCharSize();
            setGeometry(rows, cols);
            setPosition(xPos, yPos);
        }
    }

    public void componentHidden(ComponentEvent e) {
        if (DEBUG)
            System.out.println("componentHidden: " + e);
    }

    public synchronized void componentResized(ComponentEvent e) {
        if (DEBUG)
            System.out.println("componentResized: " + e);

        if (controller == null)
            return;

        Dimension dim = getSize();
        int newCols = (dim.width  - (2 * borderWidth))  / charWidth;
        int newRows = (dim.height - (2 * borderHeight)) / charHeight;

        if (DEBUG)
            System.out.println(cols + "x" + rows + " --> " + newCols + "x" + newRows);

        if ((e != null && e.getComponent() != this) ||
            (newCols <= 0 || newRows <= 0)) {
            return;
        }

        if (!resizable) {
            newRows = rows;
            newCols = cols;
        }
        controller.displayDragResize(newRows, newCols);
    }

    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
        visTop = e.getValue();
        updateScrollbarValues();
        repaint();
    }

    private final int mouseRow(int y) {
        int mouseRow = (y - borderHeight) / charHeight;
        if (mouseRow < 0)
            mouseRow = 0;
        else if (mouseRow >= rows)
            mouseRow = rows - 1;
        return mouseRow;
    }
    private final int mouseCol(int x) {
        int mouseCol = (x - borderWidth)  / charWidth;
        if (mouseCol < 0)
            mouseCol = 0;
        else if (mouseCol >= cols)
            mouseCol = cols - 1;
        return mouseCol;
    }

    private static int getWhichButton(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e))
            return DisplayController.LEFT_BUTTON;
        else if (SwingUtilities.isMiddleMouseButton(e))
            return DisplayController.MIDDLE_BUTTON;
        else if (SwingUtilities.isRightMouseButton(e))
            return DisplayController.RIGHT_BUTTON;
        return DisplayController.UNKNOWN_BUTTON;
    }

    public void mouseMoved(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public synchronized void mouseClicked(MouseEvent e) {
        if (e == null)
            return;

        int row = mouseRow(e.getY());
        int col = mouseCol(e.getX());
        int mod = e.getModifiers();

        if (controller != null)
            controller.mouseClicked(visTop, row, col, mod, getWhichButton(e));
    }
    public synchronized void mousePressed(MouseEvent e) {
        if (e == null)
            return;

        int row = mouseRow(e.getY());
        int col = mouseCol(e.getX());
        int mod = e.getModifiers();

        if (controller != null)
            controller.mousePressed
                (visTop, row, col, mod, 
                 getWhichButton(e), e.getX(), e.getY());
    }
    public synchronized void mouseReleased(MouseEvent e) {
        if (e == null)
            return;

        int row = mouseRow(e.getY());
        int col = mouseCol(e.getX());
        int mod = e.getModifiers();

        if (controller != null)
            controller.mouseReleased(visTop, row, col, mod, getWhichButton(e));
    }

    public synchronized void mouseDragged(MouseEvent e) {
        if (e == null)
            return;

        int delta = 0;
        int vt = visTop;
        int row = (e.getY() - borderHeight) / charHeight;
        int col = mouseCol(e.getX());
        
        if (visTop + row >= model.getBufferRows())
            row = model.getBufferRows() - visTop - 1;
        if (visTop + row < 0)
            row = 0;

        if (row >= rows) {
            controller.scrollDown();
            vt++;
            row = rows - 1;
            delta = -1;
            col = cols - 1;
        } else if (row < 0) {
            controller.scrollUp(); 
            vt--;
            row = 0;
            delta = 1;
            col = 0;
        }

        int mod = e.getModifiers();
        if (controller != null)
            controller.mouseDragged(vt, row, col, mod, getWhichButton(e), delta);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        switch (e.getScrollType()) {
        case MouseWheelEvent.WHEEL_UNIT_SCROLL:
            int amount = e.getScrollAmount();
            if (e.getWheelRotation() > 0) {
                while (amount-- > 0) controller.scrollDown();
            } else {
                while (amount-- > 0) controller.scrollUp();
            }
            break;

        case MouseWheelEvent.WHEEL_BLOCK_SCROLL:
            break;
        }
    }

    //
    // Methods overridden from super-class Component + some helper functions
    //

    private void calculateCharSize() {
        int charMaxAscent;
        int charLeading;
        FontMetrics fm = getFontMetrics(getFont());
        charWidth      = -1; // !!! Does not seem to work: fm.getMaxAdvance();
        charHeight     = fm.getHeight() + lineSpaceDelta;
        charMaxAscent  = fm.getMaxAscent();
        fm.getMaxDescent();
        charLeading    = fm.getLeading();
        baselineIndex  = charMaxAscent + charLeading - 1;

        if (charWidth == -1) {
            int widths[] = fm.getWidths();
            for (int i=32; i<127; i++) {
                if (widths[i] > charWidth) {
                    charWidth = widths[i];
                }
            }
        }
    }

    public boolean isWide(char c) {
        return !DisplayUtil.isBoxOrBlockChar(c) && 
            getFontMetrics(getFont()).charWidth(c) > charWidth;
    }

    public Dimension getDimensionOfText(int rows, int cols) {
        //calculateCharSize();
        return new Dimension((cols * charWidth) + (2 * borderHeight),
                             (rows * charHeight) + (2 * borderWidth));
    }

    public Dimension getPreferredSize() {
        Dimension dim = getDimensionOfText(rows, cols);
        if (DEBUG) System.out.println("getPreferredSize " + cols + "x" + rows
                                     + "(" + dim + ")");
        return dim;
    }

    public Dimension getMinimumSize() {
        return getDimensionOfText(MIN_ROWS, MIN_COLS);
    }

    public Dimension getMaximumSize() {
        return getDimensionOfText(MAX_ROWS, MAX_COLS);
    }

    final Rectangle getClipRect(Graphics g) {
        Rectangle clipRect = g.getClipBounds();
        if (clipRect == null) {
            Dimension winSize = getSize();
            clipRect = new Rectangle(0, 0, winSize.width, winSize.height);
        }
        return clipRect;
    }

    private void clearDirtyArea(Graphics source, int left, int top, int right, int bottom) {
        boolean clearAll = (left   == 0    &&
                            right  == cols &&
                            top    == 0    &&
                            bottom == rows);
        int x, y, w, h;

        if(clearAll) {
            Dimension dim = getSize();
            x = 0;
            y = 0;
            w = dim.width;
            h = dim.height;
        } else {
            x = borderWidth + (charWidth   * left);
            y = borderHeight + (top    * charHeight);
            w = (charWidth   * (right  - left));
            h = (charHeight  * (bottom - top));
        }

        source.setColor(origBgColor);
        source.fillRect(x, y, w, h);
        source.setColor(origFgColor);
    }
    
    void doRepaint() {
        super.repaint();
    }
    public void repaint() {
        repaint(false);
    }
    
    public void repaint(boolean force) {
        if (repainter != null)
            repainter.repaint(force);
    }

    private StringBuffer charbuf = new StringBuffer();
    private void drawBufferedString(int x, int y, Color col, boolean bold) { 
         if (charbuf.length() == 0) return;
         Color fgcol = memGraphics.getColor();
         memGraphics.setColor(col);
         if (bold) memGraphics.setFont(boldFont);
         memGraphics.drawString(charbuf.toString(), x, y);
         if (bold) memGraphics.setFont(plainFont);
         charbuf.setLength(0);
         memGraphics.setColor(fgcol);
    }

    public void paintComponent(Graphics g) {
        {
            //add by haley
            Graphics2D g2d = (Graphics2D)g;
            // 消除锯齿

            RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            //renderingHints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2d.setRenderingHints(renderingHints);
        }
        super.paintComponent(g);
        
        if (model == null)
            return;

        // This should not happen but better safe than sorry...
        if (hpixels == 0 || vpixels == 0) {
            Dimension dim = getSize();
            vpixels = dim.height;
            hpixels = dim.width;
            System.out.println("h="+hpixels+" v="+vpixels);
            if (hpixels == 0 || vpixels == 0) {
                return;
            }
        }

        int dirtyLeft, dirtyRight, dirtyTop, dirtyBottom;
        boolean isDirty;
        
        synchronized (this) {
            dirtyLeft   = this.dirtyLeft;
            dirtyRight  = this.dirtyRight;
            dirtyTop    = this.dirtyTop;
            dirtyBottom = this.dirtyBottom;
            isDirty     = this.isDirty;

            // Reset dirty area (i.e. we have take responsibility for it)
            this.isDirty = false;
        }

        if((memGraphics == null) ||
           (memImageSize == null) ||
                 (hpixels != memImageSize.width) ||
           (vpixels != memImageSize.height)) {
            memImageSize = new Dimension(hpixels, vpixels);
            memImage     = createImage(hpixels, vpixels);
        }
        memGraphics = memImage.getGraphics();
        memGraphics.setFont(plainFont);

	if (System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0) {
	    Graphics2D g2d = (Graphics2D)memGraphics;
	    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}


        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
            //add by haley
            Graphics2D g2d = (Graphics2D)memGraphics;
            // 消除锯齿
            RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            //renderingHints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHints(renderingHints);
        }


        if(isDirty) {
            clearDirtyArea(memGraphics, dirtyLeft, dirtyTop, dirtyRight, dirtyBottom);
        } else {
            // If nothing is dirty, the cause for update must be
            // "destroyed" window content.
            makeAllDirty();
            dirtyTop    = 0;
            dirtyBottom = rows;
            dirtyLeft   = 0;
            dirtyRight  = cols;
            
            Rectangle clipRect = getClipRect(g);
            memGraphics.setClip(clipRect.x, clipRect.y, clipRect.width,
                                clipRect.height);
            memGraphics.setColor(origBgColor);
            memGraphics.fillRect(clipRect.x, clipRect.y, clipRect.width,
                                 clipRect.height);
            memGraphics.setColor(origFgColor);
        }

        int x, y, curX = 0, curY = 0;
        boolean doCursor = false;
        boolean doCursorInverse = false;

        for(int i = dirtyTop; i < dirtyBottom; i++) {
            y = borderHeight + (i * charHeight);
            int[] attrRow = model.getAttribs(visTop,  i);
            char[] charRow = model.getChars(visTop, i);

            // Sanity checks to see if the model is resized between calls
            // of getAttribs() and getChars()
            if (attrRow == null || charRow == null)
                continue;
            
            if (attrRow.length != charRow.length)
                continue;
            
            if (dirtyLeft > attrRow.length || dirtyRight > attrRow.length)
                continue;

            charbuf.setLength(0);
            int xpos = dirtyLeft;
            Color char_fg = null;
            boolean char_bold = false;

            for(int j = dirtyLeft; j < dirtyRight; j++) {
                Color bgColor  = origBgColor;
                Color fgColor  = origFgColor;
                int attr       = attrRow[j];
                int attrMasked = (attr & DisplayModel.MASK_ATTR);
                boolean doDraw = false;

                x = borderWidth  + (charWidth * j);
                if (((attr & DisplayModel.ATTR_INVERSE) != 0) ^
                           isInsideSelection(visTop + i, j)) {
                    if ((attr & DisplayModel.ATTR_FGCOLOR) != 0) {
                        bgColor = termColors[(attr & DisplayModel.MASK_FGCOL)
                                             >>> DisplayModel.SHIFT_FGCOL];
                    } else {
                        bgColor = origFgColor;
                    }
                    if ((attr & DisplayModel.ATTR_BGCOLOR) != 0) {
                        fgColor = termColors[(attr & DisplayModel.MASK_BGCOL)
                                             >>> DisplayModel.SHIFT_BGCOL];
                    } else {
                        fgColor = origBgColor;
                    }

                    if ((attr & DisplayModel.ATTR_LOWINTENSITY) != 0) {
                        bgColor = DisplayUtil.makeDimmerColor(bgColor);
                    }
                    doDraw = true;
                } else {
                    if ((attr & DisplayModel.ATTR_BGCOLOR) != 0) {
                        bgColor = termColors[(attr & DisplayModel.MASK_BGCOL)
                                             >>> DisplayModel.SHIFT_BGCOL];
                        doDraw = true;
                    }
                    if ((attr & DisplayModel.ATTR_FGCOLOR) != 0) {
                        fgColor = termColors[(attr & DisplayModel.MASK_FGCOL)
                                             >>> DisplayModel.SHIFT_FGCOL];
                        doDraw = true;
                    }

                    if ((attr & DisplayModel.ATTR_LOWINTENSITY) != 0) {
                        fgColor = DisplayUtil.makeDimmerColor(fgColor);
                        doDraw = true;
                    }
                }

                if (hasCursor && (visTop + i) == curRow && j == curCol) {
                    doCursor = true;
                    doCursorInverse = ((attr & DisplayModel.ATTR_INVERSE) != 0);
                    curX = x;
                    curY = y;
                }

		if ((attr & DisplayModel.ATTR_DWIDTH_R) == 0) {
                    // Only draw if bg is different from what we cleared with
                    if (doDraw) {
                        memGraphics.setColor(bgColor);
                        int width = charWidth;
                        if ((attr & DisplayModel.ATTR_DWIDTH_L) != 0) {
                            width *= 2;
                        }
                        memGraphics.fillRect(x, y, width, charHeight);
                    }
                    memGraphics.setColor(fgColor);
                }

                if ((attrMasked & DisplayModel.ATTR_CHARDRAWN) != 0) {
                    if ((attr & DisplayModel.ATTR_INVISIBLE) != 0) {
                        // Don't draw anything invisible, but the
                        // underline should be drawn anyway.
                        drawBufferedString(xpos, baselineIndex + y,
                                           char_fg, char_bold);

                    } else if ((attr & DisplayModel.ATTR_DWIDTH_L) != 0) {
                        // Doublewidth character, this position holds
                        // the char
                        drawBufferedString(xpos, baselineIndex + y,
                                           char_fg, char_bold);
                        boolean isbold = 
                            (attr & DisplayModel.ATTR_BOLD) != 0 ||
                            (attr & DisplayModel.ATTR_BLINKING) != 0;
                        if (isbold) memGraphics.setFont(boldFont);
                        memGraphics.drawChars(charRow, j, 1,
                                              x, y + baselineIndex);
                        if (isbold) memGraphics.setFont(plainFont);

                    } else if ((attr & DisplayModel.ATTR_DWIDTH_R) != 0) {
                        // This is the right part of a doublewidth character
                        drawBufferedString(xpos, baselineIndex + y,
                                           char_fg, char_bold);

                    } else if ((attr & DisplayModel.ATTR_LINEDRAW) != 0) {
                        // Line drawing character
                        drawBufferedString(xpos, baselineIndex + y,
                                           char_fg, char_bold);
                        DisplayUtil.drawLineDrawChar(memGraphics,
                                                     x, y, baselineIndex,
                                                     charRow[j],
                                                     charWidth, charHeight);

//                    } else if ((attr & DisplayModel.ATTR_BOLD) != 0 ||
//                               (attr & DisplayModel.ATTR_BLINKING) != 0) {
//                         System.out.println("drawBoldChar: " + (int)charRow[j]);
//                         drawBufferedString(xpos, baselineIndex + y);
//                         // Approximate blinking with bold font until
//                         // a special update thread is implemented
//                         memGraphics.setFont(boldFont);
//                         memGraphics.drawChars(charRow, j, 1,
//                                               x, y + baselineIndex);
//                         memGraphics.setFont(plainFont);

                    } else if (DisplayUtil.isBoxOrBlockChar(charRow[j])) {
                        drawBufferedString
                            (xpos, baselineIndex + y, char_fg, char_bold);
                        DisplayUtil.drawBoxOrBlockChar
                            (memGraphics, x, y, baselineIndex, 
                             charRow[j], charWidth, charHeight);
                    } else {
                        // Plain character
                        boolean isbold = 
                            (attr & DisplayModel.ATTR_BOLD) != 0 ||
                            (attr & DisplayModel.ATTR_BLINKING) != 0;
                        
                        if (char_fg != null
                            && (char_bold != isbold || 
                                char_fg != memGraphics.getColor())) {
                            drawBufferedString(xpos, baselineIndex + y,
                                               char_fg, char_bold);
                        }
                        if (charbuf.length() == 0) {
                            xpos = x;
                            char_fg = memGraphics.getColor();
                            char_bold = isbold;
                        }
                        charbuf.append(charRow[j]);
                    }
                    if ((attr & DisplayModel.ATTR_UNDERLINE) != 0)
                        memGraphics.drawLine(x, y + baselineIndex,
                                             x + charWidth, y + baselineIndex);
                } else {
                    drawBufferedString(xpos, baselineIndex + y,
                                       char_fg, char_bold);
                }
            }

            drawBufferedString(xpos, baselineIndex + y, char_fg, char_bold);
        }

        if (doCursor) {
            memGraphics.setColor(cursorColor);
            memGraphics.setXORMode(doCursorInverse ? origFgColor : origBgColor);
            if (cursorHollow) {
                memGraphics.drawRect(curX, curY, charWidth-1, charHeight-1);
            } else {
                memGraphics.fillRect(curX, curY, charWidth, charHeight);
            }
            memGraphics.setPaintMode();
            memGraphics.setColor(origFgColor);
        }

        if (logoDraw && logoImg != null) {
            x = logoX;
            y = logoY;
            if (x == -1) {
                if (centerLogoX == -1) {
                    centerLogoX = (hpixels / 2) - (logoW / 2);
                }
                x = centerLogoX;
            }
            if (y == -1) {
                if (centerLogoY == -1) {
                    centerLogoY = (vpixels / 2) - (logoH / 2);
                }
                y = centerLogoY;
            }
            memGraphics.setClip(g.getClip());
            memGraphics.drawImage(logoImg, x, y, this);
        }

        g.drawImage(memImage, 0, 0, null);

        if (updateScrollbar) {
            updateScrollbar = false;
            updateScrollbarValues(visTop, rows, 0, model.getBufferRows());
        }
    }
    

    public void setPosition(final int x, final int y) {
        if (SwingUtilities.isEventDispatchThread()) {
            setPositionI(x, y);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        setPositionI(x, y);
                    }
                });
            } catch (Throwable t) {
            }
        }
    }

    private void setPositionI(int xPos, int yPos) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w == null)
            return;

        Dimension sDim  = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension tDim  = getDimensionOfText(rows, cols);
        Insets    fIns  = w.getInsets();
        int       sbSz  = (haveScrollbar? getScrollbarSize().width:0);

        if (xPos < 0) {
            xPos += sDim.width - tDim.width - fIns.left - fIns.right - sbSz;
        }
        if (yPos < 0) {
            yPos += sDim.height - tDim.height - fIns.top - fIns.bottom;
        }
        this.xPos = xPos;
        this.yPos = yPos;

        if (isShowing()) {
            w.setLocation(xPos, yPos);
            revalidate();
            requestFocus();
        } else {
            pendingShow = true;
        }
        repaint(true);
    }

    public void setGeometry(final int row, final int col) {
        if (DEBUG)
            System.out.println("setGeometry: " + col + "x"+ row + ", " + isShowing());
        if (SwingUtilities.isEventDispatchThread()) {
            setGeometryI(row, col); 
        } else {
            try {
                Dimension tDim  = getDimensionOfText(row, col);
                if (vpixels == tDim.height && hpixels == tDim.width)
                    return;

                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        setGeometryI(row, col);
                    }
                });
            } catch (Throwable t) {
            }
        }
    }

    private void setGeometryI(int row, int col) {
        if (DEBUG)
            System.out.println("setGeometryI: " + col + "x"+ row + ", " + isShowing());
        
        /*
        if (row == rows && col == cols) {
            return;
        }
        */

        Dimension tDim  = getDimensionOfText(row, col);
        if (vpixels == tDim.height && hpixels == tDim.width) 
            return;

        vpixels = tDim.height;
        hpixels = tDim.width;
        rows = row;
        cols = col;
        setSize(tDim);

        if (visTopChangePending) {
            this.visTop = fenceVisTop(visTopChange);
            visTopChangePending = false;
        }

        if (isShowing()) {
            memGraphics = null;
            updateScrollbarValues();
            makeAllDirty();
            revalidate();
            requestFocus();
        } else {
            pendingShow = true;
        }
        repaint();

        if (controller != null) {
            controller.displayResized(row, col, vpixels, hpixels);
        }
    }

    public void setResizable(boolean resizable) {
        Component c = SwingUtilities.getRoot(this);
        if (c instanceof Frame) {
            ((Frame)c).setResizable(resizable);
        }
    }

    public synchronized void resetSelection() {
        hasSelection = false;
        makeSelectionDirty();
        repaint();
    }

    public synchronized void setSelection(int row1, int col1,
                                          int row2, int col2) {

        if (hasSelection) {
            makeSelectionDirty();
        } else {
            hasSelection = true;
        }

        if (row1 < row2) {
            selectionTopRow =    row1;
            selectionTopCol =    col1;
            selectionBottomRow = row2;
            selectionBottomCol = col2;
        } else if (row1 == row2) {
            selectionTopRow = selectionBottomRow = row1;
            if (col1 < col2) {
                selectionTopCol    = col1;
                selectionBottomCol = col2;
            } else {
                selectionTopCol    = col2;
                selectionBottomCol = col1;
            }
        } else {
            selectionTopRow =    row2;
            selectionTopCol =    col2;
            selectionBottomRow = row1;
            selectionBottomCol = col1;
        }

        makeSelectionDirty();
        repaint();
    }

    public void setNoCursor() {
        if (hasCursor) {
            hasCursor = false;
            makeCursorDirty();
            repaint();
        }
    }
    public synchronized void setCursorPosition(int row, int col) {
        makeCursorDirty();
        if (!hasCursor) {
            hasCursor = true;
        }
        curRow = row;
        curCol = col;
        makeCursorDirty();
    }

    public void setBackgroundColor(Color c) {
        origBgColor = c;
        setBackground(origBgColor);
        makeAllDirty();
        repaint();
    }

    public void setForegroundColor(Color c) {
        origFgColor = c;
        setForeground(origFgColor);
        makeAllDirty();
        repaint();
    }

    public void setCursorColor(Color c) {
        cursorColor = c;
        makeAllDirty();
        repaint();
    }

    public void reverseColors() {
        Color swap  = origBgColor;
        origBgColor = origFgColor;
        origFgColor = swap;
        makeAllDirty();
        repaint();
    }

    public void doBell() {
        doBell(false);
    }

    public void doBell(boolean visualBell) {
        if (visualBell) {
            reverseColors();
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
            reverseColors();
        } else {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            if (toolkit != null) {
                try {
                    toolkit.beep();
                } catch (Exception e) {
                    // Could not beep, we are probably an unpriviliged applet
                    // Automatically enable visual-bell now and "sound" it
                    // instead
                    doBell(true);
                }
            }
        }
    }

    public Component getAWTComponent() { return this; }

    public void setIgnoreClose() {
        // XXX Fix this
//         Window w = SwingUtilities.getWindowAncestor(this);
//         if (w != null) {
//             w.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
//         }
    }

    public void windowClosed() {
        removeComponentListener(this);
        removeFocusListener(this);
        removeMouseMotionListener(this);
        removeMouseListener(this);
        runRepaintThread = false;
        controller = null;
        model      = null;
        repainter  = null;

        if (myPanel != null) 
            myPanel.removeAll();
        myPanel = null;
        scrollbar = null;
        removeAll();
    }

    public Component mkButton(String label, String cmd, ActionListener listener) {
        JButton button = new JButton(label);
        button.setActionCommand(cmd);
        button.addActionListener(listener);
        return button;
    }

    /*
     * ImageObserver interface
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y,
                               int width, int height) {
        if (infoflags == ImageObserver.ALLBITS) {
            return false;
        }
        repaint();
        return true;
    }
}
