package fr.redboxing.athena;

import fr.redboxing.athena.database.entities.Server;
import fr.redboxing.athena.manager.ServerManager;
import io.prometheus.client.Gauge;

import java.util.List;

public class Metrics {
    private static final Gauge serverCount = Gauge.build()
            .name("server_count")
            .help("Number of servers in the database")
            .register();

    private static final Gauge serverOnline = Gauge.build()
            .name("server_online")
            .help("Number of servers online")
            .labelNames()
            .register();

    private static final Gauge serverVersion = Gauge.build()
            .name("server_version")
            .help("Number of servers by version")
            .labelNames("version")
            .register();

    private static final Gauge serverModloader = Gauge.build()
            .name("server_modloader")
            .help("Number of servers by modloader")
            .labelNames("modloader")
            .register();

    private static final Gauge serverModded = Gauge.build()
            .name("server_modded")
            .help("Number of modded servers.")
            .register();

    private static final Gauge onlinePlayers = Gauge.build()
            .name("online_players")
            .help("Number of players online")
            .register();

    private static final Gauge serverLocation = Gauge.build()
            .name("server_location")
            .help("Number of servers by location")
            .labelNames("city", "latitude", "longitude")
            .register();

    public void updateMetrics(Server oldServer, Server currentServer, boolean newServer) {
        if(newServer) {
            serverCount.inc();
            serverOnline.inc();

            if(!currentServer.getModloader().equals("VANILLA")) {
                serverModded.inc();
            }

            serverVersion.labels(currentServer.getVersion().split(" ")[0]).inc();
            serverModloader.labels(currentServer.getModloader()).inc();
            onlinePlayers.inc(currentServer.getOnlinePlayers());
            serverLocation.labels(currentServer.getCity(), String.valueOf(currentServer.getLatitude()), String.valueOf(currentServer.getLongitude())).inc();
        } else {
            if(oldServer.isOnline() && !currentServer.isOnline()) {
                serverOnline.dec();
            } else if(!oldServer.isOnline() && currentServer.isOnline()) {
                serverOnline.inc();
            }

            if(oldServer.getModloader().equals("VANILLA") && !currentServer.getModloader().equals("VANILLA")) {
                serverModded.inc();
            } else if(!oldServer.getModloader().equals("VANILLA") && currentServer.getModloader().equals("VANILLA")) {
                serverModded.dec();
            }

            if(!oldServer.getVersion().equals(currentServer.getVersion())) {
                serverVersion.labels(oldServer.getVersion()).dec();
                serverVersion.labels(currentServer.getVersion()).inc();
            }

            if(!oldServer.getModloader().equals(currentServer.getModloader())) {
                serverModloader.labels(oldServer.getModloader()).dec();
                serverModloader.labels(currentServer.getModloader()).inc();
            }

            if(oldServer.getOnlinePlayers() != currentServer.getOnlinePlayers()) {
                onlinePlayers.dec(oldServer.getOnlinePlayers());
                onlinePlayers.inc(currentServer.getOnlinePlayers());
            }

            if(oldServer.getCity() != currentServer.getCity()) {
                serverLocation.labels(oldServer.getCity(), String.valueOf(oldServer.getLatitude()), String.valueOf(oldServer.getLongitude())).dec();
                serverLocation.labels(currentServer.getCity(), String.valueOf(currentServer.getLatitude()), String.valueOf(currentServer.getLongitude())).inc();
            }
        }
    }

    public void updateAllMetrics() {
        List<Server> servers = ServerManager.getAllServers();
        serverCount.set(servers.size());
        serverOnline.set(servers.stream().filter(Server::isOnline).count());
        serverModded.set(servers.stream().filter(server -> !server.getModloader().equals("VANILLA")).count());
        serverVersion.clear();
        serverModloader.clear();

        servers.forEach(server -> {
            serverVersion.labels(server.getVersion()).inc();
            serverModloader.labels(server.getModloader()).inc();
            serverLocation.labels(server.getCity(), String.valueOf(server.getLatitude()), String.valueOf(server.getLongitude())).inc();
        });

        onlinePlayers.set(servers.stream().mapToInt(Server::getOnlinePlayers).sum());
    }
}
