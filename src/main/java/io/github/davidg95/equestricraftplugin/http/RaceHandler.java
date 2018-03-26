/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.race.RaceController;
import io.github.davidg95.equestricraftplugin.race.RacePlayer;
import io.github.davidg95.equestricraftplugin.race.RaceTrack;
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
        RaceController cont = plugin.raceController;
        JSONObject main = new JSONObject();
        JSONArray tracks = new JSONArray();
        for (RaceTrack track : cont.getTracks()) {
            JSONObject t = new JSONObject();
            t.put("name", track.getName());
            t.put("state", track.getState());
            if (track.getState() == 1) {
                t.put("laps", track.getRace().laps());
                JSONObject prizes = new JSONObject();
                prizes.put("p1", track.getRace().prize1());
                prizes.put("p2", track.getRace().prize2());
                prizes.put("p3", track.getRace().prize3());
                t.put("prizes", prizes);
                List<RacePlayer> entrants = track.getRace().getPlayers();
                JSONArray players = new JSONArray();
                for (RacePlayer player : entrants) {
                    JSONObject p = new JSONObject();
                    p.put("uuid", player.getPlayer().getUniqueId().toString());
                    p.put("name", player.getPlayer().getName());
                    players.add(p);
                }
                t.put("entrants", players);
            } else if (track.getState() == 2 || track.getState() == 3) {
                JSONObject prizes = new JSONObject();
                prizes.put("p1", track.getRace().prize1());
                prizes.put("p2", track.getRace().prize2());
                prizes.put("p3", track.getRace().prize3());
                t.put("prizes", prizes);
                JSONArray entrants = new JSONArray();
                for (RacePlayer p : track.getRace().getPlayers()) {
                    JSONObject entrant = new JSONObject();
                    entrant.put("uuid", p.getPlayer().getUniqueId().toString());
                    entrant.put("name", p.getPlayer().getName());
                    if (track.getState() == 3) {
                        entrant.put("lap", p.getLap());
                        entrant.put("lastCrossTime", p.getLastCrossTime());
                    }
                    entrants.add(entrant);
                }
                t.put("startTime", track.getRace().getStartTime());
                t.put("entrants", entrants);
            } else if (track.getState() == 4) {
                JSONObject prizes = new JSONObject();
                prizes.put("p1", track.getRace().prize1());
                prizes.put("p2", track.getRace().prize2());
                prizes.put("p3", track.getRace().prize3());
                t.put("prizes", prizes);
                JSONArray results = new JSONArray();
                for (RacePlayer p : track.getRace().getPlayers()) {
                    JSONObject entrant = new JSONObject();
                    entrant.put("uuid", p.getPlayer().getUniqueId().toString());
                    entrant.put("name", p.getPlayer().getName());
                    entrant.put("position", p.getPosition());
                    entrant.put("time", p.getTime());
                    results.add(entrant);
                }
                t.put("results", results);
            }
            tracks.add(t);
        }
        main.put("tracks", tracks);
        String response = main.toString();
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        he.getResponseHeaders().add("Content-Type", "application/json");
        he.sendResponseHeaders(200, response.length());
        try (OutputStream os = he.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
