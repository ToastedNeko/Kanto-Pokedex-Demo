/*
* Allows a user to import or export their data for the app
* via DataStore file called "caught_prefs" that reads/writes the caught list
* can remove and add ids from this list
* done for specific game version that's chosen by a user
*
* */

package com.puggles.pokedexdemo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "caught_prefs")

class CaughtPreferences(private val context: Context) {
    companion object {
        // Pokemon caught per specific game version, RED, YELLOW, or BLUE
        private fun keyFor(version: GameVersion) =
            stringSetPreferencesKey("caught_ids_${version.name}")
    }


    // get current set of caught Pokemon
    fun caughtIds(version: GameVersion): Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[keyFor(version)]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    // add or remove an id from the saved set of Pokemon for specific game version
    suspend fun toggleCaught(version: GameVersion, id: Int) {
        context.dataStore.edit { prefs ->
            val key = keyFor(version)
            val current = prefs[key]?.toMutableSet() ?: mutableSetOf()
            val idStr = id.toString()
            if (current.contains(idStr)) current.remove(idStr) else current.add(idStr)
            prefs[key] = current
        }
    }
}