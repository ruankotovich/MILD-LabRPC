/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labrpc.secondquestion;

import labrpc.secondquestion.model.MessageHandler;
import labrpc.secondquestion.model.AbstractFolder;
import labrpc.secondquestion.model.ProgressListener;
import labrpc.secondquestion.model.PennyroyalPair;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import labrpc.secondquestion.model.DMAHandle;
import labrpc.secondquestion.model.SocketHandle;
import labrpc.secondquestion.model.Zipper;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author dmitry
 */
public class Client extends javax.swing.JFrame {

    private Icon OPENED_FOLDER_ICON, CLOSED_FOLDER_ICON;
    private DefaultMutableTreeNode root;

    private final DefaultTreeModel treeModel;
    private final DefaultTreeCellRenderer renderer;
//    private final ProgressListener pListener;
    private final DefaultTableModel tableModel;
    private final TreeMap<String, String> folderMap;
    private final TreeSet<String> choosenFileNames;
    private final String SERVER_VIRTUAL_DRIVE_NAME = "virtual://root";
    private String SERVER_ADDRESS = "10.208.6.62";
    private int SERVER_PORT = 6464;
    private final int BLOCK_SIZE = 4096;
    private final File FILE_RECEIVED_RAW = new File("received/raw");
    private final File FILE_RECEIVED_VIRTUAL = new File("received/virtual");
    private final Color AUTO_REFRESH_BACKGROUND_COLOR;

    private String beforeEditValue;
    private SocketHandle socketHandle;

    public Client() {
        initComponents();
        
        {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("client_config.json")));
                StringBuilder stringBuffer = new StringBuilder();
                while (bufferedReader.ready()) {
                    stringBuffer.append(bufferedReader.readLine());
                }
                JSONObject jsonObject = new JSONObject(stringBuffer.toString());

                if (jsonObject.has("server")) {
                    JSONObject serverConfig = jsonObject.getJSONObject("server");
                    SERVER_ADDRESS = serverConfig.getString("ip");
                    SERVER_PORT = serverConfig.getInt("port");
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid configuration file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(null, "Configuration file not found, using default settings.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Configuration file couldn't be readen, using default settings.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        FILE_RECEIVED_RAW.mkdirs();
        FILE_RECEIVED_VIRTUAL.mkdir();

        try {
            this.socketHandle = new SocketHandle(SERVER_ADDRESS, SERVER_PORT);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Conection Refused!\n" + ex.toString() + "\n\nMake sure the server is open", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try {
            OPENED_FOLDER_ICON = new ImageIcon(ImageIO.read(Client.class.getResource("/labrpc/secondquestion/gfx/opened.png").openStream()).getScaledInstance(16, 16, 0));
            CLOSED_FOLDER_ICON = new ImageIcon(ImageIO.read(Client.class.getResource("/labrpc/secondquestion/gfx/closed.png").openStream()).getScaledInstance(16, 16, 0));
           this.setIconImage(ImageIO.read(Client.class.getResource("/labrpc/secondquestion/gfx/carrotida.png").openStream()));
           this.setTitle("CarroTIDA :: Client");
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

        folderMap = new TreeMap();
        choosenFileNames = new TreeSet<>();

        renderer = (DefaultTreeCellRenderer) jTree1.getCellRenderer();
        treeModel = (DefaultTreeModel) jTree1.getModel();
        tableModel = (DefaultTableModel) jTableQueue.getModel();

        renderer.setOpenIcon(OPENED_FOLDER_ICON);
        renderer.setClosedIcon(CLOSED_FOLDER_ICON);
        renderer.setLeafIcon(CLOSED_FOLDER_ICON);

        AUTO_REFRESH_BACKGROUND_COLOR = jBtoggleAutoRefresh.getBackground();

        jTableQueue.addPropertyChangeListener((PropertyChangeEvent e) -> {
            if ("tableCellEditor".equals(e.getPropertyName())) {
                if (jTableQueue.isEditing()) {
                    beforeEditValue = tableModel.getValueAt(jTableQueue.getSelectedRow(), jTableQueue.getSelectedColumn()).toString();
                    System.out.println("Editing " + beforeEditValue);
                } else {
                    String currentValue = tableModel.getValueAt(jTableQueue.getSelectedRow(), jTableQueue.getSelectedColumn()).toString();
                    System.out.println("Set to " + currentValue);

                    if (choosenFileNames.contains(currentValue) && !beforeEditValue.equals(currentValue)) {
                        JOptionPane.showMessageDialog(null, "This filename has already been choosen.", "Error", JOptionPane.ERROR_MESSAGE);
                        jTableQueue.setValueAt(beforeEditValue, jTableQueue.getSelectedRow(), jTableQueue.getSelectedColumn());
                    } else {
                        choosenFileNames.add(currentValue);
                    }

                }
            }
        });

//        pListener = new ProgressListener() {
//            @Override
//            public void notifyProgress(Object obj, int amount) {
//                jProgressBar1.setValue(amount);
//                jFileNameHandler.setText(obj.toString());
//            }
//
//            @Override
//            public void then(Object obj) {
//                jProgressLabel.setText(obj.toString());
//                jFileNameHandler.setText("");
//            }
//
//            @Override
//            public void onStart(Object obj) {
//                jProgressLabel.setText(obj.toString());
//                jFileNameHandler.setText("Precaching...");
//            }
//
//            @Override
//            public void clear() {
//                jProgressBar1.setValue(0);
//                jFileNameHandler.setText("");
//                jProgressLabel.setText("");
//            }
//
//            @Override
//            public void dmaSend(Object obj, long length) {
//                try {
//                    File file = new File(FILE_RECEIVED_RAW.getAbsolutePath() + "/" + obj.toString());
//                    FileOutputStream fOutputStream = new FileOutputStream(file.getAbsoluteFile());
//                    byte[] buffer = new byte[BLOCK_SIZE];
//                    pListener.onStart("Receiving file...");
//                    for (int curLength, accumulator = 0; accumulator < length; fOutputStream.write(buffer, 0, curLength)) {
//                        pListener.notifyProgress("Receiving parts from " + accumulator + " to " + length, (int) ((accumulator / (float) length) * 100));
//                        accumulator += (curLength = socketHandle.getDataInputStream().read(buffer));
//                    }
//
//                    pListener.then("File received.");
//
//                    fOutputStream.close();
//                    Zipper.unzip(file, FILE_RECEIVED_VIRTUAL, pListener);
//
//                } catch (FileNotFoundException ex) {
//                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (IOException ex) {
//                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        };
        this.setLocationRelativeTo(null);

        try {
            listRemoteFolder();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Conection Refused!\n" + ex.toString() + "\n\nMake sure the server is open", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void listRemoteFolder() throws IOException {
        ((AbstractFolder) root.getUserObject()).setCanonicalName(SERVER_ADDRESS + ":" + SERVER_PORT).setCanonicalPath(SERVER_VIRTUAL_DRIVE_NAME);
        root.removeAllChildren();
        treeModel.nodeChanged(root);

        socketHandle.getDataOutputStream().writeUTF(new JSONObject().put("command", MessageHandler.ConnectionMessage.REQUEST_LIST.toString()).toString());
        populateTree(socketHandle.getDataInputStream().readUTF());

        treeModel.reload();
    }

    int unlockCount = 0;

    private synchronized void tryUnlock() {
        --unlockCount;
        if (unlockCount <= 0) {
            jBdownload.setEnabled(true);
            jBadd.setEnabled(true);
            jBremove.setEnabled(true);
            jBforceRefresh.setEnabled(true);
            jBtoggleAutoRefresh.setEnabled(true);
            tableModel.setRowCount(0);
        }
    }

    private void yieldDownloads() {
        jBdownload.setEnabled(false);
        jBadd.setEnabled(false);
        jBremove.setEnabled(false);
        jBforceRefresh.setEnabled(false);
        jBtoggleAutoRefresh.setEnabled(false);
        unlockCount = tableModel.getRowCount();

        for (int i = 0; i < tableModel.getRowCount(); ++i) {
            try {

                DMAHandle dmaHandle = new DMAHandle(new SocketHandle(SERVER_ADDRESS, SERVER_PORT), i, tableModel, FILE_RECEIVED_RAW, FILE_RECEIVED_VIRTUAL, BLOCK_SIZE);
                JSONObject requisition = new JSONObject();
                requisition.put("command", MessageHandler.ConnectionMessage.REQUEST_DOWNLOAD.toString());

                JSONArray jsonArray = new JSONArray();

                String fileName = (String) tableModel.getValueAt(i, 0);
                String fileOutputAlias = (String) tableModel.getValueAt(i, 1);

                JSONObject jsonObjectInner = new JSONObject()
                        .put("fileName", fileName)
                        .put("fileOutputAlias", fileOutputAlias);
                jsonArray.put(jsonObjectInner);

                requisition.put("parameters", jsonArray);

                dmaHandle.getSocket().getDataOutputStream().writeUTF(requisition.toString());

                new Thread(() -> {

                    try {
                        String command;
                        do {

                            JSONObject jsonObject = new JSONObject(dmaHandle.getSocket().getDataInputStream().readUTF());

                            if (jsonObject.has("command") && jsonObject.has("parameters")) {
                                JSONArray parameters = jsonObject.getJSONArray("parameters");
                                command = jsonObject.getString("command");

                                if (parameters.length() > 0) {
                                    switch (parameters.getString(0)) {
                                        case "notifyProgress": {
                                            dmaHandle.getListener().notifyProgress(parameters.getString(1), parameters.getInt(2));
                                        }
                                        break;
                                        case "then": {
                                            dmaHandle.getListener().then(parameters.getString(1));
                                        }
                                        break;
                                        case "onStart": {
                                            dmaHandle.getListener().onStart(parameters.getString(1));
                                        }
                                        break;

                                        case "clear": {
                                            dmaHandle.getListener().clear();
                                        }
                                        break;

                                        case "dmaSend": {
                                            dmaHandle.getListener().dmaSend(parameters.getString(1), parameters.getLong(2));
                                        }
                                        break;
                                    }
                                }

                            } else {
                                break;
                            }

                        } while (command == null ? MessageHandler.ConnectionMessage.OVER.toString() != null : !command.equals(MessageHandler.ConnectionMessage.OVER.toString()));

                    } catch (IOException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    tryUnlock();
                }).start();

//                socketHandle.getDataOutputStream().writeUTF(requisition.toString());
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /*
    private void downloadSelectedFiles() throws IOException {
        jBdownload.setEnabled(false);
        jBadd.setEnabled(false);
        jBremove.setEnabled(false);
        jBforceRefresh.setEnabled(false);
        jBtoggleAutoRefresh.setEnabled(false);
        jTableQueue.setEnabled(false);

        JSONObject requisition = new JSONObject();
        requisition.put("command", MessageHandler.ConnectionMessage.REQUEST_DOWNLOAD.toString());

        JSONArray jsonArray = new JSONArray();

        int i = jTableQueue.getSelectedRow();

        if (i < 0) {
            if (jTableQueue.getRowCount() > 0) {
                i = 0;
            } else {
                JOptionPane.showMessageDialog(null, "There's nothing to download yet.", "Erro", JOptionPane.ERROR_MESSAGE);
                jBdownload.setEnabled(true);
                jBadd.setEnabled(true);
                jBremove.setEnabled(true);
                jBforceRefresh.setEnabled(true);
                jBtoggleAutoRefresh.setEnabled(true);
                jTableQueue.setEnabled(true);
                return;
            }
        }

        final int toRemove = i;

        String fileName = (String) tableModel.getValueAt(i, 0);
        String fileOutputAlias = (String) tableModel.getValueAt(i, 1);

        JSONObject jsonObjectInner = new JSONObject()
                .put("fileName", fileName)
                .put("fileOutputAlias", fileOutputAlias);
        jsonArray.put(jsonObjectInner);

        requisition.put("parameters", jsonArray);
        socketHandle.getDataOutputStream().writeUTF(requisition.toString());

        new Thread(() -> {

            try {
                String command;
                do {

                    JSONObject jsonObject = new JSONObject(socketHandle.getDataInputStream().readUTF());

                    if (jsonObject.has("command") && jsonObject.has("parameters")) {
                        JSONArray parameters = jsonObject.getJSONArray("parameters");
                        command = jsonObject.getString("command");

                        if (parameters.length() > 0) {
                            switch (parameters.getString(0)) {
                                case "notifyProgress": {
                                    pListener.notifyProgress(parameters.getString(1), parameters.getInt(2));
                                }
                                break;
                                case "then": {
                                    pListener.then(parameters.getString(1));
                                }
                                break;
                                case "onStart": {
                                    pListener.onStart(parameters.getString(1));
                                }
                                break;

                                case "clear": {
                                    pListener.clear();
                                }
                                break;

                                case "dmaSend": {
                                    pListener.dmaSend(parameters.getString(1), parameters.getLong(2));
                                }
                                break;
                            }
                        }

                    } else {
                        break;
                    }

                } while (command == null ? MessageHandler.ConnectionMessage.OVER.toString() != null : !command.equals(MessageHandler.ConnectionMessage.OVER.toString()));

            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }

            jBdownload.setEnabled(true);
            jBadd.setEnabled(true);
            jBremove.setEnabled(true);
            jBforceRefresh.setEnabled(true);
            jBtoggleAutoRefresh.setEnabled(true);
            jTableQueue.setEnabled(true);
            tableModel.removeRow(toRemove);
            jTableQueue.clearSelection();

        }).start();

    }
     */
    private void addFiles() {
        TreePath paths[] = jTree1.getSelectionModel().getSelectionPaths();

        for (TreePath path : paths) {
            AbstractFolder curFolder = (AbstractFolder) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            String fileName = folderMap.get(curFolder.getCanonicalPath());

            if (fileName == null) {

                String curFolderName = curFolder.getCanonicalName();
                String baseName = curFolderName;

                while (choosenFileNames.contains(curFolderName)) {
                    curFolderName = baseName + String.format("_%h", new Random().nextInt(100));
                }

                choosenFileNames.add(curFolderName);
                folderMap.put(curFolder.getCanonicalPath(), curFolderName);
                tableModel.addRow(new String[]{curFolder.getCanonicalPath(), curFolderName});
            }
        }
    }

    private void erodeFiles() {
        int index = jTableQueue.getSelectedRow();

        if (index < 0) {
            return;
        }

        String deletedFolderEntryName = (String) tableModel.getValueAt(index, 0);
        String deletedOutputEntryName = (String) tableModel.getValueAt(index, 1);

        int response;

        if (!jCbDontAsk.isSelected()) {
            response = JOptionPane.showConfirmDialog(null, "Are you sure you wanna delete this entry?\n{" + deletedFolderEntryName + " <> " + deletedOutputEntryName + "}", "Confirmation", JOptionPane.YES_NO_OPTION);
        } else {
            response = JOptionPane.YES_OPTION;
        }

        if (response == JOptionPane.YES_OPTION) {

            folderMap.remove(deletedFolderEntryName);
            choosenFileNames.remove(deletedOutputEntryName);

            tableModel.removeRow(index);
        }

    }

    private void populateTree(String jsonInput) {
        JSONArray currentChildren;
        Queue<PennyroyalPair<JSONObject, DefaultMutableTreeNode>> toInvestigate = new LinkedList<>();

        root.removeAllChildren();

        toInvestigate.add(new PennyroyalPair<>(new JSONObject(jsonInput), root));
        while (!toInvestigate.isEmpty()) {
            PennyroyalPair<JSONObject, DefaultMutableTreeNode> currentObject = toInvestigate.poll();

            if (currentObject == null) {
                continue;
            }

            if (currentObject.first.has("children")) {

                currentChildren = currentObject.first.getJSONArray("children");

                for (int i = 0; i < currentChildren.length(); ++i) {
                    JSONObject curChild = currentChildren.getJSONObject(i);
                    String curChildName = curChild.getString("name");
                    String curChildParentPath = ((AbstractFolder) currentObject.second.getUserObject()).getCanonicalPath();
                    DefaultMutableTreeNode curChildNode = new DefaultMutableTreeNode(new AbstractFolder(
                            curChildParentPath + "/" + curChildName,
                            curChildName
                    ));

                    currentObject.second.add(curChildNode);
                    toInvestigate.add(new PennyroyalPair<>(curChild, curChildNode));
                }
            }

            treeModel.nodeChanged(currentObject.second);
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

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new JTree(root = new DefaultMutableTreeNode(new AbstractFolder("virtual://root","virtual")));
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableQueue = new javax.swing.JTable();
        jBdownload = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jBadd = new javax.swing.JButton();
        jBremove = new javax.swing.JButton();
        jCbDontAsk = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        jBtoggleAutoRefresh = new javax.swing.JToggleButton();
        jBforceRefresh = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(254, 254, 254));

        jTree1.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jScrollPane1.setViewportView(jTree1);

        jPanel3.setBackground(new java.awt.Color(254, 254, 254));
        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jTableQueue.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Folder", "Output File Name", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableQueue.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jScrollPane2.setViewportView(jTableQueue);
        if (jTableQueue.getColumnModel().getColumnCount() > 0) {
            jTableQueue.getColumnModel().getColumn(0).setResizable(false);
            jTableQueue.getColumnModel().getColumn(0).setPreferredWidth(200);
            jTableQueue.getColumnModel().getColumn(1).setResizable(false);
            jTableQueue.getColumnModel().getColumn(1).setPreferredWidth(100);
            jTableQueue.getColumnModel().getColumn(2).setResizable(false);
            jTableQueue.getColumnModel().getColumn(2).setPreferredWidth(300);
        }

        jBdownload.setBackground(new java.awt.Color(0, 185, 59));
        jBdownload.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        jBdownload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/labrpc/secondquestion/gfx/001-download-to-storage-drive.png"))); // NOI18N
        jBdownload.setText("Download All");
        jBdownload.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jBdownload.setFocusable(false);
        jBdownload.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jBdownload.setIconTextGap(10);
        jBdownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBdownloadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 781, Short.MAX_VALUE)
            .addComponent(jBdownload, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jBdownload, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel4.setBackground(new java.awt.Color(254, 254, 254));
        jPanel4.setBorder(null);
        jPanel4.setLayout(new java.awt.GridLayout(0, 1));

        jBadd.setBackground(new java.awt.Color(132, 254, 171));
        jBadd.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        jBadd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/labrpc/secondquestion/gfx/003-plus.png"))); // NOI18N
        jBadd.setText("Add");
        jBadd.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jBadd.setFocusable(false);
        jBadd.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jBadd.setIconTextGap(10);
        jBadd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBaddActionPerformed(evt);
            }
        });
        jPanel4.add(jBadd);

        jBremove.setBackground(new java.awt.Color(255, 123, 129));
        jBremove.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        jBremove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/labrpc/secondquestion/gfx/002-delete.png"))); // NOI18N
        jBremove.setText("Remove");
        jBremove.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jBremove.setFocusable(false);
        jBremove.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jBremove.setIconTextGap(10);
        jBremove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBremoveActionPerformed(evt);
            }
        });
        jPanel4.add(jBremove);

        jCbDontAsk.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N
        jCbDontAsk.setText("Don't ask  to remove");
        jCbDontAsk.setFocusable(false);
        jCbDontAsk.setPreferredSize(new java.awt.Dimension(150, 24));
        jPanel4.add(jCbDontAsk);

        jPanel5.setOpaque(false);
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jBtoggleAutoRefresh.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jBtoggleAutoRefresh.setFocusable(false);
        jBtoggleAutoRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtoggleAutoRefreshActionPerformed(evt);
            }
        });
        jPanel5.add(jBtoggleAutoRefresh, new org.netbeans.lib.awtextra.AbsoluteConstraints(132, 0, -1, 62));

        jBforceRefresh.setText("Refresh");
        jBforceRefresh.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jBforceRefresh.setFocusable(false);
        jBforceRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBforceRefreshActionPerformed(evt);
            }
        });
        jPanel5.add(jBforceRefresh, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 62));

        jPanel4.add(jPanel5);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/labrpc/secondquestion/gfx/cuelo.png"))); // NOI18N
        jPanel4.add(jLabel1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jBaddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBaddActionPerformed
        if (jTableQueue.getRowCount() < 8) {

            addFiles();

        } else {
            JOptionPane.showMessageDialog(null, "Isn't possible to keep more than eight connections.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jBaddActionPerformed

    private void jBremoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBremoveActionPerformed
        erodeFiles();
    }//GEN-LAST:event_jBremoveActionPerformed

    private void jBdownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBdownloadActionPerformed
        if (tableModel.getRowCount() > 0) {
            yieldDownloads();
        }
    }//GEN-LAST:event_jBdownloadActionPerformed

    private void jBtoggleAutoRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtoggleAutoRefreshActionPerformed
        if (jBtoggleAutoRefresh.isSelected()) {
            jBforceRefresh.setEnabled(false);
            jBforceRefresh.setText("Auto Refresh");
            jBforceRefresh.setBackground(Color.GREEN);
        } else {
            jBforceRefresh.setEnabled(true);
            jBforceRefresh.setText("Refresh");
            jBforceRefresh.setBackground(AUTO_REFRESH_BACKGROUND_COLOR);
        }
    }//GEN-LAST:event_jBtoggleAutoRefreshActionPerformed

    private void jBforceRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBforceRefreshActionPerformed
        try {
            listRemoteFolder();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Conection Refused!\n" + ex.toString() + "\n\nMake sure the server is open", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }//GEN-LAST:event_jBforceRefreshActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
            socketHandle.getDataOutputStream().writeUTF(new JSONObject().put("command", MessageHandler.ConnectionMessage.CLOSE_CONNECTION.toString()).toString());
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_formWindowClosing

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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new Client().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBadd;
    private javax.swing.JButton jBdownload;
    private javax.swing.JButton jBforceRefresh;
    private javax.swing.JButton jBremove;
    private javax.swing.JToggleButton jBtoggleAutoRefresh;
    private javax.swing.JCheckBox jCbDontAsk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTableQueue;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
}
