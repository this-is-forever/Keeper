package swingextended;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public abstract class ExFrame extends JFrame implements WindowListener {

    private int componentSpacing, topSpacing;

    public ExFrame(int componentSpacing) {
        ExLookAndFeel.set();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new ExLayeredLayout(this));
        this.componentSpacing = componentSpacing;
        topSpacing = componentSpacing;
        addWindowListener(this);
    }

    public ExLayer newLayer(int alignment) {
        ExLayer layer = new ExLayer(alignment, componentSpacing);
        layer.setBorder(new EmptyBorder(topSpacing, componentSpacing, componentSpacing, componentSpacing));
        topSpacing = 0;
        add(layer);
        return layer;
    }

    public abstract void createAndShow();

    public abstract boolean closingAttempted();

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        if(closingAttempted())
            dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {

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
