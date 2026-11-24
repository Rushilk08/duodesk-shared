package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.AttachmentRef
import com.example.model.DocumentKind
import java.util.UUID

data class PickedFileMeta(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val kind: DocumentKind,
    val extension: String,
    val uriString: String
)

object FileHelpers {
    fun readPickedFileMeta(context: Context, uri: Uri): PickedFileMeta {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        var displayName = "Document"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) {
                    displayName = cursor.getString(nameIdx) ?: displayName
                }
                if (sizeIdx >= 0) {
                    size = cursor.getLong(sizeIdx)
                }
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val (kind, ext) = Formatters.detectDocumentKind(displayName)
        return PickedFileMeta(
            name = displayName,
            sizeBytes = if (size > 0) size else 128_000L,
            mimeType = mime,
            kind = kind,
            extension = ext,
            uriString = uri.toString()
        )
    }

    fun toAttachmentRef(meta: PickedFileMeta): AttachmentRef = AttachmentRef(
        id = "att-${UUID.randomUUID().toString().take(8)}",
        name = meta.name,
        sizeBytes = meta.sizeBytes,
        kind = meta.kind.name.lowercase(),
        mimeType = meta.mimeType,
        previewUri = if (meta.kind == DocumentKind.IMAGE) meta.uriString else null
    )
}
