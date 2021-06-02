
package xyz.neilanthony;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.imglib2.img.array.ArrayImg;
import ome.xml.model.enums.PixelType;


public class Params {
    
    // parameters to pass to each dataset panel
    public static class PanelParams {
        public String dsName;
        public int dsTimeStamp;
        public int dsIndex;
        public BufferedImage bufImg;
        public ArrayImg arrImg;
        public int psx = 186;
        public int psy = 142;
        public int nx;
        public int ny;
        public short[] lambdas;
        public String[] chnNames;
        public boolean dymin;
        public boolean rescue;
        public boolean stack;
        public boolean timeLapse;
        public boolean panelSelected = false;
    }
    
    // parameters for creating ImagePlus
    public static class ImageParams {
        public String name = "";
        public String chnName = "";
        public int sx, sy, sz, st;
        public double dx, dy, dz, dt;
        public PixelType pxType;
        public short emissionLambda;
    }
    
    public static class DatasetParams {
        

    }
    
    // pareameters to pass to each image file panel
    public static class FileParams {
        public String fileName;
        public int numDatasets;
        public Map<Integer,String> labelsUsed;
        public boolean panelSelected = false;
        public int abbeFilesVectIndex = -1;
    }
    
}

/** TODO List:
 *  
 *  select all/none code
 *  set last file opened to be selected and highlighted selected
 *  debug file panel order on dragging many files - order gets mixed up
 *  check option for opening single channel - stack required error
 *  apply correct LUTs for opening
 *  function to pull color of channel text - debug why occasionally black
 *  icons for zstack, timelapse etc
 *  icons for dymin and rescue
 *  icons for 2d vs 3d sted
 *  change timestamp option to actual datetime
 *  max project zstack for thumb - maybe skip planes
 *  tile images
 *  histograms or min/max counts
 *  centralize thumbs in panel
 *  offset subpanel in panel
 *  check drag and drop new while cooking
 *  add working gif plus update text on loading
 *  add load selected on file panels
 *  add load all selected at top
 * 
 *  
 */
