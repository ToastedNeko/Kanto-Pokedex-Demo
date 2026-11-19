package com.puggles.pokedexdemo

import android.R.attr.contentDescription
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp

class MainActivity : ComponentActivity() {

    private val viewModel: PokemonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokedexDemoTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0,0,0,0)
                ) { innerPadding ->
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
                            PokemonDetailScreen(
                                pokemonId = pokemonId, viewModel = viewModel
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
    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            viewModel.exportAllCaught { data ->
                scope.launch {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(data.toByteArray())
                    }
                    android.widget.Toast.makeText(
                        context, "Exported all versions", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // launcher that opens the file picker when the user taps Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val text = BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                    viewModel.importAllCaught(text)
                    android.widget.Toast.makeText(
                        context, "Imported all versions", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Kanto Dex", fontWeight = FontWeight.Bold) },

            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = gameColor(currentVersion),
                titleContentColor = onGameColor(currentVersion),
                actionIconContentColor = onGameColor(currentVersion)
            ),

            actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Export Caught") }, onClick = {
                    showMenu = false
                    exportLauncher.launch("pokedex_caught.txt")
                })
                DropdownMenuItem(text = { Text("Restore Caught") }, onClick = {
                    showMenu = false
                    importLauncher.launch("*/*")
                })
                DropdownMenuItem(text = { Text("Reset All") }, onClick = {
                    showMenu = false
                    showResetDialog = true
                })
            }
        })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameVersion.entries.forEach { version ->
                Button(
                    onClick = { viewModel.setVersion(version) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (version == currentVersion) gameColor(version)
                        else MaterialTheme.colorScheme.surfaceVariant,

                        contentColor = if (version == currentVersion) onGameColor(version)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(version.name)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(pokemonList) { pokemon ->
                PokemonItem(
                    pokemon = pokemon,
                    version = currentVersion,
                    onToggleCaught = { viewModel.toggleCaught(pokemon.id) },
                    onClick = { onPokemonClick(pokemon.id) })
            }
        }
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset all?") },
                text = { Text("This will clear every Pokémon caught for Red, Blue, and Yellow.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetDialog = false
                            viewModel.resetAll()
                            android.widget.Toast.makeText(
                                context, "All versions reset.", android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                })
        }
    }
}

@Composable
fun PokemonItem(
    pokemon: Pokemon, version: GameVersion, onToggleCaught: () -> Unit, onClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        val placeholder = BitmapPainter(
            ImageBitmap.imageResource(R.drawable.placeholder_pokeball),
            filterQuality = FilterQuality.None
        )

        coil.compose.AsyncImage(
            model = spriteUrl(pokemon.id, version),
            contentDescription = pokemon.name,
            modifier = Modifier.size(64.dp),
            filterQuality = FilterQuality.None,
            placeholder = placeholder, // show placeholder while loading sprite
            error = placeholder // show placeholder if not sprite loaded
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${pokemon.id.toString().padStart(3, '0')}  ${pokemon.name}",
                fontSize = 18.sp
            )
            if(pokemon.types.isNotEmpty()){
                Text(
                    text = pokemon.types.joinToString  (" / "),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PokeBallButton(
            isCaught = pokemon.isCaught,
            onClick = onToggleCaught,
            size = 32.dp,
            tapPadding = 20.dp
        )
    }
}

@Composable
fun PokeBallButton(
    isCaught: Boolean,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    tapPadding: Dp = 8.dp
){
    Image(
        bitmap = ImageBitmap.imageResource(
            if(isCaught) R.drawable.pokeball else R.drawable.pokeball_gray
        ),
        contentDescription = if (isCaught) "Caught" else "Not caught",
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(tapPadding)
            .size(size),

        filterQuality = FilterQuality.None
    )
}

//@Preview(showBackground = true)
//@Composable
//fun PokemonItemPreview() {
//    PokedexDemoTheme {
//        PokemonItem(
//            pokemon = Pokemon(id = 25, name = "Pikachu", isCaught = true),
//            version = GameVersion.YELLOW,
//            onToggleCaught = { },
//            onClick = { }
//        )
//    }
//}