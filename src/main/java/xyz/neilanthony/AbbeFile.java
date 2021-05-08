/*
Used to encapsulate all the information required for each Abberior file thats imported

*/
package xyz.neilanthony;

import static com.google.common.primitives.Shorts.max;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import loci.formats.FormatException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import net.imagej.lut.DefaultLUTService;
import net.imagej.lut.LUTService;
import net.imglib2.display.ColorTable;
import org.scijava.plugin.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
import ome.units.quantity.Length;
import ome.xml.model.enums.PixelType;
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
       
    public JPanel abbeDatasetPanels = null;
    public int panelCount = 0;
    
    private OBFReader reader = new OBFReader();
    private IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
    private BufferedImageReader bufImgReader= new BufferedImageReader(reader);

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder builder = null;
    
    private String omexml = null;
    private Document xmlDoc = null;
    private Path fPath = null;
    public Params.FileParams fParams = new Params.FileParams();
    
    public final ArrayList<AbbeFolder> abbeFolderVect = new ArrayList<>();
    // cast to sychronized (abbeFolderVect) when multithreading
    
    private String[] folderNames = null;
    /* array of associated indxs  -  List<imageIndx>[datasetIndx]
    An array of Lists, where each list contains the imageIdxs in that dataset */
    private ArrayList<Integer>[] datImgLsts;
    
    // AbbeFile Constructor
    AbbeFile(Path filePath, int abbeFileVectIndex) throws FormatException, IOException, ParserConfigurationException, SAXException {
        this.fPath = filePath;
        this.fParams.labelsUsed = new HashMap<Integer,String>();
        this.fParams.fileName = filePath.getFileName().toString();
        this.fParams.abbeFilesVectIndex = abbeFileVectIndex;
        //  start waiting thread
        reader.setMetadataStore(omeMeta);
        reader.setId(this.fPath.toString());
        // end waiting thread
    }

    

    /** Nested classes AbbeFolder -> AbbeDataset -> AbbeImage
     * requires the images in each dataset be predetermined before
     * creating the datasets in each folder, which requires the overlap of images in
     * dataset and images in folders to be the first step
     */
    public class AbbeFolder {
        
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
            ArrayList<Integer> incChns = null;
            public String datasetName;
            public String datasetID;
            public int datasetIndex = -1;
            public boolean tiled = false;
            public int timeStampIdx;
            public int imageCount = -1;
            public Integer[] imageIndxs = null;
            public int parentFolderIndex = -1;
            public Params.PanelParams pParams = new Params.PanelParams();
            public boolean addToPanel = true;
            
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
                
                checkIncludedChannels();
                // below require incChns to be detemined in checkIncludedChannels();
                fillParams();
                createThumbEtc();
                checkImgComplete();
            }
            
            public class AbbeImage {

                public String imageName;
                public String imageID;
                public int imageIndex = -1;
                public short[] data = null;
                public short[] mask = null; // note data is uint8, but Java only has int8; storing in short[] 
                //public int[] thumbData = null;
                public short[] shortThumbData = null;
                public Params.ImageParams imgParams = new Params.ImageParams();
                public boolean addToComposite;  // for excluding DyMIN and RESCue from display
                public boolean tiled = false;
                private short minimumMax = 35; // to stop channels of mostly noise 'swamping' thumbnail
                private short offset = 2; // chops the lower values to remove a little noise
                
                @Parameter
                private final LUTService ls = new DefaultLUTService();
                String lutName = "Grays.lut"; // 
                private Color[] colorTable;
                private int ctSize;

                // constructor
                AbbeImage(int imgIdx) throws IOException, FormatException {
                    this.imageIndex = imgIdx;
                    this.imageID = String.format("Image:%d", imgIdx);
                    this.imageName = omeMeta.getImageName(imgIdx);
                    this.imgParams.pxType = omeMeta.getPixelsType(imgIdx);
                    this.imgParams.chnName = omeMeta.getChannelName(imgIdx, 0);
                    if (this.imageName.contains("DyMIN") | this.imageName.contains("rescue")){
                    //if (this.imgParams.pxType == PixelType.UINT8){
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

                public void pullImageData (int imgIdx) throws IOException, FormatException {
                    reader.setSeries(imgIdx);
                    imgParams.sx = reader.getSizeX();
                    imgParams.sy = reader.getSizeY();
                    imgParams.sz = reader.getSizeZ();
                    imgParams.st = reader.getSizeT();
                    
                    // TODO - consider z-stacks and time-lapses here
                    int zPlane = ( (imgParams.sz + 1) / 2 ) - 1;
                    Object dat = reader.openPlane(zPlane, 0, 0, imgParams.sx, imgParams.sy);
                    byte[] bytes = (byte[])dat;
                    this.data = new short[bytes.length/2];
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(this.data);
                }
                public void pullMaskData (int imgIdx) throws IOException, FormatException {
                    reader.setSeries(imgIdx);
                    imgParams.sx = reader.getSizeX();
                    imgParams.sy = reader.getSizeY();
                    imgParams.sz = reader.getSizeZ();
                    imgParams.st = reader.getSizeT();
                    // TODO - consider z-stacks and time-lapses here
                    Object dat = reader.openPlane(0, 0, 0, imgParams.sx, imgParams.sy);
                    byte[] bytes = (byte[])dat;
                    if (reader.getBitsPerPixel()==1) { // unsigned 8 bit data
                        this.mask = new short[bytes.length];
                        for (int i=0;i<bytes.length;i++){
                            this.mask[i] = (short)(bytes[i]+128);
                        }
                    } else { // signed 16 bit data
                        this.mask = new short[bytes.length/2];
                        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(this.mask);
                    }
                    
                }
                public void resizeForThumb () {
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
                    
                    BufferedImage imgBuf = new BufferedImage(nx, ny, BufferedImage.TYPE_USHORT_GRAY);
                    Graphics2D g = imgBuf.createGraphics();
                    g.setRenderingHints(renderingHints);
                    g.drawImage(origGrayBuf, 0, 0, nx, ny, null);
                    g.dispose();
                    this.shortThumbData = new short[nx * ny];
//                    this.thumbData = new int[nx * ny];
                    imgBuf.getRaster().getDataElements(0, 0, nx, ny, this.shortThumbData);
                    //imgBuf.getRaster().getPixels(0, 0, nx, ny, this.thumbData);
//                    for (int i = 0; i < (nx * ny); i++ ) {
//                        this.thumbData[i] = (int)this.shortThumbData[i];
//                    }
                    AbbeDataset.this.pParams.nx = nx;
                    AbbeDataset.this.pParams.ny = ny;
                }
                public void rescaleThumbRange () {
                    
                    short[] scaledThumb = new short[this.shortThumbData.length];
                    short max = minimumMax;
                    short value = 0;
                    for (int i=0; i<this.shortThumbData.length; i++){
                        if (this.shortThumbData[i] > max) { max = this.shortThumbData[i]; }
                    }
                    for (int i=0; i<this.shortThumbData.length; i++){
                        value = (short) ((this.shortThumbData[i] - offset < 0) ? 0 : this.shortThumbData[i] - offset);
                        scaledThumb[i] = (short) (255.0 * value / max );
                    }
                    this.shortThumbData = scaledThumb;
                }
                public void setColorTable () throws IOException {
                    Length emis = omeMeta.getChannelEmissionWavelength(this.imageIndex, 0);
                    Number lambda = emis.value();
                    short nm = (short)(lambda.doubleValue()*1e9);
                    this.imgParams.emissionLambda = nm;
                    fParams.labelsUsed.put((int)nm, omeMeta.getChannelFluor(this.imageIndex, 0));
                    if (nm < 454) { this.lutName = "Blue.lut"; }
                    else if (nm < 490) { this.lutName = "Cyan.lut"; }
                    else if (nm < 535) { this.lutName = "Green.lut"; }
                    else if (nm < 580) { this.lutName = "Yellow.lut"; }
                    else if (nm < 640) { this.lutName = "Orange Hot.lut"; }
                    else { this.lutName = "Red.lut"; }
                    getColorTable(this.lutName);
                    
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
                public void getColorTable(String colormap) throws IOException {
                    String lutPath = "/luts/" + colormap;
                    InputStream lutStream = getClass().getResourceAsStream(lutPath);
                    ColorTable ct = ls.loadLUT(lutStream);
                    //ColorTable ct = ls.loadLUT(ls.findLUTs().get(colormap));
                    this.ctSize = ct.getLength();
                    this.colorTable = new Color[this.ctSize];
                    for (int i = 0; i < this.ctSize; i++) {
                        this.colorTable[i] = new Color(ct.get(ColorTable.RED, i), ct.get(ColorTable.GREEN, i), ct.get(ColorTable.BLUE, i));
                    }
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
            private int sum (int[] a) {
                int b = 0;
                for (int i = 0; i < a.length; i++) {
                    b += a[i];
                }
                if (b < 256) { return b; }
                else { return (int) 255; }
            }
            private void checkIncludedChannels () {
                this.incChns = new ArrayList<>();
                for (int k = 0; k < imageCount; k++) {
                    if (abbeImagesVect.get(k).addToComposite) {
                        this.incChns.add(k);
                    }
                }
            }
            private void createThumbEtc () {
                
                this.pParams.bufImg = new BufferedImage(pParams.nx, pParams.ny,
                        BufferedImage.TYPE_INT_RGB);
                
                int nx = pParams.nx;
                int ny = pParams.ny;
                int nlen = nx * ny;
                int incChnLength = this.incChns.size();
                
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
                        value = abbeImagesVect.get(this.incChns.get(k)).shortThumbData[i];
                        //  assumes that shortThumbData is normalized to max 255
                        //  also assumes that color table has 255 elements
                        clr = abbeImagesVect.get(this.incChns.get(k)).colorTable[value];
                        r[k] = clr.getRed();
                        g[k] = clr.getGreen();
                        b[k] = clr.getBlue();
                    }
                    tr = sum(r);
                    tg = sum(g);
                    tb = sum(b);
                    thumbImageArr[i] = new Color(tr,tg,tb).getRGB();
                }
                this.pParams.bufImg.setRGB(0,0,nx,ny,thumbImageArr, 0, nx);
            }
            private void fillParams () {
                int incChnLength = this.incChns.size();
                this.pParams.dsIndex = this.datasetIndex;
                this.pParams.dsTimeStamp = this.timeStampIdx;
                this.pParams.chnNames = new String[incChnLength];
                this.pParams.lambdas = new short[incChnLength];
                for (int i = 0; i < incChnLength; i++) {
                    this.pParams.chnNames[i] = abbeImagesVect.get(this.incChns.get(i)).imgParams.chnName;
                    this.pParams.lambdas[i] = abbeImagesVect.get(i).imgParams.emissionLambda;
                }
            }
            private void checkImgComplete () {
                // get image data
                // if more than 5 lines are all zeros mark as addToPanel = false;
                int rows = 0;
                int cols = 0;
                int empties = 0;
                
                int incChnLength = this.incChns.size();
                
                short[] imgData = null;
                short[] line = null;
                AbbeImage abImg = null;
                for (int k = 0; k < incChnLength; k++) {
                    abImg = abbeImagesVect.get(this.incChns.get(k));
                    rows = abImg.imgParams.sy;
                    cols = abImg.imgParams.sx;
                    imgData = abImg.data;
                    empties = 0;
                    for (int j = 0; j < rows; j++) {
                        line = Arrays.copyOfRange(imgData, j*cols, ((j+1)*cols)-1);
                        if (max(line) == 0) { empties++; }
                        else { empties = 0; }
                        if (empties == 5) {
                            this.addToPanel = false;
                            return;
                        }
                        } 
                    }
                panelCount++;
                }
            }
        }

    
    public void fillPanels () throws IOException {
        //  for each dataset
        //  create AbbeDatasetJPanel and add to abbeDatasetPanels
        // panelCount++ currently in AbbeDataset.checkImgComplete
        this.abbeDatasetPanels = new JPanel(new GridLayout(panelCount, 1, 4, 11));
        this.abbeDatasetPanels.setBackground(Color.black);
        Params.PanelParams p = null;
        
        int col = 1;
        int row = 0;
        for (AbbeFolder abF : abbeFolderVect) {
            for (AbbeFolder.AbbeDataset abDs : abF.abbeDatasetVect) {
                if (abDs.addToPanel) {
                    p = abDs.pParams;
                    p.dsName = abF.folderName;
                    
                    JPanel dsPanel = new AbbeDatasetJPanel(p);
                    abbeDatasetPanels.add(dsPanel);
                }
                
            }
        }
    }
    
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
        int imgRefCount = -1;
        int imageCounter = 0;
        String imgID;
        datImgLsts = new ArrayList[datasetCount];
        for (int i = 0; i < datasetCount; i++) {  //  for each dataset
            imgRefCount = omeMeta.getDatasetImageRefCount(i);
            datImgLsts[i] = new ArrayList<>();
            for (int j = 0; j < imgRefCount; j++) {  //  for each image in each dataset
                imgID = omeMeta.getDatasetImageRef(i, j);
                //datImgLsts[i].add(j, Integer.valueOf(imgID.replace("Image:", ""))-1); // inconsistent when images/dataset deleted/editted
                datImgLsts[i].add(j, imageCounter);
                imageCounter++;
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
        
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }
    
    /* old functions - review for deletion */
    
    public void updatePath (Path filePath) {
        fPath = filePath;
    }    
}

