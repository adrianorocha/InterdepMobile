package com.interdep.interdepmobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ClonarMaterialScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    var selectedDb by remember { mutableStateOf("Brasfit") }
    var codigoOrigem by remember { mutableStateOf("") }
    var codigoDestino by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(Modifier.background(Color.White)) {
                PremiumTopBar(
                    title = "Clonar Material",
                    subtitle = "Cópia de Estrutura e Valores",
                    icon = Icons.Default.ContentCopy,
                    onBack = onFinish
                )
                ResponsiveDbSelector(
                    selectedDb = selectedDb,
                    onDbSelected = { selectedDb = it }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost) { data ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    PremiumSnackbar(Icons.Default.ContentCopy,data)
                }
            }
        },
        containerColor = Slate100
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Card de Formulário
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Dados da Clonagem", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy900)
                    Text("Informe o material base e o novo código gerado.", fontSize = 13.sp, color = Slate500)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Campo Código Origem
                    OutlinedTextField(
                        value = codigoOrigem,
                        onValueChange = { codigoOrigem = it.uppercase() },
                        label = { Text("Código de Origem") },
                        placeholder = { Text("Ex: MWM 922903270044") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, tint = Navy700) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Navy700,
                            focusedLabelColor = Navy700
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Código Destino
                    OutlinedTextField(
                        value = codigoDestino,
                        onValueChange = { codigoDestino = it.uppercase() },
                        label = { Text("Novo Código (Destino)") },
                        placeholder = { Text("Ex: MWM2 922903270044") },
                        leadingIcon = { Icon(Icons.Default.QrCode, null, tint = Emerald500) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald500,
                            focusedLabelColor = Emerald500
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botão de Ação
            PremiumButton(
                text = if (carregando) "Processando..." else "Executar Clonagem",
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (carregando) Slate500 else Emerald500,
                icon = Icons.Default.ContentCopy
            ) {
                if (codigoOrigem.isBlank() || codigoDestino.isBlank()) {
                    scope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        snackbarHost.showSnackbar("Atenção | Preencha ambos os códigos para continuar | Erro", duration = SnackbarDuration.Short)
                    }
                    return@PremiumButton
                }

                carregando = true
                scope.launch(Dispatchers.IO) {
                    val sucesso = executarClonagemMaterial(selectedDb, codigoOrigem.trim(), codigoDestino.trim())

                    launch(Dispatchers.Main) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        carregando = false

                        if (sucesso) {
                            codigoOrigem = ""
                            codigoDestino = ""
                        }
                    }

                    launch {
                        snackbarHost.showSnackbar(
                            message = if (sucesso) "Sucesso! | Material clonado com êxito | Sucesso" else "Falha | Verifique se a origem existe e o destino já não está em uso | Erro",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }
}

// ─── LÓGICA JDBC TRANSAKCIONAL ───
private fun executarClonagemMaterial(dbName: String, origem: String, destino: String): Boolean {
    var sucesso = false
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            // Desabilita o auto-commit para garantir a integridade (tudo ou nada)
            conn.autoCommit = false

            try {
                // 1. Clonar MATERIAL
                val sqlMaterial = """
                    INSERT INTO [dbo].[MATERIAL] (
                        [DES_REDUZIDA], [IND_CONTROLE_ESTOQUE], [NUM_PRAZO_ENTREGA], [COD_SITUACAO],
                        [DES_COMPLETA], [VAL_VENDA], [PER_IPI], [COD_MATERIAL], [COD_CATEGORIA_MATERIAL],
                        [COD_UNIDADE], [COD_GRUPO], [COD_SUB_GRUPO], [COD_CLAS_FISCAL],
                        [COD_APLICACAO_MATERIAL], [QTD_PRAZO_ENTREGA], [QTD_ESTOQUE_SEGURANCA],
                        [IND_ITEM_SEGURANCA], [IND_ITEM_CATALOGO], [IND_ITEM_MONTADO],
                        [IND_MONTAGEM_OBRIGATORIA], [IND_BLOQUEIO_VENDA], [IND_COMISSAO_FIXA],
                        [CADASTRADO_POR], [VAL_CUSTO], [COD_ORIGEM_MERCADORIA], [COD_TRIBUTACAO_ICMS],
                        [IND_RED_BC], [DAT_CADASTRO], [PER_COFINS], [PER_PIS], [PER_II], [PESO],
                        [PESO_NOVO], [PESO_ANTIGO], [COD_PEDIDO_COMPRA], [NUM_LINHA_COMPRA],
                        [DES_REDUZIDA_INGLES], [DES_COMPLETA_INGLES], [DES_REDUZIDA_ESPANHOL],
                        [DES_COMPLETA_ESPANHOL], [EMPRESA], [COD_UNIDADE_BKP], [DAT_ATUALIZACAO],
                        [ESTOQ_MINIMO], [IND_ESPECIAL], [COD_CLAS_FISCAL_BKP], [IND_SOB_CONSULTA],
                        [VAL_BENEF], [LOTE_MINIMO], [VAL_BENEF_BKP], [LOTE_MINIMO_BKP]
                    )
                    SELECT
                        [DES_REDUZIDA], [IND_CONTROLE_ESTOQUE], [NUM_PRAZO_ENTREGA], [COD_SITUACAO],
                        [DES_COMPLETA], [VAL_VENDA], [PER_IPI], ?, [COD_CATEGORIA_MATERIAL],
                        [COD_UNIDADE], [COD_GRUPO], [COD_SUB_GRUPO], [COD_CLAS_FISCAL],
                        [COD_APLICACAO_MATERIAL], [QTD_PRAZO_ENTREGA], [QTD_ESTOQUE_SEGURANCA],
                        [IND_ITEM_SEGURANCA], [IND_ITEM_CATALOGO], [IND_ITEM_MONTADO],
                        [IND_MONTAGEM_OBRIGATORIA], [IND_BLOQUEIO_VENDA], [IND_COMISSAO_FIXA],
                        [CADASTRADO_POR], [VAL_CUSTO], [COD_ORIGEM_MERCADORIA], [COD_TRIBUTACAO_ICMS],
                        [IND_RED_BC], GETDATE(), [PER_COFINS], [PER_PIS], [PER_II], [PESO],
                        [PESO_NOVO], [PESO_ANTIGO], [COD_PEDIDO_COMPRA], [NUM_LINHA_COMPRA],
                        [DES_REDUZIDA_INGLES], [DES_COMPLETA_INGLES], [DES_REDUZIDA_ESPANHOL],
                        [DES_COMPLETA_ESPANHOL], [EMPRESA], [COD_UNIDADE_BKP], GETDATE(),
                        [ESTOQ_MINIMO], [IND_ESPECIAL], [COD_CLAS_FISCAL_BKP], [IND_SOB_CONSULTA],
                        [VAL_BENEF], [LOTE_MINIMO], [VAL_BENEF_BKP], [LOTE_MINIMO_BKP]
                    FROM [dbo].[MATERIAL]
                    WHERE [COD_MATERIAL] = ?
                """.trimIndent()

                conn.prepareStatement(sqlMaterial).use { ps ->
                    ps.setString(1, destino)
                    ps.setString(2, origem)
                    val rowsAffected = ps.executeUpdate()
                    if (rowsAffected == 0) {
                        throw Exception("Material de origem não encontrado.")
                    }
                }

                // 2. Clonar MATERIAL_ESTRUTURA
                val sqlEstrutura = """
                    INSERT INTO [dbo].[MATERIAL_ESTRUTURA] (
                        [QTD_MATERIAL], [COD_ESTRUTURA], [COD_MATERIAL], [COD_MATERIAL_ESTRUTURA]
                    )
                    SELECT
                        [QTD_MATERIAL], [COD_ESTRUTURA], ?, [COD_MATERIAL_ESTRUTURA]
                    FROM [dbo].[MATERIAL_ESTRUTURA]
                    WHERE [COD_MATERIAL] = ?
                """.trimIndent()

                conn.prepareStatement(sqlEstrutura).use { ps ->
                    ps.setString(1, destino)
                    ps.setString(2, origem)
                    ps.executeUpdate()
                }

                // 3. Clonar VENDA_VALOR_MINIMO
                val sqlVendaMin = """
                    INSERT INTO [dbo].[VENDA_VALOR_MINIMO] (
                        [COD_MATERIAL], [MIN_0], [MIN_7], [MIN_12], [MIN_18], [RETORNO_0], [RETORNO_7],
                        [RETORNO_12], [RETORNO_18], [MIN_5E15], [MIN_ACIMA15], [RETORNO_5E15],
                        [RETORNO_ACIMA15], [RETORNO_0_BKP], [MIN_0_BKP], [MIN_COM_4], [FCA_5_15_0],
                        [FCA_5_15_4], [FCA_5_15_8], [FCA_AC15_0], [FCA_AC15_4], [FCA_AC15_8],
                        [FOB_5_15_0], [FOB_5_15_4], [FOB_5_15_8], [FOB_AC15_0], [FOB_AC15_4],
                        [FOB_AC15_8], [IND_0], [IND_4]
                    )
                    SELECT
                        ?, [MIN_0], [MIN_7], [MIN_12], [MIN_18], [RETORNO_0], [RETORNO_7],
                        [RETORNO_12], [RETORNO_18], [MIN_5E15], [MIN_ACIMA15], [RETORNO_5E15],
                        [RETORNO_ACIMA15], [RETORNO_0_BKP], [MIN_0_BKP], [MIN_COM_4], [FCA_5_15_0],
                        [FCA_5_15_4], [FCA_5_15_8], [FCA_AC15_0], [FCA_AC15_4], [FCA_AC15_8],
                        [FOB_5_15_0], [FOB_5_15_4], [FOB_5_15_8], [FOB_AC15_0], [FOB_AC15_4],
                        [FOB_AC15_8], [IND_0], [IND_4]
                    FROM [dbo].[VENDA_VALOR_MINIMO]
                    WHERE [COD_MATERIAL] = ?
                """.trimIndent()

                conn.prepareStatement(sqlVendaMin).use { ps ->
                    ps.setString(1, destino)
                    ps.setString(2, origem)
                    ps.executeUpdate()
                }

                // Efetiva a transação se tudo der certo
                conn.commit()
                sucesso = true

            } catch (e: Exception) {
                // Desfaz qualquer inserção parcial caso dê erro
                conn.rollback()
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return sucesso
}