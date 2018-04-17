/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labrpc.secondquestion.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import labrpc.secondquestion.Client;

/**
 *
 * @author dmitry
 */
public class DMAHandle {

    private final SocketHandle socket;
    private final int index;
    private final ProgressListener listener;
    DefaultTableModel model;
    private String action;

    private final int BLOCK_SIZE;
    private final File FILE_RECEIVED_RAW;
    private final File FILE_RECEIVED_VIRTUAL;

    public DMAHandle(SocketHandle socket, int index, DefaultTableModel model, File REC_RAW, File REC_VIRT, int BLOCK_SIZE) {
        this.socket = socket;
        this.index = index;
        this.model = model;
        this.FILE_RECEIVED_RAW = REC_RAW;
        this.FILE_RECEIVED_VIRTUAL = REC_VIRT;
        this.BLOCK_SIZE = BLOCK_SIZE;

        listener = new ProgressListener() {
            @Override
            public void notifyProgress(Object obj, int amount) {
                model.setValueAt(action + " [" + (amount >= 100 ? 100 : amount) + "/" + "100]", index, 2);
            }

            @Override
            public void then(Object obj) {
                action = obj.toString();
                model.setValueAt(action, index, 2);
            }

            @Override
            public void onStart(Object obj) {
                action = obj.toString();
                model.setValueAt(action, index, 2);
            }

            @Override
            public void dmaSend(Object obj, long length) {

                try {
                    File file = new File(FILE_RECEIVED_RAW.getAbsolutePath() + "/" + obj.toString());
                    FileOutputStream fOutputStream = new FileOutputStream(file.getAbsoluteFile());
                    byte[] buffer = new byte[BLOCK_SIZE];
                    this.onStart("Receiving file...");
                    for (int curLength, accumulator = 0; accumulator < length; fOutputStream.write(buffer, 0, curLength)) {
                        this.notifyProgress("Receiving parts from " + accumulator + " to " + length, (int) ((accumulator / (float) length) * 100));
                        accumulator += (curLength = getSocket().getDataInputStream().read(buffer));
                    }

                    this.then("File received.");

                    fOutputStream.close();
                    Zipper.unzip(file, FILE_RECEIVED_VIRTUAL, this);

                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            @Override
            public void clear() {

            }
        };
    }

    public SocketHandle getSocket() {
        return socket;
    }

    public int getIndex() {
        return index;
    }

    public ProgressListener getListener() {
        return listener;
    }

    public DefaultTableModel getModel() {
        return model;
    }

}
