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
import kotlinx.coroutines.flow.first
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

    // import/export caught Pokemon for ALL versions at once
    suspend fun exportAll(): String {
        val red = caughtIds(GameVersion.RED).first()
        val blue = caughtIds(GameVersion.BLUE).first()
        val yellow = caughtIds(GameVersion.YELLOW).first()

        return "red:${red.joinToString ( "," )}|blue:${blue.joinToString ( "," )}|yellow:${yellow.joinToString ( "," )}"
    }

    suspend fun importAll(data: String){
        // split the exported string into three version pieces
        // ie "red:1,2,3|..." becomes -> "red:1,2,3",...
        val parts = data.split("|")
        for (part in parts) {
            // split each piece on the colon
            val pieces = part.split(":")
            if(pieces.size == 2){
                val versionName = pieces[0] // "red", "blue", "yellow" versions
                val ids = pieces[1].split(",").mapNotNull { it.toIntOrNull() }.toSet()

                // convert to GameVersion ENUM (RED, BLUE, YELLOW)
                val version = when (versionName) {
                    "red" -> GameVersion.RED
                    "blue" -> GameVersion.BLUE
                    "yellow" -> GameVersion.YELLOW
                    else -> continue
                }

                // replace all three versions' caught Pokemon with imported data
                // old data is written over
                context.dataStore.edit { prefs ->
                    prefs[keyFor(version)] = ids.map { it.toString() }.toSet()
                }
            }
        }
    }
}