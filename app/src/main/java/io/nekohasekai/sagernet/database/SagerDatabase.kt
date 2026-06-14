package io.nekohasekai.sagernet.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.matrix.roomigrant.GenerateRoomMigrations
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.fmt.KryoConverters
import io.nekohasekai.sagernet.fmt.gson.GsonConverters
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Database(
    entities = [ProxyGroup::class, ProxyEntity::class, RuleEntity::class],
    version = 8,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7)
    ]
)
@TypeConverters(value = [KryoConverters::class, GsonConverters::class])
@GenerateRoomMigrations
abstract class SagerDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `proxy_entities_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `groupId` INTEGER NOT NULL,
                        `type` INTEGER NOT NULL,
                        `userOrder` INTEGER NOT NULL,
                        `tx` INTEGER NOT NULL,
                        `rx` INTEGER NOT NULL,
                        `status` INTEGER NOT NULL,
                        `ping` INTEGER NOT NULL,
                        `uuid` TEXT NOT NULL,
                        `error` TEXT,
                        `socksBean` BLOB,
                        `httpBean` BLOB,
                        `ssBean` BLOB,
                        `ssrBean` BLOB DEFAULT NULL,
                        `vmessBean` BLOB,
                        `trojanBean` BLOB,
                        `trojanGoBean` BLOB,
                        `mieruBean` BLOB,
                        `naiveBean` BLOB,
                        `hysteriaBean` BLOB,
                        `tuicBean` BLOB,
                        `juicityBean` BLOB,
                        `sshBean` BLOB,
                        `wgBean` BLOB,
                        `shadowTLSBean` BLOB,
                        `anyTLSBean` BLOB,
                        `chainBean` BLOB,
                        `nekoBean` BLOB,
                        `configBean` BLOB,
                        `snellBean` BLOB DEFAULT NULL
                    )
                """.trimIndent())
                
                val cursor = database.query("PRAGMA table_info(proxy_entities)")
                val existingColumns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(1))
                }
                cursor.close()

                val targetColumns = listOf(
                    "id", "groupId", "type", "userOrder", "tx", "rx", "status", "ping", "uuid", "error",
                    "socksBean", "httpBean", "ssBean", "ssrBean", "vmessBean", "trojanBean", "trojanGoBean",
                    "mieruBean", "naiveBean", "hysteriaBean", "tuicBean", "juicityBean", "sshBean", "wgBean",
                    "shadowTLSBean", "anyTLSBean", "chainBean", "nekoBean", "configBean", "snellBean"
                )
                val columnsToCopy = existingColumns.filter { targetColumns.contains(it) }
                val columnsCsv = columnsToCopy.joinToString(", ") { "`$it`" }

                if (columnsCsv.isNotEmpty()) {
                    database.execSQL("INSERT INTO `proxy_entities_new` ($columnsCsv) SELECT $columnsCsv FROM `proxy_entities`")
                }

                database.execSQL("DROP TABLE `proxy_entities`")
                database.execSQL("ALTER TABLE `proxy_entities_new` RENAME TO `proxy_entities`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `groupId` ON `proxy_entities` (`groupId`)")

                // The `rules` table gained a `ruleset` column (TEXT NOT NULL DEFAULT '') in v8.
                // Older fork DBs at "version 7" may lack it, which fails schema validation.
                val rulesCursor = database.query("PRAGMA table_info(`rules`)")
                val ruleColumns = mutableListOf<String>()
                while (rulesCursor.moveToNext()) {
                    ruleColumns.add(rulesCursor.getString(1))
                }
                rulesCursor.close()
                if (!ruleColumns.contains("ruleset")) {
                    database.execSQL("ALTER TABLE `rules` ADD COLUMN `ruleset` TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        @Suppress("EXPERIMENTAL_API_USAGE")
        val instance by lazy {
            SagerNet.application.getDatabasePath(Key.DB_PROFILE).parentFile?.mkdirs()
            Room.databaseBuilder(SagerNet.application, SagerDatabase::class.java, Key.DB_PROFILE)
                .addMigrations(MIGRATION_7_8)
                .setJournalMode(JournalMode.TRUNCATE)
                .allowMainThreadQueries()
                .enableMultiInstanceInvalidation()
                .fallbackToDestructiveMigration()
                .setQueryExecutor { GlobalScope.launch { it.run() } }
                .build()
        }

        val groupDao get() = instance.groupDao()
        val proxyDao get() = instance.proxyDao()
        val rulesDao get() = instance.rulesDao()

    }

    abstract fun groupDao(): ProxyGroup.Dao
    abstract fun proxyDao(): ProxyEntity.Dao
    abstract fun rulesDao(): RuleEntity.Dao

}
