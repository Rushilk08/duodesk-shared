package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.SeedData
import com.example.model.DocumentItem
import com.example.model.Visibility
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.util.Formatters

@Composable
fun DocumentPreviewDialog(
    doc: DocumentItem,
    onDismiss: () -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = doc.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview", tint = palette.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val drawableId = when (doc.previewUri) {
                    SeedData.PREVIEW_PRESENTATION -> R.drawable.img_doc_presentation_preview
                    SeedData.PREVIEW_WIREFRAME -> R.drawable.img_doc_architecture_wireframe
                    else -> null
                }

                when {
                    drawableId != null -> {
                        Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = doc.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                    !doc.previewUri.isNullOrBlank() && doc.previewUri.startsWith("content://") -> {
                        AsyncImage(
                            model = doc.previewUri,
                            contentDescription = doc.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.subtleSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (doc.visibility == Visibility.SHARED) palette.lavender else palette.pink,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = doc.summary.ifBlank {
                                        if (doc.visibility == Visibility.SHARED) {
                                            "Shared document stored in DuoDesk."
                                        } else {
                                            "Private document stored in My Space."
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${doc.extension} · ${Formatters.formatFileSize(doc.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    if (!doc.previewUri.isNullOrBlank() && doc.previewUri.startsWith("content://")) {
                        Button(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(doc.previewUri), doc.mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryButtonBg,
                                contentColor = palette.primaryButtonText
                            )
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open file", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
