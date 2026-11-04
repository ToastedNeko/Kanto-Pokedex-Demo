

package com.puggles.pokedexdemo

import /**/androidx.lifecycle.ViewModel
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

    private val api: PokemonApi = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PokemonApi::class.java)

    init {
        loadPokemon()
    }

    private fun loadPokemon() {
        viewModelScope.launch {
            try {
                val response = api.getPokemonList(limit = 151, offset = 0)
                val caughtIds = preferences.caughtIds.first()
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

    fun toggleCaught(pokemonId: Int) {
        viewModelScope.launch {
            preferences.toggleCaught(pokemonId)
            _pokemonList.value = _pokemonList.value.map { pokemon ->
                if (pokemon.id == pokemonId) {
                    pokemon.copy(isCaught = !pokemon.isCaught)
                } else {
                    pokemon
                }
            }
        }
    }
}