
package xyz.neilanthony;

import java.awt.image.BufferedImage;
import net.imglib2.img.array.ArrayImg;


public class Params {
    
    public static class PanelParams {
        public BufferedImage bufImg;
        public ArrayImg arrImg;
        
    }
    
    public static class ImageParams {
        public String name = "";
        public int sx, sy, sz, st;
        public double dx, dy, dz, dt;
        
    }
    
    public static class DatasetParams {
        
        
    }
    
}
