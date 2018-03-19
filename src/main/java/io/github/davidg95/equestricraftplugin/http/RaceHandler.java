/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.race.RaceController;
import io.github.davidg95.equestricraftplugin.race.RacePlayer;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author david
 */
public class RaceHandler implements HttpHandler {

    private final EquestriCraftPlugin plugin;

    public RaceHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

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
                info.put("startTime", cont.race.getStartTime());
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
                info.put("laps", cont.race.laps());
                info.put("p1", cont.race.prize1());
                info.put("p2", cont.race.prize2());
                info.put("p3", cont.race.prize3());
                List<RacePlayer> players = cont.race.getCompletedPlayers();
                JSONArray complete = new JSONArray();
                for (RacePlayer p : players) {
                    JSONObject player = new JSONObject();
                    player.put("uuid", p.getPlayer().getUniqueId().toString());
                    player.put("name", p.getPlayer().getName());
                    player.put("time", p.getTime());
                    player.put("position", p.getPosition());
                    complete.add(player);
                }
                main.put("entrants", complete);
                main.put("info", info);
                break;
            }
            default: {
                main.put("info", info);
            }
        }
        String response = main.toString();
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        he.getResponseHeaders().add("Content-Type", "application/json");
        he.sendResponseHeaders(200, response.length());
        try (OutputStream os = he.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
