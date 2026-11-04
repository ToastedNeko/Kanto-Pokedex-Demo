/*
* Calls PokeApi to get details for each Pokemon
* when a Pokemon is selected in the app
* */

package com.puggles.pokedexdemo

import android.R.attr.text
import android.system.Os.stat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun PokemonDetailScreen(pokemonId: Int) {
    var detail by remember { mutableStateOf<PokemonDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pokemonId) {
        scope.launch {
            try {
                val api = Retrofit.Builder()
                    .baseUrl("https://pokeapi.co/api/v2/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(PokemonApi::class.java)
                detail = api.getPokemonDetail(pokemonId)
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
                .padding(16.dp)
        ) {
            Text(
                text = "#${
                    p.id.toString().padStart(3, '0')
                } ${p.name.replaceFirstChar { it.uppercase() }}",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Types: ${p.types.joinToString(" / ") { it.type.name.replaceFirstChar { c -> c.uppercase() } }}")// Height: API gives decimeters → feet and inches
            val totalInches = (p.height * 3.93701).toInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            val pounds = kotlin.math.round(p.weight * 0.220462).toInt()

            Text(text = "Height: ${feet}'${inches}\"")
            Text(text = "Weight: ${pounds} lbs")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Base Stats", fontSize = 18.sp)
            p.stats.forEach { stat ->
                Text(text = "${stat.stat.name.replaceFirstChar { it.uppercase() }}: ${stat.base_stat}")
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text("Failed to load details")
        }
    }
}