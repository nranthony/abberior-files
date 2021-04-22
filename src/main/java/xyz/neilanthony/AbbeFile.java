/*
Used to encapsulate all the information required for each Abberior file thats imported

*/
package xyz.neilanthony;

import io.scif.gui.BufferedImageReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

import java.nio.file.Path;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.MetadataTools;
import loci.formats.in.OBFReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author nelly
 */
public class AbbeFile {
    
    private OBFReader reader = new OBFReader();
    private IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
    
    private String omexml = null;
    private Document xmlDoc = null;
    private int index = -1;
    private Path fPath = null;
    
    public final Vector<AbbeFolder> abbeFolderVect = new Vector<>();
    // cast to sychronized (abbeFolderVect) when multithreading
    private String[] folderNames = null;
    
    private class AbbeFolder {
        
        public String folderName;
        public String folderID;
        public int folderIndex = -1;
        // TODO - add subfolder count and list; add roi count and list
        private final Vector<AbbeDataset> abbeDatasetVect = new Vector<>();
        private final Vector<AbbeImage> abbeImagesVect = new Vector<>();
        
        AbbeFolder(int fldrIndex, String fldrIDStr, String fldrName) {
            // constructor
            this.folderName = fldrName;
            this.folderID = fldrIDStr;
            this.folderIndex = fldrIndex;
            
            int imgCount = omeMeta.getFolderImageRefCount(fldrIndex);
            for (int i = 0; i < imgCount; i++) {
                abbeImagesVect.add(i, new AbbeImage(omeMeta.getFolderImageRef(fldrIndex, i)));
                
            }
        }
    }
    
    private class AbbeDataset {

        public String datasetName;
        public String datasetID;
        public int datasetIndex = -1;
        
        public boolean tiled = false;
        public int imageCount = -1;
        public int[] imgIndxs = null;
        
        AbbeDataset(int datIndex, String datIDStr, String datName) {
            // constructor
            this.datasetName = datName;
            this.datasetID = datIDStr;
            this.datasetIndex = datIndex;
            
            int imgCount = omeMeta.getDatasetImageRefCount(datIndex);
            imgIndxs = new int[imgCount];
            for (int i = 0; i < imgCount; i++) {
                imgIndxs[i] = Integer.valueOf(omeMeta
                                              .getDatasetImageRef(datIndex, i)
                                              .replace("Image:", "")
                                              );
            }
        }
    }
    
    private class AbbeImage {
        
        public String imageName;
        public String imageID;
        public int imageIndex = -1;
        
        public boolean tiled = false;

        AbbeImage(String imgIDStr) {
            // constructor
            this.imageID = imgIDStr;
            this.imageIndex = Integer.valueOf(imgIDStr.replace("Image:", ""));
        }
    }
    
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = null;
    
    // AbbeFile Constructor
    AbbeFile(Path filePath) throws FormatException, IOException {
        this.fPath = filePath;
        //  start waiting thread
        reader.setMetadataStore(omeMeta);
        reader.setId(this.fPath.toString());
        // end waiting thread
        
    }

    public void setIndex (int index) {
        this.index = index;
    }

    public void updatePath (Path filePath) {
        fPath = filePath;
    }
    
    public String getOMEXML () throws IOException {
        if ( this.omexml == null ) {
            this.pullOMEXMLRaw();
        }
        return this.omexml;
    }
    
    public void createXMLDoc () throws ParserConfigurationException, SAXException, IOException {
        if ( this.omexml == null ) {
            this.pullOMEXMLRaw();
        }
        builder = factory.newDocumentBuilder();
        xmlDoc = builder.parse(new InputSource(new StringReader(omexml)));
    }
    
    public String testXMLDoc () {
        return this.xmlDoc.getDocumentElement().getNodeName();
    }
    
    public String[] getOMEFolderNames () throws FormatException, IOException {
        if ( this.folderNames == null ) {
            this.pullOMEFolders();
        }
        return this.folderNames;
    }
    
    public void pullOMEFolders() throws FormatException, IOException {
        int folderCount = omeMeta.getFolderCount();
        this.folderNames = new String[folderCount];
        for (int i = 0; i < folderCount; i++) {
            this.folderNames[i] = omeMeta.getFolderName(i);
            this.abbeFolderVect.add(i, new AbbeFolder(i, omeMeta.getFolderID(i), 
                                                      this.folderNames[i])
                                    );
        }
    }
    
    public void pullOMEDatasets() {
        int datasetCount = omeMeta.getDatasetCount();
        for (int i = 0; i < datasetCount; i++) {
             // TODO - either add Project list to AbbeFile class
             // or
             // add all Datasets vect to AbbeFile class
             // question of all datasets vs datasets in each folder
             // NEED to get images in each dataset and cross referecne
             // with images in folders
             // ...
//             .add(i, new AbbeFolder(i, omeMeta.getFolderID(i), 
//                                                      this.folderNames[i])
//                                    );
    }
    
    public void pullOMEXMLRaw() throws FileNotFoundException, IOException {
        
        // get memory mapped buffer of last ~2MB of obf file
        // TODO - check location of xml within .msr files
        final FileChannel channel = new FileInputStream(this.fPath.toString()).getChannel();
        long mapSize;
        long fileSize;
        fileSize = channel.size();
        mapSize = (long) Math.min(fileSize, Math.pow(2, 21)); // 2,097,152   ~2MB
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, fileSize - mapSize, mapSize);
        
        // create integer from xml 4 chars at start
        byte[] xmlStartBytes = "<?xm".getBytes(Charset.forName("UTF-8"));
        ByteBuffer xmlStartByteBuf = ByteBuffer.wrap(xmlStartBytes);
        int xmlStartInt = xmlStartByteBuf.getInt();

        //  scan through int sized chunks of buffer and compare to xmlStartByteBuf
        int offset = -1; int compInt = 0; int xorInt = 1;
        for (int i = 0; i < mapSize - 8; i++) {
            compInt = buffer.getInt(i);
            xorInt = compInt ^ xmlStartInt;
            if (xorInt == 0) { // XOR bitwise operator equals zero if all bits the same
                offset = i;
                break;
            }
        }
        if (offset == -1) {
            this.omexml = "";
            channel.close();
            return;
        }
        
        int readLength = (int)(mapSize - offset);
        byte[] xmlBytes = new byte[readLength];
        buffer.position(offset-1);
        buffer.get(xmlBytes);
        
        this.omexml = decodeUTF8(xmlBytes).replace("&quot;", "\"");;
        channel.close();
    }
        
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }
}

