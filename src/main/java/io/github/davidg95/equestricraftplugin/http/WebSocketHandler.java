/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.http;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author david
 */
public class WebSocketHandler {

    public static final int PORT = 20557;

    private ServerSocket server;

    private final EquestriCraftPlugin plugin;

    public WebSocketHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException {
        plugin.getLogger().info("Starting WebSocket on port number " + PORT);
        server = new ServerSocket(PORT);
        final Runnable run = () -> {
            listen();
        };
        final Thread thread = new Thread(run, "LISTEN_THREAD");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() throws IOException {
        plugin.getLogger().log(Level.INFO, "Stopping WebSocket");
        server.close();
    }

    public void restart() throws IOException {
        stop();
        start();
    }

    private void listen() {
        while (true) {
            try {
                Socket client = server.accept();
                new ConnectionHandler(client).start();
            } catch (IOException ex) {
                Logger.getLogger(WebSocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ConnectionHandler extends Thread {

        private final Socket socket;

        public ConnectionHandler(Socket client) {
            super(client.getInetAddress().getHostAddress() + "_CONNECTION");
            socket = client;
        }

        @Override
        public void run() {
            InputStream in = null;
            try {
                in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                String data = new Scanner(in, "UTF-8").useDelimiter("\\r\\n\\r\\n").next();
                Matcher get = Pattern.compile("^GET").matcher(data);
                if (get.find()) {
                    Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                    match.find();
                    byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                            + "Connection: Upgrade\r\n"
                            + "Upgrade: websocket\r\n"
                            + "Sec-WebSocket-Accept: "
                            + DatatypeConverter.printBase64Binary(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"))) + "\r\n\r\n").getBytes("UTF-8");
                    out.write(response, 0, response.length);
                }
                
                while(true){
                    String input = new Scanner(in, "UTF-8").useDelimiter("\\r\\n\\r\\n").next();
                    plugin.getLogger().log(Level.INFO, "Received - " + input);
                }
            } catch (IOException | NoSuchAlgorithmException ex) {
                Logger.getLogger(WebSocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(WebSocketHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

}
