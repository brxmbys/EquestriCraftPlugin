/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.race.RaceController;
import io.github.davidg95.equestricraftplugin.race.RaceTrack;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author david
 */
public class HTTPHandler implements CommandExecutor {

    public static final int PORT = 11725;

    EquestriCraftPlugin plugin;

    HttpServer server;

    boolean run;

    public HTTPHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {

//        SSLContext sslContext = SSLContext.getInstance("TLS");
//
//        // initialise the keystore
//        char[] password = "Quarterback19".toCharArray();
//        KeyStore ks = KeyStore.getInstance("JKS");
//        FileInputStream fis = new FileInputStream("lig.keystore");
//        ks.load(fis, password);
//
//        // setup the key manager factory
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//        kmf.init(ks, password);
//
//        // setup the trust manager factory
//        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
//        tmf.init(ks);
//
//        // setup the HTTPS context and parameters
//        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
//        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
//            @Override
//            public void configure(HttpsParameters params) {
//                try {
//                    // initialise the SSL context
//                    SSLContext c = SSLContext.getDefault();
//                    SSLEngine engine = c.createSSLEngine();
//                    params.setNeedClientAuth(false);
//                    params.setCipherSuites(engine.getEnabledCipherSuites());
//                    params.setProtocols(engine.getEnabledProtocols());
//
//                    // get the default parameters
//                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
//                    params.setSSLParameters(defaultSSLParameters);
//                } catch (Exception ex) {
//                    plugin.getLogger().log(Level.SEVERE, "Failed to create HTTPS port");
//                }
//            }
//        });
        plugin.getLogger().log(Level.INFO, "Starting HTTP server on " + PORT);
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/ping", new PingHandler());
        server.createContext("/player", new PlayersHandler());
        server.createContext("/race/control", new RaceControlHandler());
        server.createContext("/race/main", new RaceHandler(plugin));
        server.createContext("/race/add", (HttpExchange he) -> {
            Map<String, String> params = queryToMap(he.getRequestURI().getQuery());
            String player = params.get("player");
            RaceTrack track = plugin.raceController.getTrackyName(params.get("track"));
            String response;
            if (track == null) {
                response = "2";
            } else {
                boolean result = plugin.raceController.addPlayer(player, track);
                if (result) {
                    response = "1";
                } else {
                    response = "2";
                }
            }
            addHeaders(he);
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.createContext("/race/remove", (HttpExchange he) -> {
            Map<String, String> params = queryToMap(he.getRequestURI().getQuery());
            String player = params.get("player");
            String response = "1";
            RaceTrack track = plugin.raceController.getTrackyName(params.get("track"));
            if (track == null) {
                response = "2";
            } else {
                plugin.raceController.withdrawPlayer(player, track);
            }
            addHeaders(he);
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.createContext("/online", (HttpExchange he) -> {
            JSONArray players = new JSONArray();
            for (Player p : Bukkit.getOnlinePlayers()) {
                JSONObject player = new JSONObject();
                player.put("uuid", p.getUniqueId().toString());
                player.put("name", p.getName());
                players.add(player);
            }
            String output = players.toString();
            addHeaders(he);
            he.getResponseHeaders().add("Content-Type", "application/json");
            he.sendResponseHeaders(200, output.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(output.getBytes());
            }
        });
        server.start();
        plugin.getLogger().log(Level.INFO, "HTTP Server started");
    }

    public void stop() {
        plugin.getLogger().log(Level.INFO, "Stopping HTTP Server");
        server.stop(1);
        plugin.getLogger().log(Level.INFO, "HTTP server stopped");
    }

    public void restart() throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        stop();
        start();
    }

    private void addHeaders(HttpExchange he) {
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return true;
    }

    public class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            JSONObject info = new JSONObject();
            info.put("version", Bukkit.getBukkitVersion());
            info.put("wheatCost", plugin.foodController.wheat_cost);
            info.put("seedsCost", plugin.foodController.seeds_cost);
            info.put("waterCost", plugin.foodController.water_cost);
            info.put("vaccCost", plugin.SINGLE_VACCINATION_COST);
            String response = info.toString();
            addHeaders(he);
            he.getResponseHeaders().add("Content-Type", "application/json");
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    public class PingHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            String response = Bukkit.getOnlinePlayers().size() + "";
            addHeaders(he);
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    public class PlayersHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            Map<String, String> params = queryToMap(he.getRequestURI().getQuery());
            UUID uuid = UUID.fromString(params.get("player"));
            Player player = Bukkit.getPlayer(uuid);
            String response;
            if (player == null) {
                response = "NO";
            } else {
                response = "YES";
            }
            addHeaders(he);
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

    }

    public class RaceControlHandler implements HttpHandler {

        RaceController controller = plugin.raceController;

        @Override
        public void handle(HttpExchange he) throws IOException {
            Map<String, String> params = queryToMap(he.getRequestURI().getQuery());
            String response = "1";
            if (params.get("operation").equalsIgnoreCase("open")) {
                String trackName = params.get("track");
                int laps = Integer.parseInt(params.get("laps"));
                double p1 = Double.parseDouble(params.get("p1"));
                double p2 = Double.parseDouble(params.get("p2"));
                double p3 = Double.parseDouble(params.get("p3"));
                RaceTrack track = null;
                for (RaceTrack t : controller.getTracks()) {
                    if (t.getName().equalsIgnoreCase(trackName)) {
                        track = t;
                    }
                }
                if (track == null) {
                    plugin.getLogger().log(Level.INFO, "Could not find track" + trackName);
                    response = "0";
                } else {
                    controller.open(track, laps, p1, p2, p3);
                }
            } else if (params.get("operation").equalsIgnoreCase("countdown")) {
                RaceTrack track = controller.getTrackyName(params.get("track"));
                if (track == null) {
                    response = "2";
                } else {
                    if (!controller.countdown(track)) {
                        response = "2";
                    }
                }
            } else if (params.get("operation").equalsIgnoreCase("end")) {
                RaceTrack track = controller.getTrackyName(params.get("track"));
                if (track == null) {
                    response = "2";
                } else {
                    controller.end(track);
                }
            } else if (params.get("operation").equalsIgnoreCase("broadcast")) {
                String message = params.get("message");
                Bukkit.broadcastMessage(ChatColor.GREEN + "[RACE BROADCAST] " + ChatColor.RESET + message);
            } else {
                response = "0";
            }
            addHeaders(he);
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }
}
