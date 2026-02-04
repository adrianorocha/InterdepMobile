package com.interdep.interdepmobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionButton(
    text: String,
    cor: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 0.dp) // zera o minWidth
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(cor))
    ) {
        Icon(Icons.Default.DoneOutline, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 10.sp)
    }
}

fun getUrl(dbName: String): String =
    "jdbc:jtds:sqlserver://200.205.57.178;databaseName=$dbName;user=sa;password=sqlsa;"
//"jdbc:jtds:sqlserver://10.0.0.4;databaseName=$dbName;user=sa;password=sqlsa;"

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MenuTopBar(
    texto1: String,
    texto2: String = "",
    trailingIcon: ImageVector = Icons.Default.Person,
    trailingIconContentDescription: String? = null
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ícone da esquerda (não clicável)
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                tint = Color.Black
            )

            // título central
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = texto1, fontSize = 18.sp, color = Color.Black)
                if (texto2.isNotBlank()) {
                    Text(text = texto2, fontSize = 18.sp, color = Color.Black)
                }
            }

            // ícone da direita (não clicável)
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingIconContentDescription,
                tint = Color.Black
            )
        }
    }
}
