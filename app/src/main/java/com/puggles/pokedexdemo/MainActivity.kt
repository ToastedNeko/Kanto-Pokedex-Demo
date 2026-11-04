package com.puggles.pokedexdemo

import android.R.attr.onClick
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puggles.pokedexdemo.ui.theme.PokedexDemoTheme
import androidx.compose.foundation.clickable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val viewModel: PokemonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokedexDemoTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "list",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("list") {
                            PokemonListScreen(
                                viewModel = viewModel,
                                onPokemonClick = { id ->
                                    navController.navigate("detail/$id")
                                }
                            )
                        }
                        composable("detail/{pokemonId}") { backStackEntry ->
                            val pokemonId = backStackEntry.arguments?.getString("pokemonId")?.toIntOrNull() ?: 1
                            PokemonDetailScreen(pokemonId = pokemonId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PokemonListScreen(
    viewModel: PokemonViewModel,
    onPokemonClick: (Int) -> Unit
) {
    val pokemonList = viewModel.pokemonList.collectAsState().value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pokemonList) { pokemon ->
            PokemonItem(
                pokemon = pokemon,
                onToggleCaught = { viewModel.toggleCaught(pokemon.id) },
                onClick = { onPokemonClick(pokemon.id) }   // ← NEW
            )
        }
    }
}

@Composable
fun PokemonItem(
    pokemon: Pokemon, onToggleCaught: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }   // ← NEW
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${pokemon.id.toString().padStart(3, '0')}  ${pokemon.name}",
                fontSize = 18.sp
            )
            Text(
                text = pokemon.types.joinToString(" / "),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(onClick = onToggleCaught) {
            Text(if (pokemon.isCaught) "Caught" else "Not Caught")
        }
    }
}