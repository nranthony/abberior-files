/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package xyz.neilanthony;

import java.io.IOException;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;
import javax.xml.parsers.ParserConfigurationException;
import loci.formats.FormatException;
import org.scijava.log.LogService;
import org.scijava.ui.UserInterface;
import org.xml.sax.SAXException;

/**
 * This example illustrates how to create an ImageJ {@link Command} plugin.
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 * <p>
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>EmoryICI>Open Abberior Files")
public class OpenAbberior<T extends RealType<T>> implements Command {
    //
    // Feel free to add more parameters here...
    //
    
    @Parameter
    private LogService logService;
    
//    @Parameter
//    private Dataset currentData;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Override
    public void run() {
        
        final UserInterface ui = uiService.getDefaultUI();
        // open GUI window
        OpenAbbeJFrame AbbeFrame = null;
        try {
            AbbeFrame = new OpenAbbeJFrame(ui);
//            AbbeFrame = new OpenAbbeJFrame();
        } catch (FormatException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(OpenAbberior.class.getName()).log(Level.SEVERE, null, ex);
        }
        AbbeFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        AbbeFrame.setLocation(64, 64);
        
        //  TODO - make icon images of all sizes
        //ImageIcon icon = new ImageIcon(AbbeFrame.getClass().getResource("icon.png"));
        //AbbeFrame.setIconImage(icon.getImage());

        //AbbeFrame.invalidate();
        //AbbeFrame.repaint();
        AbbeFrame.setVisible(true);
    }

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        System.setProperty("scijava.log.level", "info");
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        // invoke the plugin
        ij.command().run(OpenAbberior.class, true);

    }

}
