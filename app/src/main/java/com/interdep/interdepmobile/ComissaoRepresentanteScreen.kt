package com.interdep.interdepmobile.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.components.ActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.sql.DriverManager
import java.time.YearMonth
import com.interdep.interdepmobile.ui.components.getUrl
import com.interdep.interdepmobile.ui.components.MenuTopBar

/* ──────────────────── modelos ──────────────────── */
data class RepresentanteComissao(
    val codigo: String,
    val nomeFantasia: String,
    val razaoSocial: String,
    val indFuncionario: Boolean,
    val valComissao: BigDecimal
)

/* ──────────────────── tela de comissão ──────────────────── */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ComissaoRepresentanteScreen(
    dbName: String = "Brasfit",
    onDone: () -> Unit
) {
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // meses e anos
    val meses = listOf(
        "01 — Janeiro","02 — Fevereiro","03 — Março","04 — Abril",
        "05 — Maio","06 — Junho","07 — Julho","08 — Agosto",
        "09 — Setembro","10 — Outubro","11 — Novembro","12 — Dezembro"
    )
    val anos = (2021..2025).map { it.toString() }  // ou ajuste seu intervalo

    // estados do filtro
    var expandedMes   by remember { mutableStateOf(false) }
    var selectedMes   by remember { mutableStateOf(meses.first()) }
    var expandedAno   by remember { mutableStateOf(false) }
    var selectedAno   by remember { mutableStateOf(anos.last()) }

    // resultado
    var carregando    by remember { mutableStateOf(false) }
    var lista         by remember { mutableStateOf(listOf<RepresentanteComissao>()) }
    Scaffold(
        topBar = {
            MenuTopBar("Comissão de","Representantes",Icons.Default.AttachMoney)
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.End
            ) {
                ActionButton("Fechar",0xFFF44336 ,Modifier.weight(1f),onDone)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF3EEFF))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── filtros ───
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedMes,
                    onExpandedChange = { expandedMes = !expandedMes }
                ) {
                    OutlinedTextField(
                        value = selectedMes,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mês") },
                        modifier = Modifier
                            .weight(1f)
                            .width(185.dp)
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMes)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMes,
                        onDismissRequest = { expandedMes = false }
                    ) {
                        meses.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = {
                                selectedMes = m
                                expandedMes = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedAno,
                    onExpandedChange = { expandedAno = !expandedAno }
                ) {
                    OutlinedTextField(
                        value = selectedAno,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ano") },
                        modifier = Modifier
                            .weight(1f)
                            .width(115.dp)
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAno)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAno,
                        onDismissRequest = { expandedAno = false }
                    ) {
                        anos.forEach { y ->
                            DropdownMenuItem(text = { Text(y) }, onClick = {
                                selectedAno = y
                                expandedAno = false
                            })
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                // botão Buscar
                ActionButton("Buscar",0xFFF44336, Modifier
                    .weight(1f)          // ocupa toda a largura
                    .height(48.dp)) {
                    // monta datas: 01/MM/AAAA e último dia
                    val mm = selectedMes.substring(0,2).toInt()
                    val aa = selectedAno.toInt()
                    val ym = YearMonth.of(aa, mm)
                    val inicial = "01/${"%02d".format(mm)}/${aa}"
                    val final   = "${ym.lengthOfMonth()}/${"%02d".format(mm)}/${aa}"

                    carregando = true
                    scope.launch(Dispatchers.IO) {
                        fetchComissaoRep(dbName, inicial, final) { resultado ->
                            lista = resultado
                            carregando = false
                        }
                    }
                }
                ActionButton(
                    "Limpar",
                    0xFF9E9E9E,
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    // Restaura filtros
                    selectedMes = meses.first()               // volta para “01 — Janeiro”
                    selectedAno = anos.last()                // ou outro valor default se preferir
                    // Esvazia a lista
                    lista = emptyList()
                }


            }
            // lista de resultados
            Box(Modifier.fillMaxSize()) {
                if (carregando) {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = Color(0xFF7E57C2)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(lista, key = { it.codigo }) { item ->
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(item.nomeFantasia, fontWeight = FontWeight.SemiBold)
                                        Text(item.razaoSocial, fontSize = 12.sp, color = Color.Gray)
                                        //Text("Funcionário: ${if (item.indFuncionario) "Sim" else "Não"}",
                                        //    fontSize = 12.sp)
                                        Text(
                                            "R\$ ${item.valComissao.setScale(2)}",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF388E3C)

                                        )
                                        // Colocando botões abaixo do valor
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ActionButton("Aprovar", 0xFF55BB22,Modifier.weight(1f)) {
                                                val mm = selectedMes.substring(0,2).toInt()
                                                val aa = selectedAno.toInt()
                                                val ym = YearMonth.of(aa, mm)
                                                val inicial = "${aa}/01/${"%02d".format(mm)}"
                                                val final   = "${aa}/${ym.lengthOfMonth()}/${"%02d".format(mm)}"
//                                                val inicial = "01/${"%02d".format(mm)}/${aa}"
//                                                val final   = "${ym.lengthOfMonth()}/${"%02d".format(mm)}/${aa}"
                                                scope.launch {
                                                    if (Comissao(
                                                            inicial,
                                                            final,
                                                            item.codigo,
                                                            "A"
                                                        ) == true
                                                    ) {
                                                        if (emailRepresentante(
                                                                inicial,
                                                                final,
                                                                final,
                                                                item.codigo
                                                            ) == true
                                                        ) {
                                                            snackbarHost.showSnackbar("Comissão Aprovada!!")
                                                        }
                                                    }
                                                }

                                            }
                                            ActionButton("Reprovar",0xFFF44336,Modifier.weight(1f)) {
                                                val mm = selectedMes.substring(0,2).toInt()
                                                val aa = selectedAno.toInt()
                                                val ym = YearMonth.of(aa, mm)
                                                val inicial = "${aa}/01/${"%02d".format(mm)}"
                                                val final   = "${aa}/${ym.lengthOfMonth()}/${"%02d".format(mm)}"

                                                scope.launch {
                                                    if(Comissao(
                                                            inicial,
                                                            final,
                                                            item.codigo,
                                                            "R"

                                                        )){
                                                        snackbarHost.showSnackbar("Comissão Reprovada!!")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── função JDBC ───
private fun fetchComissaoRep(
    dbName: String,
    sDataInicial: String,
    sDataFinal: String,
    onResult: (List<RepresentanteComissao>) -> Unit
) = Thread {
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
}.start()

fun Comissao(dDataInicial: String, dDataFinal: String, iCodRepresentante: String,dCond: String): Boolean {
    var resultado = false
    var sql = ""
    var linhas =0
    if(dCond=="A"){
        sql = """
        UPDATE COMISSAO_REPRESENTANTE
        SET COD_SITUACAO = 'B', 
            DAT_APROVACAO = GETDATE() 
        WHERE COD_REPRESENTANTE = $iCodRepresentante 
        AND DAT_EMISSAO BETWEEN '$dDataInicial' AND '$dDataFinal';
    """.trimIndent()

    }else if (dCond=="R"){
        sql = """
        UPDATE COMISSAO_REPRESENTANTE 
           SET COD_SITUACAO = 'A'
               NUM_MOVIMENTO = NULL,
               DAT_VINCULO_MF = NULL, 
               DAT_APROVACAO = NULL
         WHERE COD_REPRESENTANTE = $iCodRepresentante
           AND DAT_EMISSAO BETWEEN '$dDataInicial' AND '$dDataFinal';
    """.trimIndent()
    }

    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl("Brasfit")).use { conn ->
            conn.autoCommit = false
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, iCodRepresentante)   // substitui o ?
                linhas = ps.executeUpdate()
                conn.commit()
                if(linhas > 0){resultado=true}else{resultado=false}                            // true se algo mudou
            }
        }    } catch (e: Exception) {
        e.printStackTrace()
        resultado = false
    }
    return resultado
}

fun emailRepresentante(
    dbName: String,
    dDataInicial: String,
    dDataFinal: String,
    iCodRepresentante: String
): Boolean {
    var sucesso = false

    // Chamada parametrizada da procedure (3 parâmetros IN)
    val callSql = "{ CALL SP_envia_comissao_representante( ?, ?, ? ) }"

    try {
        // 1) Carrega driver (jTDS ou Microsoft)
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        // Se preferir driver oficial da MS, use:
        // Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")

        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            // Opcional: controlar manualmente transação
            conn.autoCommit = false

            conn.prepareCall(callSql).use { cstmt ->
                // 2) Seta parâmetros IN
                cstmt.setString(1, iCodRepresentante)
                cstmt.setString(2, dDataInicial)
                cstmt.setString(3, dDataFinal)

                // 3) Executa; use executeUpdate() se a SP retornar linhas afetadas,
                //    ou execute() se ela gerar result-sets ou status variados.
                val count = cstmt.executeUpdate()

                // 4) Commit só após execução bem-sucedida
                conn.commit()

                // Considere sucesso = true se count >=0 (SQL Server retorna -1 se for exec sem DML)
                sucesso = count != 0 || count == -1
            }
        }
    }
    catch (e: Exception) {
        e.printStackTrace()
        // Se falhar, encerre transação para não deixar conexões “penduradas”
        sucesso = false
    }

    return sucesso
}