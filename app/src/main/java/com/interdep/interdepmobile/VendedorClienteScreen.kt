package com.interdep.interdepmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.DriverManager
import com.interdep.interdepmobile.ui.components.ActionButton
import com.interdep.interdepmobile.ui.components.MenuTopBar
import com.interdep.interdepmobile.ui.components.getUrl

// --------------------------------------------------
// Sua tela agora com novo topBar e botões estilizados
// --------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendedorClienteScreen(
    dbName: String = "Brasfit",
    onDone: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackHost = remember { SnackbarHostState() }

    val sellers = listOf(
        "Renan" to 59,
        "Gabriel" to 61,
        "Rose" to 148,
        "Matheus" to 794,
        "Cruz" to 530
    )
    val sellerNames = sellers.map { it.first }
    val sellerCodes = sellers.toMap()

    var expandedSeller by remember { mutableStateOf(false) }
    var selectedSellerName by remember { mutableStateOf("") }
    var cliente by remember { mutableStateOf("") }

    val codV : Int? = sellerCodes[selectedSellerName]

    Scaffold(
        topBar = {
            MenuTopBar("Incluir Vendedor ↔ Cliente","",Icons.Default.Business)
        },
        snackbarHost = { SnackbarHost(snackHost) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedSeller,
                onExpandedChange = { expandedSeller = !expandedSeller }
            ) {
                OutlinedTextField(
                    value = selectedSellerName,
                    onValueChange = {},
                    shape = RoundedCornerShape(50),
                    readOnly = true,
                    label = { Text("Vendedor") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSeller) }
                )
                ExposedDropdownMenu(
                    expanded = expandedSeller,
                    onDismissRequest = { expandedSeller = false }
                ) {
                    sellerNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedSellerName = name
                                expandedSeller = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = cliente,
                onValueChange = { cliente = it },
                shape = RoundedCornerShape(50),
                label = { Text("Cliente") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            )  {

                ActionButton( "Salvar",0xFFF44336, Modifier.weight(1f)) {
                    scope.launch(Dispatchers.IO) {
                        val qtd = upsertVendedorCliente(dbName, codV, cliente)
                        if (qtd >= 0) {
                            snackHost.showSnackbar("Foram afetadas $qtd linha(s).")
                            selectedSellerName = ""
                            cliente = ""
                        } else {
                            snackHost.showSnackbar("Erro ao gravar no banco.")
                        }
                    }
            }
                ActionButton("Fechar",0xFFF44336, Modifier.weight(1f), onDone)
            }
        }
    }
}

fun upsertVendedorCliente(
    dbName: String,
    codVendedor: Int?,
    codCliente: String
): Int {
    var total = 0
    val clienteEscaped = codCliente.replace("'", "''")  // Escape de aspas simples

    val sql = """
        MERGE INTO vendedor_cliente AS tgt
        USING (VALUES ($codVendedor, '$clienteEscaped')) 
               AS src(cod_vendedor_novo, cod_cliente)
        ON tgt.cod_cliente = src.cod_cliente
        WHEN MATCHED THEN
            UPDATE SET tgt.cod_vendedor = src.cod_vendedor_novo
        WHEN NOT MATCHED THEN
            INSERT (cod_vendedor, cod_cliente)
            VALUES (src.cod_vendedor_novo, src.cod_cliente);
    """.trimIndent()

    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { stmt ->
                total = stmt.executeUpdate(sql)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        total = -1
    }
    return total
}
