
package xyz.neilanthony;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AbbeLogging {
    
    static private Logger logger = null;
    static private ConsoleHandler handler = null;
    
    // update level here
    static private Level loggingLevel = Level.FINER;
    
    static {
        
        logger = Logger.getLogger("AbbeLog");
        logger.setLevel(loggingLevel);
        handler = new ConsoleHandler();
        // PUBLISH this level
        handler.setLevel(loggingLevel);
        logger.addHandler(handler);
        
    }
    
    static public void postToLog (Level lev, String classStr, String methStr, String msg) {
        logger.logp(lev, classStr, methStr, msg);
    }
    
}
