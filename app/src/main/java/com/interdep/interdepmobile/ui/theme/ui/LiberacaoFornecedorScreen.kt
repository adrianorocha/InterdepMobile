package com.interdep.interdepmobile.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.interdep.interdepmobile.ui.theme.*
import com.interdep.interdepmobile.ui.theme.ui.PremiumButton
import com.interdep.interdepmobile.ui.theme.ui.PremiumSnackbar
import com.interdep.interdepmobile.ui.theme.ui.PremiumTopBar
import com.interdep.interdepmobile.ui.theme.ui.ResponsiveDbSelector
import com.interdep.interdepmobile.ui.theme.ui.getUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiberacaoFornecedorScreen(onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDb by remember { mutableStateOf("Brasfit") }
    var codInput by remember { mutableStateOf("") }

    // Estados do Fornecedor
    var fantasia by remember { mutableStateOf("") }
    var razao by remember { mutableStateOf("") }
    var situacao by remember { mutableStateOf("") } // 'A' para Ativo, 'I' para Inativo, etc.

    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current
    val snackbarHost = remember { SnackbarHostState() }

    fun buscar() {
        if(codInput.isEmpty()) return
        loading = true; errorMsg = null; fantasia = ""; razao = ""; situacao = ""
        scope.launch(Dispatchers.IO) {
            val res = fetchFornecedorByCode(selectedDb, codInput)
            loading = false
            if (res != null) {
                fantasia = res.first
                razao = res.second
                situacao = res.third
            } else {
                errorMsg = "Fornecedor não encontrado no banco $selectedDb"
            }
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(Color.White)) {
                PremiumTopBar(
                    "Liberação Fornecedor",
                    "Desbloquear Cadastro",
                    Icons.Default.LocationCity,
                    onBack = onFinish
                )
                ResponsiveDbSelector(selectedDb = selectedDb, onDbSelected = {
                    selectedDb = it
                    fantasia = ""
                })
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost) { data ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    PremiumSnackbar(data)
                }
            }
        },
        containerColor = Slate100
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Busca
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = codInput, onValueChange = { codInput = it },
                        label = { Text("Código do Fornecedor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { buscar() }),
                        trailingIcon = { IconButton(onClick = { buscar() }) { Icon(Icons.Default.Search, null, tint = Navy700) } },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Navy700)

            errorMsg?.let { Text(it, color = Rose500, modifier = Modifier.align(Alignment.CenterHorizontally)) }

            // Resultado Dinâmico
            if (fantasia.isNotEmpty()) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fornecedor Encontrado ($selectedDb)", style = MaterialTheme.typography.labelMedium, color = Slate500)
                        Text(fantasia, style = MaterialTheme.typography.titleLarge, color = Navy900)
                        Text(razao, style = MaterialTheme.typography.bodyMedium, color = Slate500)

                        Divider(Modifier.padding(vertical = 12.dp))

                        // --- VALIDAÇÃO DE STATUS ---
                        if (situacao == "A") {
                            // Caso já esteja liberado, mostra um aviso amigável
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Emerald500.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Emerald500)
                                Spacer(Modifier.width(8.dp))
                                Text("Este fornecedor já está liberado.", color = Emerald500, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Caso esteja bloqueado, mostra o botão de ação
                            PremiumButton(
                                "Liberar Cadastro",
                                Modifier.fillMaxWidth(),
                                Emerald500,
                                Icons.Default.CheckCircle
                            ) {
                                scope.launch(Dispatchers.IO) {
                                    val qtd = liberateFornecedor(selectedDb, codInput)
                                    launch(Dispatchers.Main) {
                                        // Dispara a vibração
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                        if (qtd >= 0) {
                                            Toast.makeText(
                                                ctx,
                                                "Fornecedor | Liberado com sucesso! | Sucesso",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            buscar() // Recarrega para mostrar o status de sucesso
                                        } else {
                                            Toast.makeText(
                                                ctx,
                                                "Fornecedor | Erro ao liberar | Erro",
                                                Toast.LENGTH_SHORT
                                            ).show()
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

// --- JDBC ATUALIZADO ---

private fun fetchFornecedorByCode(dbName: String, cod: String): Triple<String, String, String>? {
    var res: Triple<String, String, String>? = null
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                // Agora buscamos também o campo COD_SITUACAO
                val rs = st.executeQuery("SELECT NOM_FANTASIA, RAZ_SOCIAL, COD_SITUACAO FROM FORNECEDOR WHERE COD_FORNECEDOR = '$cod'")
                if(rs.next()) {
                    res = Triple(
                        rs.getString(1).trim(),
                        rs.getString(2).trim(),
                        rs.getString(3).trim() // COD_SITUACAO
                    )
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return res
}

private fun liberateFornecedor(dbName: String, cod: String): Int {
    var total = 0
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn -> // Usa o dbName aqui
            conn.createStatement().use { st ->
                // Exemplo de update para liberar fornecedor (ajuste o campo conforme seu banco)
                total = st.executeUpdate("UPDATE FORNECEDOR SET COD_SITUACAO = 'A' WHERE COD_FORNECEDOR = '$cod'")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        total = -1
    }
    return total
}