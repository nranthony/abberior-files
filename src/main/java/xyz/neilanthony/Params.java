
package xyz.neilanthony;

import java.awt.image.BufferedImage;
import net.imglib2.img.array.ArrayImg;
import ome.xml.model.enums.PixelType;


public class Params {
    
    public static class PanelParams {
        public BufferedImage bufImg;
        public ArrayImg arrImg;
        public int psx = 186;
        public int psy = 142;
        public int nx;
        public int ny;
        public short[] lambdas;
        public String dsName;
        public int dsTimeStamp;
        public String[] chnNames;
        public boolean dymin;
        public boolean rescue;
        public boolean stack;
        public boolean timeLapse;
    }
    
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
    
}

/** TODO List:
 *                         
 *  add channel labels in loop
 *  function to pull color of channel text
 *  icons for zstack, timelapse etc
 *  icons for dymin and rescue
 *  icons for 2d vs 3d sted
 *  image name / folder name + timestamp
 *  pull zstack mid point for thumb
 *    or maybe max project
 *  add files panels for multiple abbefiles
 *    swap between on selection
 *  add panel highlight on click
 *  tile images
 *  histograms or min/max counts
 *  centralize thumbs in panel
 *  offset subpanel in panel
 *  check drag and drop new while cooking
 *  add file panels
 *  add working gif plus update text on loading
 *  add load selected on file panels
 *  add load all selected at top
 * 
 *  
 */
