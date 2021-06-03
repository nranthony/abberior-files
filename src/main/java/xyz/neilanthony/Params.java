
package xyz.neilanthony;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.imglib2.img.array.ArrayImg;
import ome.xml.model.enums.PixelType;


class Params {
    
    // parameters to pass to each dataset panel
    static class PanelParams {
        String dsName;
        int dsTimeStamp;
        int dsIndex;
        BufferedImage bufImg;
        ArrayImg arrImg;
        int psx = 186;
        int psy = 142;
        int nx;
        int ny;
        short[] lambdas;
        String[] chnNames;
        boolean dymin;
        boolean rescue;
        boolean stack;
        boolean timeLapse;
        boolean panelSelected = false;
    }
    
    // parameters for creating ImagePlus
    static class ImageParams {
        String name = "";
        String chnName = "";
        int sx, sy, sz, st;
        double dx, dy, dz, dt;
        PixelType pxType;
        short emissionLambda;
    }
    
    static class DatasetParams {
        

    }
    
    // pareameters to pass to each image file panel
    static class FileParams {
        String fileName;
        int numDatasets;
        Map<Integer,String> labelsUsed;
        boolean panelSelected = false;
        int abbeFilesVectIndex = -1;
        int fpIndex = -1;
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
