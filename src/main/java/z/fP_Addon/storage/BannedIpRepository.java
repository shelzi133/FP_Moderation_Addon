package z.fP_Addon.storage;

import z.fP_Addon.FP_Addon;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BannedIpRepository {

    public record Status(boolean active, boolean justExpired) {
    }

    private final FP_Addon plugin;
    private final String jdbcUrl;
    private Connection connection;

    public BannedIpRepository(FP_Addon plugin) {
        this.plugin = plugin;
        File dbFile = new File(plugin.getDataFolder(), "fp_addon.db");
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public synchronized void init() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(jdbcUrl);

            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS banned_ips (" +
                            "ip TEXT PRIMARY KEY, " +
                            "until INTEGER NOT NULL)"
            )) {
                statement.executeUpdate();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось инициализировать базу данных FP_Addon: " + e);
        }
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    public synchronized void ban(String ip, long untilMillis) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO banned_ips (ip, until) VALUES (?, ?) " +
                        "ON CONFLICT(ip) DO UPDATE SET until = excluded.until"
        )) {
            statement.setString(1, ip);
            statement.setLong(2, untilMillis);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось сохранить бан IP " + ip + ": " + e);
        }
    }

    public synchronized void unban(String ip) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM banned_ips WHERE ip = ?"
        )) {
            statement.setString(1, ip);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось убрать бан IP " + ip + ": " + e);
        }
    }

    public synchronized boolean isBanned(String ip) {
        return status(ip).active();
    }

    public synchronized Status status(String ip) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT until FROM banned_ips WHERE ip = ?"
        )) {
            statement.setString(1, ip);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return new Status(false, false);

                long until = resultSet.getLong("until");
                if (until == -1L) return new Status(true, false);

                if (System.currentTimeMillis() >= until) {
                    unban(ip);
                    return new Status(false, true);
                }

                return new Status(true, false);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось проверить бан IP " + ip + ": " + e);
            return new Status(false, false);
        }
    }
}
