/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

/**
 *
 * @author nelly
 */
public class OpenAbbeTask implements Runnable {

    private Vector oVect;
    private AbbeFile abFile = new AbbeFile();
    private Path fPath;
    
    
    OpenAbbeTask(Vector outVect, Path filePath){
        oVect = outVect;
        fPath = filePath;
        abFile.setPath(filePath);
    }
    
    @Override
    public void run() {
        try {
            abFile.pullOMEXML();
        } catch (DependencyException ex) {
            Logger.getLogger(OpenAbbeTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            Logger.getLogger(OpenAbbeTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FormatException ex) {
            Logger.getLogger(OpenAbbeTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OpenAbbeTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}
