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
 *
 * Named `userProgressProtoDataStore` (rather than just `userProgressDataStore`)
 * to disambiguate from the legacy file-private Preferences DataStore inside
 * [com.project.habithearth.data.UserProgressRepository], which uses the
 * shorter name. Both extensions hang off `Context`, so collapsing them under
 * the same name made it easy to import the wrong one in new code. Once the
 * legacy repository is deleted in the second milestone, this can drop the
 * `Proto` qualifier.
 */
val Context.userProgressProtoDataStore: DataStore<UserProgressProto> by dataStore(
    fileName = "user_progress.pb",
    serializer = UserProgressSerializer,
)
