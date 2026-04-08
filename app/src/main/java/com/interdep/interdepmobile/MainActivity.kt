package com.interdep.interdepmobile

import android.os.Build
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.rememberNavController
import com.interdep.interdepmobile.ui.FornecedorPedidosScreen
import com.interdep.interdepmobile.navigation.Routes
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.interdep.interdepmobile.ui.AtualizarBeneficiamentoScreen
import com.interdep.interdepmobile.ui.ClonarMaterialScreen
import com.interdep.interdepmobile.ui.ComissaoRepresentanteScreen
import com.interdep.interdepmobile.ui.MainMenuScreen
import com.interdep.interdepmobile.ui.PedidosServicoScreen
import com.interdep.interdepmobile.ui.VendedorClienteScreen
import com.interdep.interdepmobile.ui.LiberacaoFornecedorScreen
import com.interdep.interdepmobile.ui.SplashScreen
import com.interdep.interdepmobile.ui.TransferenciaFuncionarioScreen


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InterdepApp()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterdepApp() {
    val navController = rememberNavController()

    NavHost(
        navController   = navController,
        startDestination = Routes.SPLASH
    ) {
        // Splash
        composable(Routes.SPLASH) {
            SplashScreen()                    // seu composable de splash

            // logo após montar a tela, aguarda e navega:
            LaunchedEffect(Unit) {
                delay(2000L)                 // tempo que quiser exibir
                navController.navigate(Routes.MAIN_MENU) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onLiberarPedidoCompras = {
                    navController.navigate(Routes.PEDIDOS_COMPRAS)
                },
                onLiberarPedidoServico = {
                    navController.navigate(Routes.PEDIDOS_SERVICO)
                },
                onCarteiraCliente      = {
                    navController.navigate(Routes.VENDEDOR_CLIENTE)
                },
                onComissaoRepresentante      = {
                    navController.navigate(Routes.COMISSAO_REPRESENTANTE)
                },
               onLiberaFornecedor = {
                   navController.navigate(Routes.LIBERA_FORNECEDOR)
               },
                onTransferirFuncionario = {
                    navController.navigate(Routes.TRANSFERIR_FUNCIONARIO)
                },
                onAtualizarBeneficiamento = {
                    navController.navigate(Routes.ATUALIZAR_BENEFICIAMENTO)
                },
                onClonarMaterial = {
                    navController.navigate(Routes.CLONAR_MATERIAL)
                }
            )
        }
        composable(Routes.PEDIDOS_COMPRAS) {
            FornecedorPedidosScreen(
                onFinish = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.PEDIDOS_SERVICO) {
            PedidosServicoScreen(
                onFinish = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.VENDEDOR_CLIENTE) {
            VendedorClienteScreen(
                onDone = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.COMISSAO_REPRESENTANTE) {
            ComissaoRepresentanteScreen(
                onDone = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.LIBERA_FORNECEDOR) {
            LiberacaoFornecedorScreen(
                onFinish = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.TRANSFERIR_FUNCIONARIO) {
            TransferenciaFuncionarioScreen(
                onFinish = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.ATUALIZAR_BENEFICIAMENTO) {
            AtualizarBeneficiamentoScreen(
                onFinish = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.CLONAR_MATERIAL) {
            ClonarMaterialScreen(
                onFinish = {
                    // Ao voltar, volta para o menu
                    navController.popBackStack()
                }
            )
        }

    }
}