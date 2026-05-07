package com.sukita.contratos.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa um apartamento/imóvel gerenciado pelo locador.
 * Os dados fixos do imóvel ficam aqui; o templateHtmlAsset aponta
 * para o arquivo HTML em assets/templates/.
 */
@Entity(tableName = "apartments")
data class Apartment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,              // Ex: "Apt. 19"
    val address: String,           // Ex: "Rua 01, Conj. Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM"
    val ucCode: String,            // Ex: "2377157-7 C-21"
    val conjunto: String,          // Ex: "Boas Novas"
    val templateHtmlAsset: String  // Ex: "templates/contract_base.html"
)
