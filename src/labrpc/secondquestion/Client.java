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
    private final ProgressListener pListener;
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
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public Client() {
        initComponents();
        {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("config.json")));
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
            this.socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            this.dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Conection Refused!\n" + ex.toString() + "\n\nMake sure the server is open", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try {
            OPENED_FOLDER_ICON = new ImageIcon(ImageIO.read(Client.class.getResource("/labrpc/secondquestion/gfx/opened.png").openStream()).getScaledInstance(16, 16, 0));
            CLOSED_FOLDER_ICON = new ImageIcon(ImageIO.read(Client.class.getResource("/labrpc/secondquestion/gfx/closed.png").openStream()).getScaledInstance(16, 16, 0));
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

        pListener = new ProgressListener() {
            @Override
            public void notifyProgress(Object obj, int amount) {
                jProgressBar1.setValue(amount);
                jFileNameHandler.setText(obj.toString());
            }

            @Override
            public void then(Object obj) {
                jProgressLabel.setText(obj.toString());
                jFileNameHandler.setText("");
            }

            @Override
            public void onStart(Object obj) {
                jProgressLabel.setText(obj.toString());
                jFileNameHandler.setText("Precaching...");
            }

            @Override
            public void clear() {
                jProgressBar1.setValue(0);
                jFileNameHandler.setText("");
                jProgressLabel.setText("");
            }

            @Override
            public void dmaSend(Object obj, long length) {
                try {
                    File file = new File(FILE_RECEIVED_RAW.getAbsolutePath() + "/" + obj.toString());
                    FileOutputStream fOutputStream = new FileOutputStream(file.getAbsoluteFile());
                    byte[] buffer = new byte[BLOCK_SIZE];
                    pListener.onStart("Receiving file...");
                    for (int curLength, accumulator = 0; accumulator < length; fOutputStream.write(buffer, 0, curLength)) {
                        pListener.notifyProgress("Receiving parts from " + accumulator + " to " + length, (int) ((accumulator / (float) length) * 100));
                        accumulator += (curLength = dataInputStream.read(buffer));
                    }

                    pListener.then("File received.");

                    fOutputStream.close();
                    Zipper.unzip(file, FILE_RECEIVED_VIRTUAL, pListener);

                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

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

        dataOutputStream.writeUTF(new JSONObject().put("command", MessageHandler.ConnectionMessage.REQUEST_LIST.toString()).toString());
        populateTree(dataInputStream.readUTF());

        treeModel.reload();
    }

    private void downloadSelectedFiles() throws IOException {
        JSONObject requisition = new JSONObject();
        requisition.put("command", MessageHandler.ConnectionMessage.REQUEST_DOWNLOAD.toString());

        JSONArray jsonArray = new JSONArray();

//        for (int i = 0; i < jTableQueue.getRowCount(); ++i) {
        int i = jTableQueue.getSelectedRow();

        if (i < 0) {
            JOptionPane.showMessageDialog(null, "Select an item on the table", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String fileName = (String) tableModel.getValueAt(i, 0);
        String fileOutputAlias = (String) tableModel.getValueAt(i, 1);

        JSONObject jsonObjectInner = new JSONObject()
                .put("fileName", fileName)
                .put("fileOutputAlias", fileOutputAlias);
        jsonArray.put(jsonObjectInner);
//        }

        requisition.put("parameters", jsonArray);
        dataOutputStream.writeUTF(requisition.toString());

        new Thread(() -> {

            try {
                String command;
                do {

                    JSONObject jsonObject = new JSONObject(dataInputStream.readUTF());

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

            jTableQueue.clearSelection();

        }).start();

    }

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
        jPanel2 = new javax.swing.JPanel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jProgressLabel = new javax.swing.JLabel();
        jFileNameHandler = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableQueue = new javax.swing.JTable();
        jButton3 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jCbDontAsk = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        jBtoggleAutoRefresh = new javax.swing.JToggleButton();
        jBforceRefresh = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(254, 254, 254));

        jScrollPane1.setViewportView(jTree1);

        jPanel2.setBackground(new java.awt.Color(254, 254, 254));
        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jProgressBar1.setString("");

        jProgressLabel.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N

        jFileNameHandler.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jFileNameHandler, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jProgressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jFileNameHandler, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(254, 254, 254));
        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jTableQueue.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Folder", "Output File Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTableQueue);
        if (jTableQueue.getColumnModel().getColumnCount() > 0) {
            jTableQueue.getColumnModel().getColumn(0).setResizable(false);
            jTableQueue.getColumnModel().getColumn(0).setPreferredWidth(300);
            jTableQueue.getColumnModel().getColumn(1).setResizable(false);
            jTableQueue.getColumnModel().getColumn(1).setPreferredWidth(100);
        }

        jButton3.setBackground(new java.awt.Color(0, 185, 59));
        jButton3.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        jButton3.setText("Download");
        jButton3.setFocusable(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel4.setBackground(new java.awt.Color(254, 254, 254));
        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel4.setLayout(new java.awt.GridLayout(0, 1));

        jButton1.setBackground(new java.awt.Color(0, 185, 59));
        jButton1.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        jButton1.setText("Add");
        jButton1.setFocusable(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton1);

        jButton2.setBackground(new java.awt.Color(238, 253, 83));
        jButton2.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        jButton2.setText("Remove");
        jButton2.setFocusable(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton2);

        jCbDontAsk.setFont(new java.awt.Font("Noto Sans", 1, 12)); // NOI18N
        jCbDontAsk.setText("Don't ask  to remove");
        jCbDontAsk.setPreferredSize(new java.awt.Dimension(150, 24));
        jPanel4.add(jCbDontAsk);

        jPanel5.setOpaque(false);
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jBtoggleAutoRefresh.setFocusable(false);
        jBtoggleAutoRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtoggleAutoRefreshActionPerformed(evt);
            }
        });
        jPanel5.add(jBtoggleAutoRefresh, new org.netbeans.lib.awtextra.AbsoluteConstraints(132, 0, -1, 62));

        jBforceRefresh.setText("Refresh");
        jBforceRefresh.setFocusable(false);
        jBforceRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBforceRefreshActionPerformed(evt);
            }
        });
        jPanel5.add(jBforceRefresh, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 62));

        jPanel4.add(jPanel5);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        addFiles();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        erodeFiles();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        try {
            downloadSelectedFiles();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

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
            dataOutputStream.writeUTF(new JSONObject().put("command", MessageHandler.ConnectionMessage.CLOSE_CONNECTION.toString()).toString());
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
    private javax.swing.JButton jBforceRefresh;
    private javax.swing.JToggleButton jBtoggleAutoRefresh;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCbDontAsk;
    private javax.swing.JLabel jFileNameHandler;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JLabel jProgressLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTableQueue;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
}
