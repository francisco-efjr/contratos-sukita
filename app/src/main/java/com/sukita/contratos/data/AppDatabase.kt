package com.sukita.contratos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sukita.contratos.model.Apartment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Apartment::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun apartmentDao(): ApartmentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "contratos_sukita.db"
                )
                .fallbackToDestructiveMigration() // simplifica migração durante dev
                .addCallback(SeedCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.apartmentDao()
                    if (dao.count() == 0) {
                        dao.insertAll(initialApartments())
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Catálogo completo de imóveis do locador Francisco Edson Ferreira
// Mapeado a partir dos contratos originais em docs/contratos_originais/
//
// IMPORTANTE: para editar/adicionar imóveis basta alterar esta lista.
// Ao instalar o app pela primeira vez o banco é populado automaticamente.
// Se o app já estiver instalado, desinstale e reinstale para repopular.
// ─────────────────────────────────────────────────────────────────────────────
fun initialApartments(): List<Apartment> = listOf(

    // ── CONJUNTO BOAS NOVAS ────────────────────────────────────────────────
    // Endereço: Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM
    Apartment(name = "Apt. 01 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "471295-1",       conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 02 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778280-2 C-01",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 03 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778292-6 C-02",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 04 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778293-4 C-03",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 05 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778297-7 C-04",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 06 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778300-0 C-05",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 07 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778303-5 C-06",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 08 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778308-6 C-07",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 09 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778329-9 C-08",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 10 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "778335-3 C-09",  conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 11 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2110590-1 C-10", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 12 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2110591-0 C-11", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 13 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2110592-8 C-12", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 14 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2110593-6 C-13", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 15 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2110594-4 C-16", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 16 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2110595-2 C-17", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 17 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2111293-2 C-18", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 18 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2110596-0 C-19", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 19 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2377157-7 C-21", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 20 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2372227-4 C-20", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 21 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2377034-1 C-15", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 22 — Boas Novas",  address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM", ucCode = "2369226-0 C-14", conjunto = "Boas Novas", templateHtmlAsset = "templates/contract_base.html"),

    // ── CONJUNTO BOM PASTOR ────────────────────────────────────────────────
    // Endereço: Rua Bom Pastor, Nº 84, Flores, Manaus-AM
    Apartment(name = "Apt. 01 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "911881-0",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 02 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716492-0",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 03 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716491-2",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 04 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716490-4",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 05 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "911878-0",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 06 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "538653-5",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 07 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "538654-3",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 08 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "538655-1",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 09 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "538656-0",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 10 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716488-2",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 11 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "911873-0",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 12 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "911868-3",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 13 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "911861-6",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 14 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716498-0",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 15 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716497-1",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 16 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716496-3",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 17 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716495-5",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 18 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716494-7",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 19 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "716493-9",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Apt. 20 — Bom Pastor",  address = "Rua Bom Pastor, Nº 84, Flores, Manaus-AM", ucCode = "911875-6",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),
    Apartment(name = "Galpão — Bom Pastor",   address = "Rua Bom Pastor, Nº 84, Flores, CEP 69.028-340, Manaus-AM", ucCode = "911883-7",  conjunto = "Bom Pastor", templateHtmlAsset = "templates/contract_base.html"),

    // ── CONJUNTO BEIJA FLOR ────────────────────────────────────────────────
    Apartment(
        name = "Casa (Casão) — Beija Flor",
        address = "Rua 01, Nº 03, Conjunto Beija Flor 1, Bairro de Flores, CEP 69028-320, Manaus-AM",
        ucCode = "0101422-6",
        conjunto = "Beija Flor",
        templateHtmlAsset = "templates/contract_base.html"
    ),
    Apartment(
        name = "Casa Nº 57 — Beija Flor",
        address = "Rua Paraopeba (antiga Rua 11), Nº 57, Bairro de Flores, Manaus-AM",
        ucCode = "",  // UC não consta no contrato — preencher quando disponível
        conjunto = "Beija Flor",
        templateHtmlAsset = "templates/contract_base.html"
    ),
    Apartment(
        name = "Apto 202 Bloco D — Cond. Beija Flor 2",
        address = "Rua 02, Nº 150, Apto 202, Bloco D, Condomínio Residencial Beija Flor 2, Flores, Manaus-AM",
        ucCode = "01011260",  // Águas de Manaus / Matrícula 3451364-7
        conjunto = "Beija Flor",
        templateHtmlAsset = "templates/contract_base.html"
    ),

    // ── DOM PEDRO ──────────────────────────────────────────────────────────
    // Uso comercial — mesmo template, locador pode variar
    Apartment(
        name = "Imóvel Comercial — Dom Pedro",
        address = "Rua Jerusalém, Nº 47, Dom Pedro 1, CEP 69040-010, Manaus-AM",
        ucCode = "",  // UC/IPTU: Matrícula 2073503 — preencher quando disponível
        conjunto = "Dom Pedro",
        templateHtmlAsset = "templates/contract_base.html"
    ),

    // ── JORGE TEIXEIRA ─────────────────────────────────────────────────────
    Apartment(
        name = "Casa — Jorge Teixeira",
        address = "Rua 06 (antiga Rua Cravinho), Nº 97, Me-503, Bairro Jorge Teixeira, Manaus-AM",
        ucCode = "0369161-6",
        conjunto = "Jorge Teixeira",
        templateHtmlAsset = "templates/contract_base.html"
    ),

    // ── JUAZEIRO ───────────────────────────────────────────────────────────
    Apartment(
        name = "Apto TT05 — Cond. Porto Monte (Juazeiro)",
        address = "Rua Laura Avelar Botelho, Nº 98, Apto TT 05, Bl 01, Bairro Planalto (Lagoa Seca), Juazeiro do Norte-CE",
        ucCode = "",  // UC não consta no contrato
        conjunto = "Juazeiro",
        templateHtmlAsset = "templates/contract_base.html"
    ),

    // ── SÃO JOSÉ ───────────────────────────────────────────────────────────
    Apartment(
        name = "Imóvel Comercial — São José",
        address = "Av. Autaz Mirim, Nº 5472, São José Operário 1, CEP 69088-245, Manaus-AM",
        ucCode = "0364196-1",
        conjunto = "São José",
        templateHtmlAsset = "templates/contract_base.html"
    )
)
