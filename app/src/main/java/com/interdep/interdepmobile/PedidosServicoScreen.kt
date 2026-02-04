package com.interdep.interdepmobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.sql.DriverManager
import com.interdep.interdepmobile.ui.components.getUrl
import com.interdep.interdepmobile.ui.components.ActionButton
import com.interdep.interdepmobile.ui.components.MenuTopBar

/* ────────────────────  modelos ──────────────────── */
data class PedidoS(
    val numero: String,
    val data: String,
    val valor: BigDecimal,
    var checked: Boolean = false
)

data class FornecedorGrupoS(
    val nomeFantasia: String,
    val razaoSocial: String,
    val codigo: String,
    val pedidos: MutableList<Pedido>
)

/* ─────────────────── tela principal ─────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosServicoScreen(onFinish: () -> Unit) {
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var selectedDb by remember { mutableStateOf("Brasfit") }
    val dbList = listOf("Brasfit","Interdep","Diametal","BrakeTube")
    var dbMenuExpanded by remember { mutableStateOf(false) }

    var carregando   by remember { mutableStateOf(true) }
    var fornecedores by remember { mutableStateOf(listOf<FornecedorGrupoS>()) }

    // Mapa para rastrear estado de expansão de cada fornecedor
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }

    val selectedPedidos = remember { mutableStateListOf<String>() }
    /* carrega apenas uma vez */
    LaunchedEffect(selectedDb) {
        carregando = true
        fetchHierarquia(selectedDb) { lista ->
            fornecedores = lista
            lista.forEach { expandedState[it.codigo] = false }
            carregando = false
        }
    }
    Scaffold(
        topBar  = {
            Column {
                MenuTopBar("Pedidos de","Serviço",Icons.Default.Receipt)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Banco:", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Text(
                            selectedDb,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE0E0E0))
                                .padding(8.dp)
                                .clickable { dbMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = dbMenuExpanded,
                            onDismissRequest = { dbMenuExpanded = false }
                        ) {
                            dbList.forEach { db ->
                                DropdownMenuItem(text = { Text(db) }, onClick = {
                                    selectedDb = db
                                    dbMenuExpanded = false
                                })
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar    = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                Arrangement.SpaceBetween
            )  {
                ActionButton("Atualizar",0xFFF44336, Modifier.weight(1f)) {
                    carregando = true
                    scope.launch(Dispatchers.IO) {
                        fetchHierarquia(selectedDb) { lista ->
                            fornecedores = lista
                            carregando = false
                        }
                    }
                }

                ActionButton("Liberar",0xFFF44336, Modifier.weight(1f)) {
                    scope.launch(Dispatchers.IO) {
                        val qtd = liberarSelecionados(selectedDb, selectedPedidos.toList())
                        snackbarHost.showSnackbar(
                            if (qtd > 0) "$qtd pedido(s) liberado(s)"
                            else "Nenhum pedido marcado"
                        )
                        fetchHierarquia(selectedDb) { lista ->
                            fornecedores = lista
                            carregando = false
                        }
                    }
                }
                ActionButton("Fechar",0xFFF44336, Modifier.weight(1f), onFinish)
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF3EEFF))
        ) {
            if (carregando) {
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center), color = Color(0xFF7E57C2)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(fornecedores, key = { it.codigo }) { grupo ->
                        FornecedorCardS (
                            grupo = grupo,
                            isExpanded = expandedState[grupo.codigo] ?: false,
                            selectedPedidos = selectedPedidos,
                            onToggleExpand = { expandedState[grupo.codigo] = !(expandedState[grupo.codigo] ?: false) }
                        )
                    }
                }
            }
        }
    }
}

/* ────────────────────  UI: fornecedor (mestre) ──────────────────── */
@Composable
private fun FornecedorCardS(
    grupo: FornecedorGrupoS,
    isExpanded: Boolean,
    selectedPedidos: SnapshotStateList<String>,     //  ⬅  NOVO
    onToggleExpand: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
    ) {
        // Cabeçalho do fornecedor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(12.dp)
        ) {
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null, tint = Color(0xFF7E57C2)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(grupo.nomeFantasia, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(grupo.razaoSocial, fontSize = 12.sp, color = Color(0xFFFF3311))
            }
            Text(grupo.codigo, fontSize = 12.sp, color = Color(0xFF7E57C2))
        }

        // Lista de pedidos
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                    .background(Color(0xFFF8F4FF), RoundedCornerShape(12.dp))
            ) {
                grupo.pedidos.forEach { pedido ->
                    PedidoRow(pedido, selectedPedidos)
                    Divider(color = Color(0xFFEDE7FF))
                }
            }
        }
    }
}

/* ────────────────────  UI: linha pedido (detalhe) ──────────────────── */
@Composable
private fun PedidoRow(
    pedido: Pedido,
    selectedPedidos: SnapshotStateList<String>
) {
    val isChecked = selectedPedidos.contains(pedido.numero)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFD7C8FF))
        ) { Icon(Icons.Default.DynamicFeed, null, tint = Color(0xFFFF3333)) }

        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Text("Nº Pedido: " + pedido.numero, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(pedido.data, fontSize = 12.sp, color = Color.DarkGray)
        }
        Text("R\$ ${pedido.valor.setScale(2)}", fontSize = 12.sp,
            color = Color(0xFF388E3C), modifier = Modifier.padding(end = 8.dp))

        Checkbox(
            checked = isChecked,
            onCheckedChange = { checked ->
                if (checked) selectedPedidos.add(pedido.numero)
                else selectedPedidos.remove(pedido.numero)
            },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF7E57C2),
                uncheckedColor = Color(0xFFBDBDBD)
            )
        )
    }
}


// ─────── Fetch hierarquia parametrizada ────────────────────
private fun fetchHierarquia(
    dbName: String,
    onResult: (List<FornecedorGrupoS>) -> Unit
) = Thread {
    val mapa = linkedMapOf<String, FornecedorGrupoS>()
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("""
                Select Distinct 
                   f.NOM_FANTASIA,
                   f.COD_FORNECEDOR,
                   f.RAZ_SOCIAL,
                   p.NUM_PEDI_SERVICO,
                   CONVERT(varchar(10), p.DAT_EMISSAO, 103) as DATA_EMISSAO,
                   (p.VAL_TOTAL+p.VAL_INSS+p.VAL_IRRF+p.VAL_PCC+p.VAL_ISS) as VAL_TOTAL
                From PEDIDO_SERVICO p 
                Join ITEM_PEDI_SERVICO i on(p.NUM_PEDI_SERVICO   =   i.NUM_PEDI_SERVICO)
                Join FORNECEDOR f on (p.COD_FORNECEDOR    =	  f.COD_FORNECEDOR)
                   Where (p.COD_SITUACAO		  = 	'N')
                Order by f.NOM_FANTASIA, f.COD_FORNECEDOR

        """.trimIndent()).use { rs ->
                    while (rs.next()) {
                        val cod = rs.getString("COD_FORNECEDOR").trim()
                        val grupo = mapa.getOrPut(cod) {
                            FornecedorGrupoS(
                                nomeFantasia = rs.getString("NOM_FANTASIA").trim(),
                                razaoSocial  = rs.getString("RAZ_SOCIAL").trim(),
                                codigo       = cod,
                                pedidos      = mutableListOf()
                            )
                        }
                        // Obtenha o pedido, mas evite duplicação
                        val numPedido = rs.getString("NUM_PEDI_SERVICO").trim()
                        if (grupo.pedidos.none { it.numero == numPedido }) {
                            (grupo.pedidos as MutableList).add(
                                Pedido(
                                    numero = rs.getString("NUM_PEDI_SERVICO").trim(),
                                    data = rs.getString("DATA_EMISSAO"),
                                    valor = rs.getBigDecimal("VAL_TOTAL")
                                )
                            )
                        }
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    onResult(mapa.values.toList())
}.start()

// ─────── libera selecionados parametrizado ────────────────
private fun liberarSelecionados(
    dbName: String,
    numeros: List<String>
): Int {
    if (numeros.isEmpty()) return 0

    var total = 0
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                numeros.forEach { num ->
                    st.addBatch("""
                        UPDATE PEDIDO_SERVICO
                           SET COD_SITUACAO = 'A'
                         WHERE NUM_PEDI_SERVICO = '$num'
                           AND COD_SITUACAO    = 'N'
                    """.trimIndent())
                    total++
                }
                st.executeBatch()
            }
            conn.commit()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        total = -1
    }
    return total
}