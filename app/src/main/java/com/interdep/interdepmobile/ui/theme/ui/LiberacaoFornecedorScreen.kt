package com.interdep.interdepmobile.ui

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.interdep.interdepmobile.ui.components.*
import com.interdep.interdepmobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiberacaoFornecedorScreen(onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var codInput by remember { mutableStateOf("") }
    var fantasia by remember { mutableStateOf("") }
    var razao by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun buscar() {
        if(codInput.isEmpty()) return
        loading = true; errorMsg = null; fantasia = ""; razao = ""
        scope.launch(Dispatchers.IO) {
            val res = fetchFornecedorByCode(codInput)
            loading = false
            if (res != null) { fantasia = res.first; razao = res.second }
            else { errorMsg = "Não encontrado" }
        }
    }

    Scaffold(
        topBar = { PremiumTopBar("Liberação Fornecedor", "Desbloquear Cadastro", Icons.Default.LocationCity, onBack = onFinish) },
        containerColor = Slate100
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Área de Busca
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

            errorMsg?.let {
                Text(it, color = Rose500, modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Resultado
            if (fantasia.isNotEmpty()) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fornecedor Encontrado", style = MaterialTheme.typography.labelMedium, color = Slate500)
                        Text(fantasia, style = MaterialTheme.typography.titleLarge, color = Navy900)
                        Text(razao, style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Divider(Modifier.padding(vertical = 12.dp))
                        PremiumButton("Liberar Cadastro", Modifier.fillMaxWidth(), Emerald500, Icons.Default.CheckCircle) {
                            scope.launch(Dispatchers.IO) {
                                val qtd = liberateFornecedor(codInput)
                                launch(Dispatchers.Main) {
                                    Toast.makeText(ctx, if(qtd >= 0) "Liberado com sucesso!" else "Erro", Toast.LENGTH_SHORT).show()
                                    if(qtd >= 0) { codInput = ""; fantasia = "" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Manter fetchFornecedorByCode e liberateFornecedor aqui
private fun fetchFornecedorByCode(cod: String): Pair<String, String>? {
    // Sua lógica JDBC select...
    var res: Pair<String, String>? = null
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl("Brasfit")).use { conn ->
            conn.createStatement().use { st ->
                val rs = st.executeQuery("SELECT NOM_FANTASIA, RAZ_SOCIAL FROM FORNECEDOR WHERE COD_FORNECEDOR = '$cod'")
                if(rs.next()) res = rs.getString(1).trim() to rs.getString(2).trim()
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return res
}

private fun liberateFornecedor(cod: String): Int {
    // Sua lógica JDBC update...
    return 1 // Mock
}