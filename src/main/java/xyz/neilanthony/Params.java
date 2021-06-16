
package xyz.neilanthony;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.imglib2.img.array.ArrayImg;
import ome.xml.model.enums.PixelType;


class Params {
    
    // parameters to pass to each dataset panel
    // - upgrading to contain enough infomation to open/process datasets
    static class PanelParams {
        String dsName;
        int dsTimeStamp = -1;
        int bfDatasetIdx = -1;
        int datasetID = -1;
        int bfFldrIdx = -1;
        BufferedImage bufImg;
        ArrayImg arrImg;
        int psx = 186; // panel thumb size
        int psy = 142;
        int nx;
        int ny;
        short[] lambdas;
        String[] chnNames;
        boolean dymin = false;
        boolean rescue = false;
        boolean stack = false;
        boolean timeLapse = false;
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
        int abbeFilesMapKey = -1;
        int fpIndex = -1;
    }
    
}

/** TODO List:
 *  
 *  consider static param classes - static should be once instance, but I'm creating many
 *  function to pull color of channel text - debug why occasionally black
 *  icons for zstack, timelapse etc
 *  icons for dymin and rescue
 *  icons for 2d vs 3d sted
 *  change timestamp option to actual datetime
 *  max project zstack for thumb - maybe skip planes
 *  tile images
 *  touch drag for scrolling
 *  
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
