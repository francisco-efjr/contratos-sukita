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

@Database(entities = [Apartment::class], version = 1, exportSchema = false)
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
                .addCallback(SeedCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Popula o banco com os apartamentos do pai na primeira execução.
     * Adicione novos apartamentos aqui quando necessário.
     */
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

/**
 * Lista inicial de apartamentos/imóveis.
 * Edite este arquivo para adicionar, remover ou corrigir imóveis.
 * O campo templateHtmlAsset aponta para assets/templates/contract_base.html
 * (todos os imóveis usam o mesmo template base por enquanto).
 */
fun initialApartments(): List<Apartment> = listOf(
    Apartment(
        name = "Apt. 19 — Boas Novas",
        address = "Rua 01, Conjunto Boas Novas, Nº 20, Bairro Cidade Nova, Manaus-AM",
        ucCode = "2377157-7 C-21",
        conjunto = "Boas Novas",
        templateHtmlAsset = "templates/contract_base.html"
    )
    // Adicione outros apartamentos aqui:
    // Apartment(
    //     name = "Apt. XX — Conjunto YYY",
    //     address = "...",
    //     ucCode = "...",
    //     conjunto = "...",
    //     templateHtmlAsset = "templates/contract_base.html"
    // ),
)
