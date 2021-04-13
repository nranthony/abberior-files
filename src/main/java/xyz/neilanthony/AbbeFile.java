/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import java.io.IOException;
import loci.formats.ClassList;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.in.OBFReader;
import loci.formats.in.OMEXMLReader;

import java.nio.file.Path;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.meta.IMetadata;
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

    public void pullOMEXML () throws DependencyException, ServiceException {
        //final File file = new File(fPath.toString());
        
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata omexml = service.createOMEXMLMetadata();
        
        ClassList<IFormatReader> cl = new ClassList<>(IFormatReader.class);
        //cl.addClass(OBFReader.class);
        cl.addClass(OMEXMLReader.class);
        ImageReader reader = new ImageReader(cl);
        
        //reader.setMetadataStore(omexml);
        reader.setId(fPath.toString());
        reader.getMetadataStore()
        
        try {
            reader.setId();
            reader.
        } catch (FormatException | IOException e) {
                throw new RuntimeException(e);
        }
    }
    
    public void getOMEXML () {

    }
    
    
}

