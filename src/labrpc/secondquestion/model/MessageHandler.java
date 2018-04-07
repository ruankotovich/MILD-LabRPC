/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labrpc.secondquestion.model;

import java.util.HashMap;

/**
 *
 * @author dmitry
 */
public class MessageHandler {

    public static enum ConnectionMessage {
        REQUEST_LIST("REQUEST_LIST"),
        REQUEST_DOWNLOAD("REQUEST_DOWNLOAD"),
        REQUEST_UPLOAD("REQUEST_UPLOAD"),
        PROGRESS("PROGRESS"),
        CLOSE_CONNECTION("CLOSE_CONNECTION"),
        OVER("OVER");

        private final String name;

        ConnectionMessage(String pName) {
            this.name = pName;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final HashMap<String, ConnectionMessage> MESSAGES;

    static {
        MESSAGES = new HashMap<>();
        MESSAGES.put(MessageHandler.ConnectionMessage.REQUEST_LIST.toString(), MessageHandler.ConnectionMessage.REQUEST_LIST);
        MESSAGES.put(MessageHandler.ConnectionMessage.REQUEST_DOWNLOAD.toString(), MessageHandler.ConnectionMessage.REQUEST_DOWNLOAD);
        MESSAGES.put(MessageHandler.ConnectionMessage.REQUEST_UPLOAD.toString(), MessageHandler.ConnectionMessage.REQUEST_UPLOAD);
        MESSAGES.put(MessageHandler.ConnectionMessage.PROGRESS.toString(), MessageHandler.ConnectionMessage.PROGRESS);
        MESSAGES.put(MessageHandler.ConnectionMessage.CLOSE_CONNECTION.toString(), MessageHandler.ConnectionMessage.CLOSE_CONNECTION);
        MESSAGES.put(MessageHandler.ConnectionMessage.OVER.toString(), MessageHandler.ConnectionMessage.OVER);
    }
}
