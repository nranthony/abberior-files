/*
Used to encapsulate all the information required for each Abberior file thats imported

*/
package xyz.neilanthony;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import static com.google.common.primitives.Shorts.max;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.HyperStackConverter;
import ij.process.LUT;
import ij.process.ShortProcessor;
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
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import ome.units.UNITS;
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
class AbbeFile {

    JPanel abbeDatasetPanels = null;
    int panelCount = 0;
    
    OBFReader reader = new OBFReader();
    IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
    BufferedImageReader bufImgReader= new BufferedImageReader(reader);

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder builder = null;
    
    private String omexml = null;
    private Document xmlDoc = null;
    private Path fPath = null;
    Params.FileParams fParams = new Params.FileParams();
    
    final ArrayList<AbbeFolder> abbeFolderVect = new ArrayList<>();
    // cast to sychronized (abbeFolderVect) when multithreading
    
    private String[] folderNames = null;
    /* array of associated indxs  -  List<imageIndx>[datasetIndx]
    An array of Lists, where each list contains the imageIdxs in that dataset */
    //private ArrayList<Integer>[] datImgLsts;
    // change to BFIdxPair pairs for bioformats index paired with imageIDStr
    private ArrayList<BFIdxPair>[] datImgLsts;
    
    // AbbeFile Constructor
    AbbeFile(Path filePath, int abbeFileVectIndex) throws FormatException, IOException, ParserConfigurationException, SAXException {
        this.fPath = filePath;
        this.fParams.labelsUsed = new HashMap<Integer,String>();
        this.fParams.fileName = filePath.getFileName().toString();
        this.fParams.abbeFilesMapKey = abbeFileVectIndex;
        
        //  start waiting thread
        reader.setMetadataStore(omeMeta);
        reader.setId(this.fPath.toString());
        // end waiting thread
        AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "drop",
                                    String.format("AbbeFile %s Constructed, abbeFileVectIndex %d",
                this.fParams.fileName,
                this.fParams.abbeFilesMapKey));
    }

    static class BFIdxPair {
        int bfIndex; // bioformats will index from 0,1,2 ... (n-1) 
        int idIdx; // ID index is # in ID string, e.g. ID="Image:7"
    }
    
    private final Map<Integer,Integer> bfIDmap = new HashMap<>();

    /** Nested classes AbbeFolder -> AbbeDataset -> AbbeImage
     * requires the images in each dataset be predetermined before
     * creating the datasets in each folder, which requires the overlap of images in
     * dataset and images in folders to be the first step
     */
    class AbbeFolder  {
        
        String folderName;
        String folderIDStr;
        int folderIndex = -1;
        int timeStampCounter = 0;
        // TODO - add subfolder count and list; add roi count and list
        
        final ArrayList<AbbeDataset> abbeDatasetVect = new ArrayList<>();
        final ArrayList<Integer> fldrImgIndxs = new ArrayList<>();
        
        /** constructor
        * bioformats folder index appears to be 1,2,3,...N
        * ID="Folder:#" increasing and random order based on image history
        */
        AbbeFolder(int fldrIndex, String fldrIDStr, String fldrName) {
            this.folderName = fldrName;
            this.folderIDStr = fldrIDStr;
            this.folderIndex = fldrIndex;
            //  get the list of image indicies in the folder
            String imgIDStr;
            int imgCount = omeMeta.getFolderImageRefCount(fldrIndex);
            for (int i = 0; i < imgCount; i++) {
                imgIDStr = omeMeta.getFolderImageRef(fldrIndex, i);
                fldrImgIndxs.add(i, Integer.valueOf(imgIDStr.replace("Image:", "")));
            }
        }
        
        void addDataset(int datasetIndx, Integer[] imgIDArr) throws IOException, FormatException {
            this.timeStampCounter++;
            this.abbeDatasetVect.add(
                    new AbbeDataset(datasetIndx,
                                    omeMeta.getDatasetID(datasetIndx),
                                    //omeMeta.getDatasetName(datasetIndx), // changing to use folder name here
                                    this.folderName,
                                    imgIDArr,
                                    this.timeStampCounter));
        }
        
        class AbbeDataset {
            
            final ArrayList<AbbeImage> abbeImagesVect = new ArrayList<>();
            ArrayList<Integer> incChns = null;
            String datasetName;
            String datasetID;
            int datasetIndex = -1;
            boolean tiled = false;
            int timeStampIdx;
            int imageCount = -1;
            Integer[] imageIDIdxs = null;
            int parentFolderIndex = -1;
            Params.PanelParams pParams = new Params.PanelParams();
            boolean addToPanel = true;
            
            // constructor
            AbbeDataset(int datIndex, String datIDStr,
                String datName, Integer[] imgIDArr, int timeStamp) throws IOException, FormatException {
                AbbeLogging.postToLog(
                    Level.FINE, this.getClass().toString(), "",
                        String.format("Contructing AbbeDataSet %s, ID %s, idx %d", datName, datIDStr, datIndex));
                this.datasetName = datName;
                this.datasetID = datIDStr;
                this.datasetIndex = datIndex;
                this.imageIDIdxs = imgIDArr;
                this.timeStampIdx = timeStamp;
                this.imageCount = imgIDArr.length;
                for (int i = 0; i < imgIDArr.length; i++) {
                    this.abbeImagesVect.add(i, new AbbeImage(imgIDArr[i]));
                }
                
                checkIncludedChannels();
                // below require incChns to be detemined in checkIncludedChannels();
                fillParams();
                createThumbEtc();
                checkImgComplete();
            }
            
            class AbbeImage {

                String imageName;
                String imageIDStr;
                int imageIDIndex = -1;
                int bfIndex = -1;
                short[] data = null;
                short[] mask = null; // note data is uint8, but Java only has int8; storing in short[] 
                //int[] thumbData = null;
                short[] shortThumbData = null;
                Params.ImageParams imgParams = new Params.ImageParams();
                boolean addToComposite;  // for excluding DyMIN and RESCue from display
                boolean tiled = false;
                private short minimumMax = 35; // to stop channels of mostly noise 'swamping' thumbnail
                private short offset = 2; // chops the lower values to remove a little noise
                
                @Parameter
                private final LUTService ls = new DefaultLUTService();
                String lutName = "Grays.lut"; // 
                Color[] colorTable;
                int ctSize;
                ColorTable ct;

                // constructor
                AbbeImage(int imageID) throws IOException, FormatException {
                    this.imageIDIndex = imageID;
                    this.imageIDStr = String.format("Image:%d", imageID);
                    this.bfIndex = bfIDmap.get(imageID);
                    this.imageName = omeMeta.getImageName(bfIndex);
                    this.imgParams.pxType = omeMeta.getPixelsType(bfIndex);
                    this.imgParams.chnName = omeMeta.getChannelName(bfIndex, 0);
                    if (this.imageName.contains("DyMIN") | this.imageName.contains("rescue")){
                    //if (this.imgParams.pxType == PixelType.UINT8){
                        this.addToComposite = false;
                        pullParams(bfIndex);
                        pullMaskData(bfIndex);
                    } else {
                        this.addToComposite = true;
                        pullParams(bfIndex);
                        pullImageData(bfIndex);
                        resizeForThumb();
                        rescaleThumbRange();
                        setColorTable();
                    }
                }
                
                void pullParams (int imgIdx) {
                    reader.setSeries(imgIdx);
                    this.imgParams.sx = reader.getSizeX();
                    this.imgParams.sy = reader.getSizeY();
                    this.imgParams.sz = reader.getSizeZ();
                    this.imgParams.st = reader.getSizeT();
                    this.imgParams.dx = (double) omeMeta.getPixelsPhysicalSizeX(imgIdx).value(UNITS.MICROMETER);
                    this.imgParams.dy = (double) omeMeta.getPixelsPhysicalSizeY(imgIdx).value(UNITS.MICROMETER);
//                    if (this.imgParams.st > 1) {
//                        omeMeta.getpixels
//                        this.imgParams.dz = (double) omeMeta.getPixelsPhysicalSizeZ(imgIdx).value(UNITS.MICROMETER);
//                    }
                    
                }
                
                void pullImageData (int imgIdx) throws IOException, FormatException {

                    // TODO - consider z-stacks and time-lapses here
                    int zPlane = ( (imgParams.sz + 1) / 2 ) - 1;
                    Object dat = reader.openPlane(zPlane, 0, 0, imgParams.sx, imgParams.sy);
                    byte[] bytes = (byte[])dat;
                    this.data = new short[bytes.length/2];
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(this.data);
                }
                void pullMaskData (int imgIdx) throws IOException, FormatException {

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
                void resizeForThumb () {
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
                void rescaleThumbRange () {
                    
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
                void setColorTable () throws IOException {
                    Length emis = omeMeta.getChannelEmissionWavelength(bfIDmap.get(this.imageIDIndex), 0);
                    Number lambda = emis.value();
                    short nm = (short)(lambda.doubleValue()*1e9);
                    this.imgParams.emissionLambda = nm;
                    fParams.labelsUsed.put((int)nm, omeMeta.getChannelFluor(bfIDmap.get(this.imageIDIndex), 0));
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
                void getColorTable(String colormap) throws IOException {
                    String lutPath = "/luts/" + colormap;
                    InputStream lutStream = getClass().getResourceAsStream(lutPath);
                    this.ct = ls.loadLUT(lutStream);
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

    
    
    class DatasetPanelMouseEvent implements MouseListener {
        
        private int ds = -1;
        private int fldr = -1;
        
        private Params.ImageParams imgParams = null;
        //private AbbeFile.this abFile;
        
        @Override
        public void mouseClicked(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                    String.format("DatasetPanelMouseEvent MouseListener mouseClicked %d times",
                    e.getClickCount()));
            if (e.getClickCount() == 2) {
                ds = ((AbbeDatasetJPanel)e.getComponent()).p.dsIndex;
                fldr = ((AbbeDatasetJPanel)e.getComponent()).p.fldrIndex;
                AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    String.format("e click == 2, fldr % ds %d %s",
                                        fldr, ds, ((AbbeDatasetJPanel)e.getComponent()).p.dsName));
                try {
                    AbbeFile.this.openDataset(ds, imgParams);
                } catch (FormatException | IOException ex) {
                    Logger.getLogger(AbbeFile.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        @Override
        public void mousePressed(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    "DatasetPanelMouseEvent MouseListener mousePressed");
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    "DatasetPanelMouseEvent MouseListener mouseReleased");
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    "DatasetPanelMouseEvent MouseListener mouseEntered");
        }
        @Override
        public void mouseExited(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    "DatasetPanelMouseEvent MouseListener mouseExited");
        }
    }
    
    void openSingleDataset (int dsIdx, byte[] r, byte[] g, byte[] b, Params.ImageParams imgParams) throws FormatException, IOException {
        
        int fldrIdx = findDatasetFolder(dsIdx);
        
        if (fldrIdx < 0) {
            AbbeLogging.postToLog(Level.WARNING,
                this.getClass().toString(),
                "openSingleDataset",
                String.format("Dataset %d not found in AbbeFile %s", dsIdx, this.fParams.fileName));
        }
        
        ImageStack imgStk = new ImageStack();
        List luts = new ArrayList<LUT>();

        AbbeFolder abFldr = this.abbeFolderVect.get(fldrIdx);
        AbbeFolder.AbbeDataset abDs = abFldr.abbeDatasetVect.get(dsIdx);
        
        for (int i = 0; i < abDs.incChns.size(); i++) {
            int chn = abDs.incChns.get(i);
            AbbeFile.AbbeFolder.AbbeDataset.AbbeImage abImg = abDs.abbeImagesVect.get(chn);

            imgParams = abImg.imgParams;
            if (abImg.ctSize != 256) {
                // create grey scale
                for (int k = 0; k < 256; k++) {
                    r[k] = (byte) k;
                    g[k] = (byte) k;
                    b[k] = (byte) k;
                }
            } else {
                for (int k = 0; k < 256; k++) {
                    r[k] = (byte) abImg.colorTable[k].getRed();
                    g[k] = (byte) abImg.colorTable[k].getGreen();
                    b[k] = (byte) abImg.colorTable[k].getBlue();
                }
            }
            luts.add(new LUT(r, g, b));

            // for each z
            // for each t
            // data is inheriently each c for abberior as far as I've seen
            this.reader.setSeries(abImg.bfIndex);
            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
                                    String.format("%s %s", this.fParams.fileName, this.reader.getDimensionOrder()));
            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
                                    String.format("%s", this.reader.getDatasetStructureDescription()));
            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
                                    String.format("EffectiveSizeC: %d", this.reader.getEffectiveSizeC()));
            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
                                    String.format("ImageCount: %d", this.reader.getImageCount()) );

            for(int t = 0; t < imgParams.st; t++) {
                for (int z = 0; z < imgParams.sz; z++) {
                    Object dat = this.reader.openPlane(z+(t*imgParams.sz), 0, 0, imgParams.sx, imgParams.sy);
                    byte[] bytes = (byte[])dat;
                    short[] data = new short[bytes.length/2];
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
                    ShortProcessor sp = new ShortProcessor(imgParams.sx, imgParams.sy, data, null);
                    imgStk.addSlice(sp);
                }
            }
            // if needed for speed in big stacks timelapses
            // investigate setPixels(java.lang.Object pixels)
            // create empty ShortProcessor and add dat in setPixels
        }

        Calibration cali = new Calibration();
        cali.setUnit("micron");
        cali.pixelWidth = imgParams.dx;
        cali.pixelHeight = imgParams.dy;
        cali.pixelDepth = imgParams.dz;
        cali.frameInterval = imgParams.dt;

        ImagePlus imp = new ImagePlus();

        imp.setStack(String.format("%s-TS%d", abFldr.folderName, abDs.timeStampIdx), imgStk);
        //imp.setDimensions(abDs.incChns.size(), imgParams.sz, imgParams.st);
        if (abDs.incChns.size() > 1 | imgParams.sz > 1 | imgParams.st > 1) {
            imp = HyperStackConverter.toHyperStack(imp, abDs.incChns.size(),
                                                    imgParams.sz, imgParams.st, "xyztc", "composite");
        }

        imp.setCalibration(cali);
        imp = new CompositeImage(imp, CompositeImage.COMPOSITE);
        for (int i = 0; i < abDs.incChns.size(); i++) {
            ((CompositeImage)imp).setChannelLut((LUT)luts.get(i), i+1);
        }
        imp.setOpenAsHyperStack(true);
        imp.show();
        // TODO: add optional opening of rescue and dymin masks...  seperate ImagePlus
    }
    
    int findDatasetFolder (int dsIdx) {
        
        for (int f = 0; f < this.abbeFolderVect.size(); f++) {
            AbbeFolder abFldr = this.abbeFolderVect.get(f);
            for (int d = 0; d < abFldr.abbeDatasetVect.size(); d++) {
                AbbeFolder.AbbeDataset abDs = abFldr.abbeDatasetVect.get(d);
                if (abDs.datasetIndex == dsIdx) {
                    return abFldr.folderIndex;
                }
            }
        }
        return -1;

    }
    
    void openDataset (int dsIdx, Params.ImageParams imgParams) throws FormatException, IOException {

//        // for LUTs
//        byte[] r = new byte[256];
//        byte[] g = new byte[256];
//        byte[] b = new byte[256];
//        
//        ImageStack imgStk = new ImageStack();
//        List luts = new ArrayList<LUT>();
//        try {
//            //AbbeFolder abFldr = this.abbeFolderVect.get(fldrIndex);
//            //AbbeFolder.AbbeDataset abDs = abFldr.abbeDatasetVect.get(dsIndex);
//            OpenAbbeJFrame.openSingleDataset(dsIdx, this, r, g, b, imgParams);
//            AbbeLogging.postToLog(Level.INFO, this.getClass().toString(), "openDataset",
//                String.format("pFolderIndex %d, pDatasetIndex %d, FolderIndex %d, FolderID %s, DatasetIndex %d, DatasetID %s",
//                              fldrIndex, dsIdx, abFldr.folderIndex, abFldr.folderIDStr, abDs.datasetIndex, abDs.datasetID));
//        } catch (Exception ex) {
//            AbbeLogging.postToLog(Level.SEVERE, this.getClass().toString(), "openDataset",
//                                    String.format("Exception: %s", ex.toString()));
//        }
//        
//        
//        
//        
//        for (AbbeFolder abF : abbeFolderVect ) {
//            if (abF.abbeDatasetVect.size() > 0) {               
//                for (int ds = 0; ds < abF.abbeDatasetVect.size(); ds++) {
//                    abDs = abF.abbeDatasetVect.get(ds);
//                    logger.log(Level.INFO, String.format("AbbeDataset %s",
//                                abDs.datasetName));
//                }
//            }
//        }

//        int cntr = 0;
//        for (Component panel : abbeDatasetPanels.getComponents()) {
//            
//            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "drop",
//                                    String.format("panel %d, dsName %s",
//                        cntr, ((AbbeDatasetJPanel)panel).p.dsName));
//            cntr++;
//        }
                
//        for (int i = 0; i < abDs.incChns.size(); i++) {
//            int chn = abDs.incChns.get(i);
//            AbbeFile.AbbeFolder.AbbeDataset.AbbeImage abImg = abDs.abbeImagesVect.get(chn);
//
//            imgParams = abImg.imgParams;
//            if (abImg.ctSize != 256) {
//                // create grey scale
//                for (int k = 0; k < 256; k++) {
//                    r[k] = (byte) k;
//                    g[k] = (byte) k;
//                    b[k] = (byte) k;
//                }
//            } else {
//                for (int k = 0; k < 256; k++) {
//                    r[k] = (byte) abImg.colorTable[k].getRed();
//                    g[k] = (byte) abImg.colorTable[k].getGreen();
//                    b[k] = (byte) abImg.colorTable[k].getBlue();
//                }
//            }
//            luts.add(new LUT(r, g, b));
//
//            // for each z
//            // for each t
//            // data is inheriently each c for abberior as far as I've seen
//            abFile.reader.setSeries(abImg.bfIndex);
//            logger.log(Level.FINE, String.format("%s %s", abFile.fParams.fileName, abFile.reader.getDimensionOrder()));
//            logger.log(Level.FINE, String.format("%s", abFile.reader.getDatasetStructureDescription()));
//            logger.log(Level.FINE, String.format("EffectiveSizeC: %d", abFile.reader.getEffectiveSizeC()));
//            logger.log(Level.FINE, String.format("ImageCount: %d", abFile.reader.getImageCount()) );
//
//            for(int t = 0; t < imgParams.st; t++) {
//                for (int z = 0; z < imgParams.sz; z++) {
//                    Object dat = abFile.reader.openPlane(z+(t*imgParams.sz), 0, 0, imgParams.sx, imgParams.sy);
//                    byte[] bytes = (byte[])dat;
//                    short[] data = new short[bytes.length/2];
//                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
//                    ShortProcessor sp = new ShortProcessor(imgParams.sx, imgParams.sy, data, null);
//                    imgStk.addSlice(sp);
//                }
//            }
//            // if needed for speed in big stacks timelapses
//            // investigate setPixels(java.lang.Object pixels)
//            // create empty ShortProcessor and add dat in setPixels
//        }
//
//        Calibration cali = new Calibration();
//        cali.setUnit("micron");
//        cali.pixelWidth = imgParams.dx;
//        cali.pixelHeight = imgParams.dy;
//        cali.pixelDepth = imgParams.dz;
//        cali.frameInterval = imgParams.dt;
//
//        ImagePlus imp = new ImagePlus();
//
//        imp.setStack(String.format("%s-TS%d", abFldr.folderName, abDs.timeStampIdx), imgStk);
//        //imp.setDimensions(abDs.incChns.size(), imgParams.sz, imgParams.st);
//        if (abDs.incChns.size() > 1 | imgParams.sz > 1 | imgParams.st > 1) {
//            imp = HyperStackConverter.toHyperStack(imp, abDs.incChns.size(),
//                                                    imgParams.sz, imgParams.st, "xyztc", "composite");
//        }
//
//        imp.setCalibration(cali);
//        imp = new CompositeImage(imp, CompositeImage.COMPOSITE);
//        for (int i = 0; i < abDs.incChns.size(); i++) {
//            ((CompositeImage)imp).setChannelLut((LUT)luts.get(i), i+1);
//        }
//        imp.setOpenAsHyperStack(true);
//        imp.show();
//        // TODO: add optional opening of rescue and dymin masks...  seperate ImagePlus
    }
    
    
    
    void fillPanels () throws IOException {
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
                    p.fldrIndex = abF.folderIndex;
                    
                    JPanel dsPanel = new AbbeDatasetJPanel(p);
                    dsPanel.addMouseListener(new DatasetPanelMouseEvent());
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
    void pullImageInfo () {
//        reader.	getChannelEmissionWavelength(int imageIDIndex, int channelIndex)
//        reader.getChannelFluor(int imageIDIndex, int channelIndex)
//        reader.	getChannelColor(int imageIDIndex, int channelIndex)
//        
//        	getPixelsDimensionOrder(int imageIDIndex)
//                	getPixelsPhysicalSizeX(int imageIDIndex)
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
    void scanDatasetsFoldersImages() throws FormatException, IOException {
        // fill datImgLsts with image indicies for all datasets
        int datasetCount = omeMeta.getDatasetCount();
        int imgRefCount = -1;
        int imageCounter = 0;
        String imgIDStr;
        BFIdxPair bfIdxPair;
        this.datImgLsts = new ArrayList[datasetCount];
        for (int i = 0; i < datasetCount; i++) {  //  for each dataset
            imgRefCount = omeMeta.getDatasetImageRefCount(i);
            this.datImgLsts[i] = new ArrayList<>();
            for (int j = 0; j < imgRefCount; j++) {  //  for each image in each dataset
                bfIdxPair = new BFIdxPair();
                imgIDStr = omeMeta.getDatasetImageRef(i, j);
                bfIdxPair.idIdx = (int)Integer.valueOf(imgIDStr.replace("Image:", ""));
                bfIdxPair.bfIndex = imageCounter;
                imageCounter++;
                this.datImgLsts[i].add(j, bfIdxPair);
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
        
        // TODO - fuse/replace bfIdxPair with bfIDmap
        int imageCount = omeMeta.getImageCount();
        int keyID, valueBFidx;
        for (int i = 0; i < imageCount; i++) {
            imgIDStr = omeMeta.getImageID(i);
            keyID = (int)Integer.valueOf(imgIDStr.replace("Image:", ""));
            valueBFidx = i;
            bfIDmap.put(keyID, valueBFidx);
        }
    }
    
    void collateFolderImages() throws IOException, FormatException {
        /*
        private ArrayList<Integer>[] datImgLsts;
        In AbbeFolder:
        final ArrayList<Integer> fldrImgIndxs = new ArrayList<>();
        
        for each folder:
            for each dataset:
                if dataset image is in folder image list
                    add dataset to folder
        */
        //Set<Integer> datasetImages = null;
        Set<Integer> folderImages = new HashSet<>();
        int dsImgCount = 0;
        for (AbbeFolder abF : abbeFolderVect ) {
            // check that all the dataset images are in the folder images
            // if not, then one or both lists are incorrect or dataset corrupt
            // if all dataset images in folder, create new dataset in folder
            
            if (abF.fldrImgIndxs.size() > 0) {
                
                folderImages.clear();
                folderImages.addAll(abF.fldrImgIndxs);
                
                for (int ds = 0; ds < datImgLsts.length; ds++) {
                    
                    dsImgCount = datImgLsts[ds].size();
                    Set<Integer> imgIDSet = new HashSet<>();
                    for (int i=0;i<dsImgCount;i++) { imgIDSet.add(datImgLsts[ds].get(i).idIdx); }
                    
                    //  all the images in dataset ds are in the current folder
                    if (folderImages.containsAll(imgIDSet)) {
                        Integer[] imgIDArr = new Integer[dsImgCount];
                        imgIDArr = imgIDSet.toArray(imgIDArr);
                        AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "collateFolderImages",
                                    String.format("Creating ds%d", ds));
                        abF.addDataset(ds, imgIDArr);
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
    void createXMLDoc () throws ParserConfigurationException, SAXException, IOException {
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
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "drop",
                                    imgNode.toString());
        }
    }
    
    String[] getFolderNames () throws FormatException, IOException {
        if ( this.folderNames == null ) {
            this.scanDatasetsFoldersImages();
        }
        return this.folderNames;
    }
    
    String getOMEXML () throws IOException {
        if ( this.omexml == null ) {
            this.pullOMEXMLRaw();
        }
        return this.omexml;
    }
    
    void pullOMEXMLRaw() throws FileNotFoundException, IOException {
        
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
    
    void updatePath (Path filePath) {
        fPath = filePath;
    }    
}

