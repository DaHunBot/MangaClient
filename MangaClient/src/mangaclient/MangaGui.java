package mangaclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.swing.*;

/**
 *
 * @author David Huynh
 */
public class MangaGui extends JFrame {

    /*
    * Variables and Other Objects
    * Used to create and hold data within the UI/Interface
     */
    private ImagePanel page; //Manga Displayed on Page
    private JPanel pageUI;
    private JPanel toolbar;
    private Container pane;

    //Webscraping from Manga Engines
    private MorEngine morEngine;

    //User Buttons for MOR Interface
    private JButton next;
    private JButton previous;

    //private JLabel background;
    //Toolbar items
    private JTextField mangaSel;
    private AutoSuggestor autoSelect;
    private JComboBox<String> chapterSel;
    private JComboBox<String> pageSel;
    private JComboBox<String> engineSel;

    //Store manga APIs
    private LinkedHashMap<String, MorEngine> mangaEngineMap = new LinkedHashMap<>();

    //Creates and stores keystrokes
    private HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();

    private static final String instructions = "<html><center>"
            + "<p><font size =\"32\">Under Development... Currently broken since sites are down"
            + "<font size =\"32\"></p>" + "</center></html>";

    /**
     * **********************************************************************************************
     */
    public MangaGui() {
        super();
        gui();
    }

    private void gui() {
        this.setTitle("Manga Offline Reader");
        this.setBackground(Color.BLACK);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Previous Button
        previous = new JButton("Previous Page");
        previous.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadPage(morEngine.getPreviousPage());
            }
        });

        //Next Button
        next = new JButton("Next Page");
        next.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadPage(morEngine.getNextPage());
            }
        });

        //Textfield for user to input name of manga
        mangaSel = new JTextField("Type your manga into here");
        mangaSel.setEditable(true);
        mangaSel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disableNavigation(false);
                loadPage(morEngine.getMangaURL(mangaSel.getText()));
                refreshList();
            }
        });

        //Loads Manga Engines in a separate Thread to reduce start up time
        engineSel = new JComboBox<String>(new HashMapComboBox<String, MorEngine>(mangaEngineMap));

        new Thread(new Runnable() {
            private boolean engineLoadingFailed = false;

            @Override
            public void run() {

                try {
                    //Loading MangaHereAPI
                    mangaEngineMap.put("MangaHere", new MangaHereAPI());
                } catch (IOException ex) {
                    catcher(ex);
                }

                try {
                    //Loading MangaReaderAPI
                    mangaEngineMap.put("MangaReader", new MangaReaderAPI());
                } catch (IOException ex) {
                    catcher(ex);
                }

                if (engineLoadingFailed) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(MangaGui.this, "ERROR",
                                    "M.O.R. Engine Could Not Be Loaded", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }

            private void catcher(Exception e) {
                Toolkit.getDefaultToolkit().beep();
                Logger.log(e);
                engineLoadingFailed = true;
            }
        }).start();

        //Loads The First Manga that is chosen
        while (morEngine == null) {
            try {
                if (mangaEngineMap.keySet().isEmpty()) {
                    Thread.sleep(100);
                } else {
                    String first = mangaEngineMap.keySet().toArray()[0].toString();
                    engineSel.setSelectedItem(first);
                    morEngine = new Preloader(this, mangaEngineMap.get(first));
                }
            } catch (InterruptedException e) {
                System.exit(ABORT);
            }
        }
        /**
         * **********************************************************************************************
         * ToolBar Construction
         *
         * This section is dedicated to creating the toolbar for ease of use in
         * navigation through the M.O.R. Application
         *
         */

        //Instantionates and sets up asthetics for toolbar
        toolbar = new JPanel() {

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(0, 0,
                        getBackground().brighter(), 0, getHeight(),
                        getBackground().darker());

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        /*
         * Chapter Selection
         * Giving the option of navigating through chapters with dropdown
         */
        chapterSel = new JComboBox<String>(StringUtil.formatChapterNames(
                morEngine.getChapterNames()));
        chapterSel.setToolTipText("Chapter Navigation");
        chapterSel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (evt.getModifiers() != 0 && evt.getSource() instanceof JComboBox
                        && ((JComboBox<?>) evt.getSource()).isPopupVisible()) {
                    int index = chapterSel.getSelectedIndex();
                    try {
                        loadPage(morEngine.loadImg(morEngine.getChapterList()[index]));
                        refreshList();
                        autoSelect.setDictionary(morEngine.getMangaList());
                        updateStatus();
                    } catch (IOException e) {
                        Logger.log(e);
                    }
                }
            }
        });


        /*
         * Page Selection
         * Giving the option of flipping through pages with dropdown
         */
        pageSel = generateComboBox("Pg: ", morEngine.getPageList().length);
        pageSel.setToolTipText("Page Navigation");
        pageSel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                //Checks if it wasn't programmatically fired.
                if (evt.getModifiers() != 0 && evt.getSource() instanceof JComboBox
                        && ((JComboBox<?>) evt.getSource()).isPopupVisible()) {
                    int index = pageSel.getSelectedIndex();
                    try {
                        loadPage(morEngine.loadImg(morEngine.getPageList()[index]));
                        updateStatus();
                    } catch (IOException e) {
                        Logger.log(e);
                    }
                }
            }
        });

        /*
         * Engine Selection
         * Allows Users to use different sources, ie. MangaHere, ,etc      
         */
        engineSel.setToolTipText("Manga Source Selection");
        engineSel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (evt.getModifiers() != 0) {//Checks if it wasn't programmatically fired.
                    String engineName = (String) engineSel.getSelectedItem();
                    try {
                        if (morEngine instanceof Closeable) {
                            ((Closeable) morEngine).close();
                        }
                        morEngine = new Preloader(MangaGui.this, mangaEngineMap.get(engineName));
                        loadPage((BufferedImage) null);
                        page.setText(instructions + "</center></html>");
                        disableNavigation(true);
                        refreshList();
                        autoSelect.setDictionary(morEngine.getMangaList());//Resets the list
                    } catch (IOException e) {
                        Logger.log(e);
                    }
                }
            }
        });

        //Wraps the TextField with my custom autosuggestion box
        autoSelect = new AutoSuggestor(mangaSel, this, morEngine.getMangaList(),
                Color.WHITE.brighter(), Color.BLUE, Color.MAGENTA.darker(), 1f);

        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toolbar.setBackground(Color.DARK_GRAY.brighter());

        //Adds Items to toolbar
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.add(mangaSel);
        toolbar.add(engineSel);
        toolbar.add(chapterSel);
        toolbar.add(pageSel);

        /**
         * **********************************************************************************************
         * Adding everything into a JFrame
         */
        //next.setPreferredSize(previous.getPreferredSize());////Makes the buttons the same size
        //Background Screen
        /*
        ImageIcon bg = new ImageIcon(getClass().getResource("BoyFace.png"));
        Image scaleBG = bg.getImage();
        scaleBG = scaleBG.getScaledInstance(scaleBG.getWidth(null) * 5, scaleBG.getHeight(null) * 5, scaleBG.SCALE_SMOOTH);
        bg.setImage(scaleBG);
        background = new JLabel(bg);
         */
        pane = getContentPane();
        pane.setLayout(new BorderLayout());

        page = new ImagePanel();
        page.setLayout(new BorderLayout());
        page.setPreferredSize(getEffectiveScreenSize());
        page.setBackground(Color.BLACK);

        //page.add(background, BorderLayout.CENTER);
        page.setForeground(next.getForeground());
        page.setFont(next.getFont().deriveFont(72f));

        page.setDoubleBuffered(true);

        //User Welcome Screen!
        page.setText("<html><center><p> Welcome to M.O.R!</p> "
                + "<h1>A Java Manga Application</h1>" + instructions
                + "<p>Enjoy!</p></center>");

        //adding to User Window
        pageUI = new JPanel(new BorderLayout(0, 0));
        pageUI.add(next, BorderLayout.EAST);
        pageUI.add(previous, BorderLayout.WEST);
        pageUI.add(page, BorderLayout.CENTER);

        pane.add(toolbar, BorderLayout.NORTH);
        pane.add(pageUI, BorderLayout.CENTER);

        disableNavigation(true);
        initKeyboard();
    }

    /**
     * **********************************************************************************************
     * Actions
     */
    private void initKeyboard() {
        Action nextPageAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (pageSel.isEnabled()) {
                    next.doClick();
                }
            }
        };

        Action previousPageAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (pageSel.isEnabled()) {
                    previous.doClick();
                }
            }
        };

        //Loads Bindings into JFrame
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "NEXT");
        this.getRootPane().getActionMap().put("NEXT", nextPageAction);

        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "PREV");
        this.getRootPane().getActionMap().put("PREV", previousPageAction);
    }

    /**
     * **********************************************************************************************
     * Behind the scenes of updating pages when an action is performed
     */
    /**
     * Loads manga from source URL from M.O.R. Engine
     */
    private void loadPage(String URL) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                if (morEngine.getCurrentURL().equals(URL)) {
                    return;//No need to waste time reloading a page.
                }

                loadPage(morEngine.loadImg(URL));
                page.setText(null);
                updateStatus();
                return;
            } catch (IOException e) {
                Logger.log(e);
                page.loadImage(null);
                page.setText("<html><p><center>An error has occurred :(</p>"
                        + "<h1>Sorry, the currently requested title or page number could not be found. "
                        + "Please try a different page, chapter, or manga source. "
                        + "If you encountered this error while searching for a manga title, "
                        + "the manga you currently have requested is most likely licensed; "
                        + "hence, not available in your country and/or region. "
                        + "We apologize for this inconvenience.</h1> "
                        + "<h1>Thank you for your cooperation!</h1></center></html>");
                Toolkit.getDefaultToolkit().beep();
                return;
            }
        }
    }

    // Loads Image into page
    private void loadPage(BufferedImage image) {
        page.loadImage(image);
    }

    // Define ScreenSize with the toolbar
    private Dimension getEffectiveScreenSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int taskBarSize = scnMax.bottom;
        return new Dimension(screenSize.width, screenSize.height - taskBarSize);
    }

    //Creates a combo box 
    private JComboBox<String> generateComboBox(String prefix, int size) {
        String[] out = new String[size];
        for (int i = 0; i < size; i++) {
            out[i] = (prefix + (i + 1));
        }
        return new JComboBox<String>(out);
    }

    //Disables Buttons on startup, until a manga is loaded
    private void disableNavigation(boolean disabled) {
        boolean enabled = !disabled;
        pageSel.setEnabled(enabled);
        chapterSel.setEnabled(enabled);
        next.setVisible(enabled);
        previous.setVisible(enabled);
    }

    private void updateStatus() {
        String chapter = "Ch: " + morEngine.getCurrentChapNum();
        String currentPage = "Pg: " + morEngine.getCurrentPageNum();
        chapterSel.getModel().setSelectedItem(chapter);//Work around to forcefully change combobox
        pageSel.getModel().setSelectedItem(currentPage);
    }

    //refreshs ComboBoxes
    private void refreshList() {
        //Work around to forcefully refresh
        chapterSel.setModel(new DefaultComboBoxModel<String>(morEngine.getChapterNames()));
        String[] pages = new String[morEngine.getPageList().length];
        for (int i = 0; i < morEngine.getPageList().length; i++) {
            pages[i] = ("Pg: " + (i + 1));
        }
        System.out.print("UPDATED ");
        pageSel.setModel(new DefaultComboBoxModel<String>(pages));
    }
}
