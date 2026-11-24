/*
* Calls PokeApi to get details for each Pokemon
* when a Pokemon is selected in the app
* */

package com.puggles.pokedexdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(pokemonId: Int, viewModel: PokemonViewModel, onBack: () -> Unit) {
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
        val gridColor =
            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.2f)

        // match the caught or not caught state of each pokemon
        val pokemon = viewModel.pokemonList.collectAsState().value.find { it.id == pokemonId }

        Column(modifier = Modifier.fillMaxSize()) {

            TopAppBar(
                title = {
                    Text(
                        "#${p.id.toString().padStart(3, '0')}", fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                        )
                    }
                },

                actions = {
                    val isCaught = pokemon?.isCaught == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable { viewModel.toggleCaught(pokemonId) }
                            .padding(start = 14.dp, end = 6.dp)) {
                        Text(
                            if (isCaught) "Caught" else "Catch",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        PokeBallButton(
                            isCaught = isCaught,
                            onClick = { viewModel.toggleCaught(pokemonId) },
                            size = 24.dp,
                            tapPadding = 4.dp
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = gameColor(currentVersion),
                    titleContentColor = onGameColor(currentVersion),
                    navigationIconContentColor = onGameColor(currentVersion),
                    actionIconContentColor = onGameColor(currentVersion)
                )
            )
            HorizontalDivider(thickness = 3.dp, color = Color.Black)

            // scrolling content with the grid behind it
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .gridBackground(gridColor)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // game colored block with all the boxes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gameColor(currentVersion), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // name box
                    DexBox {
                        Text(
                            text = p.name.uppercase(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // sprite box
                    coil.compose.AsyncImage(
                        model = spriteUrl(p.id, currentVersion),
                        contentDescription = p.name,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                            .padding(4.dp)
                            .size(160.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .graphicsLayer(scaleX = 1.3f, scaleY = 1.3f),

                        filterQuality = FilterQuality.None
                    )

                    val totalInches = (p.height * 3.93701).toInt()
                    val feet = totalInches / 12
                    val inches = totalInches % 12
                    val pounds = kotlin.math.round(p.weight * 0.220462).toInt()

                    // info box
                    DexBox {
                        Text("Number: #${p.id.toString().padStart(3, '0')}")
                        Text("Type: ${p.types.joinToString(" / ") { it.type.name.replaceFirstChar { c -> c.uppercase() } }}")
                        Text("Height: ${feet}'$inches\"")
                        Text("Weight: $pounds lbs")
                    }

                    // stats box
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
                                Text(statLabel(stat.stat.name))
                                Text("${stat.baseStat}")
                            }
                        }
                    }

                    // description box
                    val flavorText =
                        flavorTextEntries.firstOrNull { it.language?.name == "en" }?.flavorText?.replace(
                            "\n", " "
                        )?.replace("\u000c", " ")

                    if (!flavorText.isNullOrBlank()) {
                        DexBox {
                            Text(text = flavorText, fontSize = 16.sp)
                        }
                    }
                }
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

// add grid background for detail view of each Pokemon
fun Modifier.gridBackground(lineColor: Color, spacing: Dp = 24.dp): Modifier = this.drawBehind {

    val step = spacing.toPx()

    // vertical lines
    var x = 0f
    while (x < size.width) {
        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
        x += step
    }

    // horizontal lines
    var y = 0f
    while (y < size.height) {
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
        y += step
    }
}

// formatting for stats
fun statLabel(apiName: String): String = if (apiName == "hp") {
    "HP"
} else {
    apiName.split("-").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}