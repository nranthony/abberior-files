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
import loci.formats.in.ImprovisionTiffReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

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
        //final File file = new File(fPath.toString());
        
          /** Configuration tree structure containing dataset metadata. */
          //made for testing by ome
        //public static ConfigurationTree configTree;
        
//        ServiceFactory factory = new ServiceFactory();
//        OMEXMLService service = factory.getInstance(OMEXMLService.class);
//        IMetadata omexml = service.createOMEXMLMetadata();
//        
//        ClassList<IFormatReader> cl = new ClassList<>(IFormatReader.class);
//        cl.addClass(OBFReader.class);
//        //cl.addClass(OMEXMLReader.class);
//        ImageReader reader = new ImageReader(cl);
//        
//        ImporterOptions impOpts = new ImporterOptions();
//        ImportProcess impProc = new ImportProcess(impOpts);
        
        
        // initialize configuration tree
        if (config == null) {
            try {
                synchronized (configTree) {
                config = configTree.get(id);
            }
        }
            catch (IOException e) { }
        }

        if (reader == null) {
            setupReader();
        }
        
        
        MetadataStore store = null;
        store = omexmlService.createOMEXMLMetadata();
        
        MetadataStore store = reader.getMetadataStore();
        MetadataRetrieve retrieve = omexmlService.asRetrieve(store);
        String xml = omexmlService.getOMEXML(retrieve);
        
        //reader.setMetadataStore(omexml);
//        reader.setId(fPath.toString());
//        Object metaObj = reader.getMetadataStoreRoot();
        //reader.getMetadataStore()
        
//        try {
//            reader.setId();
//            reader.
//        } catch (FormatException | IOException e) {
//                throw new RuntimeException(e);
//        }
    }
    
    public void getOMEXML () {

    }
    
    
}

