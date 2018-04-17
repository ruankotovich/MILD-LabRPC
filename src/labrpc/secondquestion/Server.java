/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labrpc.secondquestion;

import java.io.BufferedReader;
import labrpc.secondquestion.model.Zipper;
import labrpc.secondquestion.model.MessageHandler;
import labrpc.secondquestion.model.ProgressListener;
import labrpc.secondquestion.model.PennyroyalPair;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author dmitry
 */
public class Server {

    public static final String DEFAULT_DRIVE_LOCATION = "/home/dmitry/Tests";
    public static final String DEFAULT_DRIVE_NAME = "virtual://root";
    public static final int DEFAULT_PORT = 6464;

    private volatile ServerSocket abstractSocket;
    private final Thread eventLoop;
    private boolean serverOpen = false;
    private final String SERVER_VIRTUAL_DRIVE_LOCATION;
    private final String SERVER_VIRTUAL_DRIVE_NAME;
    private final int SERVER_PORT;
    private final int BLOCK_SIZE = 4096;

    public Server(String pSERVER_VIRTUAL_DRIVE_LOCATION, String pSERVER_VIRTUAL_DRIVE_NAME, int pSERVER_PORT) {

        this.SERVER_VIRTUAL_DRIVE_LOCATION = pSERVER_VIRTUAL_DRIVE_LOCATION;
        this.SERVER_VIRTUAL_DRIVE_NAME = pSERVER_VIRTUAL_DRIVE_NAME;
        this.SERVER_PORT = pSERVER_PORT;
        System.out.println("CarroTIDA Server started on port " + SERVER_PORT);
        eventLoop = new Thread(() -> {
            while (isServerOpen()) {
                try {
                    Socket chainedSocket = this.abstractSocket.accept();
                    System.out.println("Server received request of " + chainedSocket.getRemoteSocketAddress().toString());
                    yieldSocket(chainedSocket);
                } catch (SocketException ex) {
                    System.err.println("Connection Closed");
                    setServerOpen(false);
                    break;
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public void yieldSocket(Socket chainedSocket) {
        new Thread(() -> {

            DataInputStream inputStream;
            DataOutputStream outputStream;
            String inp;
            JSONObject object;

            try {

                boolean toBeContinued = true;
                inputStream = new DataInputStream(chainedSocket.getInputStream());
                outputStream = new DataOutputStream(chainedSocket.getOutputStream());

                do {

                    inp = inputStream.readUTF();
                    System.out.println("\t --- Received request from client " + chainedSocket.getRemoteSocketAddress().toString());
                    object = new JSONObject(inp);
                    System.out.println("\t --- Received: " + object.toString());

                    if (object.has("command")) {

                        MessageHandler.ConnectionMessage message = MessageHandler.MESSAGES.get(object.getString("command"));

                        if (message != null) {
                            if (object.has("parameters")) {

                                JSONArray parameters = object.getJSONArray("parameters");

                                switch (message) {

                                    case REQUEST_DOWNLOAD: {

                                        ProgressListener progressListener = new ProgressListener() {
                                            @Override
                                            public void notifyProgress(Object obj, int amount) {
                                                try {
                                                    outputStream.writeUTF(new JSONObject()
                                                            .put("command", MessageHandler.ConnectionMessage.PROGRESS.toString())
                                                            .put("parameters", new Object[]{"notifyProgress", obj, amount})
                                                            .toString());
                                                } catch (IOException ex) {
                                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }

                                            @Override
                                            public void then(Object obj) {
                                                try {
                                                    outputStream.writeUTF(new JSONObject()
                                                            .put("command", MessageHandler.ConnectionMessage.PROGRESS.toString())
                                                            .put("parameters", new Object[]{"then", obj})
                                                            .toString());
                                                } catch (IOException ex) {
                                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }

                                            @Override
                                            public void onStart(Object obj) {
                                                try {
                                                    outputStream.writeUTF(new JSONObject()
                                                            .put("command", MessageHandler.ConnectionMessage.PROGRESS.toString())
                                                            .put("parameters", new Object[]{"onStart", obj})
                                                            .toString());
                                                } catch (IOException ex) {
                                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }

                                            @Override
                                            public void clear() {
                                                try {
                                                    outputStream.writeUTF(new JSONObject()
                                                            .put("command", MessageHandler.ConnectionMessage.PROGRESS.toString())
                                                            .put("parameters", new Object[]{"clear"})
                                                            .toString());
                                                } catch (IOException ex) {
                                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }

                                            @Override
                                            public void dmaSend(Object obj, long size) {
                                                try {
                                                    outputStream.writeUTF(new JSONObject()
                                                            .put("command", MessageHandler.ConnectionMessage.PROGRESS.toString())
                                                            .put("parameters", new Object[]{"dmaSend", obj, size})
                                                            .toString());
                                                } catch (IOException ex) {
                                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }
                                        };

                                        System.out.println("\t --- Download request detected from client " + chainedSocket.getRemoteSocketAddress().toString());

                                        JSONObject fileObject = parameters.getJSONObject(0);
                                        String fileName = fileObject.getString("fileName");
                                        String fileOutputAlias = fileObject.getString("fileOutputAlias");
                                        File outputFile = new File(fileOutputAlias + (fileOutputAlias.endsWith(".zip") ? "" : ".zip"));

                                        System.out.println("\t --- Zipping file " + outputFile.getAbsolutePath());

                                        try {

                                            Thread zipperThread = new Zipper(fileName.replace(SERVER_VIRTUAL_DRIVE_NAME, SERVER_VIRTUAL_DRIVE_LOCATION), fileName, outputFile.getAbsolutePath()).zip(progressListener);
                                            zipperThread.start();
                                            zipperThread.join();

                                            long length = outputFile.length();

                                            System.out.println("\t --- Sending file " + outputFile.getName() + "of " + outputFile.length() + "b to client" + chainedSocket.getRemoteSocketAddress().toString());
                                            try (FileInputStream fileInputStream = new FileInputStream(outputFile)) {
                                                progressListener.dmaSend(outputFile.getName(), length);

                                                byte[] buffer = new byte[BLOCK_SIZE];

                                                for (int curLength, accumulator = 0; accumulator < length; outputStream.write(buffer, 0, curLength)) {
                                                    System.out.println("\tReading part [" + accumulator + "," + length + "]");
                                                    accumulator += (curLength = fileInputStream.read(buffer));
                                                    System.out.println("\tSending part [" + accumulator + "," + length + "]");
                                                }

                                                System.out.println("\t --- File sent to client " + chainedSocket.getRemoteSocketAddress().toString());
                                            }

                                            outputFile.delete();

                                        } catch (InterruptedException ex) {
                                            System.err.println("\t --- Aborted thread");
                                        } catch (IOException ex) {
                                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                        }

                                        try {
                                            Thread.sleep(1000);
                                            progressListener.clear();
                                            outputStream.writeUTF(new JSONObject()
                                                    .put("command", MessageHandler.ConnectionMessage.OVER.toString())
                                                    .toString());

                                        } catch (IOException | InterruptedException ex) {
                                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                        }

                                        toBeContinued = false;
                                    }
                                    break;

                                    default: {

                                    }
                                }

                            } else {

                                switch (message) {

                                    case REQUEST_LIST: {
                                        System.out.println("\t --- Sending list to client " + chainedSocket.getRemoteSocketAddress().toString());
                                        outputStream.writeUTF(listFolder(SERVER_VIRTUAL_DRIVE_LOCATION));
                                    }
                                    break;

                                    case CLOSE_CONNECTION: {
                                        System.out.println("\t --- Closing connection with client " + chainedSocket.getRemoteSocketAddress().toString());
                                        toBeContinued = false;
                                    }
                                    break;

                                    default: {

                                    }
                                }
                            }

                        }

                    }

                } while (toBeContinued);

                outputStream.close();
                inputStream.close();
                System.out.println("Socket closed with client" + chainedSocket.getRemoteSocketAddress().toString());
                chainedSocket.close();

            } catch (IOException ex) {
                System.err.println("Socket went rupted with client " + chainedSocket.getRemoteSocketAddress().toString());
            }
        }).start();
    }

    private String listFolder(String location) {
        File root = new File(location);
        JSONObject currentChild;

        if (root.isDirectory()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", root.getName());

            Queue<PennyroyalPair<JSONObject, File>> toInvestigate = new LinkedList<>();
            toInvestigate.add(new PennyroyalPair<>(jsonObject, root));

            while (!toInvestigate.isEmpty()) {
                PennyroyalPair<JSONObject, File> currentObject = toInvestigate.poll();

                if (currentObject == null) {
                    continue;
                }

                ArrayList<JSONObject> objects = new ArrayList<>();

                for (File f : currentObject.second.listFiles()) {
                    if (f.isDirectory()) {
                        currentChild = new JSONObject();
                        currentChild.put("name", f.getName());

                        objects.add(currentChild);
                        toInvestigate.add(new PennyroyalPair<>(currentChild, f));
                    }
                }
                if (!objects.isEmpty()) {
                    currentObject.first.put("children", objects);
                }
            }
            return jsonObject.toString();
        }
        return "{}";
    }

    public synchronized void start() {
        if (eventLoop != null && !eventLoop.isAlive()) {
            try {
                abstractSocket = new ServerSocket(this.SERVER_PORT);
                System.out.println("Server running on port " + abstractSocket.getLocalPort());

            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            setServerOpen(true);
            eventLoop.start();
        }
    }

    private synchronized void stop() {
        if (eventLoop != null && eventLoop.isAlive() && abstractSocket != null) {
            setServerOpen(false);
            try {
                abstractSocket.close();
            } catch (IOException ex) {
                System.err.println("Couldn't close socket");
            }
        }
    }

    public boolean isServerOpen() {
        return serverOpen;
    }

    private synchronized void setServerOpen(boolean serverOpen) {
        this.serverOpen = serverOpen;
    }

    public static void main(String[] args) {
        String driveLocation = DEFAULT_DRIVE_LOCATION;
        String driveName = DEFAULT_DRIVE_NAME;
        int port = DEFAULT_PORT;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("server_config.json")));
            StringBuilder stringBuffer = new StringBuilder();
            while (bufferedReader.ready()) {
                stringBuffer.append(bufferedReader.readLine());
            }
            JSONObject jsonObject = new JSONObject(stringBuffer.toString());

            if (jsonObject.has("config")) {
                JSONObject serverConfig = jsonObject.getJSONObject("config");

                driveLocation = serverConfig.getString("drive_location");
                driveName = serverConfig.getString("drive_name");
                port = serverConfig.getInt("port");
            } else {
                JOptionPane.showMessageDialog(null, "Invalid configuration file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "Configuration file not found, using default settings.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Configuration file couldn't be read, using default settings.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        new Server(driveLocation, driveName, port).start();
    }
}
