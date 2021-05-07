
package xyz.neilanthony;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class AbbeImageJPanel extends javax.swing.JPanel {

    private final Color colorBkgdPanel = Color.getHSBColor(0.0f, 0.0f, 0.13f);
    private final Color colorBkgdMouseOver = Color.getHSBColor(0.0f, 0.0f, 0.15f);
    private final Color colorBkgdSelected = Color.getHSBColor(0.55f, 0.6f, 0.1f);
    private final Color colorThumb = Color.getHSBColor(0.0f, 0.0f, 0.08f);
    private final Color colorChnText = Color.getHSBColor(0.54f, 0.46f, 0.66f);
    private Params.PanelParams p = null;
    private final JLabel thumbLabel = null;
    private final JPanel jPanel_Thumb = new JPanel();
    
    // Constructor
    public AbbeImageJPanel(Params.PanelParams params) throws IOException {
        initComponents();
        
        this.p = params;
        this.setBackground(colorBkgdPanel);
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
            jPanel_Thumb.setBackground(colorThumb);
        }
        
        
        this.jLabel_ImageName.setText(String.format("%s: TS%d", p.dsName, p.dsTimeStamp));
        this.jLabel_ImageName.setForeground(new Color(197,197,197));
        
        List<JLabel> labels;
        labels = new ArrayList<JLabel>();
        for (int i = 0; i < p.chnNames.length; i++) {
            JLabel lab = new JLabel();
            lab.setText(p.chnNames[i]);
            lab.setBackground(null);
            lab.setForeground(spectral_color((double) p.lambdas[i]));
            System.out.println(String.format("Added %s - %dnm to panel", p.chnNames[i], p.lambdas[i]));
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
                .addComponent(jLabel_ImageName, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel_ImageName)
                .addGap(0, 129, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_PlaceHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
        if (!p.panelSelected) {
        evt.getComponent().setBackground(colorBkgdMouseOver);
        }
    }//GEN-LAST:event_formMouseEntered

    private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
        if (!p.panelSelected) {
            evt.getComponent().setBackground(colorBkgdPanel);
        }
    }//GEN-LAST:event_formMouseExited

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        if (p.panelSelected) {
            p.panelSelected = false;
            evt.getComponent().setBackground(colorBkgdPanel);
        } else {
            p.panelSelected = true;
            evt.getComponent().setBackground(colorBkgdSelected);
        }
    }//GEN-LAST:event_formMouseClicked

    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
        if (p.panelSelected) {
            p.panelSelected = false;
            evt.getComponent().setBackground(colorBkgdPanel);
        } else {
            p.panelSelected = true;
            evt.getComponent().setBackground(colorBkgdSelected);
        }
    }//GEN-LAST:event_formMousePressed

    
    //  https://stackoverflow.com/a/22681410/5824166
    private Color spectral_color(double l) // RGB <0,1> <- lambda l <400,700> [nm]
    {
        double t;
        double r=0.0;
        double g=0.0;
        double b=0.0;
             if ((l>=400.0)&&(l<410.0)) { t=(l-400.0)/(410.0-400.0); r=    +(0.33*t)-(0.20*t*t); }
        else if ((l>=410.0)&&(l<475.0)) { t=(l-410.0)/(475.0-410.0); r=0.14         -(0.13*t*t); }
        else if ((l>=545.0)&&(l<595.0)) { t=(l-545.0)/(595.0-545.0); r=    +(1.98*t)-(     t*t); }
        else if ((l>=595.0)&&(l<650.0)) { t=(l-595.0)/(650.0-595.0); r=0.98+(0.06*t)-(0.40*t*t); }
        else if ((l>=650.0)&&(l<700.0)) { t=(l-650.0)/(700.0-650.0); r=0.65-(0.84*t)+(0.20*t*t); }
             if ((l>=415.0)&&(l<475.0)) { t=(l-415.0)/(475.0-415.0); g=             +(0.80*t*t); }
        else if ((l>=475.0)&&(l<590.0)) { t=(l-475.0)/(590.0-475.0); g=0.8 +(0.76*t)-(0.80*t*t); }
        else if ((l>=585.0)&&(l<639.0)) { t=(l-585.0)/(639.0-585.0); g=0.84-(0.84*t)           ; }
             if ((l>=400.0)&&(l<475.0)) { t=(l-400.0)/(475.0-400.0); b=    +(2.20*t)-(1.50*t*t); }
        else if ((l>=475.0)&&(l<560.0)) { t=(l-475.0)/(560.0-475.0); b=0.7 -(     t)+(0.30*t*t); }
             
        return new Color((float)r,(float)g,(float)b);
    }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel_ImageName;
    private javax.swing.JPanel jPanel_PlaceHolder;
    // End of variables declaration//GEN-END:variables
}
