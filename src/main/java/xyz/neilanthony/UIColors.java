
package xyz.neilanthony;

import java.awt.Color;


public class UIColors {
    
    public static final Color colorBkgd = Color.getHSBColor(0.0f, 0.0f, 0.10f);
    public static final Color colorBkgdDark = Color.getHSBColor(0.0f, 0.0f, 0.06f);
    public static final Color colorB4 = Color.getHSBColor(0.0f, 0.0f, 0.4f);
    public static final Color colorB3 = Color.getHSBColor(0.0f, 0.0f, 0.3f);
    public static final Color colorB2 = Color.getHSBColor(0.0f, 0.0f, 0.2f);
    
    public static final Color colorBkgdPanel = Color.getHSBColor(0.0f, 0.0f, 0.13f);
    public static final Color colorBkgdMouseOver = Color.getHSBColor(0.0f, 0.0f, 0.15f);
    public static final Color colorBkgdSelected = Color.getHSBColor(0.6f, 0.6f, 0.15f);

    public static  final Color colorThumb = Color.getHSBColor(0.0f, 0.0f, 0.08f);
    public static  final Color colorChnText = Color.getHSBColor(0.54f, 0.46f, 0.66f);
    
    public static Color greyLevel (float level) {
        Color col; return col = Color.getHSBColor(0.0f, 0.0f, (level * 0.01f));
    }
    
}
