package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val strings = LocalStrings.current
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        delay(700L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF001233),
                        Color(0xFF001F54),
                        Color(0xFF032B69)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
            .systemBarsPadding()
            .padding(24.dp)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .scale(scale.value)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 310.dp)
                    .aspectRatio(1f),
                shape = CircleShape,
                color = Color(0xFF001B44),
                shadowElevation = 16.dp,
                border = BorderStroke(3.dp, Color(0xFF38BDF8).copy(alpha = 0.65f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_splash_photo),
                    contentDescription = strings.appFullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .testTag("splash_hero_image")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Narendra Weight & Money Calculator",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.developerTagline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBAE6FD),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .widthIn(max = 160.dp)
                    .fillMaxWidth(0.5f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFFACC15),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
