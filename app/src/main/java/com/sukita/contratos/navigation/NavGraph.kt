package com.sukita.contratos.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukita.contratos.ui.screens.ApartmentListScreen
import com.sukita.contratos.ui.screens.ContractFormScreen
import com.sukita.contratos.ui.screens.DocumentScanScreen
import com.sukita.contratos.ui.screens.FinalPageScreen
import com.sukita.contratos.ui.screens.PdfPreviewScreen
import com.sukita.contratos.ui.screens.SettingsScreen
import com.sukita.contratos.viewmodel.ContractViewModel

sealed class Screen(val route: String) {
    object ApartmentList : Screen("apartment_list")
    object DocumentScan  : Screen("document_scan")
    object ContractForm  : Screen("contract_form")
    object FinalPage     : Screen("final_page")
    object Settings      : Screen("settings")
    object PdfPreview    : Screen("pdf_preview/{pdfPath}") {
        fun withPath(path: String) = "pdf_preview/$path"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    vm: ContractViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ApartmentList.route
    ) {
        composable(Screen.ApartmentList.route) {
            ApartmentListScreen(
                vm = vm,
                onApartmentSelected = { navController.navigate(Screen.ContractForm.route) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.DocumentScan.route) {
            DocumentScanScreen(
                vm = vm,
                onDone = { navController.popBackStack() }
            )
        }

        composable(Screen.ContractForm.route) {
            ContractFormScreen(
                vm = vm,
                onScanDocument = { navController.navigate(Screen.DocumentScan.route) },
                onNext = { navController.navigate(Screen.FinalPage.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FinalPage.route) {
            FinalPageScreen(
                vm = vm,
                onGenerate = { pdfPath ->
                    val encoded = android.net.Uri.encode(pdfPath)
                    navController.navigate(Screen.PdfPreview.withPath(encoded))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PdfPreview.route) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("pdfPath") ?: ""
            val pdfPath = android.net.Uri.decode(encoded)
            PdfPreviewScreen(
                pdfPath = pdfPath,
                onNewContract = {
                    vm.resetForm()
                    navController.popBackStack(Screen.ApartmentList.route, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
