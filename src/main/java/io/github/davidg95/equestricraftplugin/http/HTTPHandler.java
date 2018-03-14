/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.race.RaceController;
import io.github.davidg95.equestricraftplugin.race.RacePlayer;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
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

    public static final int PORT = 10557;

    EquestriCraftPlugin plugin;

    HttpServer server;

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
        server.createContext("/race/control", new RaceControlHandler());
        server.createContext("/race/main", new MainRaceHandler());
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

    public void restart() throws IOException {
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

    public class MainRaceHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            int state;
            RaceController cont = plugin.raceController;
            if (cont.race == null) {
                state = 0;
            } else {
                state = cont.race.getState();
            }
            JSONObject main = new JSONObject();
            JSONObject info = new JSONObject();
            info.put("state", state);
            switch (state) {
                case 0: { //No session
                    main.put("info", info);
                    break;
                }
                case 1: { //Open
                    List<RacePlayer> entrants = cont.race.getPlayers();
                    info.put("laps", cont.race.laps());
                    info.put("p1", cont.race.prize1());
                    info.put("p2", cont.race.prize2());
                    info.put("p3", cont.race.prize3());
                    JSONArray players = new JSONArray();
                    for (RacePlayer player : entrants) {
                        JSONObject p = new JSONObject();
                        p.put("uuid", player.getPlayer().getUniqueId().toString());
                        p.put("name", player.getPlayer().getName());
                        players.add(p);
                    }
                    main.put("info", info);
                    main.put("entrants", players);
                    break;
                }
                case 2:
                case 3: { //Started
                    List<RacePlayer> entrants = cont.race.getPlayers();
                    info.put("laps", cont.race.laps());
                    info.put("p1", cont.race.prize1());
                    info.put("p2", cont.race.prize2());
                    info.put("p3", cont.race.prize3());
                    JSONArray players = new JSONArray();
                    for (RacePlayer player : entrants) {
                        JSONObject p = new JSONObject();
                        p.put("uuid", player.getPlayer().getUniqueId().toString());
                        p.put("name", player.getPlayer().getName());
                        p.put("lap", player.getLap());
                        p.put("lastCross", player.getLastCrossTime());
                        players.add(p);
                    }
                    main.put("info", info);
                    main.put("entrants", players);
                    break;
                }
                case 4: { //Finished
                    main.put("info", info);
                    break;
                }
                default: {
                    main.put("info", info);
                }
            }
            String response = main.toString();
            addHeaders(he);
            he.getResponseHeaders().add("Content-Type", "application/json");
            he.sendResponseHeaders(200, response.length());
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

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
//            if (player.hasPermission(plugin.doctorPerm)) {
//                response += ",Y";
//            } else {
//                response += ",N";
//            }
//            if (player.hasPermission(plugin.farrierPerm)) {
//                response += ",Y";
//            } else {
//                response += ",N";
//            }
//            if (player.hasPermission(plugin.dentistPerm)) {
//                response += ",Y";
//            } else {
//                response += ",N";
//            }
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
