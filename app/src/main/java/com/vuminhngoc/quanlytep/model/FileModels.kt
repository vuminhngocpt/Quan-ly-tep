package com.vuminhngoc.quanlytep.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class StorageSource(
    val id: String,
    val label: String,
    val defaultTag: String,
    val defaultPath: String,
    val defaultUsedBytes: Long,
    val defaultTotalBytes: Long,
    val defaultHost: String,
    val defaultPort: String,
    val defaultMode: String,
    val defaultUser: String,
    val dotColorHex: Long
) {
    DEVICE(
        id = "device",
        label = "Bộ nhớ máy",
        defaultTag = "UFS 4.0",
        defaultPath = "/storage/emulated/0",
        defaultUsedBytes = (98.4 * 1024 * 1024 * 1024).toLong(),
        defaultTotalBytes = (256.0 * 1024 * 1024 * 1024).toLong(),
        defaultHost = "localhost",
        defaultPort = "",
        defaultMode = "Local",
        defaultUser = "owner",
        dotColorHex = 0xFF3B82F6
    ),
    SD(
        id = "sd",
        label = "Thẻ nhớ",
        defaultTag = "SDXC UHS-I",
        defaultPath = "/storage/sdcard1",
        defaultUsedBytes = (18.4 * 1024 * 1024 * 1024).toLong(),
        defaultTotalBytes = (32.0 * 1024 * 1024 * 1024).toLong(),
        defaultHost = "localhost",
        defaultPort = "",
        defaultMode = "SDXC",
        defaultUser = "owner",
        dotColorHex = 0xFF22C55E
    ),
    OTG(
        id = "otg",
        label = "USB OTG",
        defaultTag = "exFAT • USB 3.2",
        defaultPath = "/storage/usbotg",
        defaultUsedBytes = (12.6 * 1024 * 1024 * 1024).toLong(),
        defaultTotalBytes = (64.0 * 1024 * 1024 * 1024).toLong(),
        defaultHost = "usb-bus-01",
        defaultPort = "",
        defaultMode = "exFAT",
        defaultUser = "owner",
        dotColorHex = 0xFF10B981
    ),
    NAS(
        id = "nas",
        label = "NAS",
        defaultTag = "SMB • 192.168.1.50",
        defaultPath = "192.168.1.50/volume1",
        defaultUsedBytes = (1800.0 * 1024 * 1024 * 1024).toLong(),
        defaultTotalBytes = (4096.0 * 1024 * 1024 * 1024).toLong(),
        defaultHost = "192.168.1.50",
        defaultPort = "5005",
        defaultMode = "SMB3 + WebDAV",
        defaultUser = "admin",
        dotColorHex = 0xFFA855F7
    ),
    SMB(
        id = "smb",
        label = "SMB",
        defaultTag = "SMBv3 • 192.168.1.50",
        defaultPath = "//192.168.1.50/share",
        defaultUsedBytes = (420.0 * 1024 * 1024 * 1024).toLong(),
        defaultTotalBytes = (1024.0 * 1024 * 1024 * 1024).toLong(),
        defaultHost = "192.168.1.50",
        defaultPort = "445",
        defaultMode = "SMBv3",
        defaultUser = "workgroup_user",
        dotColorHex = 0xFF06B6D4
    ),
    FTP(
        id = "ftp",
        label = "FTP",
        defaultTag = "PASV • 192.168.1.21:21",
        defaultPath = "ftp://192.168.1.21:21",
        defaultUsedBytes = (85.0 * 1024 * 1024 * 1024).toLong(),
        defaultTotalBytes = (512.0 * 1024 * 1024 * 1024).toLong(),
        defaultHost = "192.168.1.21",
        defaultPort = "21",
        defaultMode = "Passive (PASV)",
        defaultUser = "ftp_user",
        dotColorHex = 0xFFF59E0B
    );

    val isNetwork: Boolean
        get() = this == NAS || this == SMB || this == FTP
}

enum class FileFilter(val id: String, val label: String) {
    ALL("all", "Tất cả"),
    FOLDER("folder", "Thư mục"),
    IMAGE("image", "Ảnh"),
    VIDEO("video", "Video"),
    AUDIO("audio", "Âm thanh"),
    DOC("doc", "Tài liệu"),
    ARCHIVE("archive", "Nén")
}

enum class ItemKind {
    FOLDER, IMAGE, VIDEO, AUDIO, DOC, ARCHIVE, FILE
}

enum class ThemeMode(val label: String) {
    DARK("Tối"),
    LIGHT("Sáng"),
    AUTO("Tự động")
}

enum class SortOption(val label: String) {
    NAME("Tên (A → Z)"),
    TYPE("Loại mục (Thư mục trước)"),
    DATE("Ngày cập nhật")
}

enum class ViewMode {
    LIST, GRID
}

enum class ClipboardMode {
    COPY, MOVE
}

data class PdfPageData(
    val pageNumber: Int,
    val title: String,
    val body: String
)

data class ArchiveEntryItem(
    val name: String,
    val size: String,
    val isFolder: Boolean = false
)

data class FileNode(
    val id: String = "node_${UUID.randomUUID().toString().take(8)}",
    val name: String,
    val isFolder: Boolean,
    val modified: String = todayShort(),
    val size: String = "",
    val notes: String = "",
    val textContent: String? = null,
    val pdfPages: List<PdfPageData>? = null,
    val archiveEntries: List<ArchiveEntryItem>? = null,
    val children: List<FileNode> = emptyList(),
    val realFilePath: String? = null
) {
    fun detectKind(): ItemKind {
        if (isFolder) return ItemKind.FOLDER
        val ext = getExtension(name)
        if (ext in ARCHIVE_EXTS || !archiveEntries.isNullOrEmpty()) return ItemKind.ARCHIVE
        if (ext in IMAGE_EXTS) return ItemKind.IMAGE
        if (ext in VIDEO_EXTS) return ItemKind.VIDEO
        if (ext in AUDIO_EXTS) return ItemKind.AUDIO
        if (ext in DOC_EXTS) return ItemKind.DOC
        return ItemKind.FILE
    }

    fun deepClone(newModified: String = todayShort()): FileNode {
        return copy(
            id = "clone_${UUID.randomUUID().toString().take(8)}",
            modified = newModified,
            children = children.map { it.deepClone(newModified) }
        )
    }
}

data class FolderStep(
    val id: String,
    val name: String
)

data class HistoryEntry(
    val source: StorageSource,
    val folderStack: List<FolderStep>
)

data class ClipboardState(
    val mode: ClipboardMode,
    val fromSource: StorageSource,
    val fromStack: List<FolderStep>,
    val sourceItemIds: List<String>,
    val items: List<FileNode>
)

data class StorageMetadata(
    val source: StorageSource,
    val label: String,
    val shortTag: String,
    val path: String,
    val usedBytes: Long,
    val totalBytes: Long,
    val host: String,
    val port: String,
    val mode: String,
    val user: String,
    val encoding: String = "UTF-8"
)

data class NetworkProfile(
    val name: String,
    val host: String,
    val path: String,
    val port: String,
    val mode: String,
    val user: String,
    val encoding: String = "UTF-8"
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val sortBy: SortOption = SortOption.NAME,
    val showHidden: Boolean = false,
    val showSize: Boolean = true,
    val compactMode: Boolean = false,
    val autoReconnect: Boolean = true,
    val confirmDelete: Boolean = true,
    val searchCache: Boolean = true,
    val useRealDeviceStorage: Boolean = true
)

val ARCHIVE_EXTS = setOf("zip", "tar", "tar.gz", "tgz", "gz", "7z", "rar")
val PDF_EXTS = setOf("pdf")
val DOC_EXTS = setOf(
    "txt", "md", "markdown", "log", "json", "csv", "tsv",
    "xml", "yaml", "yml", "ini", "conf", "cfg", "env",
    "sh", "py", "js", "ts", "css", "html", "sql", "pdf", "kt"
)
val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v")
val AUDIO_EXTS = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg")

fun getExtension(fileName: String): String {
    val lower = fileName.lowercase(Locale.ROOT).trim()
    if (lower.endsWith(".tar.gz")) return "tar.gz"
    val dot = lower.lastIndexOf('.')
    return if (dot >= 0) lower.substring(dot + 1) else ""
}

fun todayShort(): String {
    return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
}

fun formatSize(bytes: Long): String {
    val v = bytes.coerceAtLeast(0L).toDouble()
    if (v < 1024) return "${v.toInt()} B"
    val kb = v / 1024.0
    if (kb < 1024) return String.format(Locale.forLanguageTag("vi-VN"), "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.forLanguageTag("vi-VN"), "%.1f MB", mb)
    val gb = mb / 1024.0
    if (gb < 1024) return String.format(Locale.forLanguageTag("vi-VN"), "%.1f GB", gb)
    return String.format(Locale.forLanguageTag("vi-VN"), "%.1f TB", gb / 1024.0)
}

fun parseSizeToBytes(sizeStr: String): Long {
    val cleaned = sizeStr.trim().replace(',', '.')
    val regex = Regex("""^([0-9.]+)\s*(B|KB|MB|GB|TB)$""", RegexOption.IGNORE_CASE)
    val match = regex.find(cleaned) ?: return 1024L
    val num = match.groupValues[1].toDoubleOrNull() ?: 1.0
    val mult = when (match.groupValues[2].uppercase(Locale.ROOT)) {
        "B" -> 1.0
        "KB" -> 1024.0
        "MB" -> 1024.0 * 1024.0
        "GB" -> 1024.0 * 1024.0 * 1024.0
        "TB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
        else -> 1.0
    }
    return (num * mult).toLong()
}
