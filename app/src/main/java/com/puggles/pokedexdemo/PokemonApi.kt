/*
 * API calls being done to PokeApi to generate each Pokemon result
 * Type details include: id, name, and details about each Pokemon
 *
 */

package com.puggles.pokedexdemo

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class PokemonListResponse(
    val results: List<PokemonResult>
)

data class PokemonResult(
    val name: String,
    val url: String
)

data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val types: List<TypeSlot>,
    val stats: List<StatSlot>,
    val flavorTextEntries: List<FlavorTextEntry> = emptyList()
)

data class PokemonSpeciesResponse(
    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<FlavorTextEntry>? = emptyList()
)

data class TypeSlot(
    val type: Type
)

data class Type(
    val name: String
)

data class StatSlot(
    @SerializedName("base_stat")
    val baseStat: Int,
    val stat: Stat
)

data class Stat(
    val name: String
)

data class FlavorTextEntry(
    @SerializedName("flavor_text")
    val flavorText: String?,
    val language: FlavorLanguage?
)

data class FlavorLanguage(
    val name: String
)

// flavor text (Pokedex description) comes from the species endpoint.
// The regular /pokemon/{id} endpoint does not include it.
interface PokemonApi {
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 151,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse

    @GET("pokemon/{id}")
    suspend fun getPokemonDetail(@Path("id") id: Int): PokemonDetailResponse

    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpecies(@Path("id") id: Int): PokemonSpeciesResponse

}