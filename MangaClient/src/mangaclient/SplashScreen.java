/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mangaclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author David Huynh
 */
public class SplashScreen extends JFrame {

    private static SplashScreen instance;

    private boolean paintCalled = false;

    private Image image;

    private SplashScreen(Image image) {
        super();
        this.image = image;
        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(image));
        label.setBackground(Color.WHITE);
        this.add(label);
        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.pack();
        this.setLocationRelativeTo(null);
    }

    private SplashScreen(JLabel label) {
        super();
        this.getContentPane().add(label);
        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        Dimension lSize = label.getPreferredSize();
        if (lSize.width < 500 || lSize.height < 250) {
            this.setSize(500, 250);
        } else {
            this.pack();
        }
        this.setLocationRelativeTo(null);
    }

    /**
     * Displays a splash screen with the specified font, using the specified
     * foreground and background.
     *
     * @param splashText The text
     * @param font The font
     * @param foreground The color of the text
     * @param background The color of the background
     */
    public static void splash(String splashText, Font font, Color foreground, Color background) {
        JLabel label = new JLabel(splashText);
        label.setFont(font);
        label.setForeground(foreground);
        label.setBackground(background);
        label.setOpaque(true);
        label.setHorizontalAlignment(JLabel.HORIZONTAL);
        label.setBorder(BorderFactory.createLineBorder(foreground));
        splash(label);
    }

    /**
     * A URL pointing to an image you would like to display from a URL.
     *
     * @param imageURL
     */
    public static void splash(URL imageURL) {
        if (imageURL != null) {
            splash(Toolkit.getDefaultToolkit().createImage(imageURL));
        }
    }

    /**
     * Displays the specified image as the SplashScreen
     *
     * @param image The image you want to display.
     */
    public static void splash(Image image) {
        if (instance == null && image != null) {
            instance = new SplashScreen(image);
            instance.setVisible(true);

            paint();
        }
    }

    /**
     * Displays an image using the specified JLabel. Allows custom formating of
     * screen.
     *
     * @param label The JLabel you want to display.
     */
    public static void splash(JLabel label) {
        if (instance == null && label != null) {
            instance = new SplashScreen(label);
            instance.setVisible(true);

            paint();
        }
    }

    private static void paint() {
        if (!EventQueue.isDispatchThread() && Runtime.getRuntime().availableProcessors() == 1) {
            synchronized (instance) {
                while (!instance.paintCalled) {
                    try {
                        instance.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        if (image == null) {
            super.paint(g);
        }
        g.drawImage(image, 0, 0, this);

        if (!paintCalled) {
            paintCalled = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Closes the splash screen.
     */
    public static void disposeSplash() {
        if (instance == null) {
            return;
        }
        instance.setVisible(false);
        instance.dispose();
    }
}
