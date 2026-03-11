package com.interdep.interdepmobile.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.theme.*
import com.interdep.interdepmobile.ui.theme.ui.PremiumButton
import com.interdep.interdepmobile.ui.theme.ui.PremiumSnackbar
import com.interdep.interdepmobile.ui.theme.ui.PremiumTopBar
import com.interdep.interdepmobile.ui.theme.ui.getUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.sql.DriverManager
import java.time.YearMonth

data class RepresentanteComissao(
    val codigo: String, val nomeFantasia: String, val razaoSocial: String,
    val indFuncionario: Boolean, val valComissao: BigDecimal
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComissaoRepresentanteScreen(dbName: String = "Brasfit", onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // Filtros e Estado
    val meses = listOf("01 — Janeiro","02 — Fevereiro","03 — Março","04 — Abril","05 — Maio","06 — Junho","07 — Julho","08 — Agosto","09 — Setembro","10 — Outubro","11 — Novembro","12 — Dezembro")
    val anos = (2021..2029).map { it.toString() }
    var selectedMes by remember { mutableStateOf(meses.first()) }
    var selectedAno by remember { mutableStateOf(anos.last()) }
    var expandedMes by remember { mutableStateOf(false) }
    var expandedAno by remember { mutableStateOf(false) }

    var carregando by remember { mutableStateOf(false) }
    var lista by remember { mutableStateOf(listOf<RepresentanteComissao>()) }

    Scaffold(
        topBar = {
            PremiumTopBar(
                "Comissões",
                "Aprovação de Pagamentos",
                Icons.Default.AttachMoney,
                onBack = onDone
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost) { data ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    PremiumSnackbar(Icons.Default.AttachMoney, data)
                }
            }
        },
        containerColor = Slate100
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Card de Filtros
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Dropdown Mês (Simplificado)
                        ExposedDropdownMenuBox(expanded = expandedMes, onExpandedChange = { expandedMes = !expandedMes }, modifier = Modifier.weight(1.5f)) {
                            OutlinedTextField(value = selectedMes, onValueChange = {}, readOnly = true, label = { Text("Mês") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMes) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(8.dp))
                            ExposedDropdownMenu(expanded = expandedMes, onDismissRequest = { expandedMes = false }) { meses.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { selectedMes = m; expandedMes = false }) } }
                        }
                        // Dropdown Ano
                        ExposedDropdownMenuBox(expanded = expandedAno, onExpandedChange = { expandedAno = !expandedAno }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = selectedAno, onValueChange = {}, readOnly = true, label = { Text("Ano") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAno) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(8.dp))
                            ExposedDropdownMenu(expanded = expandedAno, onDismissRequest = { expandedAno = false }) { anos.forEach { y -> DropdownMenuItem(text = { Text(y) }, onClick = { selectedAno = y; expandedAno = false }) } }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PremiumButton(
                        "Buscar Comissões",
                        Modifier.fillMaxWidth(),
                        Navy700,
                        Icons.Default.FilterList
                    ) {
                        val mm = selectedMes.substring(0, 2).toInt();
                        val aa = selectedAno.toInt()
                        val ym = YearMonth.of(aa, mm)
                        val inicial = "01/${"%02d".format(mm)}/${aa}";
                        val final = "${ym.lengthOfMonth()}/${"%02d".format(mm)}/${aa}"
                        carregando = true
                        scope.launch(Dispatchers.IO) {
                            fetchComissaoRep(dbName, inicial, final) { res ->
                                lista = res; carregando = false
                            }
                        }
                    }
                }
            }

            // Lista
            if (carregando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Navy700) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(lista, key = { it.codigo }) { item ->
                        ComissaoCard(item, selectedMes, selectedAno, scope, snackbarHost)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ComissaoCard(item: RepresentanteComissao, mes: String, ano: String, scope: kotlinx.coroutines.CoroutineScope, snack: SnackbarHostState) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(item.nomeFantasia, fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                    Text(item.razaoSocial, color = Slate500, fontSize = 12.sp)
                }
                Text("R\$ ${item.valComissao.setScale(2)}", fontWeight = FontWeight.Bold, color = Emerald500, fontSize = 18.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumButton("Aprovar", Modifier.weight(1f), Emerald500, Icons.Default.Check) {
                    val mm = mes.substring(0, 2).toInt();
                    val aa = ano.toInt();
                    val ym = YearMonth.of(aa, mm)
                    // Nota: Sua lógica de data original foi mantida (formato YYYY/MM/DD para o banco?)
                    val inicial = "${aa}/01/${"%02d".format(mm)}";
                    val final = "${aa}/${ym.lengthOfMonth()}/${"%02d".format(mm)}"

                    scope.launch(Dispatchers.IO) {
                        if (Comissao(inicial, final, item.codigo, "A")) {
                            // Tenta enviar email
                            emailRepresentante(inicial, final, final, item.codigo)
                            snack.showSnackbar("Aprovado com sucesso!")
                        }
                    }
                }
                PremiumButton("Reprovar", Modifier.weight(1f), Rose500, Icons.Default.Close) {
                    // Mesma lógica de datas para Reprovar
                    val mm = mes.substring(0, 2).toInt();
                    val aa = ano.toInt();
                    val ym = YearMonth.of(aa, mm)
                    val inicial = "${aa}/01/${"%02d".format(mm)}";
                    val final = "${aa}/${ym.lengthOfMonth()}/${"%02d".format(mm)}"
                    scope.launch(Dispatchers.IO) {
                        if (Comissao(
                                inicial,
                                final,
                                item.codigo,
                                "R"
                            )
                        ) snack.showSnackbar("Reprovado.")
                    }
                }
            }
        }
    }
}

// Função para buscar as comissões
private fun fetchComissaoRep(
    dbName: String,
    sDataInicial: String,
    sDataFinal: String,
    onResult: (List<RepresentanteComissao>) -> Unit
) {
    // Como já estamos dentro de um CoroutineScope IO na chamada da UI,
    // não precisamos criar uma nova Thread() aqui explicitamente.
    // O código rodará sequencialmente dentro da thread de IO da UI.

    val lista = mutableListOf<RepresentanteComissao>()
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                val sql = """
                  SELECT 
                    R.COD_REPRESENTANTE,
                    R.NOM_FANTASIA,
                    R.RAZ_SOCIAL,
                    R.IND_FUNCIONARIO,
                    DBO.FU_GET_VAL_COMISSAO_REPRESENTANTE(
                      R.COD_REPRESENTANTE,'$sDataInicial','$sDataFinal'
                    ) AS VAL_COMISSAO
                  FROM REPRESENTANTE R
                  WHERE R.COD_SITUACAO = 'A'
                  ORDER BY R.NOM_FANTASIA
                """.trimIndent()

                st.executeQuery(sql).use { rs ->
                    while (rs.next()) {
                        lista += RepresentanteComissao(
                            codigo         = rs.getString("COD_REPRESENTANTE").trim(),
                            nomeFantasia   = rs.getString("NOM_FANTASIA").trim(),
                            razaoSocial    = rs.getString("RAZ_SOCIAL").trim(),
                            indFuncionario = rs.getString("IND_FUNCIONARIO") == "S",
                            valComissao    = rs.getBigDecimal("VAL_COMISSAO")
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    onResult(lista)
}

// Função para Aprovar ("A") ou Reprovar ("R")
private fun Comissao(
    dDataInicial: String,
    dDataFinal: String,
    iCodRepresentante: String,
    dCond: String
): Boolean {
    var resultado = false

    // Uso de PreparedStatement para segurança e limpeza do código
    val sql = if (dCond == "A") {
        """
        UPDATE COMISSAO_REPRESENTANTE
        SET COD_SITUACAO = 'B', DAT_APROVACAO = GETDATE() 
        WHERE COD_REPRESENTANTE = ? 
        AND DAT_EMISSAO BETWEEN ? AND ?
        """
    } else {
        """
        UPDATE COMISSAO_REPRESENTANTE 
        SET COD_SITUACAO = 'A', NUM_MOVIMENTO = NULL, DAT_VINCULO_MF = NULL, DAT_APROVACAO = NULL
        WHERE COD_REPRESENTANTE = ?
        AND DAT_EMISSAO BETWEEN ? AND ?
        """
    }

    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        // Assumindo "Brasfit" fixo aqui conforme lógica original,
        // ou você pode passar dbName como parâmetro se preferir dinâmico.
        DriverManager.getConnection(getUrl("Brasfit")).use { conn ->
            conn.autoCommit = false
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, iCodRepresentante)
                ps.setString(2, dDataInicial)
                ps.setString(3, dDataFinal)

                val linhas = ps.executeUpdate()
                conn.commit()
                resultado = linhas > 0
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        resultado = false
    }
    return resultado
}

// Função para disparar a procedure de e-mail
private fun emailRepresentante(
    dDataInicial: String,
    dDataFinal: String,
    dDataExtra: String, // Parâmetro extra mantido para compatibilidade com sua chamada
    iCodRepresentante: String
): Boolean {
    var sucesso = false
    val callSql = "{ CALL SP_envia_comissao_representante( ?, ?, ? ) }"

    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl("Brasfit")).use { conn ->
            conn.prepareCall(callSql).use { cstmt ->
                cstmt.setString(1, iCodRepresentante)
                cstmt.setString(2, dDataInicial)
                cstmt.setString(3, dDataFinal)

                val count = cstmt.executeUpdate()
                // No SQL Server, executeUpdate pode retornar -1 para procedures que não retornam contagem de linhas
                sucesso = count != 0 || count == -1
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        sucesso = false
    }
    return sucesso
}