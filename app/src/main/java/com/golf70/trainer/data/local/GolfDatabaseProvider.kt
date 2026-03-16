package com.golf70.trainer.data.local

import android.content.Context
import androidx.room.Room

object GolfDatabaseProvider {
    @Volatile
    private var db: GolfDatabase? = null

    fun get(context: Context): GolfDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                GolfDatabase::class.java,
                "golf70.db"
            ).build().also { db = it }
        }
    }
}
