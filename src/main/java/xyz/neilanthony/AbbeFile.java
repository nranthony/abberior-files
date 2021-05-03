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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.IntBuffer;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
import ome.units.quantity.Length;
import ome.xml.model.enums.PixelType;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.scijava.log.LogService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
    
    
    /**
     * for each image in dataset
     *      pull data
     *      resize for thumb
     *      rescale to uint8, but keep in short, for applying RGB LUTs
     *      get color and LUT
     * for each dataset
     *      overlay/blend colors into RGB for display
     *      get image info for making ImagePlus
     *      fill params and create panel
     */
    public void pullImageInfo () {
//        reader.	getChannelEmissionWavelength(int imageIndex, int channelIndex)
//        reader.getChannelFluor(int imageIndex, int channelIndex)
//        reader.	getChannelColor(int imageIndex, int channelIndex)
//        
//        	getPixelsDimensionOrder(int imageIndex)
//                	getPixelsPhysicalSizeX(int imageIndex)
//                        Y & Z
                                
    }

    /** Nested classes AbbeFolder -> AbbeDataset -> AbbeImage
     * requires the images in each dataset be predetermined before
     * creating the datasets in each folder, which requires the overlap of images in
     * dataset and images in folders to be the first step
     */
    private class AbbeFolder {
        
        public String folderName;
        public String folderID;
        public int folderIndex = -1;
        public int timeStampCounter = 0;
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
                fldrImgIndxs.add(i, Integer.valueOf(imgID.replace("Image:", ""))-1);
            }
        }
        
        public void addDataset(int datasetIndx, Integer[] imgIdxs) throws IOException, FormatException {
            this.timeStampCounter++;
            this.abbeDatasetVect.add(
                    new AbbeDataset(datasetIndx, omeMeta.getDatasetID(datasetIndx),
                                    omeMeta.getDatasetName(datasetIndx), imgIdxs,
                                    this.timeStampCounter));
        }
        
        public class AbbeDataset {
            
            public final ArrayList<AbbeImage> abbeImagesVect = new ArrayList<>();
            public String datasetName;
            public String datasetID;
            public int datasetIndex = -1;
            public boolean tiled = false;
            public int timeStampIdx;
            public int imageCount = -1;
            public Integer[] imageIndxs = null;
            public int parentFolderIndex = -1;
            public Params.PanelParams pParams = new Params.PanelParams();
            
            // constructor
            AbbeDataset(int datIndex, String datIDStr,
                    String datName, Integer[] imgIndxs, int timeStamp) throws IOException, FormatException {
                System.out.println("Contructing AbbeDataSet");
                this.datasetName = datName;
                this.datasetID = datIDStr;
                this.datasetIndex = datIndex;
                this.imageIndxs = imgIndxs;
                this.timeStampIdx = timeStamp;
                this.imageCount = imgIndxs.length;

                for (int i = 0; i < imgIndxs.length; i++) {
                    this.abbeImagesVect.add(i, new AbbeImage(imgIndxs[i]));
                }
            }
            
            private class AbbeImage {

                public String imageName;
                public String imageID;
                public int imageIndex = -1;
                public Color_Table ct = null;
                public short[] data = null;
                public short[] mask = null; // note data is uint8, but Java only has int8; storing in short[] 
                public int[] thumbData = null;
                public Params.ImageParams imgParams = new Params.ImageParams();
                String lutName = "Grays.lut"; // 

                public boolean addToComposite;  // for excluding DyMIN and RESCue from display

                public boolean tiled = false;

                // constructor
                AbbeImage(int imgIdx) throws IOException, FormatException {
                    this.imageIndex = imgIdx;
                    this.imageID = String.format("Image:%d", imgIdx);
                    this.imageName = omeMeta.getImageName(imgIdx);
                    this.imgParams.pxType = omeMeta.getPixelsType(imgIdx);
                    //if (this.imageName.contains("DyMIN") | this.imageName.contains("rescue")){
                    if (this.imgParams.pxType == PixelType.UINT8){
                        this.addToComposite = false;
                        pullMaskData(imgIdx);
                    } else {
                        this.addToComposite = true;
                        pullImageData(imgIdx);
                        resizeForThumb();
                        rescaleThumbRange();
                        setColorTable();
                    }
                }

                private void pullImageData (int imgIdx) throws IOException, FormatException {
                    reader.setSeries(imgIdx);
                    imgParams.sx = reader.getSizeX();
                    imgParams.sy = reader.getSizeY();
                    // TODO - consider z-stacks and time-lapses here
                    Object dat = reader.openPlane(0, 0, 0, imgParams.sx, imgParams.sy);
                    byte[] bytes = (byte[])dat;
                    this.data = new short[bytes.length/2];
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(this.data);
                }
                private void pullMaskData (int imgIdx) throws IOException, FormatException {
                    reader.setSeries(imgIdx);
                    imgParams.sx = reader.getSizeX();
                    imgParams.sy = reader.getSizeY();
                    // TODO - consider z-stacks and time-lapses here
                    Object dat = reader.openPlane(0, 0, 0, imgParams.sx, imgParams.sy);
                    byte[] bytes = (byte[])dat;
                    this.mask = new short[bytes.length];
                    //ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(this.mask);
                    for (int i=0;i<bytes.length;i++){
                        this.mask[i] = (short)(bytes[i]+128);
                    }
                }
                private void resizeForThumb () {
                    /*
                    retain aspect ratio and ensure both:
                    tsx <= JPanel width
                    tsy <= JPanel height
                    */
                    // thumb (and panel) x and y
                    int tsx = pParams.psx;
                    int tsy = pParams.psy;
                    // new x and y
                    int nx = tsx;
                    int ny = (int) Math.round( 1.0 * nx * imgParams.sy / imgParams.sx);
                    if (ny > tsy) {
                        ny = tsy;
                        nx = (int) Math.round( 1.0 * ny * imgParams.sx / imgParams.sy);
                    }
                    
                    RenderingHints renderingHints = new RenderingHints(null);
                    renderingHints.put(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    
                    BufferedImage origGrayBuf = new BufferedImage(
                            imgParams.sx, imgParams.sy, BufferedImage.TYPE_USHORT_GRAY);
                    origGrayBuf.getRaster().setDataElements(0, 0, imgParams.sx, imgParams.sy, this.data);
                    
                    BufferedImage imgBuf = new BufferedImage(nx, ny, origGrayBuf.getType());
                    Graphics2D g = imgBuf.createGraphics();
                    g.setRenderingHints(renderingHints);
                    g.drawImage(origGrayBuf, 0, 0, nx, ny, null);
                    g.dispose();
                    this.thumbData = new int[nx * ny];
                    imgBuf.getRaster().getPixels(0, 0, nx, ny, this.thumbData);
                    AbbeDataset.this.pParams.nx = nx;
                    AbbeDataset.this.pParams.ny = ny;
                }
                private void rescaleThumbRange () {
                    
                    int[] scaledThumb = new int[this.thumbData.length];
                    int max = 0;
                    for (int i=0; i<this.thumbData.length; i++){
                        if (this.thumbData[i] > max) { max = this.thumbData[i]; }
                    }
                    for (int i=0; i<this.thumbData.length; i++){
                        scaledThumb[i] = (int) (255 * ( this.thumbData[i] / max ));
                    }
                    this.thumbData = scaledThumb;
                }
                private void setColorTable () throws IOException {
                    Length emis = omeMeta.getChannelEmissionWavelength(this.imageIndex, 0);
                    Number lambda = emis.value();
                    short nm = (short)(lambda.doubleValue()*1e9);
                    if (nm < 454) { this.lutName = "Blue.lut"; }
                    else if (nm < 490) { this.lutName = "Cyan.lut"; }
                    else if (nm < 535) { this.lutName = "Green.lut"; }
                    else if (nm < 580) { this.lutName = "Yellow.lut"; }
                    else if (nm < 640) { this.lutName = "Orange Hot.lut"; }
                    else { this.lutName = "Red.lut"; }
                    this.ct = new Color_Table(this.lutName);
                    
                    /*        
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
                    * Cyan Hot.lut */
                }
            }
            
            private int inv (int a) {
                int b = 255 - a;
                if (b < 0) { return 0; }
                else if (b > 255) { return 255; }
                else { return b; }
            }
            // screen will need to be adapted for more than 2 channels
            private int screen (int[] a) {
                if (a.length == 1) { return a[0]; } // for single image dataset
                int na0 = inv(a[0]);
                for (int i = 1; i < a.length; i++) {
                    na0 *= inv(a[i]);
                }
                return inv(na0);
            }
            private int mean (int[] a) {
                int b = 0;
                for (int i = 0; i < a.length; i++) {
                    b += a[i];
                }
                return (int)(1.0 * b / a.length);
            }
            private void createThumb () {
                
                ArrayList<Integer> incChns = new ArrayList<>();
                ArrayList<Integer> ctSizes = new ArrayList<>();
                for (int k = 0; k < imageCount; k++) {
                    if (abbeImagesVect.get(k).addToComposite) {
                        ctSizes.add(abbeImagesVect.get(k).ct.size);
                        incChns.add(k);
                    }
                }
                
                this.pParams.bufImg = new BufferedImage(pParams.nx, pParams.ny,
                        BufferedImage.TYPE_INT_RGB);
                
                int nx = pParams.nx;
                int ny = pParams.ny;
                int nlen = nx * ny;
                int incChnLength = incChns.size();
                
                int[] r = new int[incChnLength];
                int[] g = new int[incChnLength];
                int[] b = new int[incChnLength];
                int tr, tg, tb;
                
                int[] thumbImageArr = new int[nlen];
                Color clr = null;
                int value = 0;
                
                for (int i = 0; i < nlen; i++) {
                    //  create Color(r, g, b).getRGB() for each i
                    //  r, g, b for each i is combination/blend of
                    //  each r, g, b from included image channels
                    for (int k = 0; k < incChnLength; k++) {
                        value = abbeImagesVect.get(incChns.get(k)).thumbData[i];
                        //  assumes that thumbData is normalized to max 255
                        clr = abbeImagesVect.get(incChns.get(k)).
                                ct.CT[(int)(1.0 * value * ctSizes.get(k) / 255)];
                        r[k] = clr.getRed();
                        g[k] = clr.getGreen();
                        b[k] = clr.getBlue();
                    }
                    tr = mean(r);
                    tg = mean(g);
                    tb = mean(b);
                    thumbImageArr[i] = new Color(tr,tg,tb).getRGB();
                }
                this.pParams.bufImg.setRGB(0,0,nx,ny,thumbImageArr, 0, nx);
            }
        }
    }

    // AbbeFile Constructor
    AbbeFile(Path filePath) throws FormatException, IOException, ParserConfigurationException, SAXException {
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
     * Scans AbbeFile omeMeta for all Datasets and creates Folders.
     * Datasets are stored in datImgLsts variable in AbbeFile class
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
                datImgLsts[i].add(j, Integer.valueOf(imgID.replace("Image:", ""))-1);
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
    
    public void collateFolderImages() throws IOException, FormatException {
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
                        System.out.println(String.format("Creating ds%d", ds));
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
        factory.setNamespaceAware(true);
        builder = factory.newDocumentBuilder();
        xmlDoc = builder.parse(new InputSource(new StringReader(omexml)));
        NodeList imageNodeList = xmlDoc.getElementsByTagName("Image");
        Node imgNode;
        for (int n=0; n<imageNodeList.getLength(); n++) {
            imgNode = imageNodeList.item(n);
            System.out.println(imgNode.toString());
        }
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
        
        this.omexml = decodeUTF8(xmlBytes).replace("&quot;", "\"");
        // above could be adjsted to not capture non-UTF-8 chars; below removes from start and end
        this.omexml = this.omexml.trim().replaceFirst("^([\\W]+)<","<");
        this.omexml = this.omexml.replaceFirst(">([\\W]+)$", ">");
        channel.close();
    }
    
    /* little functions */
    
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
    * @author alex.vergara
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
    
    public JPanel useColorTable () throws IOException {
        Color_Table ct = new Color_Table("Orange Hot.lut");
        
        int sx, sy;
        sx = 255;
        sy = 255;
        JPanel jPanel_New = new JPanel();
        jPanel_New.setBounds(0, 0, sx, sy);
        
        BufferedImage bufImg = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
        
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

