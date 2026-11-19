package com.puggles.pokedexdemo

import androidx.compose.ui.graphics.Color

// address for gen 1 sprite set game versions
private const val SPRITE_BASE_URL =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-i/"

enum class GameVersion {
    RED, BLUE, YELLOW
}

// pick sprite folder for game chosen
// Yellow has its own; Red and Blue share sprites
fun spriteUrl(id: Int, version: GameVersion): String {
    val folder = when (version) {
        GameVersion.YELLOW -> "yellow"
        else -> "red-blue"
    }
    return "$SPRITE_BASE_URL$folder/transparent/$id.png"
}

// color for each specific game version
fun gameColor(version: GameVersion): Color = when (version) {
    GameVersion.RED -> Color(0xFFD32F2F)
    GameVersion.BLUE -> Color(0xFF1E5BC6)
    GameVersion.YELLOW -> Color(0xFFF8D030)
}

// text color switches depending on if Yellow version or not
fun onGameColor(version: GameVersion): Color = when (version) {
    GameVersion.YELLOW -> Color.Black
    else -> Color.White
}
