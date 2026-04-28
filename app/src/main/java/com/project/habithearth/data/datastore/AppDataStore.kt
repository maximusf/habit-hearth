package com.project.habithearth.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.project.habithearth.data.proto.UserProgressProto

/**
 * Process-singleton typed DataStore holding every restart-surviving piece of
 * app state (tasks, gem/coin/XP progress, owned buildings, profile settings).
 *
 * Filename intentionally differs from the legacy Preferences DataStore used by
 * [com.project.habithearth.data.UserProgressRepository] so the two stores can
 * coexist while Phase 3..6 of the refactor migrate consumers off the legacy
 * blob. Once the legacy repository is deleted in Phase 6, the old preferences
 * file becomes orphaned and can be removed in a follow-up cleanup.
 *
 * Access is exposed as a [Context] extension property because [dataStore]
 * delegates require a Context receiver and this matches the official Android
 * Proto DataStore guidance.
 */
val Context.userProgressDataStore: DataStore<UserProgressProto> by dataStore(
    fileName = "user_progress.pb",
    serializer = UserProgressSerializer,
)
