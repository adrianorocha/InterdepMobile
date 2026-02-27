package com.interdep.interdepmobile.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interdep.interdepmobile.ui.components.PremiumTopBar
import com.interdep.interdepmobile.ui.theme.Blue600
import com.interdep.interdepmobile.ui.theme.Emerald500
import com.interdep.interdepmobile.ui.theme.Navy900
import com.interdep.interdepmobile.ui.theme.Rose500
import com.interdep.interdepmobile.ui.theme.Slate100
import com.interdep.interdepmobile.ui.theme.Slate500
import kotlin.system.exitProcess

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
    // 1. Obtém o contexto da Activity atual
    val context = LocalContext.current
    val activity = context as? Activity

    // 2. Função para matar o App
    fun killApp() {
        activity?.finishAffinity() // Fecha todas as telas abertas
        android.os.Process.killProcess(android.os.Process.myPid()) // Mata o processo no OS
        exitProcess(0) // Encerra a JVM
    }

    // 3. Intercepta o botão "Voltar" do celular para matar o app
    BackHandler {
        killApp()
    }

    Scaffold(
        topBar = { PremiumTopBar("Interdep Mobile", "Menu Principal") },
        containerColor = Slate100
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Itens do Menu
            MenuCard("Compras", "Liberar Pedidos", Icons.Default.ShoppingCart, Blue600, onLiberarPedidoCompras)
            MenuCard("Serviços", "Liberar Serviços", Icons.Default.Receipt, Color(0xFF8B5CF6), onLiberarPedidoServico)
            MenuCard("Vendas", "Carteira de Clientes", Icons.Default.Business, Color(0xFFEC4899), onCarteiraCliente)
            MenuCard("Financeiro", "Comissão Representantes", Icons.Default.AttachMoney, Emerald500, onComissaoRepresentante)
            MenuCard("Cadastro", "Liberar Fornecedor", Icons.Default.LocationCity, Color(0xFFF59E0B), onLiberaFornecedor)
            MenuCard("RH", "Transferir Funcionário", Icons.Default.CompareArrows, Color(0xFF10B981), onTransferirFuncionario)
            MenuCard("Beneficiamento", "Atualizar Beneficiamento", Icons.Default.AccountBalance, Color(0xFF64748B), onAtualizarBeneficiamento)

            Spacer(modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
            ) // Empurra o botão Sair para baixo

            // 4. Botão Explicito de Sair
            MenuCard("Sistema", "Sair do Aplicativo", Icons.Default.ExitToApp, Rose500) {
                killApp()
            }
        }
    }
}

// Reutilizando seu MenuCard (caso não tenha o código anterior, aqui está a versão atualizada)
@Composable
fun MenuCard(category: String, title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Slate500)
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy900)
            }
        }
    }
}