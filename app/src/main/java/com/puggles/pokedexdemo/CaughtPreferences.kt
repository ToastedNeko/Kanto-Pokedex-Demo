/*
* Allows a user to import or export their data for the app
* via DataStore file called "caught_prefs" that reads/writes the caught list
* can remove and add ids from this list
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
        private val CAUGHT_IDS = stringSetPreferencesKey("caught_ids")
    }


    // get current set of caught Pokemon
    val caughtIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[CAUGHT_IDS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    // add or remove an id from the saved set of Pokemon
    suspend fun toggleCaught(id: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[CAUGHT_IDS]?.toMutableSet() ?: mutableSetOf()
            val idStr = id.toString()
            if (current.contains(idStr)) {
                current.remove(idStr)
            } else {
                current.add(idStr)
            }
            prefs[CAUGHT_IDS] = current
        }
    }
}