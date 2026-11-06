package com.puggles.pokedexdemo

import android.R.attr.version
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
    private val _currentVersion = MutableStateFlow(GameVersion.YELLOW)
    val currentVersion: StateFlow<GameVersion> = _currentVersion.asStateFlow()

    // retrieve the list of Pokemon from PokeApi
    private val api: PokemonApi = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PokemonApi::class.java)

    init {
        loadPokemon()
    }

    // switch version and reload the caught status for chosen version
    fun setVersion(version: GameVersion){
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

    fun importCaught(ids: Set<Int>) {
        viewModelScope.launch {
            val version = _currentVersion.value
            // IDs that are currently marked caught in the app
            val current = _pokemonList.value.filter { it.isCaught }.map { it.id }.toSet()
            // IDs we need to un-catch (in app but not in the imported file)
            val toRemove = current - ids
            // IDs we need to catch (in the imported file but not currently caught)
            val toAdd = ids - current
            // update DataStore so the changes survive app restarts
            toRemove.forEach { preferences.toggleCaught(version,it) }
            toAdd.forEach { preferences.toggleCaught(version,it) }
            // update the list the UI is showing right now
            _pokemonList.value = _pokemonList.value.map { pokemon ->
                pokemon.copy(isCaught = ids.contains(pokemon.id))
            }
        }
    }
}