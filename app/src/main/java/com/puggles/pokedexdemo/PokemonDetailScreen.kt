/*
* Calls PokeApi to get details for each Pokemon
* when a Pokemon is selected in the app
* */

package com.puggles.pokedexdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

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
        val currentVersion = viewModel.currentVersion.collectAsState().value
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gameColor(currentVersion), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                DexBox {
                    Text(
                        text = p.name.uppercase(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // sprite for user selected game version
                coil.compose.AsyncImage(
                    model = spriteUrl(p.id, currentVersion),
                    contentDescription = p.name,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                        .size(160.dp),

                    filterQuality = FilterQuality.None
                )

                val totalInches = (p.height * 3.93701).toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                val pounds = kotlin.math.round(p.weight * 0.220462).toInt()

                DexBox {
                    Column(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Number: #${p.id.toString().padStart(3, '0')}")
                        Text("Type: ${p.types.joinToString(" / ") { it.type.name.replaceFirstChar { c -> c.uppercase() } }}")
                        Text("Height: ${feet}'$inches\"")
                        Text("Weight: $pounds lbs")
                    }
                }
                DexBox {
                    Text(
                        text = "Base Stats",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    p.stats.forEach { stat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stat.stat.name.replaceFirstChar { it.uppercase() })
                            Text("${stat.baseStat}")
                        }
                    }
                }
            }

            val flavorText =
                flavorTextEntries.firstOrNull { it.language?.name == "en" }?.flavorText?.replace(
                    "\n", " "
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
                modifier = Modifier.clickable { viewModel.toggleCaught(pokemonId) }) {
                PokeBallButton(
                    isCaught = pokemon?.isCaught == true,
                    onClick = { viewModel.toggleCaught(pokemonId) },
                    size = 30.dp
                )
                Text(
                    if (pokemon?.isCaught == true) "Caught" else "Not caught",
                    fontWeight = if (pokemon?.isCaught == true) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load details")
        }
    }
}

@Composable
fun DexBox(content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(6.dp))
                .border(4.dp, Color.Black, RoundedCornerShape(6.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}