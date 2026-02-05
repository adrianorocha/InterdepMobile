package com.interdep.interdepmobile.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.components.*
import com.interdep.interdepmobile.ui.theme.*
import com.interdep.interdepmobile.util.toDataPremium
import com.interdep.interdepmobile.util.toPrecoFormatado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.sql.DriverManager

// --- Modelos Específicos para Serviço ---
data class PedidoS(val numero: String, val data: String, val valor: BigDecimal)
data class FornecedorGrupoS(val nomeFantasia: String, val razaoSocial: String, val codigo: String, val pedidos: MutableList<PedidoS>)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosServicoScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var selectedDb by remember { mutableStateOf("Brasfit") }
    var carregando by remember { mutableStateOf(true) }
    var fornecedores by remember { mutableStateOf(listOf<FornecedorGrupoS>()) }
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val selectedPedidos = remember { mutableStateListOf<String>() }

    LaunchedEffect(selectedDb) {
        carregando = true
        fetchHierarquiaServico(selectedDb) { lista ->
            fornecedores = lista
            lista.forEach { expandedState[it.codigo] = false }
            carregando = false
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(Color.White)) {
                PremiumTopBar("Pedidos de Serviço", selectedDb, Icons.Default.Receipt, onBack = onFinish)
                ResponsiveDbSelector(
                    selectedDb = selectedDb,
                    onDbSelected = { newDb -> selectedDb = newDb }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            Surface(shadowElevation = 16.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumButton("Atualizar", Modifier.weight(1f), Navy700, Icons.Default.Refresh) {
                        carregando = true
                        scope.launch(Dispatchers.IO) {
                            fetchHierarquiaServico(selectedDb) { lista -> fornecedores = lista; carregando = false }
                        }
                    }
                    PremiumButton("Liberar (${selectedPedidos.size})", Modifier.weight(1f), Emerald500, Icons.Default.Check) {
                        scope.launch(Dispatchers.IO) {
                            val qtd = liberarSelecionadosServico(selectedDb, selectedPedidos.toList())
                            snackbarHost.showSnackbar(if (qtd > 0) "$qtd Serviços Liberados" else "Nenhum selecionado")
                            fetchHierarquiaServico(selectedDb) { lista -> fornecedores = lista; carregando = false }
                            selectedPedidos.clear()
                        }
                    }
                }
            }
        },
        containerColor = Slate100
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (carregando) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Navy700)
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(fornecedores, key = { it.codigo }) { grupo ->
                        FornecedorCardS(grupo, expandedState[grupo.codigo] ?: false, selectedPedidos) {
                            expandedState[grupo.codigo] = !(expandedState[grupo.codigo] ?: false)
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FornecedorCardS(grupo: FornecedorGrupoS, isExpanded: Boolean, selectedPedidos: SnapshotStateList<String>, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Column {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Build, null, tint = Color(0xFF7C3AED))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(grupo.nomeFantasia, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                    Text(grupo.razaoSocial, fontSize = 12.sp, color = Slate500)
                }
                Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Slate500)
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Divider(color = Slate100)
                    grupo.pedidos.forEach { pedido ->
                        val isChecked = selectedPedidos.contains(pedido.numero)
                        Row(
                            Modifier.fillMaxWidth().clickable { if(isChecked) selectedPedidos.remove(pedido.numero) else selectedPedidos.add(pedido.numero) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = { if(it) selectedPedidos.add(pedido.numero) else selectedPedidos.remove(pedido.numero) }, colors = CheckboxDefaults.colors(checkedColor = Emerald500))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Serviço #${pedido.numero}", fontWeight = FontWeight.SemiBold, color = Navy900)
                                Text(pedido.data.toDataPremium(), fontSize = 12.sp, color = Slate500)
                            }
                            Text(pedido.valor.toPrecoFormatado(), fontWeight = FontWeight.Bold, color = Emerald500)
                        }
                    }
                }
            }
        }
    }
}

// ─── Lógica JDBC Serviço (Copiar lógica original, renomeei para evitar conflito) ───
private fun fetchHierarquiaServico(dbName: String, onResult: (List<FornecedorGrupoS>) -> Unit) = Thread {
    val mapa = linkedMapOf<String, FornecedorGrupoS>()
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                // Sua query original aqui...
                st.executeQuery("""
                Select Distinct f.NOM_FANTASIA, f.COD_FORNECEDOR, f.RAZ_SOCIAL, p.NUM_PEDI_SERVICO,
                   CONVERT(varchar(10), p.DAT_EMISSAO, 103) as DATA_EMISSAO,
                   (p.VAL_TOTAL+p.VAL_INSS+p.VAL_IRRF+p.VAL_PCC+p.VAL_ISS) as VAL_TOTAL
                From PEDIDO_SERVICO p 
                Join ITEM_PEDI_SERVICO i on(p.NUM_PEDI_SERVICO = i.NUM_PEDI_SERVICO)
                Join FORNECEDOR f on (p.COD_FORNECEDOR = f.COD_FORNECEDOR)
                Where (p.COD_SITUACAO = 'N') Order by f.NOM_FANTASIA
                """.trimIndent()).use { rs ->
                    while (rs.next()) {
                        val cod = rs.getString("COD_FORNECEDOR").trim()
                        val grupo = mapa.getOrPut(cod) {
                            FornecedorGrupoS(rs.getString("NOM_FANTASIA").trim(), rs.getString("RAZ_SOCIAL").trim(), cod, mutableListOf())
                        }
                        val num = rs.getString("NUM_PEDI_SERVICO").trim()
                        if (grupo.pedidos.none { it.numero == num }) {
                            grupo.pedidos.add(PedidoS(num, rs.getString("DATA_EMISSAO"), rs.getBigDecimal("VAL_TOTAL")))
                        }
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    onResult(mapa.values.toList())
}.start()

private fun liberarSelecionadosServico(dbName: String, numeros: List<String>): Int {
    if (numeros.isEmpty()) return 0
    var total = 0
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                numeros.forEach { num ->
                    st.addBatch("UPDATE PEDIDO_SERVICO SET COD_SITUACAO = 'A' WHERE NUM_PEDI_SERVICO = '$num' AND COD_SITUACAO = 'N'")
                    total++
                }
                st.executeBatch()
            }
        }
    } catch (e: Exception) { e.printStackTrace(); total = -1 }
    return total
}