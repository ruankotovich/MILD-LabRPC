/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labrpc.firstquestion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmitry
 */
public class Server {

    private ServerSocket abstractSocket;
    private final Thread mainThread;
    boolean loop = true;

    private final Runnable eventLoop;

    public Server() {
        try {
            this.abstractSocket = new ServerSocket(6464);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        eventLoop = () -> {
            try {
                while (loop) {
                    System.out.println("Waiting new connection...");
                    Socket eventSocket = abstractSocket.accept();
                    System.out.println("Connected with " + eventSocket.getRemoteSocketAddress().toString());
                    DataInputStream input = new DataInputStream(eventSocket.getInputStream());
                    DataOutputStream output = new DataOutputStream(eventSocket.getOutputStream());

                    String receivedInput = input.readUTF();
                    output.writeUTF(standardDeviation(receivedInput));

                    input.close();
                    output.close();
                    eventSocket.close();

                }
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        mainThread = new Thread(eventLoop);
    }

    public void start() {
        if (!mainThread.isAlive()) {
            mainThread.start();
            System.out.println("Server started on port " + this.abstractSocket.getLocalPort());
        }
    }

    private String standardDeviation(String miraInput_xo) {
        String recoveredNumbersAsString[] = miraInput_xo.substring(1, miraInput_xo.length() - 1).split(",");

        double recoveredNumbers[] = new double[recoveredNumbersAsString.length];
        double average = 0;
        double individualIterator;
        double standardDeviationAccumulator = 0;

        int index = 0;

        for (String curElement : recoveredNumbersAsString) {
            recoveredNumbers[index] = individualIterator = Double.parseDouble(curElement);
            average += individualIterator;
            ++index;
        }

        if (recoveredNumbers.length > 1) {
            average /= recoveredNumbers.length;
        } else {
            return "[Error] make sure to send more than one number";
        }

        for (double arrayElement : recoveredNumbers) {
            standardDeviationAccumulator += Math.pow(arrayElement - average, 2);
        }

        if (recoveredNumbers.length > 1) {
            standardDeviationAccumulator /= (recoveredNumbers.length - 1);
            standardDeviationAccumulator = Math.sqrt(standardDeviationAccumulator);
        } else {
            standardDeviationAccumulator = 0;
        }

        return String.format("%.3f", standardDeviationAccumulator);
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
