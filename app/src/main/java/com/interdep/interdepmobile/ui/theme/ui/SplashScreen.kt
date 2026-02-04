package com.interdep.interdepmobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.R
import com.interdep.interdepmobile.ui.theme.Navy900
import com.interdep.interdepmobile.ui.theme.Slate500

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900), // Fundo escuro premium
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Garanta que o drawable 'logo' exista. Se não, use um Icon placeholder
            // Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(100.dp))

            // Placeholder caso não tenha logo:
            Box(Modifier.size(80.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape))

            Spacer(Modifier.height(24.dp))
            Text(
                text = "INTERDEP",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 4.sp
            )
            Text(
                text = "MOBILE",
                fontSize = 14.sp,
                color = Slate500,
                letterSpacing = 8.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Text(
            text = "v1.0.0",
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            fontSize = 12.sp
        )
    }
}