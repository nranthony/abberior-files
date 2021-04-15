/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import io.scif.gui.BufferedImageReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

import java.nio.file.Path;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;

/**
 *
 * @author nelly
 */
public class AbbeFile {
    
    private String omexml;
    private int index = -1;
    private Path fPath = null;
    
    AbbeFile() {
        // constructor
    }

    public void setIndex () {

    }

    public void setPath (Path filePath) {
        fPath = filePath;
    }

    public void pullOMEXML () throws DependencyException, ServiceException, FormatException, IOException {

        OMEXMLService omexmlService = null;
        ServiceFactory factory = new ServiceFactory();
        omexmlService = factory.getInstance(OMEXMLService.class);

        MetadataStore omexmlStore = null;
        omexmlStore = omexmlService.createOMEXMLMetadata();
        
        IFormatReader reader = null;
        reader = new ImageReader();
        reader.setId(this.fPath.toString());
                
        MetadataStore readerMetaStore = reader.getMetadataStore();
        MetadataRetrieve retrieve = omexmlService.asRetrieve(readerMetaStore);
        this.omexml = omexmlService.getOMEXML(retrieve);
        
    }
    
    public void pullOMEXMLRaw () throws FileNotFoundException, IOException {
        File file = new File(this.fPath.toString());
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        
        byte[] readBytes = new byte[5];
        int n = 5;
        Boolean looking = true;
        while (looking) {            
            n += 1;
            raf.seek(file.length() - n);
            raf.read(readBytes);
            
            if (decodeUTF8(readBytes).contains("<?xml")) {
                looking = false;
            }
        }
        byte[] rawXMLBytes = new byte[n];
        raf.read(rawXMLBytes);
        
        raf.close();
        this.omexml = decodeUTF8(rawXMLBytes);
    }   
    
    public String getOMEXML () {
        return this.omexml;
    }
    
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }
}

