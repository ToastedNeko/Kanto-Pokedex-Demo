package com.puggles.pokedexdemo

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Matrix
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
                                viewModel = viewModel, onPokemonClick = { id ->
                                    navController.navigate("detail/$id")
                                })
                        }
                        composable("detail/{pokemonId}") { backStackEntry ->
                            val pokemonId =
                                backStackEntry.arguments?.getString("pokemonId")?.toIntOrNull() ?: 1
                            PokemonDetailScreen(pokemonId = pokemonId,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonListScreen(
    viewModel: PokemonViewModel, onPokemonClick: (Int) -> Unit
) {
    val pokemonList = viewModel.pokemonList.collectAsState().value
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentVersion = viewModel.currentVersion.collectAsState().value
    var showMenu by remember { mutableStateOf(false) }

    // launcher that opens the file picker when the user taps Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val ids = mutableSetOf<Int>()
                // open the file the user selected and read its text
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        val json = reader.readText()
                        val array = JSONArray(json)
                        // convert the JSON array (e.g. [1,4,7,25]) into a Set of Ints
                        for (i in 0 until array.length()) {
                            ids.add(array.getInt(i))
                        }
                    }
                }
                // tell the ViewModel to update caught status to match the imported IDs
                viewModel.importCaught(ids)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // add settings bar that allows for importing and exporting caught Pokémon
        TopAppBar(
            title = { Text("Pokédex")},
            actions = {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export") },
                        onClick = {
                            showMenu = false
                            val caughtIds = pokemonList.filter { it.isCaught }.map { it.id }
                            val json = JSONArray(caughtIds).toString()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, json)
                                type = "text/plain"
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    sendIntent,
                                    "Export Caught Pokémon"
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import") },
                        onClick = {
                            showMenu = false
                            importLauncher.launch("*/*")
                        }
                    )
                }
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // select version: YELLOW, RED, or BLUE
            GameVersion.entries.forEach { version ->
                Button(
                    onClick = { viewModel.setVersion(version)},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (version == currentVersion)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(version.name)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pokemonList) { pokemon ->
                PokemonItem(
                    pokemon = pokemon,
                    onToggleCaught = { viewModel.toggleCaught(pokemon.id) },
                    onClick = { onPokemonClick(pokemon.id) })
            }
        }
    }
}

@Composable
fun PokemonItem(
    pokemon: Pokemon, onToggleCaught: () -> Unit, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }   // ← NEW
        .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        coil.compose.AsyncImage(
            model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.id}.png",
            contentDescription = pokemon.name,
            modifier = Modifier.size(64.dp)
        )
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