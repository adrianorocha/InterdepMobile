package com.interdep.interdepmobile.ui

import android.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.components.MenuTopBar

/**
 * Tela de menu principal com 3 opções:
 *  - Liberar Pedido de Compras
 *  - Liberar Pedido de Serviço
 *  - Carteira de Cliente (Vendedor)
 *  - Comissão de Representantes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onLiberarPedidoCompras: () -> Unit,
    onLiberarPedidoServico: () -> Unit,
    onCarteiraCliente: () -> Unit,
    onComissaoRepresentante: () -> Unit,
    onLiberaFornecedor: () -> Unit
) {
    Scaffold(

        topBar = {
                MenuTopBar("Interdep Mobile","")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MenuItem(
                icon = Icons.Default.ShoppingCart,
                title = "Liberar Pedido de Compras",
                onClick = onLiberarPedidoCompras
            )
            MenuItem(
                icon = Icons.Default.Receipt,
                title = "Liberar Pedido de Serviço",
                onClick = onLiberarPedidoServico
            )
            MenuItem(
                icon = Icons.Default.Business,
                title = "Carteira de Cliente (Vendedor)",
                onClick = onCarteiraCliente
            )
            MenuItem(
                icon = Icons.Default.AttachMoney,
                title = "Comissão de Representantes",
                onClick = onComissaoRepresentante
            )
            MenuItem(
                icon = Icons.Default.LocationCity,
                title = "Liberação de Fornecedores",
                onClick = onLiberaFornecedor
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(16.dp))
            Text(title, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainMenu() {
    MainMenuScreen(
        onLiberarPedidoCompras = {},
        onLiberarPedidoServico  = {},
        onCarteiraCliente       = {},
        onComissaoRepresentante = {},
        onLiberaFornecedor = {}
    )
}
