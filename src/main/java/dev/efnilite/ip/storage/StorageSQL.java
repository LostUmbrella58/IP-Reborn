package dev.efnilite.ip.storage;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.foundation.util.Colls;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MySQL storage manager.
 * All access to the shared connection is synchronized on this class. All values are passed as statement parameters.
 *
 * @since 5.0.0
 */
class StorageSQL {

    /**
     * Minimum time between two reconnection attempts.
     */
    private static final long RECONNECT_COOLDOWN_MS = 30_000;

    private static Connection connection;
    private static long lastConnectAttempt = 0;
    private static boolean closed = false;

    static {
        // only disable the plugin when this happens on the main thread (during startup), never from async tasks
        if (!connect() && Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().disablePlugin(IP.getPlugin()); // disable plugin since data handling without db will go horribly wrong
        }
    }

    public static synchronized void init(String mode) {
        Connection c = getConnection();
        if (c == null) {
            IP.logging().error("Could not create SQL table for %s: no connection to MySQL".formatted(mode));
            return;
        }

        execute(c, """
                CREATE TABLE IF NOT EXISTS `%s`
                (
                    uuid       CHAR(36) NOT NULL PRIMARY KEY,
                    name       VARCHAR(16),
                    time       VARCHAR(16),
                    difficulty VARCHAR(3),
                    score      INT
                )
                CHARSET = utf8 ENGINE = InnoDB;
                """
                .formatted(getTableName(mode)), false);
    }

    public static synchronized void close() {
        closed = true;
        closeConnection();
        IP.log("Closed connection to MySQL");
    }

    public static synchronized @Nullable Map<UUID, Score> readScores(@NotNull String mode) {
        Connection c = getConnection();
        if (c == null) {
            IP.logging().warn("Skipping reading SQL data of %s: no connection to MySQL".formatted(mode));
            return null;
        }

        try (PreparedStatement statement = c.prepareStatement("SELECT * FROM `%s`;".formatted(getTableName(mode)));
             ResultSet results = statement.executeQuery()) {
            Map<UUID, Score> scores = new HashMap<>();

            while (results.next()) { // advance row
                String uuid = results.getString("uuid");

                try {
                    scores.put(UUID.fromString(uuid), new Score(
                            results.getString("name"),
                            results.getString("time"),
                            results.getString("difficulty"),
                            results.getInt("score")));
                } catch (IllegalArgumentException ex) {
                    IP.logging().warn("Skipping score with invalid uuid %s in %s".formatted(uuid, mode));
                }
            }

            return scores;
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to read SQL data of %s".formatted(mode), ex);
            return null;
        }
    }

    public static synchronized boolean writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores, @NotNull Set<UUID> removed) {
        Connection c = getConnection();
        if (c == null) {
            IP.logging().warn("Skipping saving SQL data of %s: no connection to MySQL, retrying on the next save".formatted(mode));
            return false;
        }

        String table = getTableName(mode);

        try {
            if (!removed.isEmpty()) {
                try (PreparedStatement statement = c.prepareStatement("DELETE FROM `%s` WHERE uuid = ?;".formatted(table))) {
                    for (UUID uuid : removed) {
                        statement.setString(1, uuid.toString());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }

            if (!scores.isEmpty()) {
                try (PreparedStatement statement = c.prepareStatement("""
                        INSERT INTO `%s`
                            (uuid, name, time, difficulty, score)
                        VALUES (?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE name       = ?,
                                                time       = ?,
                                                difficulty = ?,
                                                score      = ?;
                        """.formatted(table))) {
                    for (Map.Entry<UUID, Score> entry : scores.entrySet()) {
                        Score score = entry.getValue();

                        statement.setString(1, entry.getKey().toString());
                        statement.setString(2, score.name());
                        statement.setString(3, score.time());
                        statement.setString(4, score.difficulty());
                        statement.setInt(5, score.score());
                        statement.setString(6, score.name());
                        statement.setString(7, score.time());
                        statement.setString(8, score.difficulty());
                        statement.setInt(9, score.score());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }

            return true;
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to write SQL data of %s".formatted(mode), ex);
            return false;
        }
    }

    // returns leaderboard table name
    private static String getTableName(String mode) {
        return "%sleaderboard-%s".formatted(Option.SQL_PREFIX, mode);
    }

    public static synchronized void readPlayer(@NotNull ParkourPlayer player) {
        Connection c = getConnection();
        if (c == null) {
            IP.logging().warn("Could not read SQL data of %s: no connection to MySQL, using default settings".formatted(player.getName()));
            player.settingsUnavailable = true;
            player.setSettings(new HashMap<>());
            return;
        }

        try (PreparedStatement statement = c.prepareStatement("SELECT * FROM `%soptions` WHERE uuid = ?;".formatted(Option.SQL_PREFIX))) {
            statement.setString(1, player.getUUID().toString());

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) { // move cursor
                    player.setSettings(new HashMap<>());
                    return;
                }

                Map<String, Object> settings = Colls.thread(ParkourPlayer.PLAYER_COLUMNS).mapv((key, value) -> {
                    try {
                        return results.getObject(key);
                    } catch (SQLException ex) {
                        IP.logging().stack("Error while trying to read SQL data of %s, option = %s".formatted(player.getName(), key), ex);
                        return null;
                    }
                }).get();

                player.setSettings(settings);
            }
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to read SQL data of %s".formatted(player.getName()), ex);
            player.settingsUnavailable = true;
            player.setSettings(new HashMap<>());
        }
    }

    public static synchronized void writePlayer(@NotNull ParkourPlayer player) {
        if (player.settingsUnavailable) {
            IP.logging().warn("Not saving SQL data of %s: their stored data couldn't be read when they joined".formatted(player.getName()));
            return;
        }
        Connection c = getConnection();
        if (c == null) {
            IP.logging().warn("Could not save SQL data of %s: no connection to MySQL".formatted(player.getName()));
            return;
        }

        Object[] values = {
                player.style,
                player.blockLead,
                Boolean.TRUE.equals(player.particles),
                Boolean.TRUE.equals(player.useSpecialBlocks),
                Boolean.TRUE.equals(player.showFallMessage),
                Boolean.TRUE.equals(player.showScoreboard),
                player.selectedTime,
                player.collectedRewards == null ? "" : String.join(",", player.collectedRewards),
                player.locale,
                player.schematicDifficulty,
                Boolean.TRUE.equals(player.sound)
        };

        try (PreparedStatement statement = c.prepareStatement("""
                INSERT INTO `%soptions`
                (uuid, style, blockLead, useParticles, useSpecial, showFallMsg, showScoreboard,
                 selectedTime, collectedRewards, locale, schematicDifficulty, sound)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE style               = ?,
                                        blockLead           = ?,
                                        useParticles        = ?,
                                        useSpecial          = ?,
                                        showFallMsg         = ?,
                                        showScoreboard      = ?,
                                        selectedTime        = ?,
                                        collectedRewards    = ?,
                                        locale              = ?,
                                        schematicDifficulty = ?,
                                        sound               = ?;
                """.formatted(Option.SQL_PREFIX))) {
            statement.setString(1, player.getUUID().toString());
            for (int i = 0; i < values.length; i++) {
                statement.setObject(2 + i, values[i]); // insert values
                statement.setObject(2 + values.length + i, values[i]); // update values
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to save SQL data of %s".formatted(player.getName()), ex);
        }
    }

    /**
     * (Re)connects to the database. Never disables the plugin, so it is safe to call from async threads.
     *
     * @return True if the connection was established.
     */
    private static synchronized boolean connect() {
        lastConnectAttempt = System.currentTimeMillis();
        closeConnection();

        try {
            IP.log("Connecting to MySQL");

            try { // load drivers
                Class.forName("com.mysql.cj.jdbc.Driver"); // for newer versions
            } catch (ClassNotFoundException old) {
                Class.forName("com.mysql.jdbc.Driver"); // for older versions
            }

            Connection c = DriverManager.getConnection(("jdbc:mysql://%s:%d/%s?allowPublicKeyRetrieval=true" +
                    "&useSSL=false&useUnicode=true&characterEncoding=utf-8" +
                    "&autoReconnect=true&maxReconnects=5&rewriteBatchedStatements=true").formatted(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB), Option.SQL_USERNAME, Option.SQL_PASSWORD);

            execute(c, "CREATE DATABASE IF NOT EXISTS `%s`;".formatted(Option.SQL_DB), false);
            execute(c, "USE `%s`;".formatted(Option.SQL_DB), false);

            execute(c, "CREATE TABLE IF NOT EXISTS `%soptions` (`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(32), `blockLead` INT, `useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useSpecial` BOOLEAN, `showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8;".formatted(Option.SQL_PREFIX), false);

            // v3.0.0
            execute(c, "ALTER TABLE `%soptions` DROP COLUMN `time`;".formatted(Option.SQL_PREFIX), true);
            execute(c, "ALTER TABLE `%soptions` ADD `selectedTime` INT NOT NULL;".formatted(Option.SQL_PREFIX), true);

            // v3.1.0
            execute(c, "ALTER TABLE `%soptions` ADD `collectedRewards` MEDIUMTEXT;".formatted(Option.SQL_PREFIX), true);

            // v3.6.0
            execute(c, "ALTER TABLE `%s` ADD `locale` VARCHAR(8);".formatted(Option.SQL_PREFIX + "options"), true);
            execute(c, "ALTER TABLE `%s` ADD `schematicDifficulty` DOUBLE;".formatted(Option.SQL_PREFIX + "options"), true);

            // v4.0.0
            execute(c, "ALTER TABLE `%s` ADD `sound` BOOLEAN;".formatted(Option.SQL_PREFIX + "options"), true);

            // 5.0.0
            execute(c, "ALTER TABLE `%soptions` DROP COLUMN `useDifficulty`;".formatted(Option.SQL_PREFIX), true);
            execute(c, "ALTER TABLE `%soptions` DROP COLUMN `useStructure`;".formatted(Option.SQL_PREFIX), true);

            connection = c;
            IP.log("Connected to MySQL");
            return true;
        } catch (Exception ex) {
            IP.logging().stack("Could not connect to MySQL", "check your SQL settings in the config", ex);
            return false;
        }
    }

    // closes the current connection, if any
    private static synchronized void closeConnection() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to close connection to SQL database", ex);
        } finally {
            connection = null;
        }
    }

    // returns a valid connection, reconnecting if needed. null if there is no connection available right now.
    private static synchronized @Nullable Connection getConnection() {
        if (closed) {
            return null;
        }

        try {
            if (connection != null && connection.isValid(10)) {
                return connection;
            }
        } catch (SQLException ignored) {
            // treated as invalid
        }

        if (System.currentTimeMillis() - lastConnectAttempt < RECONNECT_COOLDOWN_MS) {
            return null; // don't hammer the database, retry later
        }

        return connect() ? connection : null;
    }

    // executes an update, optionally ignoring errors
    private static void execute(Connection c, String sql, boolean suppressErrors) {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (!suppressErrors) {
                IP.logging().stack("Error while sending query %s".formatted(sql), ex);
            }
        }
    }
}
