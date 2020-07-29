package swingextended;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * A {@link JFrame} whose child components are laid out top to bottom, with space added between and around each
 * component
 */
public abstract class ExFrame extends JFrame implements WindowListener {

    // Defines the padding added around the left, bottom, right and top of each layer
    private int componentSpacing, topSpacing;

    /**
     * Creates a new {@link ExFrame} with given padding around each layer. Sets Swing's look and feel the the OS's
     * look and feel in the process.
     * @param componentSpacing The space, in pixels, to place between each layer.
     */
    public ExFrame(int componentSpacing) {
        ExLookAndFeel.set();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new ExLayeredLayout(this));
        this.componentSpacing = componentSpacing;
        topSpacing = componentSpacing;
        addWindowListener(this);
    }

    /**
     * Creates a new {@link ExLayer} which will be added to the frame's content pane. Returns a reference to the
     * created layer.
     * @param alignment Defines how the components in this layer should be aligned. See {@link ExLayer}
     * @return a reference to the created {@link ExLayer}
     */
    public ExLayer newLayer(int alignment) {
        ExLayer layer = new ExLayer(alignment, componentSpacing);
        layer.setBorder(new EmptyBorder(topSpacing, componentSpacing, componentSpacing, componentSpacing));
        topSpacing = 0;
        add(layer);
        return layer;
    }

    /**
     * A method to be implemented that constructs the frame's child components, lays them out within the frame
     * and makes the frame visible
     */
    public abstract void createAndShow();

    /**
     * A method to be implemented that will be called when the user attempts to close the window. In the event the
     * window should not be closed, the method should return false, otherwise true.
     * @return true if the frame should be closed (calling {@link JFrame#dispose}, otherwise false
     */
    public abstract boolean closingAttempted();

    /**
     * Method called when the user attempts to close the window. The {@link ExFrame#closingAttempted()} method is
     * called, disposing the window if it returns true, otherwise closing is aborted
     * @param eventInfo Event information passed by Swing
     */
    @Override
    public void windowClosing(WindowEvent eventInfo) {
        if(closingAttempted())
            dispose();
    }

    // Remaining methods are required implementations for the WindowListener interface
    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
