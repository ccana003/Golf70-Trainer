package com.golf70.trainer.ui.navigation

import android.content.Context
import com.golf70.trainer.data.local.GolfDatabaseProvider
import com.golf70.trainer.repository.GolfRepository

object Dependencies {
    fun repository(context: Context): GolfRepository = GolfRepository(GolfDatabaseProvider.get(context))
}
