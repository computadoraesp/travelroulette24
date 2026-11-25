package com.example.travelroulette24.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Locale
import kotlin.math.min
import kotlin.math.round

/**
 * Local SQLite database storing the historical lowest observed price per destination
 * for the "Favorite Trip" (3-destination tracker) feature.
 *
 * Used strictly to track historical lows and compute the user's acceptance range
 * (e.g. currentPrice <= historicalLowest * (1 + tolerancePercent / 100)).
 */
open class FavoritePriceHistoryDb(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION
) {
    data class HistoricalPriceRecord(
        val destinationHub: String,
        val lowestPrice: Double,
        val lastPrice: Double,
        val observations: Int,
        val updatedAt: Long
    )

    data class AcceptanceEvaluation(
        val currentPrice: Double,
        val lowestHistoricalPrice: Double,
        val maxAcceptablePrice: Double,
        val isNewRecordLow: Boolean,
        val isWithinAcceptanceRange: Boolean,
        val deltaPercent: Double
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_DEST_HUB TEXT PRIMARY KEY,
                $COL_LOWEST_PRICE REAL NOT NULL,
                $COL_LAST_PRICE REAL NOT NULL,
                $COL_OBSERVATIONS INTEGER NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Records an observed price for [destinationHub] and updates the historical lowest price if lower.
     */
    @Synchronized
    fun recordOfferPrice(destinationHub: String, price: Double): HistoricalPriceRecord {
        val hubKey = destinationHub.trim().uppercase(Locale.US)
        val now = System.currentTimeMillis()
        val existing = getHistoryForDestination(hubKey)

        val newLowest = if (existing != null && existing.lowestPrice > 0.0) {
            min(existing.lowestPrice, price)
        } else {
            price
        }
        val newObs = (existing?.observations ?: 0) + 1

        val values = ContentValues().apply {
            put(COL_DEST_HUB, hubKey)
            put(COL_LOWEST_PRICE, newLowest)
            put(COL_LAST_PRICE, price)
            put(COL_OBSERVATIONS, newObs)
            put(COL_UPDATED_AT, now)
        }

        writableDatabase.insertWithOnConflict(
            TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        return HistoricalPriceRecord(
            destinationHub = hubKey,
            lowestPrice = newLowest,
            lastPrice = price,
            observations = newObs,
            updatedAt = now
        )
    }

    /**
     * Records a batch of observed offer prices inside a single SQLite transaction
     * and returns the updated map of all historical records.
     */
    @Synchronized
    fun recordObservedOffers(offers: List<Pair<String, Double>>): Map<String, HistoricalPriceRecord> {
        if (offers.isEmpty()) return getAllHistory()
        val now = System.currentTimeMillis()
        val existingMap = getAllHistory().toMutableMap()
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((hub, price) in offers) {
                if (price <= 0.0 || hub.isBlank()) continue
                val hubKey = hub.trim().uppercase(Locale.US)
                val existing = existingMap[hubKey]
                val newLowest = if (existing != null && existing.lowestPrice > 0.0) {
                    min(existing.lowestPrice, price)
                } else {
                    price
                }
                val newObs = (existing?.observations ?: 0) + 1
                val values = ContentValues().apply {
                    put(COL_DEST_HUB, hubKey)
                    put(COL_LOWEST_PRICE, newLowest)
                    put(COL_LAST_PRICE, price)
                    put(COL_OBSERVATIONS, newObs)
                    put(COL_UPDATED_AT, now)
                }
                db.insertWithOnConflict(
                    TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                existingMap[hubKey] = HistoricalPriceRecord(
                    destinationHub = hubKey,
                    lowestPrice = newLowest,
                    lastPrice = price,
                    observations = newObs,
                    updatedAt = now
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return existingMap
    }

    @Synchronized
    fun getHistoryForDestination(destinationHub: String): HistoricalPriceRecord? {
        val hubKey = destinationHub.trim().uppercase(Locale.US)
        readableDatabase.query(
            TABLE_NAME,
            arrayOf(COL_DEST_HUB, COL_LOWEST_PRICE, COL_LAST_PRICE, COL_OBSERVATIONS, COL_UPDATED_AT),
            "$COL_DEST_HUB = ?",
            arrayOf(hubKey),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return HistoricalPriceRecord(
                destinationHub = cursor.getString(0),
                lowestPrice = cursor.getDouble(1),
                lastPrice = cursor.getDouble(2),
                observations = cursor.getInt(3),
                updatedAt = cursor.getLong(4)
            )
        }
    }

    @Synchronized
    open fun getAllHistory(): Map<String, HistoricalPriceRecord> {
        val map = mutableMapOf<String, HistoricalPriceRecord>()
        readableDatabase.query(
            TABLE_NAME,
            arrayOf(COL_DEST_HUB, COL_LOWEST_PRICE, COL_LAST_PRICE, COL_OBSERVATIONS, COL_UPDATED_AT),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val rec = HistoricalPriceRecord(
                    destinationHub = cursor.getString(0),
                    lowestPrice = cursor.getDouble(1),
                    lastPrice = cursor.getDouble(2),
                    observations = cursor.getInt(3),
                    updatedAt = cursor.getLong(4)
                )
                map[rec.destinationHub] = rec
            }
        }
        return map
    }

    companion object {
        private const val DB_NAME = "favorite_price_history.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "price_history"
        private const val COL_DEST_HUB = "destination_hub"
        private const val COL_LOWEST_PRICE = "lowest_price"
        private const val COL_LAST_PRICE = "last_price"
        private const val COL_OBSERVATIONS = "observations"
        private const val COL_UPDATED_AT = "updated_at"

        /**
         * Evaluates whether [currentPrice] falls within the user's acceptance range
         * relative to [lowestHistoricalPrice] and [tolerancePercent].
         */
        fun evaluateAcceptance(
            currentPrice: Double,
            lowestHistoricalPrice: Double,
            tolerancePercent: Int
        ): AcceptanceEvaluation {
            val safeLowest = if (lowestHistoricalPrice > 0.0) lowestHistoricalPrice else currentPrice
            val safeTolerance = tolerancePercent.coerceIn(0, 100)
            val maxAcceptable = round(safeLowest * (1.0 + safeTolerance / 100.0) * 100.0) / 100.0
            val deltaPct = if (safeLowest > 0.0) {
                round(((currentPrice - safeLowest) / safeLowest) * 1000.0) / 10.0
            } else 0.0

            return AcceptanceEvaluation(
                currentPrice = currentPrice,
                lowestHistoricalPrice = safeLowest,
                maxAcceptablePrice = maxAcceptable,
                isNewRecordLow = currentPrice <= safeLowest,
                isWithinAcceptanceRange = currentPrice <= maxAcceptable,
                deltaPercent = deltaPct
            )
        }
    }
}
