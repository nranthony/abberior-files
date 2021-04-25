/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import java.awt.Color;
import java.awt.Graphics;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import net.coobird.thumbnailator.Thumbnails;

/**
 *
 * @author nelly
 */
public class AbbeImageJPanel extends javax.swing.JPanel {

    
    private Color colorBkgdPanel = Color.getHSBColor(0.0f, 0.0f, 0.13f);
    private Color colorThumb = Color.getHSBColor(0.0f, 0.0f, 0.08f);
    private final Color colorChnText = Color.getHSBColor(0.54f, 0.46f, 0.66f);
    private AbbeImagePanelParams p = null;
    private JLabel thumbLabel = null;
    
    // Constructor
    public AbbeImageJPanel(AbbeImagePanelParams params) throws IOException {
        initComponents();
        
        this.p = params;
                
        //Graphics gThumb = null;
        this.setBackground(colorBkgdPanel);
        if (this.p.bufImg != null) {
            int newWid, newHght;
            float aspectRatio = p.bufImg.getWidth() / p.bufImg.getHeight();
            newWid = jPanel_Thumb.getPreferredSize().width;
            newHght = Math.round(newWid/aspectRatio);
            
            thumbLabel = new JLabel(new ImageIcon(
                    Thumbnails.of(p.bufImg)
                            .size(newWid,newHght)
                            .asBufferedImage()));
            this.jPanel_Thumb.add(thumbLabel);
            //this.jPanel_Thumb.repaint();
            this.jPanel_Thumb.revalidate();
            
            this.canvas1.setBackground(colorThumb);
            Graphics g = this.p.bufImg.getGraphics();
            this.canvas1.paint(g);
            this.canvas1.validate();
           
            this.jLabel_ChnName_1.setText(Integer.toString(p.bufImg.getWidth()));
            this.jLabel_ChnName_2.setText(Integer.toString(p.bufImg.getHeight()));
        } else {
            this.jPanel_Thumb.setBackground(colorThumb);
        }
        this.jLabel_ChnName_1.setForeground(colorChnText);
        this.jLabel_ChnName_2.setForeground(colorChnText);
        

        
    }
    
    private JLabel chnLabel (String labelStr) {
        JLabel label = new JLabel(labelStr);
        //label.setFont(font);
        return label;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel_Thumb = new javax.swing.JPanel();
        jLabel_ImageName = new javax.swing.JLabel();
        jLabel_ChnName_1 = new javax.swing.JLabel();
        jLabel_ChnName_2 = new javax.swing.JLabel();
        canvas1 = new java.awt.Canvas();

        jPanel_Thumb.setPreferredSize(new java.awt.Dimension(174, 96));

        javax.swing.GroupLayout jPanel_ThumbLayout = new javax.swing.GroupLayout(jPanel_Thumb);
        jPanel_Thumb.setLayout(jPanel_ThumbLayout);
        jPanel_ThumbLayout.setHorizontalGroup(
            jPanel_ThumbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 174, Short.MAX_VALUE)
        );
        jPanel_ThumbLayout.setVerticalGroup(
            jPanel_ThumbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel_ImageName.setFont(new java.awt.Font("Segoe UI Light", 1, 18)); // NOI18N
        jLabel_ImageName.setText("Image Name Goes Here");

        jLabel_ChnName_1.setFont(new java.awt.Font("Segoe UI Semilight", 0, 18)); // NOI18N
        jLabel_ChnName_1.setText("CHN 1 NAME");

        jLabel_ChnName_2.setFont(new java.awt.Font("Segoe UI Semilight", 0, 18)); // NOI18N
        jLabel_ChnName_2.setText("CHN 2 NAME");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_Thumb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel_ImageName)
                        .addContainerGap(110, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel_ChnName_1)
                            .addComponent(jLabel_ChnName_2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(canvas1, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel_Thumb, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel_ImageName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel_ChnName_1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel_ChnName_2)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(canvas1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())))))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.Canvas canvas1;
    private javax.swing.JLabel jLabel_ChnName_1;
    private javax.swing.JLabel jLabel_ChnName_2;
    private javax.swing.JLabel jLabel_ImageName;
    private javax.swing.JPanel jPanel_Thumb;
    // End of variables declaration//GEN-END:variables
}
