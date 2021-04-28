/*
Used to encapsulate all the information required for each Abberior file thats imported

*/
package xyz.neilanthony;

import io.scif.formats.ImageIOFormat;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import loci.formats.FormatException;
import java.awt.Color;
import java.io.IOException;
import net.imagej.lut.DefaultLUTService;
import net.imagej.lut.LUTService;
import net.imglib2.display.ColorTable;
import org.scijava.plugin.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import loci.formats.MetadataTools;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.OBFReader;
import loci.formats.meta.IMetadata;
import net.imagej.ImageJ;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.scijava.log.LogService;
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
    private BufferedImageReader bufImgReader= new BufferedImageReader(reader);

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder builder = null;
    
    private String omexml = null;
    private Document xmlDoc = null;
    private int index = -1;
    private Path fPath = null;
    
    public final ArrayList<AbbeFolder> abbeFolderVect = new ArrayList<>();
    // cast to sychronized (abbeFolderVect) when multithreading
    
    private String[] folderNames = null;
    /* array of associated indxs  -  List<imageIndx>[datasetIndx]
    An array of Lists, where each list contains the imageIdxs in that dataset */
    private ArrayList<Integer>[] datImgLsts;
    

    /** Nested classes AbbeFolder -> AbbeDataset -> AbbeImage
     * requires the images in each dataset be predetermined before
     * creating the datasets in each folder, which requires the overlap of images in
     * dataset and images in folders to be the first step
     */
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
        
        public void addDataset(int datasetIndx, Integer[] imgIdxs) {
            this.abbeDatasetVect.add(
                    new AbbeDataset(datasetIndx, omeMeta.getDatasetID(datasetIndx),
                                    omeMeta.getDatasetName(datasetIndx), imgIdxs));
        }
        
        public class AbbeDataset {
            
            public final ArrayList<AbbeImage> abbeImagesVect = new ArrayList<>();
            
            public String datasetName;
            public String datasetID;
            public int datasetIndex = -1;

            public boolean tiled = false;
            public int imageCount = -1;
            public Integer[] imageIndxs = null;

            public int parentFolderIndex = -1;
            
            // constructor
            AbbeDataset(int datIndex, String datIDStr, String datName, Integer[] imgIndxs) {
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
//        System.out.println(omeMeta.getDatasetID(0));
//        System.out.println(omeMeta.getImageName(0));
//        System.out.println(omeMeta.getFolderName(0));
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
            datImgLsts[i] = new ArrayList<>();
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
        //Set<Integer> datasetImages = null;
        Set<Integer> folderImages = new HashSet<>();
        
        for (AbbeFolder abF : abbeFolderVect ) {
            // check that all the dataset images are in the folder images
            // if not, then one or both lists are incorrect or dataset corrupt
            // if all dataset images in folder, create new dataset in folder
            folderImages.clear();
            if (abF.fldrImgIndxs.size() > 0) {
                folderImages.addAll(abF.fldrImgIndxs);
                for (int ds = 0; ds < datImgLsts.length; ds++) {
                    Integer[] imgIdxs = new Integer[datImgLsts[ds].size()];
                    //  all the images in dataset ds are in the current folder
                    if (folderImages.containsAll(datImgLsts[ds])) {
                        imgIdxs = datImgLsts[ds].toArray(imgIdxs);
                        abF.addDataset(ds, imgIdxs);
                    }
                }
            }
            
            
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

    /** Testing Functions
    * Includes testing of:
    * local AbbeFile - AbbeFolder - AbbeDataset - AbbeImage
    * OME Bioformats readers and the such
    */
    
    public List<String> printFileDetails () {
        
        List<String> strList = new ArrayList<>();
        strList.add(String.format("Check AbbeFile: %s", this.toString()));
        
        for (AbbeFolder abF : abbeFolderVect) {
            for (AbbeFolder.AbbeDataset abDs : abF.abbeDatasetVect) {
                for (AbbeFolder.AbbeDataset.AbbeImage abImg : abDs.abbeImagesVect) {
                    
                    strList.add(String.format("Folder %s; Dataset %s; Image %s",
                                                                abF.folderName,
                                                                abDs.datasetName,
                                                                abImg.imageName));
                }
            }
        }
        return strList;
    }
    
    public List<String> printSeriesInfo () {
        
        List<String> strList = new ArrayList<>();
        strList.add(String.format("Checking reader.series info."));
        int seriesCount = reader.getSeriesCount();
        for (int i=0; i<seriesCount; i++) {
            reader.setSeries(i);
            strList.add(String.format("pixels (x,y): (%s,%s)",
                        reader.getSizeX(), reader.getSizeY())
                        );
        }
        //Hashtable seriesHash = reader.getSeriesMetadata();
        //strList.add(seriesHash.toString());
        return strList;
    }
    
    public BufferedImage getThumbBufImg (int s) throws IOException, FormatException {
        
        reader.setSeries(s);
//        ByteArrayInputStream bis = new ByteArrayInputStream(thumbBytes);
//        BufferedImage bImage = ImageIO.read(bis);
        BufferedImage bImage = bufImgReader.openThumbImage(s);
        
        return bImage;
        
    }
    
    public short[] getScaledShorts (int s, int z) throws FormatException, IOException {
        reader.setSeries(s);
        int sc, st, sx, sy, sz;
        sc = reader.getSizeC();
        st = reader.getSizeT();
        sx = reader.getSizeX();
        sy = reader.getSizeY();
        sz = reader.getSizeZ();

        Object data = reader.openPlane(0, 0, 0, sx, sy);
        byte[] bytes = (byte[])data;
        short[] shorts = new short[bytes.length/2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        
        short max = 0;
        for (int i=0; i<shorts.length; i++){
            if (shorts[i] > max) { max = shorts[i]; }
        }
        for (int i=0; i<shorts.length; i++){
            shorts[i] = (short) (Short.MAX_VALUE * ( shorts[i] / max ));
        }
        
        return shorts;
    }
    
    public Mat GetCVMat (int s, int z) throws FormatException, IOException {
        int sx, sy;
        sx = reader.getSizeX();
        sy = reader.getSizeY();      
        Mat m = new Mat(sx,sy, CvType.CV_16UC1);
        m.put(0,0,getScaledShorts(s, z));
        return m;
//        openBytes(int no, byte[] buf)
//Obtains the specified image plane from the current file into a pre-allocated byte array of (sizeX * sizeY * bytesPerPixel * RGB channel count).
    }
    
    public BufferedImage getBufImg (int s, int z) throws FormatException, IOException {
        int sx, sy;
        sx = reader.getSizeX();
        sy = reader.getSizeY();  
        short[] imgShort = getScaledShorts(s, z);
        //ShortArrayInputStream in = new ShortArrayInputStream(imgShort);
        BufferedImage buf = new BufferedImage(sx, sy, BufferedImage.TYPE_USHORT_GRAY);
//        ShortBuffer sb = ShortBuffer.allocate(sx*sy);
//        sb.put(imgShort);
        DataBuffer sb = new DataBufferShort(imgShort, (sx*sy));

        int[] bos = new int[1];
        bos[0] = 0;
        Point pnt = new Point();
        Raster ras = Raster.createInterleavedRaster(sb,sx,sy,sx,1,bos,pnt);
        buf.setData(ras);
//        ShortBuffer sb = null;
//        sb.put(imgShort);
//        BufferedImage buf = new ImageIO.read(sb);
        
        return buf;
    }
    
    public BufferedImage getFirstByteBuf (int s, int z) throws FormatException, IOException {
        reader.setSeries(s);
        int sx, sy;
        sx = reader.getSizeX();
        sy = reader.getSizeY();
        byte[] imgBytes = new byte[sx*sy*2];
        imgBytes = reader.openBytes(z, imgBytes);
        
        byte[] firstBytes = new byte[sx*sy];
        for (int i=0; i<firstBytes.length; i++) {
            firstBytes[i] = imgBytes[i << 1];
        }
        BufferedImage buf = new BufferedImage(sx, sy, BufferedImage.TYPE_BYTE_GRAY);
        return buf;
    }
    
    public ArrayImg getArrayImg (int s) throws FormatException, IOException {
        reader.setSeries(s);
        Object data = reader.openPlane(0, 0, 0, reader.getSizeX(), reader.getSizeY());
        byte[] bytes = (byte[])data;
        short[] shorts = new short[bytes.length/2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        ArrayImg<UnsignedShortType, ShortArray> img = ArrayImgs.unsignedShorts(shorts, reader.getSizeX(), reader.getSizeY());
        //this.ij.ui().show(img);
        return img;
    }
    
    public BufferedImage getUShortGrayBuf (int s) throws FormatException, IOException {
        reader.setSeries(s);
        int sx, sy;
        sx = reader.getSizeX();
        sy = reader.getSizeY(); 
        Object data = reader.openPlane(0, 0, 0, sx, sy);
        byte[] bytes = (byte[])data;
        short[] shorts = new short[bytes.length/2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        BufferedImage convertedGrayscale = new BufferedImage(sx, sy, BufferedImage.TYPE_USHORT_GRAY);
        convertedGrayscale.getRaster().setDataElements(0, 0, sx, sy, shorts);
        return convertedGrayscale;
    }
    
    
    /* little functions */
    
    /**
    * @author alex.vergara
    */
    /** options to think about using
    * Yellow.lut
    * HiLo.lut
    * Cyan.lut
    * Grays.lut
    * Green.lut
    * Yellow Hot.lut
    * Red Hot.lut
    * Magenta Hot.lut
    * Ice.lut
    * Red.lut
    * Orange Hot.lut
    * Fire.lut
    * Blue.lut
    * Magenta.lut
    * Green Fire Blue.lut
    * Cyan Hot.lut
    */
    public class Color_Table {

        private final Color[] CT;
        private final int size;

        @Parameter
        private final LUTService ls = new DefaultLUTService();

        public Color_Table(String colormap) throws IOException {
            ColorTable ct = ls.loadLUT(ls.findLUTs().get(colormap));
            size = ct.getLength();
            CT = new Color[size];
            for (int i = 0; i < size; i++) {
                CT[i] = new Color(ct.get(ColorTable.RED, i), ct.get(ColorTable.GREEN, i), ct.get(ColorTable.BLUE, i));
            }
        }

        public int getSize() {
            return size;
        }

        public Color getColor(int index){
            return CT[index];
        }


        
        
    }
    
    public JPanel getColorTable () throws IOException {
        Color_Table ct = new Color_Table("Orange Hot.lut");
        Color myColor = null;
        
        int sx, sy;
        sx = 255;
        sy = 255;
        JPanel jPanel_New = new JPanel();
        jPanel_New.setBounds(0, 0, sx, sy);
        
        BufferedImage bufImg = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
        
//        for (int i = 0; i < ct.getSize(); i++) {
//            myColor = ct.getColor(i);
//            
//        }
        
        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                bufImg.setRGB(i, j, ct.getColor(i).getRGB());
            }
        }
        
        ImageIcon icon = new ImageIcon(bufImg);
        JLabel picLabel = new JLabel(icon);
        jPanel_New.add(picLabel);
        return jPanel_New;
    }
    
    
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }
    
    /* old functions - review for deletion */
    
    public void updatePath (Path filePath) {
        fPath = filePath;
    }    
}

