package com.project.habithearth.data.story

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

// Lightweight per-chapter persistence kept separate from the larger
// UserProgressProto refactor described in PLAN.md. Chapter 1 only needs four
// scalar fields plus a small string map, so a Preferences DataStore avoids the
// proto schema overhead and a future StoryProto rewrite can drain this into
// the unified store without breaking the screen layer.
private val Context.chapter1DataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chapter_1_progress",
)

class Chapter1ProgressRepository(context: Context) {

    private val ds = context.applicationContext.chapter1DataStore
    private val gson = Gson()
    // Map<intro-node-id, picked-choice-label>. Stored as JSON because the
    // labels can contain punctuation that complicates a delimited format.
    private val choicesType = object : TypeToken<Map<String, String>>() {}.type

    data class Snapshot(
        val visitedNodeIds: List<String> = emptyList(),
        val madeChoices: Map<String, String> = emptyMap(),
        val currentPageIndex: Int = 0,
        val hasShownEnding: Boolean = false,
    )

    suspend fun load(): Snapshot {
        val prefs = ds.data.first()
        val visited = prefs[KeyVisited]
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val choicesJson = prefs[KeyChoices]
        val choices: Map<String, String> = if (choicesJson.isNullOrBlank()) {
            emptyMap()
        } else {
            // Defensive parse: a corrupted blob (manual wipe, version mismatch)
            // shouldn't crash the screen on first frame, just drop the map.
            runCatching { gson.fromJson<Map<String, String>>(choicesJson, choicesType) }
                .getOrNull() ?: emptyMap()
        }
        return Snapshot(
            visitedNodeIds = visited,
            madeChoices = choices,
            currentPageIndex = prefs[KeyIndex] ?: 0,
            hasShownEnding = prefs[KeyEnding] ?: false,
        )
    }

    suspend fun save(snapshot: Snapshot) {
        ds.edit { prefs ->
            prefs[KeyVisited] = snapshot.visitedNodeIds.joinToString(",")
            prefs[KeyChoices] = gson.toJson(snapshot.madeChoices)
            prefs[KeyIndex] = snapshot.currentPageIndex
            prefs[KeyEnding] = snapshot.hasShownEnding
        }
    }

    suspend fun clear() {
        ds.edit { it.clear() }
    }

    private companion object {
        val KeyVisited = stringPreferencesKey("visited")
        val KeyChoices = stringPreferencesKey("choices")
        val KeyIndex = intPreferencesKey("index")
        val KeyEnding = booleanPreferencesKey("ending")
    }
}
