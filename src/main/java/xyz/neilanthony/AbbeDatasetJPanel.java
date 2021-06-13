
package xyz.neilanthony;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class AbbeDatasetJPanel extends javax.swing.JPanel {

    Params.PanelParams p = null;
    private final JLabel thumbLabel = null;
    private final JPanel jPanel_Thumb = new JPanel();
    
    // Constructor
    public AbbeDatasetJPanel(Params.PanelParams params) throws IOException {
        initComponents();
        
        this.p = params;
        this.setBackground(UIColors.colorBkgdPanel);
        //this.addMouseListener(new MyAdapter());
        
        this.jPanel_PlaceHolder.setBackground(new Color(0,0,0,0));
        //this.jPanel_PlaceHolder.addMouseListener(new MyAdapter());
        
        //  create new panel
        jPanel_Thumb.setBounds(0, 0, p.psx, p.psy);
        jPanel_Thumb.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        if (this.p.bufImg != null) {

            JLabel picLabel = new JLabel(new ImageIcon(this.p.bufImg));
            picLabel.setBackground(null);
            picLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            if (p.nx == p.psx & p.ny == p.psy) { picLabel.setBounds(0, 0, p.nx, p.ny); }
            else if (p.nx == p.psx) { // fills entire width; centralize in y
                int starty = (p.psy - p.ny) / 2;
                picLabel.setBounds(0, starty, p.nx, starty+p.ny);
                //picLabel.setVerticalAlignment(JLabel.CENTER);
            }
            else if (p.ny == p.psy) {
                int startx = (p.psx - p.nx) / 2;
                picLabel.setBounds(startx, 0, startx+p.nx, p.ny);
                //picLabel.setHorizontalAlignment(JLabel.CENTER);
            }
            
            jPanel_Thumb.add(picLabel);
            jPanel_Thumb.setBackground(null);

        } else {
            jPanel_Thumb.setBackground(UIColors.colorThumb);
        }
        
        
        this.jLabel_ImageName.setText(String.format("%s: TS%d", p.dsName, p.dsTimeStamp));
        this.jLabel_ImageName.setForeground(new Color(197,197,197));
        
        List<JLabel> labels;
        labels = new ArrayList<JLabel>();
        for (int i = 0; i < p.chnNames.length; i++) {
            JLabel lab = new JLabel();
            lab.setText(p.chnNames[i]);
            lab.setBackground(null);
            lab.setForeground(Spectral.spectral_color((double) p.lambdas[i]));
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "drop",
                                    String.format("Added %s - %dnm to panel", p.chnNames[i], p.lambdas[i]));
            lab.setOpaque(true);
            lab.setFont(new Font("Segoe UI Light", Font.PLAIN, 16));
            lab.setBounds(p.psx+36, 25 + (i*22), 555-p.psx-12, 22);
            labels.add(lab);
            this.add(lab);
        }
        this.add(jPanel_Thumb);
        
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

        jLabel_ImageName = new javax.swing.JLabel();
        jPanel_PlaceHolder = new javax.swing.JPanel();

        setAlignmentX(0.0F);
        setAlignmentY(0.0F);
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                formMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                formMouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
        });

        jLabel_ImageName.setFont(new java.awt.Font("Segoe UI Light", 1, 18)); // NOI18N
        jLabel_ImageName.setText("Image Name Goes Here");
        jLabel_ImageName.setPreferredSize(new java.awt.Dimension(359, 25));

        javax.swing.GroupLayout jPanel_PlaceHolderLayout = new javax.swing.GroupLayout(jPanel_PlaceHolder);
        jPanel_PlaceHolder.setLayout(jPanel_PlaceHolderLayout);
        jPanel_PlaceHolderLayout.setHorizontalGroup(
            jPanel_PlaceHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 186, Short.MAX_VALUE)
        );
        jPanel_PlaceHolderLayout.setVerticalGroup(
            jPanel_PlaceHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_PlaceHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel_ImageName, javax.swing.GroupLayout.PREFERRED_SIZE, 359, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel_ImageName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 129, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_PlaceHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    void unselectPanel () {
        p.panelSelected = false;
        this.setBackground(UIColors.colorBkgdPanel);
    }
    void selectPanel () {
        p.panelSelected = true;
        this.setBackground(UIColors.colorBkgdSelected);
    }
    
    private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
        if (!p.panelSelected) {
        evt.getComponent().setBackground(UIColors.colorBkgdMouseOver);
        }
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    "AbbeDatasetJPanel formMouseEntered");
    }//GEN-LAST:event_formMouseEntered

    private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
        if (!p.panelSelected) {
            evt.getComponent().setBackground(UIColors.colorBkgdPanel);
        }
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    "AbbeDatasetJPanel formMouseExited");
    }//GEN-LAST:event_formMouseExited

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
//        if (p.panelSelected) {
//            p.panelSelected = false;
//            evt.getComponent().setBackground(colorBkgdPanel);
//        } else {
//            p.panelSelected = true;
//            evt.getComponent().setBackground(colorBkgdSelected);
//        }
        
//        if (evt.getClickCount() == 2) {
//            //
//        }
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    String.format("AbbeDatasetJPanel formMouseClicked %d times",
                evt.getClickCount()));
    }//GEN-LAST:event_formMouseClicked

    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
        if (p.panelSelected) {
            p.panelSelected = false;
            evt.getComponent().setBackground(UIColors.colorBkgdPanel);
        } else {
            p.panelSelected = true;
            evt.getComponent().setBackground(UIColors.colorBkgdSelected);
        }
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "drop",
                                    "AbbeDatasetJPanel formMousePressed");
    }//GEN-LAST:event_formMousePressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel_ImageName;
    private javax.swing.JPanel jPanel_PlaceHolder;
    // End of variables declaration//GEN-END:variables
}
