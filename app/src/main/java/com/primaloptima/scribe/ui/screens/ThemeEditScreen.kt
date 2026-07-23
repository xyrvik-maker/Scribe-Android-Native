package com.primaloptima.scribe.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.primaloptima.scribe.ui.theme.parseComposeColor
import com.primaloptima.scribe.util.DefaultThemes
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditScreen(
    themeId: String,
    vm: ThemeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themes by vm.themes.observeAsState(emptyList())

    val originalTheme = remember(themes, themeId) {
        themes.firstOrNull { it.id == themeId } ?: DefaultThemes.all.first()
    }

    var name by remember(originalTheme) { mutableStateOf(originalTheme.name) }
    var bgHex by remember(originalTheme) { mutableStateOf(originalTheme.colors.background) }
    var surfaceHex by remember(originalTheme) { mutableStateOf(originalTheme.colors.surface) }
    var textHex by remember(originalTheme) { mutableStateOf(originalTheme.colors.text) }
    var accentHex by remember(originalTheme) { mutableStateOf(originalTheme.colors.accent) }
    var fontSize by remember(originalTheme) { mutableFloatStateOf(originalTheme.fontSize.toFloat()) }
    var lineHeight by remember(originalTheme) { mutableFloatStateOf(originalTheme.lineHeight) }
    var bgUri by remember(originalTheme) { mutableStateOf(originalTheme.backgroundImageUri) }
    var bgOpacity by remember(originalTheme) { mutableFloatStateOf(originalTheme.backgroundImageOpacity ?: 0.35f) }

    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            bgUri = it.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Theme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val updated = originalTheme.copy(
                            name = name,
                            fontSize = fontSize.toInt(),
                            lineHeight = lineHeight,
                            backgroundImageUri = bgUri,
                            backgroundImageOpacity = bgOpacity,
                            colors = originalTheme.colors.copy(
                                background = bgHex,
                                surface = surfaceHex,
                                text = textHex,
                                accent = accentHex
                            )
                        )
                        vm.save(updated)
                        Toast.makeText(context, "Theme saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            Text("Theme Live Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val previewBg = parseComposeColor(bgHex, Color.White)
            val previewText = parseComposeColor(textHex, Color.Black)
            val previewAccent = parseComposeColor(accentHex, Color.Blue)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(previewBg)
                ) {
                    if (!bgUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = bgUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = bgOpacity))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "The quick brown fox jumps over the lazy dog.",
                                color = previewText,
                                fontSize = fontSize.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Accent Highlight & Cursor Color",
                                color = previewAccent,
                                fontSize = (fontSize - 2).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Background Image & Opacity Control", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { bgImagePicker.launch("image/*") }
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (bgUri.isNullOrEmpty()) "Add Background Image" else "Change Image")
                }

                if (!bgUri.isNullOrEmpty()) {
                    TextButton(onClick = { bgUri = null }) {
                        Text("Remove Image", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (!bgUri.isNullOrEmpty()) {
                Column {
                    Text("Overlay Opacity: ${(bgOpacity * 100).toInt()}%", fontWeight = FontWeight.Medium)
                    Slider(
                        value = bgOpacity,
                        onValueChange = { bgOpacity = it },
                        valueRange = 0.0f..0.90f
                    )
                }
            }

            HorizontalDivider()

            Text("Color & Font Properties", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Theme Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bgHex,
                onValueChange = { bgHex = it },
                label = { Text("Background Color (Hex)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = textHex,
                onValueChange = { textHex = it },
                label = { Text("Text Color (Hex)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = accentHex,
                onValueChange = { accentHex = it },
                label = { Text("Accent Color (Hex)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Font Size: ${fontSize.toInt()} sp")
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 12f..28f
            )

            Text("Line Height: ${String.format("%.2f", lineHeight)}")
            Slider(
                value = lineHeight,
                onValueChange = { lineHeight = it },
                valueRange = 1.0f..2.5f
            )
        }
    }
}
