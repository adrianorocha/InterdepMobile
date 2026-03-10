package com.interdep.interdepmobile.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.components.PremiumTopBar
import com.interdep.interdepmobile.ui.theme.*
import kotlin.system.exitProcess

data class MenuItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onLiberarPedidoCompras: () -> Unit,
    onLiberarPedidoServico: () -> Unit,
    onCarteiraCliente: () -> Unit,
    onComissaoRepresentante: () -> Unit,
    onLiberaFornecedor: () -> Unit,
    onTransferirFuncionario: () -> Unit,
    onAtualizarBeneficiamento: () -> Unit
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    fun killApp() {
        (context as? Activity)?.finishAffinity()
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }

    BackHandler { showExitDialog = true }

    val menuItems = listOf(
        MenuItemData("Compras", "Liberar", Icons.Default.ShoppingCart, Color(0xFF2563EB), onLiberarPedidoCompras),
        MenuItemData("Serviços", "Liberar", Icons.Default.Receipt, Color(0xFF8B5CF6), onLiberarPedidoServico),
        MenuItemData("Vendas", "Clientes", Icons.Default.Business, Color(0xFFEC4899), onCarteiraCliente),
        MenuItemData("Financeiro", "Comissão", Icons.Default.AttachMoney, Emerald500, onComissaoRepresentante),
        MenuItemData("Cadastro", "Fornecedor", Icons.Default.LocationCity, Color(0xFFF59E0B), onLiberaFornecedor),
        MenuItemData("RH", "Transferir", Icons.Default.CompareArrows, Color(0xFF10B981), onTransferirFuncionario),
        MenuItemData("Produção", "Benefic.", Icons.Default.Build, Color(0xFFF43F5E), onAtualizarBeneficiamento),
        MenuItemData("Sistema", "Sair", Icons.Default.ExitToApp, Color(0xFFEF4444)) { showExitDialog = true }
    )

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFEF4444)) },
            title = { Text("Sair do Aplicativo", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("Deseja realmente encerrar a sessão?", color = Slate700) },
            confirmButton = {
                Button(onClick = { killApp() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancelar", color = Slate500) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Interdep Mobile",
                subtitle = "Painel Administrativo",
                icon = Icons.Default.Dashboard,
                onBack = { showExitDialog = true }
            )
        },
        containerColor = Slate100
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Módulos",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Navy900,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            LazyVerticalGrid(
                // --- MUDANÇA PARA 3 COLUNAS ---
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    MenuCardGrid(item)
                }
            }
        }
    }
}

@Composable
fun MenuCardGrid(item: MenuItemData) {
    Card(
        shape = RoundedCornerShape(12.dp), // Cantos um pouco menores para combinar com o tamanho
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Mantém quadrado
            .clickable { item.onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone um pouco menor (48dp em vez de 56dp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(item.iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Título e Subtítulo com fontes reduzidas para caber nas 3 colunas
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Navy900,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = item.subtitle,
                fontSize = 10.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 1
            )
        }
    }
}