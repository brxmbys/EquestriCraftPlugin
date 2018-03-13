/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.http;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.race.RacePlayer;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 *
 * @author david
 */
public class HTTPHandler {

    EquestriCraftPlugin plugin;
    ServerSocket server;

    boolean run;

    public HTTPHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException {
        plugin.getLogger().log(Level.INFO, "Starting HTTP server on 10557");
        server = new ServerSocket(10557);
        final Runnable runnable = () -> {
            try {
                listen();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Server Error", ex);
            }
        };
        final Thread thread = new Thread(runnable, "HTTP_LISTENER");
        thread.setDaemon(true);
        run = true;
        thread.start();
    }

    private void listen() throws IOException {
        while (run) {
            Socket socket = server.accept();
            new ConnectionHandler(socket).start();
        }
    }

    public void stop() throws IOException {
        run = false;
        server.close();
    }

    public class ConnectionHandler extends Thread {

        Socket socket;

        public ConnectionHandler(Socket socket) throws IOException {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter pw = new PrintWriter(socket.getOutputStream());
                pw.print("HTTP/1.1 200 \r\n"); // Version & status code
                pw.print("Content-Type: text/plain\r\n"); // The type of data
                pw.print("Connection: close\r\n"); // Will close stream
                pw.print("Access-Control-Allow-Origin: *\r\n");
                pw.print("\r\n"); // End of headers

                String request = "";
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.length() == 0) {
                        break;
                    }
                    request += line + "\r\n";
                }

                String[] requestParam = request.split(" ");
                String path = requestParam[1];
                if (path.contains("\\?")) {
                    path = path.split("\\?")[0];
                }
                plugin.getLogger().log(Level.INFO, "Request for path - " + path);

                if (path.toLowerCase().contains("/ping")) {
                    pw.write(ping());
                } else if (path.toLowerCase().contains("/player")) {
                    String[] params = path.split("\\?")[1].split("&");
                    for (String param : params) {
                        String p = param.split("=")[1];
                        pw.write(queryPlayer(p));
                    }
                } else if (path.toLowerCase().contains("/online")) {
                    pw.write(onlinePlayers());
                } else if (path.toLowerCase().contains("/race/active")) {
                    pw.write(activeRace());
                } else if (path.toLowerCase().contains("/race/results")) {
                    pw.write(raceResults());
                } else {
                    pw.write("Error");
                }
                pw.close();
                br.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(HTTPHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /**
         * Path = /ping
         *
         * @return
         */
        private String ping() {
            return Bukkit.getOnlinePlayers().size() + "";
        }

        /**
         * Path = player
         *
         * @param uuid
         * @return
         */
        private String queryPlayer(String uuid) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            return (player == null ? "NO" : "YES");
        }

        private String onlinePlayers() {
            String output = "";
            for (Player p : Bukkit.getOnlinePlayers()) {
                output += p.getUniqueId().toString() + "," + p.getName() + "\n";
            }
            return output;
        }

        private String activeRace() {
            if (plugin.raceController.race == null) {
                return "0";
            } else if (plugin.raceController.race.isFinnsihed()) {
                return "0";
            } else if (plugin.raceController.race.isStarted()) {
                return "2";
            } else {
                return "1";
            }
        }

        private String raceResults() {
            if (plugin.raceController.race == null) {
                return "-1";
            } else if (plugin.raceController.race.isFinnsihed()) {
                String output = "";
                List<RacePlayer> players = plugin.raceController.race.getCompletedPlayers();
                int pos = 1;
                for (RacePlayer player : players) {
                    output += player.getPlayer().getName() + "," + player.getTime() + "," + pos;
                    pos++;
                }
                return output;
            } else {
                return "-1";
            }
        }
    }

}
