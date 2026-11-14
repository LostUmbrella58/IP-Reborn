package dev.efnilite.iep.storage

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.generator.Settings
import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.iep.leaderboard.Score
import org.sql2o.Sql2o
import java.util.*

internal object MySQLStorage {

    private val prefix = Config.CONFIG.getString("mysql.prefix")
    private lateinit var sql2o: Sql2o

    init {
        if (Config.CONFIG.getBoolean("mysql.enabled")) {
            sql2o = Sql2o(
                "jdbc:mysql://${Config.CONFIG.getString("mysql.url")}",
                Config.CONFIG.getString("mysql.username"),
                Config.CONFIG.getString("mysql.password")
            )
        }

        sql2o.beginTransaction().use { it.createQuery("""
            CREATE TABLE IF NOT EXISTS `${prefix}settings` (
                uuid CHAR(36) PRIMARY KEY,
                locale VARCHAR(5),
                style VARCHAR(32),
                radius TINYINT,
                time INT,
                seed INT,
                fall BOOLEAN,
                metric BOOLEAN,
                info BOOLEAN,
                rewards VARCHAR(255)
            )
            """.trimIndent())
            .executeUpdate() }
    }

    fun init(leaderboard: Leaderboard) {
        sql2o.beginTransaction().use { it.createQuery("""
            CREATE TABLE IF NOT EXISTS `$prefix${leaderboard.name}` (
                uuid CHAR(36) PRIMARY KEY,
                name VARCHAR(32),
                score DOUBLE,
                time INT,
                seed INT
            )
            """.trimIndent())
            .executeUpdate()

            it.commit()
        }
    }

    fun save(uuid: UUID, settings: Settings) {
        sql2o.beginTransaction().use {
            it.createQuery("""
                INSERT INTO `${prefix}settings` (uuid, locale, style, radius, time, seed, fall, metric, info, rewards) 
                VALUES (:uuid, :locale, :style, :radius, :time, :seed, :fall, :metric, :info, :rewards)
                ON DUPLICATE KEY UPDATE
                locale = VALUES(locale),
                style = VALUES(style),
                radius = VALUES(radius),
                time = VALUES(time),
                seed = VALUES(seed),
                fall = VALUES(fall),
                metric = VALUES(metric),
                info = VALUES(info),
                rewards = VALUES(rewards)
                """.trimIndent())
                .addParameter("uuid", uuid.toString())
                .addParameter("locale", settings.locale)
                .addParameter("style", settings.style)
                .addParameter("radius", settings.radius)
                .addParameter("time", settings.time)
                .addParameter("seed", settings.seed)
                .addParameter("fall", settings.fall)
                .addParameter("metric", settings.metric)
                .addParameter("info", settings.info)
                .addParameter("rewards", settings.rewards.joinToString(",") { c -> c.toString() })
                .executeUpdate()

            it.commit()
        }
    }

    fun load(uuid: UUID): Settings? {
        var settings: SqlSettings?

        sql2o.beginTransaction().use {
            settings = it.createQuery("SELECT * FROM `${prefix}settings` WHERE uuid = :uuid")
                .addParameter("uuid", uuid.toString())
                .executeAndFetch(SqlSettings::class.java).getOrNull(0)

            it.commit()
        }

        return if (settings == null) {
            null
        } else {
            Settings(
                locale = settings?.locale!!,
                style = settings?.style!!,
                radius = settings?.radius!!,
                time = settings?.time!!,
                seed = settings?.seed!!,
                fall = settings?.fall!!,
                metric = settings?.metric!!,
                info = settings?.info!!,
                rewards = settings?.rewards!!.let {
                    return@let if (it.isNotEmpty()) {
                        it.split(",").map { n -> n.toInt() }.toMutableSet()
                    } else {
                        mutableSetOf()
                    }
                }
            )
        }
    }

    fun save(leaderboard: Leaderboard) {
        sql2o.beginTransaction().use {
            for ((uuid, score) in leaderboard.snapshot()) {
                it.createQuery("""
                    INSERT INTO `${prefix}${leaderboard.name}` (uuid, name, score, time, seed) 
                    VALUES (:uuid, :name, :score, :time, :seed)
                    ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    score = VALUES(score),
                    time = VALUES(time),
                    seed = VALUES(seed)
                    """.trimIndent())
                    .bind(SqlScore(uuid.toString(), score.name, score.score, score.time, score.seed))
                    .executeUpdate()
            }

            it.commit()
        }
    }

    fun load(leaderboard: Leaderboard) {
        sql2o.beginTransaction().use { transaction ->
            val scores = transaction.createQuery("SELECT * FROM `${prefix}${leaderboard.name}`")
                .executeAndFetch(SqlScore::class.java)

            for (score in scores) {
                leaderboard.update(UUID.fromString(score.uuid), Score(score.name!!, score.score!!, score.time!!, score.seed!!))
            }

            transaction.commit()
        }
    }

    private class SqlSettings(val uuid: UUID?, val locale: String?, val style: String?, val radius: Int?,
                              val time: Int?, val seed: Int?, val fall: Boolean?, val metric: Boolean?,
                              val info: Boolean?, val rewards: String?) {

        @Suppress("unused")
        constructor() : this(null, null, null, null, null, null, null, null, null, null)

    }

    private class SqlScore(val uuid: String?, val name: String?, val score: Double?, val time: Long?, val seed: Int?) {

        @Suppress("unused")
        constructor() : this(null, null, null, null, null)

    }
}