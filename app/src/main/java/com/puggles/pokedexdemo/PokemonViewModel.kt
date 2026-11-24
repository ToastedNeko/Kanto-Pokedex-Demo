package com.puggles.pokedexdemo

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.first

data class Pokemon(
    val id: Int,
    val name: String,
    val types: List<String> = emptyList(),
    val isCaught: Boolean = false
)

class PokemonViewModel(application: Application) : AndroidViewModel(application) {
    private val _pokemonList = MutableStateFlow<List<Pokemon>>(emptyList())
    val pokemonList: StateFlow<List<Pokemon>> = _pokemonList.asStateFlow()
    private val preferences = CaughtPreferences(application)

    // get the current game version
    private val _currentVersion = MutableStateFlow(GameVersion.RED)
    val currentVersion: StateFlow<GameVersion> = _currentVersion.asStateFlow()

    // retrieve the list of Pokemon from PokeApi
    private val api: PokemonApi = Retrofit.Builder().baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(GsonConverterFactory.create()).build().create(PokemonApi::class.java)

    // types of pokemon for gen 1
    private val gen1Types = listOf(
        "normal", "fire", "water", "electric", "grass", "ice",
        "fighting", "poison", "ground", "flying", "psychic",
        "bug", "rock", "ghost", "dragon",
    )

    // types for each of the pokemon saved on loading them
    private var typesById: Map<Int, List<String>> = emptyMap()

    private suspend fun loadTypes() {
        if (typesById.isNotEmpty()) return

        // with the Pokemon's number map it to the types with
        // slots (ie, slot 1 is grass, slot 2 poison)
        val found = mutableMapOf<Int, MutableList<Pair<Int, String>>>()

        // ask PokeAPI for Gen 1 type, one at a time
        for (typeName in gen1Types) {
            // go through every Pokemon with this type
            val response = api.getType(typeName)
            response.pokemon.forEach { entry ->
                // get the Pokemon's number out of the url, ie "/pokemon/1/" would be 1
                val id = entry.pokemon.url.trimEnd('/').substringAfterLast('/').toInt()

                // keep only Gen 1 Pokemon types
                if (id <= 151) {
                    // add this type to the Pokemon's slot list, as a pair (1, "grass")
                    // slots, 1st or 2nd, is kept so the types can be put in order later
                    found.getOrPut(id) { mutableListOf() }.add(entry.slot to typeName)
                }
            }
        }

        // put Pokemon's types in order, then keep the type names
        // ie, (2, "poison"), (1, "grass") becomes ["grass", "poison"]
        typesById = found.mapValues { (_, list) -> list.sortedBy { it.first }.map { it.second } }
    }

    init {
        loadPokemon()
    }

    // switch version and reload the caught status for chosen version
    fun setVersion(version: GameVersion) {
        _currentVersion.value = version
        loadPokemon()
    }

    // load each Pokemon from the game selected
    private fun loadPokemon() {
        viewModelScope.launch {
            try {
                val response = api.getPokemonList(limit = 151, offset = 0)
                val caughtIds = preferences.caughtIds(_currentVersion.value).first()
                val pokemon = response.results.mapIndexed { index, result ->
                    val id = index + 1
                    Pokemon(
                        id = id,
                        name = result.name.replaceFirstChar { it.uppercase() },
                        isCaught = caughtIds.contains(id)
                    )
                }
                _pokemonList.value = pokemon
                // load Gen1 Pokemon types (grass, poison, etc.)
                // give each Pokemon its type(s)
                loadTypes()
                _pokemonList.value = _pokemonList.value.map { p ->
                    // use the Gen1 types, or "normal" if none were found
                    // (only Clefairy and Clefable: they're Fairy now, which didn't exist in Gen1)
                    p.copy(types = typesById[p.id] ?: listOf("normal"))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // show or hide caught pokemon state
    fun toggleCaught(pokemonId: Int) {
        viewModelScope.launch {
            preferences.toggleCaught(_currentVersion.value, pokemonId)
            _pokemonList.value = _pokemonList.value.map { pokemon ->
                if (pokemon.id == pokemonId) {
                    pokemon.copy(isCaught = !pokemon.isCaught)
                } else {
                    pokemon
                }
            }
        }
    }

//    private val _exportResult = MutableStateFlow("")

    fun exportAllCaught(onExport: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = preferences.exportAll()
                onExport(data)          // calls lambda pass from MainActivity
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importAllCaught(data: String) {
        viewModelScope.launch {
            preferences.importAll(data)
            loadPokemon()
        }
    }

    // reset all caught Pokemon from all versions
    fun resetAll() {
        viewModelScope.launch {
            try {
                preferences.importAll("red:|blue:|yellow:")
                loadPokemon()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
