package fr.redboxing.athena.manager;

import fr.redboxing.athena.database.DatabaseManager;
import fr.redboxing.athena.database.entities.Server;
import fr.redboxing.athena.utils.Tuple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerManager {
    public static void createOrUpdate(Server server) {
        String sql = "INSERT INTO servers (id, host, port, version, description, onlinePlayers, maxPlayers, modloader, players, online, lastOnline, city, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE version = ?, description = ?, onlinePlayers = ?, maxPlayers = ?, modLoader = ?, players = ?, online = ?, lastOnline = ?, city = ?, latitude = ?, longitude = ?";
        try(Connection connection = DatabaseManager.getHikariDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if(server.getId() != null) {
                statement.setLong(1, server.getId());
            } else {
                statement.setNull(1, java.sql.Types.BIGINT);
            }

            statement.setString(2, server.getHost());
            statement.setInt(3, server.getPort());
            statement.setString(4, server.getVersion());
            statement.setString(5, server.getDescription());
            statement.setInt(6, server.getOnlinePlayers());
            statement.setInt(7, server.getMaxPlayers());
            statement.setString(8, server.getModloader());
            statement.setString(9, server.getPlayers());
            statement.setBoolean(10, server.isOnline());
            statement.setLong(11, server.getLastOnline());
            statement.setString(12, server.getCity());
            statement.setDouble(13, server.getLatitude());
            statement.setDouble(14, server.getLongitude());
            statement.setString(15, server.getVersion());
            statement.setString(16, server.getDescription());
            statement.setInt(17, server.getOnlinePlayers());
            statement.setInt(18, server.getMaxPlayers());
            statement.setString(19, server.getModloader());
            statement.setString(20, server.getPlayers());
            statement.setBoolean(21, server.isOnline());
            statement.setLong(22, server.getLastOnline());
            statement.setString(23, server.getCity());
            statement.setDouble(24, server.getLatitude());
            statement.setDouble(25, server.getLongitude());

            statement.executeQuery();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<Server> getServerByAddress(String address) {
        List<Server> servers = new ArrayList<>();

        String sql = "SELECT * FROM servers WHERE host = ? AND port = ?";
        try(Connection connection = DatabaseManager.getHikariDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, address.split(":")[0]);
            statement.setInt(2, Integer.parseInt(address.split(":")[1]));

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                servers.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return servers;
    }

    public static List<Server> getServerByDescription(String description) {
        List<Server> servers = new ArrayList<>();

        String sql = "SELECT * FROM servers WHERE description LIKE ?";
        try(Connection connection = DatabaseManager.getHikariDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + description + "%");

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                servers.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return servers;
    }

    public static int getTotalServerCounts() {
        String sql = "SELECT COUNT(*) FROM servers";
        try(Connection connection = DatabaseManager.getHikariDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    public static List<Tuple<String, Integer>> getAllAddress() {
        List<Tuple<String, Integer>> addresses = new ArrayList<>();

        String sql = "SELECT host, port FROM servers";
        try(Connection connection = DatabaseManager.getHikariDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                addresses.add(new Tuple<>(rs.getString("host"), rs.getInt("port")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return addresses;
    }

    public static List<Server> getAllServers() {
        String sql = "SELECT * FROM servers";
        List<Server> servers = new ArrayList<>();

        try(Connection connection = DatabaseManager.getHikariDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                servers.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return servers;
    }

    public static List<Server> getServerByPlayer(String username) {
        String sql = "SELECT * FROM servers WHERE players LIKE ?";
        List<Server> servers = new ArrayList<>();

        try(Connection connection = DatabaseManager.getHikariDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + username + "%");

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                servers.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return servers;
    }

    private static Server fromResultSet(ResultSet rs) throws SQLException {
        return new Server(rs.getLong("id"), rs.getString("host"), rs.getInt("port"), rs.getString("version"), rs.getString("description"), rs.getInt("onlinePlayers"), rs.getInt("maxPlayers"), rs.getString("modloader"), rs.getString("players"), rs.getBoolean("online"), rs.getLong("lastOnline"), rs.getString("city"), rs.getDouble("latitude"), rs.getDouble("longitude"));
    }
}
