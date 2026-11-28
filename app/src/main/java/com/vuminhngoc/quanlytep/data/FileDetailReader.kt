package com.vuminhngoc.quanlytep.data

import android.content.Context
import com.vuminhngoc.quanlytep.model.FileDetailInfo
import com.vuminhngoc.quanlytep.model.FileNode
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileDetailReader {

    private val timeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    fun formatTimestamp(millis: Long, fallback: String): String {
        if (millis <= 0L) return fallback.ifBlank { "—" }
        return timeFormatter.format(Date(millis))
    }

    fun detectFileFormat(item: FileNode, resolvedName: String = item.name): String {
        if (item.isFolder) return "Thư mục (Folder)"
        val lower = resolvedName.lowercase(Locale.ROOT)
        val ext = when {
            lower.endsWith(".tar.gz") -> "TAR.GZ"
            "." in lower -> lower.substringAfterLast('.').uppercase(Locale.ROOT)
            else -> "Không có đuôi"
        }
        return "$ext · ${item.detectKind().name}"
    }

    fun formatDetailedBytes(bytes: Long): String {
        if (bytes < 0L) return "0 B"
        val df = DecimalFormat("#,##0.##")
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "${df.format(bytes / 1024.0)} KB (${formatWithCommas(bytes)} B)"
            bytes < 1024L * 1024L * 1024L -> "${df.format(bytes / (1024.0 * 1024.0))} MB (${formatWithCommas(bytes)} B)"
            else -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB (${formatWithCommas(bytes)} B)"
        }
    }

    private fun formatWithCommas(value: Long): String =
        DecimalFormat("#,###").format(value)

    private fun parseFallbackSizeBytes(sizeText: String): Long {
        val clean = sizeText.trim().replace(",", ".")
        val match = Regex("""^([\d.]+)\s*(B|KB|MB|GB|TB)?$""", RegexOption.IGNORE_CASE).find(clean)
            ?: return 0L
        val num = match.groupValues[1].toDoubleOrNull() ?: return 0L
        val unit = match.groupValues[2].uppercase(Locale.ROOT)
        val multiplier = when (unit) {
            "", "B" -> 1.0
            "KB" -> 1024.0
            "MB" -> 1024.0 * 1024.0
            "GB" -> 1024.0 * 1024.0 * 1024.0
            "TB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
            else -> 1.0
        }
        return (num * multiplier).toLong().coerceAtLeast(0L)
    }

    fun readDetails(
        context: Context?,
        item: FileNode,
        fallbackPath: String
    ): FileDetailInfo {
        val rawPath = item.realFilePath?.takeIf { it.isNotBlank() }
            ?: if (fallbackPath.isNotBlank()) "$fallbackPath/${item.name}" else item.name

        if (!rawPath.startsWith("content://")) {
            val file = File(rawPath)
            if (file.exists()) {
                val timeStr = formatTimestamp(file.lastModified(), item.modified)
                val fmt = detectFileFormat(item, file.name)

                return if (file.isDirectory) {
                    inspectRealFolder(
                        root = file,
                        fileFormat = fmt,
                        formattedTime = timeStr
                    )
                } else {
                    val len = file.length().coerceAtLeast(0L)
                    FileDetailInfo(
                        fileFormat = fmt,
                        exactBytes = len,
                        formattedSize = formatDetailedBytes(len),
                        folderCount = 0,
                        fileCount = 1,
                        formattedTime = timeStr,
                        fullPath = file.absolutePath,
                        isReadable = file.canRead(),
                        isWritable = file.canWrite()
                    )
                }
            }
        }

        val fallbackBytes = parseFallbackSizeBytes(item.size)
        return FileDetailInfo(
            fileFormat = detectFileFormat(item),
            exactBytes = fallbackBytes,
            formattedSize = if (fallbackBytes > 0L) formatDetailedBytes(fallbackBytes) else item.size,
            folderCount = if (item.isFolder) item.children.count { it.isFolder } else 0,
            fileCount = if (item.isFolder) item.children.count { !it.isFolder } else 1,
            formattedTime = item.modified.ifBlank { "—" },
            fullPath = rawPath,
            isReadable = context != null,
            isWritable = false
        )
    }

    private fun inspectRealFolder(
        root: File,
        fileFormat: String,
        formattedTime: String
    ): FileDetailInfo {
        var totalBytes = 0L
        var folders = 0
        var files = 0

        fun walk(dir: File) {
            val children = runCatching { dir.listFiles() }.getOrNull().orEmpty()
            for (child in children) {
                if (child.isDirectory) {
                    folders++
                    walk(child)
                } else if (child.isFile) {
                    files++
                    totalBytes += child.length().coerceAtLeast(0L)
                }
            }
        }

        walk(root)
        return FileDetailInfo(
            fileFormat = fileFormat,
            exactBytes = totalBytes,
            formattedSize = formatDetailedBytes(totalBytes),
            folderCount = folders,
            fileCount = files,
            formattedTime = formattedTime,
            fullPath = root.absolutePath,
            isReadable = root.canRead(),
            isWritable = root.canWrite()
        )
    }
}
