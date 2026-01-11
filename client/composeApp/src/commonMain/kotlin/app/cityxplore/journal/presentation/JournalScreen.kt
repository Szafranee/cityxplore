package app.cityxplore.journal.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.map.presentation.components.PoiCategoryMarker
import app.cityxplore.map.presentation.components.PoiDetailsContent
import app.cityxplore.platform.BackHandler
import app.cityxplore.theme.AppColors
import coil3.compose.AsyncImage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    state: JournalUiState,
    searchQuery: String,
    currentFilter: JournalFilter,
    currentSort: JournalSort,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (JournalFilter) -> Unit,
    onSortChange: (JournalSort) -> Unit,
    onToggleFavorite: (MapPoi) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedPoi by remember { mutableStateOf<MapPoi?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            JournalTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search and Filters
            JournalFilters(
                searchQuery = searchQuery,
                currentFilter = currentFilter,
                currentSort = currentSort,
                onSearchQueryChange = onSearchQueryChange,
                onFilterChange = onFilterChange,
                onSortChange = onSortChange
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is JournalUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppColors.green
                        )
                    }

                    is JournalUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is JournalUiState.Content -> {
                        if (state.entries.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "No discoveries yet." else "No matches found.",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Go explore the city!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.entries) { entry ->
                                    JournalEntryCard(
                                        entry = entry,
                                        onClick = { selectedPoi = entry },
                                        onToggleFavorite = { onToggleFavorite(entry) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedPoi != null) {
                    ModalBottomSheet(
                        onDismissRequest = { selectedPoi = null },
                        sheetState = sheetState
                    ) {
                        PoiDetailsContent(
                            poi = selectedPoi!!,
                            onToggleFavorite = {
                                onToggleFavorite(selectedPoi!!)
                                // Update local selectedPoi state immediately for better UX (Optimistic update).
                                // The parent list will eventually refresh, but this ensures instant visual feedback in the modal.
                                selectedPoi = selectedPoi!!.copy(isFavorite = !selectedPoi!!.isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Discovery Journal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.green
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun JournalFilters(
    searchQuery: String,
    currentFilter: JournalFilter,
    currentSort: JournalSort,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (JournalFilter) -> Unit,
    onSortChange: (JournalSort) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search discoveries...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = currentFilter == JournalFilter.FAVORITES,
                onClick = {
                    onFilterChange(
                        if (currentFilter == JournalFilter.FAVORITES) JournalFilter.ALL else JournalFilter.FAVORITES
                    )
                },
                label = { Text("Favorites Only") },
                leadingIcon = {
                    Icon(
                        imageVector = if (currentFilter == JournalFilter.FAVORITES) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            Box {
                FilterChip(
                    selected = false,
                    onClick = { showSortMenu = true },
                    label = { Text("Sort") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Date (Newest)") },
                        onClick = { onSortChange(JournalSort.DATE_DESC); showSortMenu = false },
                        trailingIcon = if (currentSort == JournalSort.DATE_DESC) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                    DropdownMenuItem(
                        text = { Text("Date (Oldest)") },
                        onClick = { onSortChange(JournalSort.DATE_ASC); showSortMenu = false },
                        trailingIcon = if (currentSort == JournalSort.DATE_ASC) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)") },
                        onClick = { onSortChange(JournalSort.NAME_ASC); showSortMenu = false },
                        trailingIcon = if (currentSort == JournalSort.NAME_ASC) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                    DropdownMenuItem(
                        text = { Text("Name (Z-A)") },
                        onClick = { onSortChange(JournalSort.NAME_DESC); showSortMenu = false },
                        trailingIcon = if (currentSort == JournalSort.NAME_DESC) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(
    entry: MapPoi,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Darker variant
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // POI Marker
            PoiCategoryMarker(
                poi = entry,
                modifier = Modifier.size(48.dp),
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                entry.discoveryDate?.let { date ->
                    Text(
                        text = "Discovered: ${formatDate(Instant.fromEpochMilliseconds(date))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (entry.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (entry.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (entry.photos.isNotEmpty()) {
                entry.photos.firstOrNull()?.url?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

private fun formatDate(instant: Instant): String {
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.day}.${date.month.number}.${date.year}"
}
