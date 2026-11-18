package com.puggles.pokedexdemo

// address for gen 1 sprite set game versions
private const val SPRITE_BASE_URL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-i/"
enum class GameVersion {
    RED, BLUE, YELLOW
}

// pick sprite folder for game chosen
// Yellow has its own; Red and Blue share sprites
fun spriteUrl(id: Int, version: GameVersion): String{
    val folder = when(version){
        GameVersion.YELLOW -> "yellow"
        else -> "red-blue"
    }
    return "$SPRITE_BASE_URL$folder/transparent/$id.png"
}

