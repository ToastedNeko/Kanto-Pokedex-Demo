/*
* Calls PokeApi to get details for each Pokemon
* when a Pokemon is selected in the app
* */

package com.puggles.pokedexdemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PokemonDetailScreen(pokemonId: Int, viewModel: PokemonViewModel) {
    var detail by remember { mutableStateOf<PokemonDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var flavorTextEntries by remember { mutableStateOf<List<FlavorTextEntry>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pokemonId) {
        scope.launch {
            try {
                val api = Retrofit.Builder().baseUrl("https://pokeapi.co/api/v2/")
                    .addConverterFactory(GsonConverterFactory.create()).build()
                    .create(PokemonApi::class.java)
                detail = api.getPokemonDetail(pokemonId)
                val species = api.getPokemonSpecies(pokemonId)
                flavorTextEntries = species.flavorTextEntries ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (detail != null) {
        val p = detail!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "#${
                    p.id.toString().padStart(3, '0')
                } ${p.name.replaceFirstChar { it.uppercase() }}", fontSize = 24.sp
            )

            // sprite for user selected game version
            val currentVersion = viewModel.currentVersion.collectAsState().value
            coil.compose.AsyncImage(
                model = spriteUrl(p.id, currentVersion),
                contentDescription = p.name,
                modifier = Modifier.size(160.dp),
                filterQuality = FilterQuality.None
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Types: ${p.types.joinToString(" / ") { it.type.name.replaceFirstChar { c -> c.uppercase() } }}")// Height: API gives decimeters → feet and inches
            val totalInches = (p.height * 3.93701).toInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            val pounds = kotlin.math.round(p.weight * 0.220462).toInt()

            Text(text = "Height: ${feet}'${inches}\"")
            Text(text = "Weight: $pounds lbs")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Base Stats", fontSize = 18.sp)
            p.stats.forEach { stat ->
                Text(text = "${stat.stat.name.replaceFirstChar { it.uppercase() }}: ${stat.baseStat}")
            }

            val flavorText =
                flavorTextEntries.firstOrNull { it.language?.name == "en" }?.flavorText?.replace(
                        "\n",
                        " "
                    )?.replace("\u000c", " ")

            if (!flavorText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = flavorText, fontSize = 16.sp)
            }

            // match the caught or not caught state of each pokemon
            Spacer(modifier = Modifier.height(24.dp))
            val pokemon = viewModel.pokemonList.collectAsState().value.find { it.id == pokemonId }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable{viewModel.toggleCaught(pokemonId)}
            ) {
                PokeBallButton(
                    isCaught = pokemon?.isCaught == true,
                    onClick = {viewModel.toggleCaught(pokemonId) },
                    size = 30.dp
                )
                Text(if(pokemon?.isCaught == true) "Caught" else "Not caught",
                    fontWeight = if (pokemon?.isCaught == true ) FontWeight.Bold else FontWeight.Normal
                    )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load details")
        }
    }
}