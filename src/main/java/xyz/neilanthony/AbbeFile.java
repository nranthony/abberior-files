/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import io.scif.gui.BufferedImageReader;
import java.io.IOException;
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
    
    public String getOMEXML () {
        return this.omexml;
    }
    
    
}

