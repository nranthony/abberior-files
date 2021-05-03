/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import loci.formats.FormatException;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.xml.sax.SAXException;

/**
 *
 * @author nelly
 */
public class OpenAbbeJFrame extends javax.swing.JFrame {

    private Point panelOffset = new Point();
    final UserInterface ui;
    
    //final private LinkedBlockingQueue<File> todoQueue = new LinkedBlockingQueue<>();
    final private ExecutorService importPool = Executors.newFixedThreadPool(4);
    private List<Future<AbbeFile>> futAbbeList = new ArrayList<Future<AbbeFile>>();
    
    // holds information about all files dragged on to GUI
    public final Vector<AbbeFile> abbeFilesVect = new Vector<>();
    // cast to sychronized (abbeFilesVect) when multithreading
    
    private Color colorBkgd = Color.getHSBColor(0.0f, 0.0f, 0.10f);
    private Color colorBkgdDark = Color.getHSBColor(0.0f, 0.0f, 0.06f);
    private Color colorB4 = Color.getHSBColor(0.0f, 0.0f, 0.4f);
    private Color colorB3 = Color.getHSBColor(0.0f, 0.0f, 0.3f);
    private Color colorB2 = Color.getHSBColor(0.0f, 0.0f, 0.2f);
    private Color greyLevel (float level) {
        Color col; return col = Color.getHSBColor(0.0f, 0.0f, (level * 0.01f));
    }
     
    private final List<JPanel> panelList = new ArrayList<JPanel>();
    
    /* Creates new form OpenAbbeJFrame */
    public OpenAbbeJFrame(UserInterface ui) throws IOException, FormatException, ParserConfigurationException, SAXException {
        this.ui = ui;
        
        initComponents();
        
        ImageIcon imgIcon_exit = new ImageIcon("src/main/resources/close.png");
        JLabel jLabel_exit = new JLabel();
        jLabel_exit.setBounds(0,0,imgIcon_exit.getIconWidth(),imgIcon_exit.getIconHeight());
        jLabel_exit.setIcon(imgIcon_exit);
        
        jPanel_exitButton.setLayout(null);
        jPanel_exitButton.add(jLabel_exit);
        jPanel_topBar.setBackground(colorBkgdDark);
        
        this.getContentPane().setBackground(colorBkgd);
        
        // TODO - implement jScrollPane class with edits in place
        // scroll bar scroll speed
        jScrollPane_ImgPanels.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane_FilePanels.getVerticalScrollBar().setUnitIncrement(16);
        // set bkgd color
        jScrollPane_FilePanels.getViewport().setBackground(colorBkgd);
        jScrollPane_ImgPanels.getViewport().setBackground(colorBkgd);
        // scroll bar color
        jScrollPane_ImgPanels.getVerticalScrollBar().setBackground(colorB3);
        jScrollPane_FilePanels.getVerticalScrollBar().setBackground(colorB3);
        // change scrollbar
        jScrollPane_ImgPanels.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = colorB4;
                //this.incrButton.
            }
        });
        jScrollPane_FilePanels.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = colorB4;
                //this.incrButton.
            }
        });
        
        jPanel_topBar.setVisible(true);
        jPanel_exitButton.setVisible(true);
        
        panelOffset.x = 0;
        panelOffset.y = 0;
        
        this.setDropTarget(new DropTarget() {
            @SuppressWarnings("empty-statement")
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>)
                        evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    
                    futAbbeList.clear();
                    for (File file : droppedFiles) {
                        if ( file.getPath().endsWith(".obf") | file.getPath().endsWith(".msr") ) { 
                            Callable<AbbeFile> callable = new NewAbbeFile(file);
                            System.out.println(String.format("Submitting %s to pool.", file.toString()));
                            Future<AbbeFile> future = importPool.submit(callable);
                            futAbbeList.add(future);
                        } else { jLabel_Info.setText("Non Abberior files currently not supported.  Please drop .obf or .msr files."); }
                    }
                    Thread t = new Thread(new CheckLoadingAbbes());
                    t.start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        JPanel imagesPanel = this.createImagesPanel();
        jScrollPane_ImgPanels.setViewportView(imagesPanel);
    }
    
    private JPanel createImagesPanel() throws IOException {
        int N = 6;
        Params.PanelParams pParams = new Params.PanelParams();
        JPanel p = new JPanel(new GridLayout(N, 1));
        for (int i = 0; i < N; i++) {
            int row = i;
            int col = 1;
            JPanel imgPanel = new AbbeImageJPanel(pParams);
            panelList.add(imgPanel);
            p.add(imgPanel);
        }
        return p;
    }
    
    
    /* TODO
    create class that extends JLabel
    has text area that holds previous label text
    text area to be added to the viewport jScrollPane_ImgPanels
    */
    public class JHistoryLabel extends JLabel {
        TextArea textArea = new TextArea();
        
        JHistoryLabel () {
            textArea.setBackground(null);
            textArea.setFont(null);
            textArea.setForeground(null);
            textArea.setMinimumSize(new Dimension(400, jScrollPane_ImgPanels.getWidth()-10));
            
            setBackground(colorBkgd);
            setForeground(greyLevel(65f));
            setFont(new Font("Tahoma", Font.PLAIN, 14));
            
            
        }
    }
        
    public class NewAbbeFile implements Callable {
        private File fname;
        public NewAbbeFile(File f) { this.fname = f; }
        @Override
        public Object call() throws Exception {
            System.out.println("NewAbbeFile Callable; creating new AbbeFile");
            AbbeFile newAbbe = new AbbeFile(fname.toPath());
            System.out.println("NewAbbeFile Callable; scanFoldersDatasets");
            newAbbe.scanFoldersDatasets();
            System.out.println("NewAbbeFile Callable; collateFolderImages");
            newAbbe.collateFolderImages();
            System.out.println("NewAbbeFile Callable; fillPanels");
            newAbbe.fillPanels();
            System.out.println("NewAbbeFile Callable; returning newAbbe");
            return newAbbe;
        }
    }
    
    public class CheckLoadingAbbes implements Runnable {
        
        // CheckLoadingAbbes Constructor
        public CheckLoadingAbbes () {   }
        
        public void run() {
            int i = 0;
            String tDoneStrs;
            boolean allStillRunning = true;
            int abbeLen = futAbbeList.size();
            Boolean done;
            Boolean[] stillRunningLst = new Boolean[abbeLen];
            for (i=0; i<abbeLen; i++) { stillRunningLst[i] = Boolean.TRUE; }
            while (allStillRunning) {
                tDoneStrs = "";
                for (i=0; i<futAbbeList.size(); i++) {
                    if (stillRunningLst[i]) {
                        System.out.println(String.format("File %d Still Running: %s", i, stillRunningLst[i].toString()));
                        done = futAbbeList.get(i).isDone();
                        if (done) {
                            System.out.println(String.format("File %d Done", i, done.toString()));
                            stillRunningLst[i] = Boolean.FALSE;
                            try {
                                // pull AbbeFile instance and place into global vector
                               synchronized (abbeFilesVect) {
                                abbeFilesVect.add(futAbbeList.get(i).get(10, TimeUnit.SECONDS));
                               }
                            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                                Logger.getLogger(OpenAbbeJFrame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    tDoneStrs += String.valueOf(i) + " " + stillRunningLst[i].toString() + " ";
                }
                System.out.println("CheckLoadingAbbes: " + tDoneStrs);
                try {
                    
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(OpenAbbeJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (Arrays.asList(stillRunningLst).contains(Boolean.FALSE)) {
                    allStillRunning = false;
                }
            }
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel_topBar = new javax.swing.JPanel();
        jPanel_exitButton = new javax.swing.JPanel();
        jScrollPane_ImgPanels = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jScrollPane_FilePanels = new javax.swing.JScrollPane();
        jLabel_Info = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(32, 32, 32));
        setUndecorated(true);

        jPanel_topBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jPanel_topBarMouseDragged(evt);
            }
        });
        jPanel_topBar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jPanel_topBarMousePressed(evt);
            }
        });

        jPanel_exitButton.setPreferredSize(new java.awt.Dimension(23, 22));
        jPanel_exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jPanel_exitButtonMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel_exitButtonLayout = new javax.swing.GroupLayout(jPanel_exitButton);
        jPanel_exitButton.setLayout(jPanel_exitButtonLayout);
        jPanel_exitButtonLayout.setHorizontalGroup(
            jPanel_exitButtonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );
        jPanel_exitButtonLayout.setVerticalGroup(
            jPanel_exitButtonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 22, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel_topBarLayout = new javax.swing.GroupLayout(jPanel_topBar);
        jPanel_topBar.setLayout(jPanel_topBarLayout);
        jPanel_topBarLayout.setHorizontalGroup(
            jPanel_topBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel_topBarLayout.createSequentialGroup()
                .addGap(0, 883, Short.MAX_VALUE)
                .addComponent(jPanel_exitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel_topBarLayout.setVerticalGroup(
            jPanel_topBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel_exitButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jScrollPane_ImgPanels.setBorder(null);
        jScrollPane_ImgPanels.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane_ImgPanels.setViewportView(jTextArea1);

        jScrollPane_FilePanels.setBorder(null);
        jScrollPane_FilePanels.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jLabel_Info.setFont(new java.awt.Font("Segoe UI Semilight", 0, 16)); // NOI18N
        jLabel_Info.setForeground(new java.awt.Color(204, 204, 204));
        jLabel_Info.setText("Drag 'n' drop *.obf or *.msr files.");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel_topBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane_FilePanels)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane_ImgPanels, javax.swing.GroupLayout.PREFERRED_SIZE, 667, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel_Info, javax.swing.GroupLayout.PREFERRED_SIZE, 661, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel_topBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel_Info, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_ImgPanels, javax.swing.GroupLayout.PREFERRED_SIZE, 865, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane_FilePanels, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 895, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jPanel_topBarMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel_topBarMousePressed
        //        System.out.println(String.format("Pressed: %d %d", evt.getX(), evt.getY()));
        panelOffset.x = evt.getX();
        panelOffset.y = evt.getY();
    }//GEN-LAST:event_jPanel_topBarMousePressed

    private void jPanel_topBarMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel_topBarMouseDragged
        //        System.out.println(String.format("Dragging [getXYonScreen]: %d %d", evt.getXOnScreen(), evt.getYOnScreen()));
        this.setLocation(evt.getLocationOnScreen().x - panelOffset.x,
            evt.getLocationOnScreen().y - panelOffset.y);
    }//GEN-LAST:event_jPanel_topBarMouseDragged

    private void jPanel_exitButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel_exitButtonMouseReleased
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jPanel_exitButtonMouseReleased
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OpenAbbeJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OpenAbbeJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OpenAbbeJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OpenAbbeJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
    }

    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel_Info;
    private javax.swing.JPanel jPanel_exitButton;
    private javax.swing.JPanel jPanel_topBar;
    private javax.swing.JScrollPane jScrollPane_FilePanels;
    private javax.swing.JScrollPane jScrollPane_ImgPanels;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
