/*
Used to encapsulate all the information required for each Abberior file thats imported

*/
package xyz.neilanthony;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import loci.formats.FormatException;

import java.nio.file.Path;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import loci.formats.MetadataTools;
import loci.formats.in.OBFReader;
import loci.formats.meta.IMetadata;
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

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder builder = null;
    
    private String omexml = null;
    private Document xmlDoc = null;
    private int index = -1;
    private Path fPath = null;
    
    public final ArrayList<AbbeFolder> abbeFolderVect = new ArrayList<>();
    // cast to sychronized (abbeFolderVect) when multithreading
    
    private String[] folderNames = null;
    /* array of associated indxs
    List<imageIndx>[datasetIndx]
    An array of Lists, where each list contains the imageIdxs in that dataset
    usage:
    for each folder:
        for each dataset:
            if dataset image is in folder image list
                add dataset to folder
    */
    private ArrayList<Integer>[] datImgLsts;
    
    // requires the images in each dataset be predetermined before
    // creating the datasets in each folder, which requires the overlap of images in
    // dataset and images in folders to be the first step
    private class AbbeFolder {
        
        public String folderName;
        public String folderID;
        public int folderIndex = -1;
        // TODO - add subfolder count and list; add roi count and list
        
        public final ArrayList<AbbeDataset> abbeDatasetVect = new ArrayList<>();
        public final ArrayList<Integer> fldrImgIndxs = new ArrayList<>();
        
        // constructor
        AbbeFolder(int fldrIndex, String fldrIDStr, String fldrName) {
            this.folderName = fldrName;
            this.folderID = fldrIDStr;
            this.folderIndex = fldrIndex;
            //  get the list of image indicies in the folder
            String imgID;
            int imgCount = omeMeta.getFolderImageRefCount(fldrIndex);
            for (int i = 0; i < imgCount; i++) {
                imgID = omeMeta.getFolderImageRef(fldrIndex, i);
                fldrImgIndxs.add(i, Integer.valueOf(imgID.replace("Image:", "")));
            }
        }
        
        public void pullOMEDatasets(int[] datasetIndxs, int[] imgIdxs) {
            for (int i = 0; i < datasetIndxs.length; i++) {
                this.abbeDatasetVect.add(i, new AbbeDataset(datasetIndxs[i], 
                                                            omeMeta.getDatasetID(i),
                                                            omeMeta.getDatasetName(i),
                                                            imgIdxs)
                                        );
            }
        }
        
        public class AbbeDataset {
            
            public final ArrayList<AbbeImage> abbeImagesVect = new ArrayList<>();
            
            public String datasetName;
            public String datasetID;
            public int datasetIndex = -1;

            public boolean tiled = false;
            public int imageCount = -1;
            public int[] imageIndxs = null;

            public int parentFolderIndex = -1;
            
            // constructor
            AbbeDataset(int datIndex, String datIDStr, String datName, int[] imgIndxs) {
                this.datasetName = datName;
                this.datasetID = datIDStr;
                this.datasetIndex = datIndex;
                this.imageIndxs = imgIndxs;

                for (int i = 0; i < imgIndxs.length; i++) {
                    this.abbeImagesVect.add(i, new AbbeImage(imgIndxs[i]));
                }
            }
            
            private class AbbeImage {
        
                public String imageName;
                public String imageID;
                public int imageIndex = -1;

                public boolean tiled = false;
                
                // constructors
//                AbbeImage(String imgIDStr) {
//                    this.imageID = imgIDStr;
//                    this.imageIndex = Integer.valueOf(imgIDStr.replace("Image:", ""));
//                }
                AbbeImage(int imgIdx) {
                    this.imageIndex = imgIdx;
                    this.imageID = String.format("Image:%d", imgIdx);
                }
                
            }
        }
        
        
    }    

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

     /**
     * Scans AbbeFile omeMeta for all Datasets and create Folders.
     * Datasets are stored in datImgLsts, variable in AbbeFile
     * Create AbbeFolders with basic information, which then populates it's
     * variable fldrImgIndxs
     * <p>
     * After calling, fldrImgIndxs and each List in dataImgLsts are compared
     * to determine which dataset groups of images are in each folder
     * 
     * @throws FormatException
     * @throws IOException 
     */
    public void scanFoldersDatasets() throws FormatException, IOException {
        // fill datImgLsts with image indicies for all datasets
        int datasetCount = omeMeta.getDatasetCount();
        int imgCount = -1;
        String imgID;
        datImgLsts = new ArrayList[datasetCount];
        for (int i = 0; i < datasetCount; i++) {  //  for each dataset
            imgCount = omeMeta.getDatasetImageRefCount(i);
            for (int j = 0; j < imgCount; j++) {  //  for each image in each dataset
                imgID = omeMeta.getDatasetImageRef(i, j);
                datImgLsts[i].add(j, Integer.valueOf(imgID.replace("Image:", "")));
            }
        }
        // create folder objects
        int folderCount = omeMeta.getFolderCount();
        this.folderNames = new String[folderCount];
        for (int i = 0; i < folderCount; i++) {
            this.folderNames[i] = omeMeta.getFolderName(i);
            this.abbeFolderVect.add(i, new AbbeFolder(i, omeMeta.getFolderID(i), 
                                                      this.folderNames[i])
                                    );
        }
    }
    
    public void collateFolderImages() {
        /*
        private ArrayList<Integer>[] datImgLsts;
        In AbbeFolder:
        public final ArrayList<Integer> fldrImgIndxs = new ArrayList<>();
        
        for each folder:
        for each dataset:
            if dataset image is in folder image list
                add dataset to folder
        */
        
        for (AbbeFolder abF : abbeFolderVect ) {
            
        }
    }
    
    /**
     * Used for working directly from raw xml data.
     * May be needed to get finer details not available in omeMeta
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public void createXMLDoc () throws ParserConfigurationException, SAXException, IOException {
        if ( this.omexml == null ) {
            this.pullOMEXMLRaw();
        }
        builder = factory.newDocumentBuilder();
        xmlDoc = builder.parse(new InputSource(new StringReader(omexml)));
    }
        
    public String[] getFolderNames () throws FormatException, IOException {
        if ( this.folderNames == null ) {
            this.scanFoldersDatasets();
        }
        return this.folderNames;
    }
    

    public String getOMEXML () throws IOException {
        if ( this.omexml == null ) {
            this.pullOMEXMLRaw();
        }
        return this.omexml;
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

    
    
    /* little functions */
    
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }
    
    /* old functions - review for deletion */
    
    public void updatePath (Path filePath) {
        fPath = filePath;
    }    
}

