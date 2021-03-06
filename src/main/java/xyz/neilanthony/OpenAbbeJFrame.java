/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.ShortProcessor;
import ij.measure.Calibration;
import ij.IJ;
import ij.plugin.HyperStackConverter;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget; 
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import loci.plugins.in.ImagePlusReader;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import javax.swing.Box;
import javax.swing.BoxLayout;
import net.imglib2.display.ColorTable;
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
class OpenAbbeJFrame extends javax.swing.JFrame {
    
    private Point panelOffset = new Point();
    final UserInterface ui;
    
    JPanel jPanel_abbeFiles = null;
    private final List<JPanel> filesPanelList = new ArrayList<JPanel>();
    
    JPanel jPanel_dragDrop = new JPanel();
    
    //final private LinkedBlockingQueue<File> todoQueue = new LinkedBlockingQueue<>();
    final private ExecutorService importPool = Executors.newFixedThreadPool(4);
    private List<Future<AbbeFile>> futAbbeList = new ArrayList<Future<AbbeFile>>();
    // holds information about all files dragged on to GUI
    final Map<Integer, AbbeFile> abbeFilesMap = new HashMap<Integer, AbbeFile>();
    // cast to sychronized (abbeFilesMap) when multithreading
    
    
    /* Creates new form OpenAbbeJFrame */
    OpenAbbeJFrame(UserInterface ui) throws FormatException, ParserConfigurationException, SAXException {
//    OpenAbbeJFrame() throws FormatException, ParserConfigurationException, SAXException {
        this.ui = ui;

        initComponents();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int adjHeight = (int)((float)screenSize.height * 0.85f);
        this.setSize(1000, adjHeight);
        
//        this.jPanel_buttons.setLayout(new BoxLayout(this.jPanel_buttons, BoxLayout.Y_AXIS));
//        this.jPanel_buttons.add(createIconLabel("select-all.png"));
//        this.jPanel_buttons.add(Box.createRigidArea(new Dimension(0,10)));
//        this.jPanel_buttons.add(createIconLabel("select-none.png"));
//        this.jPanel_buttons.setBackground(UIColors.colorBkgd);
        //this.jPanel_buttons.addMouseListener(new SelectButtonsMouseEvent());
        
        jPanel_exitButton.setLayout(null);
        jPanel_exitButton.add(createIconLabel("close.png"));
        jPanel_topBar.setBackground(UIColors.colorBkgdDark);
        
        this.getContentPane().setBackground(UIColors.colorBkgd);
        
        // TODO - implement jScrollPane class with edits in place
        // scroll bar scroll speed
        jScrollPane_ImgPanels.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane_FilePanels.getVerticalScrollBar().setUnitIncrement(16);
        // set bkgd color
        jScrollPane_FilePanels.getViewport().setBackground(UIColors.colorBkgd);
        jScrollPane_ImgPanels.getViewport().setBackground(UIColors.colorBkgd);
        // scroll bar color
        jScrollPane_ImgPanels.getVerticalScrollBar().setBackground(UIColors.colorB3);
        jScrollPane_FilePanels.getVerticalScrollBar().setBackground(UIColors.colorB3);
        // change scrollbar
        jScrollPane_ImgPanels.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = UIColors.colorB4;
                //this.incrButton.
            }
        });
        jScrollPane_FilePanels.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = UIColors.colorB4;
                //this.incrButton.
            }
        });
        // add icon panel
        jPanel_dragDrop.add(createIconLabel("drag-drop-obf.png"));
        jPanel_dragDrop.setBackground(UIColors.colorBkgd);
        jScrollPane_ImgPanels.setViewportView(jPanel_dragDrop);
        
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
                    boolean newAdded = false;
                    futAbbeList.clear();
                    int filesPanelCount = filesPanelList.size();
                    AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "drop",
                            String.format("filesPanelList size: %d", filesPanelCount));
                    
                    for (File file : droppedFiles) {
                        //if ( file.getPath().endsWith(".obf") | file.getPath().endsWith(".msr") ) { 
                        if ( file.getPath().endsWith(".obf") ) { 
                            Params.FileParams fP = new Params.FileParams();
                            fP.fileName = file.toPath().getFileName().toString();
                            fP.abbeFilesMapKey = filesPanelCount;
                            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "drop",
                                    String.format("Dropped file %s; creating new AbbeFilePanel %d, fileParams: %s",
                                                    fP.fileName,
                                                    filesPanelCount,
                                                    fP.toString()));
                            filesPanelList.add(new AbbeFileJPanel(fP));
                            
                            Callable<AbbeFile> callable = new NewAbbeFile(file, filesPanelCount);
                            filesPanelCount++;
                            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "drop",
                                    String.format("Submitting %s to pool.", file.toString()));
                            Future<AbbeFile> future = importPool.submit(callable);
                            futAbbeList.add(future);
                            newAdded = true;
                        } else { jLabel_Info.setText("Only Abberior OBF files currently supported."); }
                    }
                    if (newAdded) {
                        jPanel_abbeFiles = createFilesPanel();
                        jScrollPane_FilePanels.getViewport().setViewSize(new Dimension(200,filesPanelCount*103));
                        jScrollPane_FilePanels.setViewportView(jPanel_abbeFiles);
                    }
                    Thread t = new Thread(new CheckLoadingAbbes());
                    t.start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

    }
    
    class NewAbbeFile implements Callable {
        private File fname;
        private int index;
        NewAbbeFile(File f, int idx) { this.fname = f; this.index = idx; }
        @Override
        public synchronized Object call() throws Exception {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "NewAbbeFile_call",
                                    String.format("creating new AbbeFile %s, idx %d",
                    fname.toPath().toString(), index));
            AbbeFile newAbbe = new AbbeFile(fname.toPath(),index);
            newAbbe.scanDatasetsFoldersImages();
            newAbbe.collateFolderImages();
            newAbbe.fillPanels();
            return newAbbe;
        }
    }
    
    class CheckLoadingAbbes implements Runnable {
        
        // CheckLoadingAbbes Constructor
        CheckLoadingAbbes () {   }
        
        public synchronized void run() {
            //int i = 0;
            String tDoneStrs;
            boolean allStillRunning = true;
            int abbeLen = futAbbeList.size();
            Boolean done;
            Boolean[] stillRunningLst = new Boolean[abbeLen];
            for (int n=0; n<abbeLen; n++) { stillRunningLst[n] = Boolean.TRUE; }
            while (allStillRunning) {
                tDoneStrs = "";
                for (int i=0; i<abbeLen; i++) {
                    if (stillRunningLst[i]) {
                        
                        done = futAbbeList.get(i).isDone();
                        if (done) {
                            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "CheckLoadingAbbes_run",
                                    String.format("File %d Done", i, done.toString()));
                            stillRunningLst[i] = Boolean.FALSE;
                            try {
                                // pull AbbeFile instance and place into global vector
                                synchronized (abbeFilesMap) {
                                    AbbeFile abF = futAbbeList.get(i).get(10, TimeUnit.SECONDS);
                                    AbbeFileJPanel abFP = (AbbeFileJPanel) (filesPanelList.get(abF.fParams.abbeFilesMapKey));
                                    abbeFilesMap.put(abF.fParams.abbeFilesMapKey, abF);
                                    abFP.loadingRunnable.stopThread(abF.fParams.labelsUsed);
                                    abFP.addMouseListener(new FilePanelMouseEvent());
                                    abFP.jButtonOpen.addMouseListener(new FilePanelOpenMouseEvent());
                                    abFP.ready = true;
                                    jScrollPane_ImgPanels.setViewportView(abF.abbeDatasetPanels);
                                    setAbbeFilePanelSelect(abF.fParams.abbeFilesMapKey);
                                    AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "CheckLoadingAbbes_run",
                                    String.format("abbeFilesMap added, length now: %d",abbeFilesMap.size()));
                                }
                            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                                Logger.getLogger(OpenAbbeJFrame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    tDoneStrs += String.valueOf(i) + " " + stillRunningLst[i].toString() + " ";
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(OpenAbbeJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (Arrays.asList(stillRunningLst).contains(Boolean.TRUE)) { allStillRunning = true; }
                else { allStillRunning = false; }
            }
        }
    }
    
    
    
    
    class SelectButtonsMouseEvent implements MouseListener {
        
        JPanel panel = null;
        JLabel label = null;
        
        @Override
        public void mouseClicked(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                                    "SelectButtonsMouseEvent MouseListener mouseClicked");
            this.panel = (JPanel) e.getComponent();
            this.label = (JLabel) panel.getComponentAt(e.getPoint());
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                                    String.format("%s", label.getIcon().toString()));
        }
        @Override
        public void mousePressed(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                    "SelectButtonsMouseEvent MouseListener mousePressed");
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","SelectButtonsMouseEvent MouseListener mousePressed");
            this.panel = (JPanel) e.getComponent();
            this.label = (JLabel) panel.getComponentAt(e.getPoint());
            this.label.setBackground(UIColors.colorBkgdSelected);
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","SelectButtonsMouseEvent MouseListener mouseReleased");
            this.panel = (JPanel) e.getComponent();
            this.label = (JLabel) panel.getComponentAt(e.getPoint());
            this.label.setBackground(UIColors.colorBkgd);
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","SelectButtonsMouseEvent MouseListener mouseEntered");
            this.panel = (JPanel) e.getComponent();
            this.label = (JLabel) panel.getComponentAt(e.getPoint());
            this.label.setBackground(UIColors.colorBkgdMouseOver);
        }
        @Override
        public void mouseExited(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","SelectButtonsMouseEvent MouseListener mouseExited");
            this.panel = (JPanel) e.getComponent();
            this.label = (JLabel) panel.getComponentAt(e.getPoint());
            this.label.setBackground(UIColors.colorBkgd);
        }
    }
    
    class FilePanelMouseEvent implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            //AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","FilePanelMouseEvent MouseListener mouseClicked");
        }
        @Override
        public void mousePressed(MouseEvent e) {
            //AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","FilePanelMouseEvent MouseListener mousePressed");
            AbbeFileJPanel abFP = (AbbeFileJPanel) e.getComponent();
            int idx = abFP.fP.abbeFilesMapKey;
            synchronized (abbeFilesMap) {
                if ( abFP.ready & idx < abbeFilesMap.size() ) {
                jScrollPane_ImgPanels.setViewportView(abbeFilesMap.get(idx).abbeDatasetPanels);
                setAbbeFilePanelSelect(idx);
                }
            }
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            //AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","FilePanelMouseEvent MouseListener mouseReleased");
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            //AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","FilePanelMouseEvent MouseListener mouseEntered");
        }
        @Override
        public void mouseExited(MouseEvent e) {
            //AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "","FilePanelMouseEvent MouseListener mouseExited");
        }
    }
    
    class FilePanelOpenMouseEvent implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                                    "FilePanelOpenMouseEvent MouseListener mouseClicked");
        }
        @Override
        public void mousePressed(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                                    "FilePanelOpenMouseEvent MouseListener mousePressed");
            JPanel openPanel = (JPanel) e.getComponent();
            AbbeFileJPanel abFP = (AbbeFileJPanel) openPanel.getParent();
            try {
                openSelectedDatasets(OpenAbbeJFrame.this.abbeFilesMap.get(abFP.fP.abbeFilesMapKey));
            } catch (IOException | FormatException ex) {
                Logger.getLogger(OpenAbbeJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                                    "FilePanelOpenMouseEvent MouseListener mouseReleased");
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                                    "FilePanelOpenMouseEvent MouseListener mouseEntered");
            JPanel openPanel = (JPanel) e.getComponent();
            AbbeFileJPanel abFP = (AbbeFileJPanel) openPanel.getParent();
            abFP.setOpenClick();
            
            //AbbeFileJPanel.this.jPanel_Button.add(AbbeFileJPanel.this.jLabel_OpenClick);
        }
        @Override
        public void mouseExited(MouseEvent e) {
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "",
                                    "FilePanelOpenMouseEvent MouseListener mouseExited");
            JPanel openPanel = (JPanel) e.getComponent();
            AbbeFileJPanel abFP = (AbbeFileJPanel) openPanel.getParent();
            abFP.setOpenNotClick();

        }
    }
    

    
    
    
    private JLabel createIconLabel(String fname) {
        ImageIcon imgIcon = new ImageIcon(getClass().getClassLoader().getResource(fname));
        JLabel jLabel_icon = new JLabel();
        jLabel_icon.setBounds(0,0,imgIcon.getIconWidth(),imgIcon.getIconHeight());
        jLabel_icon.setIcon(imgIcon);
        return jLabel_icon;
    }
    
    private JPanel createFilesPanel() throws IOException {
        int N = filesPanelList.size();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (int i = 0; i < N; i++) {
            p.add(filesPanelList.get(i));
            p.add(Box.createRigidArea(new Dimension(0,10)));
        }
        p.setBackground(UIColors.colorBkgdDark);
        return p;
    }
    
    
        /** TODO notifications / history class
    create class that extends JLabel
    has text area that holds previous label text
    text area to be added to the viewport jScrollPane_ImgPanels
    */
    class JHistoryLabel extends JLabel {
        TextArea textArea = new TextArea();
        
        JHistoryLabel () {
            textArea.setBackground(null);
            textArea.setFont(null);
            textArea.setForeground(null);
            textArea.setMinimumSize(new Dimension(400, jScrollPane_ImgPanels.getWidth()-10));
            
            setBackground(UIColors.colorBkgd);
            setForeground(UIColors.greyLevel(65f));
            setFont(new Font("Tahoma", Font.PLAIN, 14));
            
            
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
        jScrollPane_FilePanels = new javax.swing.JScrollPane();
        jLabel_Info = new javax.swing.JLabel();
        jButton_selectNone = new javax.swing.JButton();
        jButton_selectAll = new javax.swing.JButton();

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
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanel_exitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel_topBarLayout.setVerticalGroup(
            jPanel_topBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel_exitButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jScrollPane_ImgPanels.setBorder(null);
        jScrollPane_ImgPanels.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jScrollPane_FilePanels.setBorder(null);
        jScrollPane_FilePanels.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jLabel_Info.setFont(new java.awt.Font("Segoe UI Semilight", 0, 16)); // NOI18N
        jLabel_Info.setForeground(new java.awt.Color(204, 204, 204));
        jScrollPane_FilePanels.setViewportView(jLabel_Info);

        jButton_selectNone.setBackground(UIColors.colorBkgd);
        jButton_selectNone.setIcon(new javax.swing.ImageIcon(getClass().getResource("/select-none.png"))); // NOI18N
        jButton_selectNone.setBorder(null);
        jButton_selectNone.setBorderPainted(false);
        jButton_selectNone.setFocusPainted(false);
        jButton_selectNone.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton_selectNoneMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton_selectNoneMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton_selectNoneMouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jButton_selectNoneMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jButton_selectNoneMouseReleased(evt);
            }
        });

        jButton_selectAll.setBackground(UIColors.colorBkgd);
        jButton_selectAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/select-all.png"))); // NOI18N
        jButton_selectAll.setBorder(null);
        jButton_selectAll.setBorderPainted(false);
        jButton_selectAll.setFocusPainted(false);
        jButton_selectAll.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton_selectAllMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton_selectAllMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton_selectAllMouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jButton_selectAllMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jButton_selectAllMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane_FilePanels, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton_selectNone)
                    .addComponent(jButton_selectAll))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane_ImgPanels, javax.swing.GroupLayout.PREFERRED_SIZE, 550, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jPanel_topBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel_topBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane_FilePanels)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton_selectAll, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton_selectNone, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane_ImgPanels, javax.swing.GroupLayout.DEFAULT_SIZE, 816, Short.MAX_VALUE)))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jPanel_topBarMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel_topBarMousePressed
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "jPanel_topBarMousePressed",
                                    String.format("Pressed: %d %d", evt.getX(), evt.getY()));
        panelOffset.x = evt.getX();
        panelOffset.y = evt.getY();
    }//GEN-LAST:event_jPanel_topBarMousePressed

    private void jPanel_topBarMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel_topBarMouseDragged
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "jPanel_topBarMouseDragged",
                                    String.format("Dragging [getXYonScreen]: %d %d", evt.getXOnScreen(), evt.getYOnScreen()));
        this.setLocation(evt.getLocationOnScreen().x - panelOffset.x,
            evt.getLocationOnScreen().y - panelOffset.y);
    }//GEN-LAST:event_jPanel_topBarMouseDragged

    private void jPanel_exitButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel_exitButtonMouseReleased
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jPanel_exitButtonMouseReleased

    private void jButton_selectNoneMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectNoneMouseEntered
        evt.getComponent().setBackground(UIColors.colorBkgdMouseOver);
    }//GEN-LAST:event_jButton_selectNoneMouseEntered

    private void jButton_selectNoneMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectNoneMouseExited
        evt.getComponent().setBackground(UIColors.colorBkgd);
    }//GEN-LAST:event_jButton_selectNoneMouseExited

    private void jButton_selectNoneMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectNoneMousePressed
        evt.getComponent().setBackground(UIColors.colorBkgdSelected);
    }//GEN-LAST:event_jButton_selectNoneMousePressed

    private void jButton_selectNoneMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectNoneMouseReleased
        evt.getComponent().setBackground(UIColors.colorBkgdMouseOver);
    }//GEN-LAST:event_jButton_selectNoneMouseReleased

    private void jButton_selectNoneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectNoneMouseClicked
        unselectDatasetsPanels();
        //evt.getComponent().setBackground(UIColors.colorBkgdSelected);
        // TODO select none code here
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "jButton_selectNoneMouseClicked",
                                    "Select None Mouse Clicked");
    }//GEN-LAST:event_jButton_selectNoneMouseClicked

    private void jButton_selectAllMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectAllMouseClicked
        //evt.getComponent().setBackground(UIColors.colorBkgdSelected);
        // TODO select all code here
        //((OpenAbbeJFrame)evt.getComponent().getParent()).
        selectAllDatasetsPanels();
        AbbeLogging.postToLog(Level.FINEST, this.getClass().toString(), "jButton_selectAllMouseClicked",
                                    "Select All Mouse Clicked");
    }//GEN-LAST:event_jButton_selectAllMouseClicked

    private void jButton_selectAllMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectAllMouseEntered
        evt.getComponent().setBackground(UIColors.colorBkgdMouseOver);
    }//GEN-LAST:event_jButton_selectAllMouseEntered

    private void jButton_selectAllMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectAllMouseExited
        evt.getComponent().setBackground(UIColors.colorBkgd);
    }//GEN-LAST:event_jButton_selectAllMouseExited

    private void jButton_selectAllMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectAllMousePressed
        evt.getComponent().setBackground(UIColors.colorBkgdSelected);
    }//GEN-LAST:event_jButton_selectAllMousePressed

    private void jButton_selectAllMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_selectAllMouseReleased
        evt.getComponent().setBackground(UIColors.colorBkgdMouseOver);
    }//GEN-LAST:event_jButton_selectAllMouseReleased
    
    
    
    
//    static void openSingleDataset (int dsIdx, AbbeFile abFile,
//                            byte[] r, byte[] g, byte[] b, Params.ImageParams imgParams) throws FormatException, IOException {
//        
//        ImageStack imgStk = new ImageStack();
//        List luts = new ArrayList<LUT>();
//
//        for (int i = 0; i < abDs.incChns.size(); i++) {
//            int chn = abDs.incChns.get(i);
//            AbbeFile.AbbeFolder.AbbeDataset.AbbeImage abImg = abDs.abbeImagesVect.get(chn);
//
//            imgParams = abImg.imgParams;
//            if (abImg.ctSize != 256) {
//                // create grey scale
//                for (int k = 0; k < 256; k++) {
//                    r[k] = (byte) k;
//                    g[k] = (byte) k;
//                    b[k] = (byte) k;
//                }
//            } else {
//                for (int k = 0; k < 256; k++) {
//                    r[k] = (byte) abImg.colorTable[k].getRed();
//                    g[k] = (byte) abImg.colorTable[k].getGreen();
//                    b[k] = (byte) abImg.colorTable[k].getBlue();
//                }
//            }
//            luts.add(new LUT(r, g, b));
//
//            // for each z
//            // for each t
//            // data is inheriently each c for abberior as far as I've seen
//            abFile.reader.setSeries(abImg.bfIndex);
//            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
//                                    String.format("%s %s", abFile.fParams.fileName, abFile.reader.getDimensionOrder()));
//            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
//                                    String.format("%s", abFile.reader.getDatasetStructureDescription()));
//            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
//                                    String.format("EffectiveSizeC: %d", abFile.reader.getEffectiveSizeC()));
//            AbbeLogging.postToLog(Level.FINE, "AbbeFile", "openSingleDataset",
//                                    String.format("ImageCount: %d", abFile.reader.getImageCount()) );
//
//            for(int t = 0; t < imgParams.st; t++) {
//                for (int z = 0; z < imgParams.sz; z++) {
//                    Object dat = abFile.reader.openPlane(z+(t*imgParams.sz), 0, 0, imgParams.sx, imgParams.sy);
//                    byte[] bytes = (byte[])dat;
//                    short[] data = new short[bytes.length/2];
//                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
//                    ShortProcessor sp = new ShortProcessor(imgParams.sx, imgParams.sy, data, null);
//                    imgStk.addSlice(sp);
//                }
//            }
//            // if needed for speed in big stacks timelapses
//            // investigate setPixels(java.lang.Object pixels)
//            // create empty ShortProcessor and add dat in setPixels
//        }
//
//        Calibration cali = new Calibration();
//        cali.setUnit("micron");
//        cali.pixelWidth = imgParams.dx;
//        cali.pixelHeight = imgParams.dy;
//        cali.pixelDepth = imgParams.dz;
//        cali.frameInterval = imgParams.dt;
//
//        ImagePlus imp = new ImagePlus();
//
//        imp.setStack(String.format("%s-TS%d", abFldr.folderName, abDs.timeStampIdx), imgStk);
//        //imp.setDimensions(abDs.incChns.size(), imgParams.sz, imgParams.st);
//        if (abDs.incChns.size() > 1 | imgParams.sz > 1 | imgParams.st > 1) {
//            imp = HyperStackConverter.toHyperStack(imp, abDs.incChns.size(),
//                                                    imgParams.sz, imgParams.st, "xyztc", "composite");
//        }
//
//        imp.setCalibration(cali);
//        imp = new CompositeImage(imp, CompositeImage.COMPOSITE);
//        for (int i = 0; i < abDs.incChns.size(); i++) {
//            ((CompositeImage)imp).setChannelLut((LUT)luts.get(i), i+1);
//        }
//        imp.setOpenAsHyperStack(true);
//        imp.show();
//        // TODO: add optional opening of rescue and dymin masks...  seperate ImagePlus
//    }
    
    void openSelectedDatasets (AbbeFile abFile) throws IOException, FormatException {
        
        Component[] cmpnts = abFile.abbeDatasetPanels.getComponents();
        Set toOpenDs = new HashSet<>();
        
        for (int i=0; i<cmpnts.length; i++) {
            JPanel jp = (JPanel) cmpnts[i];
            int idx = ((AbbeDatasetJPanel)jp).p.bfDatasetIdx;
            boolean selected = ((AbbeDatasetJPanel)jp).p.panelSelected;
            AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "openSelectedDatasets",
                                    String.format("Dataset Index: %d, selected: %b", idx, selected));
            if ( selected ) { toOpenDs.add(idx); }
        }
        
        // for LUTs
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        
        Params.ImageParams imgParams = null;
        
        for (AbbeFile.AbbeFolder abFldr : abFile.abbeFolderVect) {
            
            for (AbbeFile.AbbeFolder.AbbeDataset abDs : abFldr.abbeDatasetVect) {
                // TODO: create inverse of incChns - these will be adaptive illumination (AI) masks/data
                if (toOpenDs.contains(abDs.bfDatasetIdx) & abDs.addToPanel) {
                    abFile.openSingleDataset(abFldr.bfFldrIdx, abDs.datasetID, r, g, b, imgParams);
                    
//                    ImageStack imgStk = new ImageStack();
//                    List luts = new ArrayList<LUT>();
//
//                    for (int i = 0; i < abDs.incChns.size(); i++) {
//                        int chn = abDs.incChns.get(i);
//                        AbbeFile.AbbeFolder.AbbeDataset.AbbeImage abImg = abDs.abbeImagesVect.get(chn);
//                        
//                        imgParams = abImg.imgParams;
//                        if (abImg.ctSize != 256) {
//                            // create grey scale
//                            for (int k = 0; k < 256; k++) {
//                                r[k] = (byte) k;
//                                g[k] = (byte) k;
//                                b[k] = (byte) k;
//                            }
//                        } else {
//                            for (int k = 0; k < 256; k++) {
//                                r[k] = (byte) abImg.colorTable[k].getRed();
//                                g[k] = (byte) abImg.colorTable[k].getGreen();
//                                b[k] = (byte) abImg.colorTable[k].getBlue();
//                            }
//                        }
//                        luts.add(new LUT(r, g, b));
//
//                        // for each z
//                        // for each t
//                        // data is inheriently each c for abberior as far as I've seen
//                        abFile.reader.setSeries(abImg.bfIndex);
//                        logger.log(Level.FINE, String.format("%s %s", abFile.fParams.fileName, abFile.reader.getDimensionOrder()));
//                        logger.log(Level.FINE, String.format("%s", abFile.reader.getDatasetStructureDescription()));
//                        logger.log(Level.FINE, String.format("EffectiveSizeC: %d", abFile.reader.getEffectiveSizeC()));
//                        logger.log(Level.FINE, String.format("ImageCount: %d", abFile.reader.getImageCount()) );
//                        
//                        for(int t = 0; t < imgParams.st; t++) {
//                            for (int z = 0; z < imgParams.sz; z++) {
//                                Object dat = abFile.reader.openPlane(z+(t*imgParams.sz), 0, 0, imgParams.sx, imgParams.sy);
//                                byte[] bytes = (byte[])dat;
//                                short[] data = new short[bytes.length/2];
//                                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
//                                ShortProcessor sp = new ShortProcessor(imgParams.sx, imgParams.sy, data, null);
//                                imgStk.addSlice(sp);
//                            }
//                        }
//                        // if needed for speed in big stacks timelapses
//                        // investigate setPixels(java.lang.Object pixels)
//                        // create empty ShortProcessor and add dat in setPixels
//                    }
//
//                    Calibration cali = new Calibration();
//                    cali.setUnit("micron");
//                    cali.pixelWidth = imgParams.dx;
//                    cali.pixelHeight = imgParams.dy;
//                    cali.pixelDepth = imgParams.dz;
//                    cali.frameInterval = imgParams.dt;
//                    
//                    ImagePlus imp = new ImagePlus();
//                    
//                    imp.setStack(String.format("%s-TS%d", abFldr.folderName, abDs.timeStampIdx), imgStk);
//                    //imp.setDimensions(abDs.incChns.size(), imgParams.sz, imgParams.st);
//                    if (abDs.incChns.size() > 1 | imgParams.sz > 1 | imgParams.st > 1) {
//                        imp = HyperStackConverter.toHyperStack(imp, abDs.incChns.size(),
//                                                                imgParams.sz, imgParams.st, "xyztc", "composite");
//                    }
//                    
//                    imp.setCalibration(cali);
//                    imp = new CompositeImage(imp, CompositeImage.COMPOSITE);
//                    for (int i = 0; i < abDs.incChns.size(); i++) {
//                        ((CompositeImage)imp).setChannelLut((LUT)luts.get(i), i+1);
//                    }
//                    imp.setOpenAsHyperStack(true);
//                    imp.show();
                    // TODO: add optional opening of rescue and dymin masks...  seperate ImagePlus
                }
            }
        }
        unselectDatasetsPanels();
    }
    
    private void getSelectedFilePanels () {
        boolean selected;
        int fileMapKey;
        
        for (JPanel abFileJP : filesPanelList) {
            selected = ((AbbeFileJPanel)abFileJP).fileSelected();
            if(selected) {
                // get index for abbeFileVect
                //AbbeFilePanel abFP = 
                fileMapKey = ((AbbeFileJPanel)abFileJP).fP.abbeFilesMapKey;
                AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "getSelectedFilePanels",
                                    String.format("fileMapKey: %d", fileMapKey));
            }
        }
    }
    
    private void getSelectedDatasetsPanels () {
        boolean selected;
        int fileMapKey;
        
        for (JPanel abFileJP : filesPanelList) {
            selected = ((AbbeFileJPanel)abFileJP).fileSelected();
            if(selected) {
                // get index for abbeFileVect
                //AbbeFilePanel abFP = 
                fileMapKey = ((AbbeFileJPanel)abFileJP).fP.abbeFilesMapKey;
                AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "getSelectedDatasetsPanels",
                                    String.format("fileMapKey: %d", fileMapKey));
            }
        }
    }
    
    private void setSelectedDatasetsPanels (int dsPanelIndex) {
        
    }

    private void unselectDatasetsPanels () {
        boolean selected;
        int fileMapKey;
        
        for (JPanel abFileJP : filesPanelList) {
            selected = ((AbbeFileJPanel)abFileJP).fileSelected();
            if(selected) {
                fileMapKey = ((AbbeFileJPanel)abFileJP).fP.abbeFilesMapKey;
                for (Component abDatasetJP : ((AbbeFile)abbeFilesMap.get(fileMapKey)).abbeDatasetPanels.getComponents()) {
                    ((AbbeDatasetJPanel)abDatasetJP).unselectPanel();
                }
                
            }
        }
    }
    private void selectAllDatasetsPanels () {
        boolean selected;
        int fileMapKey;
        
        for (JPanel abFileJP : filesPanelList) {
            selected = ((AbbeFileJPanel)abFileJP).fileSelected();
            if(selected) {
                fileMapKey = ((AbbeFileJPanel)abFileJP).fP.abbeFilesMapKey;
                for (Component abDatasetJP : ((AbbeFile)abbeFilesMap.get(fileMapKey)).abbeDatasetPanels.getComponents()) {
                    ((AbbeDatasetJPanel)abDatasetJP).selectPanel();
                }
                
            }
        }
    }
    void setAbbeFilePanelSelect(int selectedIdx) {
        JPanel panel = null;
        AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "setAbbeFilePanelSelect",
                                    String.format(
                    "OpenAbbeJFrame.filesPanelList size: %d", this.filesPanelList.size()));
        for (int i = 0; i < this.filesPanelList.size(); i++) {
            panel = this.filesPanelList.get(i);
            if (i == selectedIdx) {
                ( (AbbeFileJPanel)panel ).setPanelSelected();
                AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "setAbbeFilePanelSelect",
                                    String.format(
                    "filesPanelList idx: %d, file: %s -> selected",
                        i,
                        ( (AbbeFileJPanel)panel ).fP.fileName));
            } else {
                ( (AbbeFileJPanel)panel ).setPanelUnselected();
                AbbeLogging.postToLog(Level.FINE, this.getClass().toString(), "setAbbeFilePanelSelect",
                                    String.format(
                    "filesPanelList idx: %d, file: %s -> unselected",
                        i,
                        ( (AbbeFileJPanel)panel ).fP.fileName));
            }
        }
    }
        
    void fileFolderDatasetImage () {
        
        getSelectedFilePanels();
        
//        for (AbbeFile abFile : abbeFilesMap) {
//            for (AbbeFile.AbbeFolder abFldr : abFile.abbeFolderVect) {
//                
//                logger.log(Level.FINE, abFldr.fldrImgIDs.toString());
//                logger.log(Level.FINE, abFldr.abbeDatasetVect.toString());
//                
//                for (AbbeFile.AbbeFolder.AbbeDataset abDs : abFldr.abbeDatasetVect) {
//                    
//                    logger.log(Level.FINE, abDs.abbeImagesVect.toString());
//                    logger.log(Level.FINE, abDs.pParams.toString());
//                    
//                    for (AbbeFile.AbbeFolder.AbbeDataset.AbbeImage abImg : abDs.abbeImagesVect) {
//                        
//                        logger.log(Level.FINE, abImg.imgParams.toString());
//                        
//                    }
//                }
//            }
//        }
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    static void main(String args[]) {
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
    private javax.swing.JButton jButton_selectAll;
    private javax.swing.JButton jButton_selectNone;
    private javax.swing.JLabel jLabel_Info;
    private javax.swing.JPanel jPanel_exitButton;
    private javax.swing.JPanel jPanel_topBar;
    private javax.swing.JScrollPane jScrollPane_FilePanels;
    private javax.swing.JScrollPane jScrollPane_ImgPanels;
    // End of variables declaration//GEN-END:variables
}
