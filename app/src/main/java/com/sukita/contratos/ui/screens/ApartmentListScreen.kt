package com.sukita.contratos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukita.contratos.model.Apartment
import com.sukita.contratos.viewmodel.ContractViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentListScreen(
    vm: ContractViewModel,
    onApartmentSelected: () -> Unit,
    onSettings: () -> Unit
) {
    val apartments by vm.apartments.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val grouped = remember(apartments, searchQuery) {
        apartments
            .filter {
                searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.conjunto.contains(searchQuery, ignoreCase = true)
            }
            .groupBy { it.conjunto }
            .toSortedMap()
    }

    // Estado de expansão persistido em rotação de tela
    val expandedState = rememberSaveable(saver = androidx.compose.runtime.saveable.Saver(
        save    = { map: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean> ->
            ArrayList(map.entries.map { "${it.key}=${it.value}" })
        },
        restore = { list: ArrayList<String> ->
            val map = androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>()
            list.forEach { entry ->
                val idx = entry.lastIndexOf('=')
                if (idx > 0) map[entry.substring(0, idx)] = entry.substring(idx + 1) == "true"
            }
            map
        }
    )) { androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>() }

    // Todas as categorias iniciam FECHADAS
    LaunchedEffect(grouped.keys) {
        grouped.keys.forEach { key ->
            if (!expandedState.containsKey(key)) expandedState[key] = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecionar Imóvel") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar imóvel ou conjunto…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            when {
                apartments.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                grouped.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Nenhum imóvel encontrado.") }

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (conjunto, apts) ->
                        val isExpanded = expandedState[conjunto] == true

                        // ── Cabeçalho do grupo ── (tamanho aumentado)
                        item(key = "header_$conjunto") {
                            ConjuntoHeader(
                                title      = conjunto,
                                count      = apts.size,
                                isExpanded = isExpanded,
                                onClick    = { expandedState[conjunto] = !isExpanded }
                            )
                        }

                        // ── Itens do grupo ──
                        items(items = apts, key = { it.id }) { apt ->
                            AnimatedVisibility(visible = isExpanded) {
                                ApartmentCard(
                                    apartment = apt,
                                    onClick   = {
                                        vm.selectApartment(apt)
                                        onApartmentSelected()
                                    },
                                    modifier  = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Cabeçalho de conjunto (maior e mais destacado) ────────────────────────────

@Composable
private fun ConjuntoHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text  = if (isExpanded) "${count} imóvel(is)" else "Toque para ver ${count} imóvel(is)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        Icon(
            imageVector        = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (isExpanded) "Fechar" else "Abrir",
            tint               = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ── Card de apartamento ───────────────────────────────────────────────────────

@Composable
private fun ApartmentCard(
    apartment: Apartment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text       = apartment.name,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = apartment.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (apartment.ucCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = "UC: ${apartment.ucCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}
