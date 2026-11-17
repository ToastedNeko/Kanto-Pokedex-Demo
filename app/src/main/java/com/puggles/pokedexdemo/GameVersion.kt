package com.puggles.pokedexdemo

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
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-i/$folder/transparent/$id.png"
}

