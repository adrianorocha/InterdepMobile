package com.interdep.interdepmobile.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.interdep.interdepmobile.ui.components.ActionButton
import com.interdep.interdepmobile.ui.components.MenuTopBar
import com.interdep.interdepmobile.ui.components.getUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.DriverManager

// Função para buscar um fornecedor pelo código
private fun fetchFornecedorByCode(codFornecedor: String): Pair<String, String>? {
    var resultado: Pair<String, String>? = null
    var dbName: String = "Brasfit"
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                val rs = st.executeQuery(
                    """
                    SELECT NOM_FANTASIA, RAZ_SOCIAL 
                      FROM FORNECEDOR 
                     WHERE COD_FORNECEDOR = '$codFornecedor'
                    """.trimIndent()
                )

                if (rs.next()) {
                    val fantasia = rs.getString("NOM_FANTASIA").trim()
                    val razao    = rs.getString("RAZ_SOCIAL").trim()
                    resultado = fantasia to razao
                }
                rs.close()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return resultado
}

// Mesma função de liberação do fornecedor que você já tinha
private fun liberateFornecedor(codFornecedor: String): Int {
    var dbName: String = "Brasfit"
    var updated = 0
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                updated = st.executeUpdate(
                    "UPDATE FORNECEDOR SET COD_SITUACAO = 'A' WHERE COD_FORNECEDOR = '$codFornecedor'"
                )
            }
            conn.commit()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        updated = -1
    }
    return updated
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiberacaoFornecedorScreen(onFinish: () -> Unit) {
    val ctx    = LocalContext.current
    val scope  = rememberCoroutineScope()

    // estados da UI
    var codInput  by remember { mutableStateOf("") }
    var fantasia  by remember { mutableStateOf("") }
    var razao     by remember { mutableStateOf("") }
    var loading   by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    // dispara a busca assim que o usuário terminar de digitar
    fun buscarFornecedor(cod: String) {
        loading  = true
        errorMsg = null
        fantasia = ""
        razao    = ""
        scope.launch(Dispatchers.IO) {
            val res = fetchFornecedorByCode(cod)
            launch(Dispatchers.Main) {
                if (res != null) {
                    fantasia = res.first
                    razao    = res.second
                } else {
                    errorMsg = "Fornecedor não encontrado."
                }
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
                MenuTopBar("Liberação de ","Fornecedores",Icons.Default.LocationCity)
        },
        bottomBar = {
            // Botões Liberar e Fechar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActionButton("Liberar",0xFFF44336, Modifier.weight(1f)) {
                    scope.launch(Dispatchers.IO) {
                        val qtd = liberateFornecedor(codInput)
                        launch(Dispatchers.Main) {
                            if (qtd >= 0) {
                                Toast.makeText(ctx, "Foram atualizadas $qtd linha(s).", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(ctx, "Erro na liberação.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                ActionButton("Fechar",0xFFF44336, Modifier.weight(1f), onFinish)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = codInput,
                onValueChange = {
                    codInput = it
                    if (it.length >= 1) buscarFornecedor(it)
                },
                label = { Text("Código do Fornecedor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { buscarFornecedor(codInput) }
                )
            )

            Spacer(Modifier.height(16.dp))

            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }

            errorMsg?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

            // campos somente leitura
            OutlinedTextField(
                value = fantasia,
                onValueChange = {},
                label = { Text("Nome Fantasia") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = razao,
                onValueChange = {},
                label = { Text("Razão Social") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // intercepta o back físico pra fechar o dialogo se precisar
    BackHandler { onFinish() }
}