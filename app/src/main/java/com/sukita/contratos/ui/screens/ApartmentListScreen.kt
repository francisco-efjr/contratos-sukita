package com.sukita.contratos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukita.contratos.model.Apartment
import com.sukita.contratos.viewmodel.ContractViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentListScreen(
    vm: ContractViewModel,
    onApartmentSelected: () -> Unit
) {
    val apartments by vm.apartments.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Agrupa por conjunto e filtra pela busca
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Selecionar Imóvel") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Campo de busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar imóvel ou conjunto…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (apartments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (grouped.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Nenhum imóvel encontrado.") }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (conjunto, apts) ->
                        // Cabeçalho do conjunto
                        item {
                            Text(
                                text = conjunto,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(apts) { apt ->
                            ApartmentCard(
                                apartment = apt,
                                onClick = {
                                    vm.selectApartment(apt)
                                    onApartmentSelected()
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ApartmentCard(apartment: Apartment, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = apartment.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = apartment.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (apartment.ucCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "UC: ${apartment.ucCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}
