package org.unknown.tab.close;


import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class DnDCloseButtonTabbedPane extends JTabbedPane {

    public static final long serialVersionUID = 1L;
    private static final int LINEWIDTH = 3;
    private static final String NAME = "TabTransferData";
    private final DataFlavor FLAVOR = new DataFlavor(
            DataFlavor.javaJVMLocalObjectMimeType, NAME);
    private static GhostGlassPane s_glassPane = new GhostGlassPane();

    private boolean m_isDrawRect = false;
    private final transient Rectangle2D m_lineRect = new Rectangle2D.Double();

    private final Color m_lineColor = new Color(0, 100, 255);
    private transient TabAcceptor m_acceptor = null;

    private final DropTarget dropTarget;

    private final ImageIcon icon;
    private final Dimension buttonSize;
    private transient TabListener tabListener;

    public interface TabListener {
        void closeTab(Component var1);

        void rightMouseClickTabEvent(MouseEvent e);

        void clickTabEvent(MouseEvent e);

    }

    public DnDCloseButtonTabbedPane(TabListener tabListener) {
        super();
        this.tabListener = tabListener;
        final DragSourceListener dsl = new DragSourceListener() {
            @Override
            public void dragEnter(DragSourceDragEvent e) {
                e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
            }

            @Override
            public void dragExit(DragSourceEvent e) {
                e.getDragSourceContext()
                        .setCursor(DragSource.DefaultMoveNoDrop);
                m_lineRect.setRect(0, 0, 0, 0);
                m_isDrawRect = false;
                s_glassPane.setPoint(new Point(-1000, -1000));
                s_glassPane.repaint();
            }

            @Override
            public void dragOver(DragSourceDragEvent e) {
                //e.getLocation()
                //This method returns a Point indicating the cursor location in screen coordinates at the moment

                TabTransferData data = getTabTransferData(e);

                if (data == null) {
                    e.getDragSourceContext().setCursor(
                            DragSource.DefaultMoveNoDrop);
                    return;
                } // if


                e.getDragSourceContext().setCursor(
                        DragSource.DefaultMoveDrop);
            }

            public void dragDropEnd(DragSourceDropEvent e) {
                m_isDrawRect = false;
                m_lineRect.setRect(0, 0, 0, 0);

                if (hasGhost()) {
                    s_glassPane.setVisible(false);
                    s_glassPane.setImage(null);
                }

            }

            @Override
            public void dropActionChanged(DragSourceDragEvent e) {
                // Do nothing
            }
        };

        final DragGestureListener dgl = e -> {

            Point tabPt = e.getDragOrigin();
            int dragTabIndex = indexAtLocation(tabPt.x, tabPt.y);
            if (dragTabIndex < 0) {
                return;
            } // if

            initGlassPane(e.getComponent(), e.getDragOrigin(), dragTabIndex);
            try {
                e.startDrag(DragSource.DefaultMoveDrop,
                        new TabTransferable(dragTabIndex), dsl);
            } catch (InvalidDnDOperationException idoe) {
                idoe.printStackTrace();
            }
        };

        dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,
                new CDropTargetListener(), true);
        new DragSource().createDefaultDragGestureRecognizer(this,
                DnDConstants.ACTION_COPY_OR_MOVE, dgl);
        m_acceptor = (a_component, a_index) -> true;

        icon = new ImageIcon(getClass().getClassLoader().getResource("delete1.png"));
        buttonSize = new Dimension(icon.getIconWidth(), icon.getIconHeight());
    }

    @Override
    public void addTab(String title, final Component component) {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        JButton button = new JButton(icon);
        button.setPreferredSize(buttonSize);
        button.setUI(new BasicButtonUI());
        button.setContentAreaFilled(false);
        button.addActionListener(e -> {
            ((DnDCloseButtonTabbedPane) component.getParent()).remove(component);
            if (tabListener != null) {
                tabListener.closeTab(component);
            }
        });

        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (tabListener == null) {
                    return;
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    tabListener.rightMouseClickTabEvent(e);

                } else {
                    tabListener.clickTabEvent(e);
                }

                setSelectedIndex(getTargetTabIndex(e.getComponent().getLocation()));
            }
        });
        tab.add(label, BorderLayout.WEST);
        tab.add(button, BorderLayout.EAST);
        tab.setBorder(BorderFactory.createEmptyBorder(2, 1, 1, 1));
        super.addTab(title, component);

        setTabComponentAt(indexOfComponent(component), tab);
    }

    public TabAcceptor getAcceptor() {
        return m_acceptor;
    }

    public void setAcceptor(TabAcceptor value) {
        m_acceptor = value;
    }

    private TabTransferData getTabTransferData(DropTargetDropEvent a_event) {
        try {
            TabTransferData data = (TabTransferData) a_event.getTransferable().getTransferData(FLAVOR);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private TabTransferData getTabTransferData(DropTargetDragEvent a_event) {
        try {
            TabTransferData data = (TabTransferData) a_event.getTransferable().getTransferData(FLAVOR);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private TabTransferData getTabTransferData(DragSourceDragEvent a_event) {
        try {
            TabTransferData data = (TabTransferData) a_event.getDragSourceContext()
                    .getTransferable().getTransferData(FLAVOR);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    TabTransferData getTabTransferData(DragSourceDropEvent a_event) {
        try {
            TabTransferData data = (TabTransferData) a_event.getDragSourceContext()
                    .getTransferable().getTransferData(FLAVOR);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    class TabTransferable implements Transferable {

        private TabTransferData m_data = null;

        public TabTransferable(int a_tabIndex) {
            m_data = new TabTransferData(DnDCloseButtonTabbedPane.this, a_tabIndex);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            return m_data;
            // return DnDTabbedPane.this
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            DataFlavor[] f = new DataFlavor[1];
            f[0] = FLAVOR;
            return f;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.getHumanPresentableName().equals(NAME);
        }
    }

    class TabTransferData {

        private DnDCloseButtonTabbedPane m_tabbedPane = null;
        private int m_tabIndex = -1;

        public TabTransferData() {
        }

        public TabTransferData(DnDCloseButtonTabbedPane tabbedPane, int aTabIndex) {
            m_tabbedPane = tabbedPane;
            m_tabIndex = aTabIndex;
        }

        public DnDCloseButtonTabbedPane getTabbedPane() {
            return m_tabbedPane;
        }

        public void setTabbedPane(DnDCloseButtonTabbedPane pane) {
            m_tabbedPane = pane;
        }

        public int getTabIndex() {
            return m_tabIndex;
        }

        public void setTabIndex(int index) {
            m_tabIndex = index;
        }
    }

    private Point buildGhostLocation(Point aLocation) {
        Point retval = new Point(aLocation);

        retval = SwingUtilities.convertPoint(DnDCloseButtonTabbedPane.this,
                retval, s_glassPane);
        return retval;
    }

    class CDropTargetListener implements DropTargetListener {

        public void dragEnter(DropTargetDragEvent e) {

            if (isDragAcceptable(e)) {
                e.acceptDrag(e.getDropAction());
            } else {
                e.rejectDrag();
            } // if
        }

        public void dragExit(DropTargetEvent e) {
            m_isDrawRect = false;
        }

        public void dropActionChanged(DropTargetDragEvent e) {
            // Do nothing
        }

        public void dragOver(final DropTargetDragEvent e) {
            TabTransferData data = getTabTransferData(e);
            if (data == null) {
                return;
            }

            if (getTabPlacement() == JTabbedPane.TOP
                    || getTabPlacement() == JTabbedPane.BOTTOM) {
                initTargetLeftRightLine(getTargetTabIndex(e.getLocation()), data);
            } else {
                initTargetTopBottomLine(getTargetTabIndex(e.getLocation()), data);
            } // if-else

            repaint();
            if (hasGhost()) {
                s_glassPane.setPoint(buildGhostLocation(e.getLocation()));
                s_glassPane.repaint();
            }
        }

        @Override
        public void drop(DropTargetDropEvent aEvent) {

            if (isDropAcceptable(aEvent)) {
                convertTab(getTabTransferData(aEvent),
                        getTargetTabIndex(aEvent.getLocation()));
                aEvent.dropComplete(true);
            } else {
                aEvent.dropComplete(false);
            } // if-else

            m_isDrawRect = false;
            repaint();
        }

        public boolean isDragAcceptable(DropTargetDragEvent e) {
            Transferable t = e.getTransferable();
            if (t == null) {
                return false;
            } // if

            DataFlavor[] flavor = e.getCurrentDataFlavors();
            if (!t.isDataFlavorSupported(flavor[0])) {
                return false;
            } // if

            TabTransferData data = getTabTransferData(e);

            if (data != null) {
                if (DnDCloseButtonTabbedPane.this == data.getTabbedPane()
                        && data.getTabIndex() >= 0) {
                    return true;
                } // if

                if (DnDCloseButtonTabbedPane.this != data.getTabbedPane() && m_acceptor != null) {
                    return m_acceptor.isDropAcceptable(data.getTabbedPane(), data.getTabIndex());
                } // if
            }

            boolean transferDataFlavorFound = false;
            for (DataFlavor transferDataFlavor : t.getTransferDataFlavors()) {
                if (FLAVOR.equals(transferDataFlavor)) {
                    transferDataFlavorFound = true;
                    break;
                }
            }
            if (transferDataFlavorFound == false) {
                return false;
            }
            return false;
        }

        public boolean isDropAcceptable(DropTargetDropEvent e) {

            Transferable t = e.getTransferable();
            if (t == null) {
                return false;
            } // if

            DataFlavor[] flavor = e.getCurrentDataFlavors();
            if (!t.isDataFlavorSupported(flavor[0])) {
                return false;
            } // if

            TabTransferData data = getTabTransferData(e);
            if (data == null) {
                return false;
            }

            if (DnDCloseButtonTabbedPane.this == data.getTabbedPane()
                    && data.getTabIndex() >= 0) {
                return true;
            } // if

            if (DnDCloseButtonTabbedPane.this != data.getTabbedPane() && m_acceptor != null) {
                return m_acceptor.isDropAcceptable(data.getTabbedPane(), data.getTabIndex());
            } // if

            return false;
        }
    }

    private boolean mHasGhost = true;

    public void setPaintGhost(boolean flag) {
        mHasGhost = flag;
    }

    public boolean hasGhost() {
        return mHasGhost;
    }

    /**
     * returns potential index for drop.
     *
     * @param aPoint point given in the drop site component's coordinate
     * @return returns potential index for drop.
     */
    private int getTargetTabIndex(Point aPoint) {
        boolean isTopOrBottom = getTabPlacement() == JTabbedPane.TOP
                || getTabPlacement() == JTabbedPane.BOTTOM;

        // if the pane is empty, the target index is always zero.
        if (getTabCount() == 0) {
            return 0;
        } // if

        for (int i = 0; i < getTabCount(); i++) {
            Rectangle r = getBoundsAt(i);
            if (isTopOrBottom) {
                r.setRect(r.x - r.width / 2, r.y, r.width, r.height);
            } else {
                r.setRect(r.x, r.y - r.height / 2, r.width, r.height);
            } // if-else

            if (r.contains(aPoint)) {
                return i;
            } // if
        } // for

        Rectangle r = getBoundsAt(getTabCount() - 1);
        if (isTopOrBottom) {
            int x = r.x + r.width / 2;
            r.setRect(x, r.y, getWidth() - x, r.height);
        } else {
            int y = r.y + r.height / 2;
            r.setRect(r.x, y, r.width, getHeight() - y);
        } // if-else

        return r.contains(aPoint) ? getTabCount() : -1;
    }

    private void convertTab(TabTransferData aData, int aTargetIndex) {
        if (aData == null) {
            return;
        }
        DnDCloseButtonTabbedPane source = aData.getTabbedPane();
        int sourceIndex = aData.getTabIndex();
        if (sourceIndex < 0) {
            return;
        } // if
        //Save the tab's component, title, and TabComponent.
        Component cmp = source.getComponentAt(sourceIndex);
        String str = source.getTitleAt(sourceIndex);
        Component tcmp = source.getTabComponentAt(sourceIndex);

        if (this != source) {
            source.remove(sourceIndex);

            if (aTargetIndex == getTabCount()) {
                addTab(str, cmp);
                setTabComponentAt(getTabCount() - 1, tcmp);
            } else {
                if (aTargetIndex < 0) {
                    aTargetIndex = 0;
                } // if

                insertTab(str, null, cmp, null, aTargetIndex);
                setTabComponentAt(aTargetIndex, tcmp);
            } // if

            setSelectedComponent(cmp);
            return;
        } // if
        if (aTargetIndex < 0 || sourceIndex == aTargetIndex) {
            return;
        } // if
        if (aTargetIndex == getTabCount()) {
            source.remove(sourceIndex);
            addTab(str, cmp);
            setTabComponentAt(getTabCount() - 1, tcmp);
            setSelectedIndex(getTabCount() - 1);
        } else if (sourceIndex > aTargetIndex) {
            source.remove(sourceIndex);
            insertTab(str, null, cmp, null, aTargetIndex);
            setTabComponentAt(aTargetIndex, tcmp);
            setSelectedIndex(aTargetIndex);
        } else {
            source.remove(sourceIndex);
            insertTab(str, null, cmp, null, aTargetIndex - 1);
            setTabComponentAt(aTargetIndex - 1, tcmp);
            setSelectedIndex(aTargetIndex - 1);
        }
    }

    private void initTargetLeftRightLine(int next, TabTransferData aData) {
        if (aData == null) {
            return;
        }
        if (next < 0) {
            m_lineRect.setRect(0, 0, 0, 0);
            m_isDrawRect = false;
            return;
        } // if

        if ((aData.getTabbedPane() == this)
                && (aData.getTabIndex() == next
                || next - aData.getTabIndex() == 1)) {
            m_lineRect.setRect(0, 0, 0, 0);
            m_isDrawRect = false;
        } else if (getTabCount() == 0) {
            m_lineRect.setRect(0, 0, 0, 0);
            m_isDrawRect = false;
            return;
        } else if (next == 0) {
            Rectangle rect = getBoundsAt(0);
            m_lineRect.setRect(-LINEWIDTH / 2, rect.y, LINEWIDTH, rect.height);
            m_isDrawRect = true;
        } else if (next == getTabCount()) {
            Rectangle rect = getBoundsAt(getTabCount() - 1);
            m_lineRect.setRect(rect.x + rect.width - LINEWIDTH / 2, rect.y,
                    LINEWIDTH, rect.height);
            m_isDrawRect = true;
        } else {
            Rectangle rect = getBoundsAt(next - 1);
            m_lineRect.setRect(rect.x + rect.width - LINEWIDTH / 2, rect.y,
                    LINEWIDTH, rect.height);
            m_isDrawRect = true;
        }
    }

    private void initTargetTopBottomLine(int next, TabTransferData aData) {
        if (next < 0) {
            m_lineRect.setRect(0, 0, 0, 0);
            m_isDrawRect = false;
            return;
        } // if

        if ((aData.getTabbedPane() == this)
                && (aData.getTabIndex() == next
                || next - aData.getTabIndex() == 1)) {
            m_lineRect.setRect(0, 0, 0, 0);
            m_isDrawRect = false;
        } else if (getTabCount() == 0) {
            m_lineRect.setRect(0, 0, 0, 0);
            m_isDrawRect = false;
            return;
        } else if (next == getTabCount()) {
            Rectangle rect = getBoundsAt(getTabCount() - 1);
            m_lineRect.setRect(rect.x, rect.y + rect.height - LINEWIDTH / 2,
                    rect.width, LINEWIDTH);
            m_isDrawRect = true;
        } else if (next == 0) {
            Rectangle rect = getBoundsAt(0);
            m_lineRect.setRect(rect.x, -LINEWIDTH / 2, rect.width, LINEWIDTH);
            m_isDrawRect = true;
        } else {
            Rectangle rect = getBoundsAt(next - 1);
            m_lineRect.setRect(rect.x, rect.y + rect.height - LINEWIDTH / 2,
                    rect.width, LINEWIDTH);
            m_isDrawRect = true;
        }
    }

    private void initGlassPane(Component c, Point tabPt, int aTabIndex) {
        getRootPane().setGlassPane(s_glassPane);
        if (hasGhost()) {
            Rectangle rect = getBoundsAt(aTabIndex);
            BufferedImage image = new BufferedImage(c.getWidth(),
                    c.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.getGraphics();
            c.paint(g);
            image = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
            s_glassPane.setImage(image);
        } // if

        s_glassPane.setPoint(buildGhostLocation(tabPt));
        s_glassPane.setVisible(true);
    }

    Rectangle getTabAreaBound() {
        Rectangle lastTab = getUI().getTabBounds(this, getTabCount() - 1);
        return new Rectangle(0, 0, getWidth(), lastTab.y + lastTab.height);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (m_isDrawRect) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(m_lineColor);
            g2.fill(m_lineRect);
        } // if
    }

    public interface TabAcceptor {

        boolean isDropAcceptable(DnDCloseButtonTabbedPane a_component, int a_index);
    }
}

class GhostGlassPane extends JPanel {

    public static final long serialVersionUID = 1L;
    private final transient AlphaComposite m_composite;

    private transient Point m_location = new Point(0, 0);

    private transient BufferedImage m_draggingGhost = null;

    public GhostGlassPane() {
        setOpaque(false);
        m_composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);
    }

    public void setImage(BufferedImage draggingGhost) {
        m_draggingGhost = draggingGhost;
    }

    public void setPoint(Point a_location) {
        m_location.x = a_location.x;
        m_location.y = a_location.y;
    }

    public int getGhostWidth() {
        if (m_draggingGhost == null) {
            return 0;
        } // if

        return m_draggingGhost.getWidth(this);
    }

    public int getGhostHeight() {
        if (m_draggingGhost == null) {
            return 0;
        } // if

        return m_draggingGhost.getHeight(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        if (m_draggingGhost == null) {
            return;
        } // if

        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(m_composite);

        g2.drawImage(m_draggingGhost, (int) m_location.getX(), (int) m_location.getY(), null);
    }
}