package mangaclient;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 *
 * @author David Huynh
 */
public class MangaClient {

    public static void main(String[] args) {
        Color neptune = new Color(18, 55, 63);
        Font police = new Font("Tahoma", Font.BOLD, 12);
        Color teal = new Color(122, 216, 247);

        try {
            //Loads the Nimbus look and feel
            //Retrieval method is convulted, but stable.
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    UIManager.getLookAndFeelDefaults().put("Button.background", neptune);
                    UIManager.getLookAndFeelDefaults().put("Button.font", police);
                    UIManager.getLookAndFeelDefaults().put("Button.textForeground", teal);
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //Initializes SplashScreen
        SplashScreen.splash("M.O.R IS NOW LOADING...", police.deriveFont(32f), teal, Color.BLACK);
        MangaGui gui = new MangaGui();
        gui.setVisible(true);

        //Closes SplashScreen after application loads
        SplashScreen.disposeSplash();

        //Size the Frame
        gui.pack();

        //FullScreen on Loadup
        gui.setExtendedState(gui.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }
}
