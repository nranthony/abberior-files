/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import java.nio.file.Path;
import java.util.Vector;

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
        abFile.pullOMEXML();
    }
    
    
    
}
