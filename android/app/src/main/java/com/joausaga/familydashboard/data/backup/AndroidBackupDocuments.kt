package com.joausaga.familydashboard.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/** Solo las URI concedidas por Storage Access Framework; no necesita permisos generales de archivos. */
class AndroidBackupDocuments(private val resolver: ContentResolver) : BackupDocuments {
    override fun openRead(document: String): InputStream =
        requireNotNull(resolver.openInputStream(Uri.parse(document))) { "No se pudo leer o verificar el documento." }

    override fun openWrite(document: String): OutputStream =
        requireNotNull(resolver.openOutputStream(Uri.parse(document), "wt")) { "No se pudo escribir el documento." }
}
