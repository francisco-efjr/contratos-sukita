package com.sukita.contratos

import android.app.Application
import com.sukita.contratos.data.AppDatabase

class ContratosApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
