
package xyz.neilanthony;

import java.awt.image.BufferedImage;
import net.imglib2.img.array.ArrayImg;
import ome.xml.model.enums.PixelType;


public class Params {
    
    public static class PanelParams {
        public BufferedImage bufImg;
        public ArrayImg arrImg;
        public int psx = 186;
        public int psy = 126;
        //public 
    }
    
    public static class ImageParams {
        public String name = "";
        public int sx, sy, sz, st;
        public double dx, dy, dz, dt;
        public PixelType pxType;
    }
    
    public static class DatasetParams {
        
        
    }
    
}
