/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.race.Race;
import io.github.davidg95.equestricraftplugin.race.RaceController;
import io.github.davidg95.equestricraftplugin.race.RacePlayer;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 *
 * @author david
 */
public class HTTPHandler {

    public static final int PORT = 10557;

    EquestriCraftPlugin plugin;

    HttpServer server;
    Executor executor;

    boolean run;

    public HTTPHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException {
        plugin.getLogger().log(Level.INFO, "Starting HTTP server on " + PORT);
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/ping", new PingHandler());
        server.createContext("/player", new PlayersHandler());
        server.createContext("/race/active", new RaceActiveHandler());
        server.createContext("/race/results", new RaceResultsHandler());
        server.createContext("/race/control", new RaceControlHandler());
        server.createContext("/race/entrants", new RaceControlHandler());
        server.createContext("/online", (HttpExchange he) -> {
            String output = "";
            for (Player p : Bukkit.getOnlinePlayers()) {
                output += p.getUniqueId().toString() + "," + p.getName() + "\n";
            }
            addHeaders(he);
            he.sendResponseHeaders(200, output.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(output.getBytes());
            }
        });
        executor = Executors.newFixedThreadPool(10);
        server.setExecutor(executor);
        server.start();
        plugin.getLogger().log(Level.INFO, "HTTP Server started");
    }

    public void stop() {
        server.stop(1);
    }

    private void addHeaders(HttpExchange he) {
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    }

    public class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            String response = "Server is running";
            addHeaders(he);
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    public class PingHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            plugin.getLogger().info("Received ping request on HTTP Server");
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
            Map<String,String> params = queryToMap(he.getRequestURI().getQuery());
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

    public class RaceActiveHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            String response;
            Race race = plugin.raceController.race;
            if (race == null) {
                response = "0";
            } else if (race.isFinnsihed()) {
                response = "0";
            } else if (race.isStarted()) {
                response = "2";
            } else {
                response = "1";
            }
            addHeaders(he);
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

    }

    public class RaceResultsHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            String response;
            if (plugin.raceController.race == null) {
                response = "-1";
            } else if (plugin.raceController.race.isFinnsihed()) {
                String output = "";
                List<RacePlayer> players = plugin.raceController.race.getCompletedPlayers();
                int pos = 1;
                for (RacePlayer player : players) {
                    output += player.getPlayer().getName() + "," + player.getTime() + "," + pos;
                    pos++;
                }
                response = output;
            } else {
                response = "-1";
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
                int laps = Integer.parseInt(params.get("laps"));
                double p1 = Double.parseDouble(params.get("p1"));
                double p2 = Double.parseDouble(params.get("p2"));
                double p3 = Double.parseDouble(params.get("p3"));
                controller.open(laps, p1, p2, p3);
            } else if (params.get("operation").equalsIgnoreCase("countdown")) {
                response = "" + controller.countdown();
            } else if (params.get("operation").equalsIgnoreCase("end")) {
                response = "" + controller.end();
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

    public class RaceParticipantsHandler implements HttpHandler {

        RaceController controller = plugin.raceController;

        @Override
        public void handle(HttpExchange he) throws IOException {
            String response;
            if (controller.race == null || controller.race.isFinnsihed()) {
                response = "0";
            } else {
                String output = "";
                List<RacePlayer> entrants = controller.race.getPlayers();
                for (RacePlayer player : entrants) {
                    output += player.getPlayer().getName();
                }
                response = output;
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
