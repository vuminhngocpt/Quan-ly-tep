package com.vuminhngoc.quanlytep.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vuminhngoc.quanlytep.data.SeedDataRepository
import com.vuminhngoc.quanlytep.model.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class FileManagerUiState(
    val source: StorageSource = StorageSource.DEVICE,
    val folderStack: List<FolderStep> = emptyList(),
    val history: List<HistoryEntry> = listOf(HistoryEntry(StorageSource.DEVICE, emptyList())),
    val historyIndex: Int = 0,
    val filter: FileFilter = FileFilter.ALL,
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.LIST,
    val selectMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val clipboard: ClipboardState? = null,
    val settings: AppSettings = AppSettings(),
    val storageMeta: Map<StorageSource, StorageMetadata> = SeedDataRepository.defaultMetadataMap(),
    val profiles: Map<StorageSource, List<NetworkProfile>> = SeedDataRepository.defaultProfilesMap(),
    val trees: Map<StorageSource, List<FileNode>> = SeedDataRepository.buildAllSeedTrees(),
    val toastMessage: String? = null,
    val longPressTarget: FileNode? = null,
    val showAllSourcesModal: Boolean = false,
    val createModalKind: String? = null,
    val infoModalTarget: FileNode? = null,
    val mediaModalTarget: FileNode? = null,
    val docModalTarget: FileNode? = null,
    val archiveModalTarget: FileNode? = null,
    val showCompressModal: Boolean = false,
    val showStorageManagerModal: Boolean = false,
    val networkModalSource: StorageSource? = null,
    val pendingDeleteIds: List<String> = emptyList(),
    val showSettingsModal: Boolean = false
) {
    val canGoBack: Boolean get() = historyIndex > 0
    val canGoForward: Boolean get() = historyIndex < history.lastIndex

    fun currentMetadata(): StorageMetadata =
        storageMeta[source] ?: SeedDataRepository.defaultMetadataMap().getValue(source)

    fun currentPath(): String {
        val root = currentMetadata().path.trimEnd('/')
        if (folderStack.isEmpty()) return root
        return "$root/${folderStack.joinToString("/") { it.name }}"
    }

    fun currentRawItems(): List<FileNode> {
        if (settings.useRealDeviceStorage && (source == StorageSource.DEVICE || source == StorageSource.SD || source == StorageSource.OTG)) {
            val realList = SeedDataRepository.listRealAndroidDirectory(currentPath())
            if (realList.isNotEmpty() || File(currentPath()).exists()) {
                return realList
            }
        }
        var list = trees[source].orEmpty()
        for (step in folderStack) {
            val folder = list.find { it.isFolder && (it.id == step.id || it.name == step.name) } ?: return emptyList()
            list = folder.children
        }
        return list
    }

    fun visibleSortedItems(): List<FileNode> {
        val raw = currentRawItems()
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        val filtered = raw.filter { item ->
            if (!settings.showHidden && item.name.startsWith(".")) return@filter false
            if (filter != FileFilter.ALL) {
                val expected = when (filter) {
                    FileFilter.FOLDER -> ItemKind.FOLDER
                    FileFilter.IMAGE -> ItemKind.IMAGE
                    FileFilter.VIDEO -> ItemKind.VIDEO
                    FileFilter.AUDIO -> ItemKind.AUDIO
                    FileFilter.DOC -> ItemKind.DOC
                    FileFilter.ARCHIVE -> ItemKind.ARCHIVE
                    FileFilter.ALL -> item.detectKind()
                }
                if (item.detectKind() != expected) return@filter false
            }
            if (q.isNotEmpty()) {
                val hitName = item.name.lowercase(Locale.ROOT).contains(q)
                val hitNotes = item.notes.lowercase(Locale.ROOT).contains(q)
                if (!hitName && !hitNotes) return@filter false
            }
            true
        }
        return when (settings.sortBy) {
            SortOption.TYPE -> filtered.sortedWith(
                compareBy<FileNode> { !it.isFolder }.thenBy { it.name.lowercase(Locale.ROOT) }
            )
            SortOption.DATE -> filtered.sortedByDescending { it.modified }
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }
}

class FileManagerViewModel(private val appContext: Context? = null) {
    var uiState by mutableStateOf(FileManagerUiState())
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        refreshRealStorageMetadata(StorageSource.DEVICE)
        refreshRealStorageMetadata(StorageSource.SD)
    }

    private fun isRealLocalSource(): Boolean =
        uiState.settings.useRealDeviceStorage &&
            uiState.source in setOf(StorageSource.DEVICE, StorageSource.SD, StorageSource.OTG)

    private fun resolveRealStoragePath(source: StorageSource, fallbackPath: String): String {
        val resolvedByRepo = SeedDataRepository.resolveSourcePath(source, appContext)
        if (resolvedByRepo.isNotBlank() && File(resolvedByRepo).exists()) {
            return resolvedByRepo
        }
        if (File(fallbackPath).exists()) return fallbackPath

        val storageRoot = File("/storage")
        val mountedVolumes = storageRoot.listFiles()?.filter { dir ->
            dir.isDirectory &&
                dir.name !in setOf("emulated", "self", "sdcard0") &&
                Regex("""^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$""").matches(dir.name)
        }.orEmpty()

        return when (source) {
            StorageSource.SD -> mountedVolumes.firstOrNull()?.absolutePath ?: fallbackPath
            StorageSource.OTG -> mountedVolumes.getOrNull(1)?.absolutePath ?: fallbackPath
            else -> fallbackPath
        }
    }

    private fun buildPathFromParts(source: StorageSource, stack: List<FolderStep>): String {
        val meta = uiState.storageMeta[source] ?: SeedDataRepository.defaultMetadataMap(appContext).getValue(source)
        val root = resolveRealStoragePath(source, meta.path).trimEnd('/')
        if (stack.isEmpty()) return root
        return "$root/${stack.joinToString("/") { it.name }}"
    }

    fun refreshRealStorageMetadata(targetSource: StorageSource = uiState.source) {
        if (!uiState.settings.useRealDeviceStorage) return
        if (targetSource !in setOf(StorageSource.DEVICE, StorageSource.SD, StorageSource.OTG)) return

        val baseMeta = uiState.storageMeta[targetSource]
            ?: SeedDataRepository.defaultMetadataMap(appContext).getValue(targetSource)
        val resolvedPath = resolveRealStoragePath(targetSource, baseMeta.path)
        val dir = File(resolvedPath)
        if (!dir.exists()) return

        try {
            val stat = StatFs(dir.absolutePath)
            val total = stat.totalBytes
            val available = stat.availableBytes
            if (total > 0L && available in 0L..total) {
                val updated = baseMeta.copy(
                    path = resolvedPath,
                    usedBytes = total - available,
                    totalBytes = total
                )
                uiState = uiState.copy(
                    storageMeta = uiState.storageMeta + (targetSource to updated)
                )
            }
        } catch (_: Exception) {
        }
    }

    fun showToast(msg: String) {
        uiState = uiState.copy(toastMessage = msg)
    }

    fun clearToast() {
        uiState = uiState.copy(toastMessage = null)
    }

    fun setSource(source: StorageSource, recordHistory: Boolean = true) {
        val baseHist = if (recordHistory) {
            val sub = uiState.history.take(uiState.historyIndex + 1)
            val snap = HistoryEntry(source, emptyList())
            if (sub.lastOrNull() == snap) sub else sub + snap
        } else uiState.history

        uiState = uiState.copy(
            source = source,
            folderStack = emptyList(),
            selectedIds = emptySet(),
            selectMode = false,
            history = baseHist,
            historyIndex = baseHist.lastIndex
        )
        refreshRealStorageMetadata(source)
    }

    fun goHistory(delta: Int) {
        val nextIdx = uiState.historyIndex + delta
        if (nextIdx !in uiState.history.indices) return
        val entry = uiState.history[nextIdx]
        uiState = uiState.copy(
            source = entry.source,
            folderStack = entry.folderStack,
            historyIndex = nextIdx,
            selectedIds = emptySet(),
            selectMode = false
        )
        refreshRealStorageMetadata(entry.source)
    }

    fun navigateCrumb(index: Int) {
        val nextStack = if (index < 0) emptyList() else uiState.folderStack.take(index + 1)
        val sub = uiState.history.take(uiState.historyIndex + 1)
        val snap = HistoryEntry(uiState.source, nextStack)
        val nextHist = if (sub.lastOrNull() == snap) sub else sub + snap
        uiState = uiState.copy(
            folderStack = nextStack,
            history = nextHist,
            historyIndex = nextHist.lastIndex,
            selectedIds = emptySet(),
            selectMode = false
        )
    }

    fun setFilter(filter: FileFilter) {
        uiState = uiState.copy(filter = filter)
    }

    fun setSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    fun toggleViewMode() {
        val next = if (uiState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        uiState = uiState.copy(
            viewMode = next,
            toastMessage = if (next == ViewMode.GRID) "Chuyển sang chế độ Lưới." else "Chuyển sang chế độ Danh sách."
        )
    }

    private fun resolveArchiveFile(item: FileNode): File? {
        val candidates = mutableListOf<File>()
        item.realFilePath?.takeIf { it.isNotBlank() }?.let { rawPath ->
            candidates.add(File(rawPath))
            if (rawPath.startsWith("/sdcard/")) {
                candidates.add(File(rawPath.replaceFirst("/sdcard/", "/storage/emulated/0/")))
            }
        }

        val curDir = File(uiState.currentPath())
        candidates.add(File(curDir, item.name))

        uiState.storageMeta[uiState.source]?.path?.takeIf { it.isNotBlank() }?.let { metaRoot ->
            candidates.add(File(metaRoot, item.name))
        }

        candidates.firstOrNull { it.exists() && it.isFile }?.let { return it }

        fun findInDir(dir: File, maxDepth: Int): File? {
            if (!dir.exists() || !dir.isDirectory || maxDepth < 0) return null
            val children = dir.listFiles() ?: return null
            children.firstOrNull { f ->
                f.isFile && f.name.equals(item.name, ignoreCase = true)
            }?.let { return it }
            if (maxDepth == 0) return null
            for (sub in children) {
                if (sub.isDirectory) {
                    findInDir(sub, maxDepth - 1)?.let { return it }
                }
            }
            return null
        }

        findInDir(curDir, maxDepth = 1)?.let { return it }

        val rootPath = resolveRealStoragePath(uiState.source, uiState.currentMetadata().path)
        return findInDir(File(rootPath), maxDepth = 2)
    }

    private fun isTarGzipName(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower.endsWith(".tar.gz") || lower.endsWith(".tgz")
    }

    private fun openTarStream(tarFile: File, isGzip: Boolean): InputStream {
        val fis = BufferedInputStream(FileInputStream(tarFile))
        return if (isGzip) BufferedInputStream(GZIPInputStream(fis)) else fis
    }

    private fun readZipEntries(zipFile: File, maxEntries: Int = 500): List<ArchiveEntryItem> {
        if (!zipFile.exists() || !zipFile.isFile) return emptyList()
        return runCatching {
            val result = mutableListOf<ArchiveEntryItem>()
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null && result.size < maxEntries) {
                    val cleanName = entry.name.replace('\\', '/').trim('/')
                    if (cleanName.isNotEmpty()) {
                        val sizeText = if (entry.isDirectory) {
                            ""
                        } else if (entry.size >= 0L) {
                            formatSize(entry.size)
                        } else {
                            "Tệp ZIP"
                        }
                        result.add(
                            ArchiveEntryItem(
                                name = cleanName,
                                size = sizeText,
                                isFolder = entry.isDirectory
                            )
                        )
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun parseTarOctal(buf: ByteArray, offset: Int, length: Int): Long {
        var value = 0L
        var foundDigit = false
        val end = (offset + length).coerceAtMost(buf.size)
        for (i in offset until end) {
            val b = buf[i].toInt() and 0xFF
            if (b == 0 || b == ' '.code) {
                if (foundDigit) break
                continue
            }
            if (b in '0'.code..'7'.code) {
                foundDigit = true
                value = (value shl 3) + (b - '0'.code)
            }
        }
        return value
    }

    private fun readCString(buf: ByteArray, offset: Int, length: Int): String {
        val end = (offset + length).coerceAtMost(buf.size)
        var len = 0
        while (offset + len < end && buf[offset + len].toInt() != 0) {
            len++
        }
        return String(buf, offset, len, StandardCharsets.UTF_8).trim()
    }

    private fun readFullyBlock(input: InputStream, block: ByteArray): Boolean {
        var total = 0
        while (total < block.size) {
            val read = input.read(block, total, block.size - total)
            if (read <= 0) return false
            total += read
        }
        return true
    }

    private fun skipBytesExact(input: InputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(8192)
        while (remaining > 0L) {
            val toRead = remaining.coerceAtMost(buf.size.toLong()).toInt()
            val n = input.read(buf, 0, toRead)
            if (n <= 0) break
            remaining -= n
        }
    }

    private fun readTarEntries(tarFile: File, isGzip: Boolean = false, maxEntries: Int = 500): List<ArchiveEntryItem> {
        if (!tarFile.exists() || !tarFile.isFile) return emptyList()
        return runCatching {
            val result = mutableListOf<ArchiveEntryItem>()
            val header = ByteArray(512)
            openTarStream(tarFile, isGzip).use { stream ->
                while (result.size < maxEntries) {
                    if (!readFullyBlock(stream, header)) break
                    if (header.all { it.toInt() == 0 }) break

                    val rawName = readCString(header, 0, 100)
                    val prefix = readCString(header, 345, 155)
                    val fullName = (if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName)
                        .replace('\\', '/')
                        .trim('/')
                    val size = parseTarOctal(header, 124, 12)
                    val typeFlag = if (header.size > 156) header[156].toInt() and 0xFF else 0
                    val isDir = typeFlag == '5'.code || rawName.endsWith("/")

                    if (fullName.isNotEmpty()) {
                        result.add(
                            ArchiveEntryItem(
                                name = fullName,
                                size = if (isDir) "" else formatSize(size),
                                isFolder = isDir
                            )
                        )
                    }

                    val dataBlocks = (size + 511L) / 512L
                    skipBytesExact(stream, dataBlocks * 512L)
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun extractTarFile(
        tarFile: File,
        baseDir: File,
        selectedNames: Set<String>,
        extractAll: Boolean = false,
        isGzip: Boolean = false
    ): Int {
        var extractedCount = 0
        val header = ByteArray(512)
        val canonicalBase = baseDir.canonicalPath

        openTarStream(tarFile, isGzip).use { stream ->
            while (true) {
                if (!readFullyBlock(stream, header)) break
                if (header.all { it.toInt() == 0 }) break

                val rawName = readCString(header, 0, 100)
                val prefix = readCString(header, 345, 155)
                val cleanEntryName = (if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName)
                    .replace('\\', '/')
                    .trim('/')
                val size = parseTarOctal(header, 124, 12)
                val typeFlag = if (header.size > 156) header[156].toInt() and 0xFF else 0
                val isDir = typeFlag == '5'.code || rawName.endsWith("/")
                val dataBlocks = (size + 511L) / 512L
                val paddedBytes = dataBlocks * 512L

                val matchesSelection = cleanEntryName.isNotEmpty() && (
                    extractAll ||
                        cleanEntryName in selectedNames ||
                        selectedNames.any { sel -> cleanEntryName.startsWith("$sel/") }
                    )

                if (matchesSelection) {
                    val outFile = File(baseDir, cleanEntryName)
                    val canonicalOut = outFile.canonicalPath
                    if (canonicalOut.startsWith("$canonicalBase${File.separator}") || canonicalOut == canonicalBase) {
                        if (isDir) {
                            outFile.mkdirs()
                            extractedCount++
                            skipBytesExact(stream, paddedBytes)
                        } else {
                            outFile.parentFile?.mkdirs()
                            var left = size
                            val buf = ByteArray(8192)
                            BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                                while (left > 0L) {
                                    val toRead = left.coerceAtMost(buf.size.toLong()).toInt()
                                    val n = stream.read(buf, 0, toRead)
                                    if (n <= 0) break
                                    bos.write(buf, 0, n)
                                    left -= n
                                }
                            }
                            val remainder = paddedBytes - size
                            if (remainder > 0L) skipBytesExact(stream, remainder)
                            extractedCount++
                        }
                    } else {
                        skipBytesExact(stream, paddedBytes)
                    }
                } else {
                    skipBytesExact(stream, paddedBytes)
                }
            }
        }
        return extractedCount
    }

    fun onItemClick(item: FileNode) {
        if (uiState.selectMode || uiState.selectedIds.isNotEmpty()) {
            toggleSelectId(item.id)
            return
        }
        when (item.detectKind()) {
            ItemKind.FOLDER -> {
                val nextStack = uiState.folderStack + FolderStep(item.id, item.name)
                val sub = uiState.history.take(uiState.historyIndex + 1)
                val snap = HistoryEntry(uiState.source, nextStack)
                val nextHist = sub + snap
                uiState = uiState.copy(
                    folderStack = nextStack,
                    history = nextHist,
                    historyIndex = nextHist.lastIndex
                )
            }
            ItemKind.ARCHIVE -> {
                val isZip = item.name.endsWith(".zip", ignoreCase = true)
                val isGzipTar = isTarGzipName(item.name)
                val isTar = isGzipTar || item.name.endsWith(".tar", ignoreCase = true)

                if (isRealLocalSource() && (isZip || isTar)) {
                    val archiveFile = resolveArchiveFile(item)
                    if (archiveFile != null && archiveFile.exists()) {
                        val syncedItem = item.copy(
                            realFilePath = archiveFile.absolutePath,
                            archiveEntries = emptyList()
                        )
                        uiState = uiState.copy(
                            archiveModalTarget = syncedItem,
                            toastMessage = "Đang đọc nội dung tệp nén..."
                        )
                        Thread {
                            val realEntries = if (isZip) {
                                readZipEntries(archiveFile)
                            } else {
                                readTarEntries(archiveFile, isGzip = isGzipTar)
                            }
                            val updatedItem = syncedItem.copy(archiveEntries = realEntries)
                            mainHandler.post {
                                if (uiState.archiveModalTarget?.id == item.id) {
                                    uiState = uiState.copy(
                                        archiveModalTarget = updatedItem,
                                        toastMessage = if (realEntries.isEmpty()) {
                                            "Không đọc được danh sách mục trong tệp nén."
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }.start()
                    } else {
                        uiState = uiState.copy(
                            archiveModalTarget = item.copy(archiveEntries = emptyList()),
                            toastMessage = "Không tìm thấy tệp trên đường dẫn thực tế."
                        )
                    }
                } else if (isRealLocalSource()) {
                    uiState = uiState.copy(archiveModalTarget = item.copy(archiveEntries = emptyList()))
                } else {
                    uiState = uiState.copy(archiveModalTarget = item)
                }
            }
            ItemKind.DOC -> uiState = uiState.copy(docModalTarget = item)
            ItemKind.IMAGE, ItemKind.VIDEO, ItemKind.AUDIO -> uiState = uiState.copy(mediaModalTarget = item)
            ItemKind.FILE -> uiState = uiState.copy(infoModalTarget = item)
        }
    }

    fun onItemLongPress(item: FileNode) {
        val nextSelected = uiState.selectedIds + item.id
        uiState = uiState.copy(
            selectMode = true,
            selectedIds = nextSelected,
            longPressTarget = item
        )
    }

    fun dismissLongPressSheet() {
        uiState = uiState.copy(longPressTarget = null)
    }

    fun toggleSelectId(id: String) {
        val next = if (id in uiState.selectedIds) uiState.selectedIds - id else uiState.selectedIds + id
        uiState = uiState.copy(
            selectedIds = next,
            selectMode = next.isNotEmpty()
        )
    }

    fun setSelectMode(enabled: Boolean) {
        uiState = uiState.copy(
            selectMode = enabled,
            selectedIds = if (enabled) uiState.selectedIds else emptySet()
        )
    }

    fun selectAllVisible() {
        val ids = uiState.visibleSortedItems().map { it.id }.toSet()
        uiState = uiState.copy(
            selectedIds = ids,
            selectMode = ids.isNotEmpty()
        )
    }

    fun deselectAll() {
        uiState = uiState.copy(selectedIds = emptySet(), selectMode = false)
    }

    fun stageClipboard(mode: ClipboardMode) {
        val chosen = uiState.visibleSortedItems().filter { it.id in uiState.selectedIds }
        if (chosen.isEmpty()) {
            showToast("Vui lòng chọn ít nhất một mục.")
            return
        }
        val clip = ClipboardState(
            mode = mode,
            fromSource = uiState.source,
            fromStack = uiState.folderStack,
            sourceItemIds = chosen.map { it.id },
            items = chosen.map { it.deepClone() }
        )
        uiState = uiState.copy(
            clipboard = clip,
            selectMode = false,
            selectedIds = emptySet(),
            longPressTarget = null,
            toastMessage = if (mode == ClipboardMode.MOVE) {
                "Đã chọn di chuyển ${chosen.size} mục. Mở thư mục đích và nhấn Dán."
            } else {
                "Đã sao chép ${chosen.size} mục vào bộ nhớ tạm."
            }
        )
    }

    fun pasteClipboard() {
        val clip = uiState.clipboard ?: run {
            showToast("Bộ nhớ tạm trống. Hãy chọn mục và nhấn Sao chép hoặc Di chuyển.")
            return
        }

        if (isRealLocalSource()) {
            val destDir = File(uiState.currentPath())
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            if (!destDir.exists() || !destDir.canWrite()) {
                showToast("Không có quyền ghi vào thư mục đích (${destDir.absolutePath}).")
                return
            }

            val existingNames = (destDir.list()?.map { it.lowercase(Locale.ROOT) } ?: emptyList())
                .toMutableSet()
            val originBasePath = buildPathFromParts(clip.fromSource, clip.fromStack)
            val isMove = clip.mode == ClipboardMode.MOVE

            uiState = uiState.copy(
                longPressTarget = null,
                toastMessage = if (isMove) {
                    "Đang di chuyển ${clip.items.size} mục..."
                } else {
                    "Đang sao chép ${clip.items.size} mục..."
                }
            )

            Thread {
                var successCount = 0
                var failCount = 0

                fun uniqueRealName(desired: String, isFolder: Boolean): String {
                    if (desired.lowercase(Locale.ROOT) !in existingNames) {
                        existingNames.add(desired.lowercase(Locale.ROOT))
                        return desired
                    }
                    if (isFolder) {
                        var idx = 1
                        while ("$desired ($idx)".lowercase(Locale.ROOT) in existingNames) idx++
                        val out = "$desired ($idx)"
                        existingNames.add(out.lowercase(Locale.ROOT))
                        return out
                    }
                    val match = Regex("""(\.tar\.gz|\.[^.]+)$""", RegexOption.IGNORE_CASE).find(desired)
                    return if (match != null) {
                        val ext = match.value
                        val stem = desired.removeSuffix(ext)
                        var idx = 1
                        while ("$stem ($idx)$ext".lowercase(Locale.ROOT) in existingNames) idx++
                        val out = "$stem ($idx)$ext"
                        existingNames.add(out.lowercase(Locale.ROOT))
                        out
                    } else {
                        var idx = 1
                        while ("$desired ($idx)".lowercase(Locale.ROOT) in existingNames) idx++
                        val out = "$desired ($idx)"
                        existingNames.add(out.lowercase(Locale.ROOT))
                        out
                    }
                }

                fun copyFileOrDir(src: File, dst: File): Boolean {
                    if (!src.exists()) return false
                    if (src.isDirectory) {
                        if (!dst.exists() && !dst.mkdirs()) return false
                        val children = src.listFiles() ?: return true
                        for (child in children) {
                            if (!copyFileOrDir(child, File(dst, child.name))) {
                                return false
                            }
                        }
                        return true
                    } else {
                        dst.parentFile?.mkdirs()
                        BufferedInputStream(FileInputStream(src)).use { input ->
                            BufferedOutputStream(FileOutputStream(dst)).use { output ->
                                input.copyTo(output)
                            }
                        }
                        return dst.exists()
                    }
                }

                for (node in clip.items) {
                    val srcFile = File(node.realFilePath ?: "$originBasePath/${node.name}")
                    if (!srcFile.exists()) {
                        failCount++
                        continue
                    }
                    val finalName = uniqueRealName(node.name, srcFile.isDirectory)
                    val dstFile = File(destDir, finalName)
                    val copied = runCatching { copyFileOrDir(srcFile, dstFile) }.getOrDefault(false)
                    if (copied) {
                        if (isMove) {
                            val deletedSrc = runCatching { srcFile.deleteRecursively() }.getOrDefault(false)
                            if (deletedSrc) {
                                successCount++
                            } else {
                                failCount++
                            }
                        } else {
                            successCount++
                        }
                    } else {
                        runCatching { if (dstFile.exists()) dstFile.deleteRecursively() }
                        failCount++
                    }
                }

                val msg = when {
                    successCount > 0 && failCount == 0 && isMove ->
                        "Đã di chuyển $successCount mục sang \"${destDir.name}\"."
                    successCount > 0 && failCount == 0 ->
                        "Đã sao chép $successCount mục sang \"${destDir.name}\"."
                    successCount > 0 ->
                        "Hoàn tất $successCount mục, lỗi $failCount mục (kiểm tra quyền bộ nhớ)."
                    else ->
                        "Không thể ghi tệp vào thư mục đích. Kiểm tra quyền Quản lý tất cả tệp."
                }

                mainHandler.post {
                    uiState = uiState.copy(
                        clipboard = if (isMove && successCount > 0) null else clip,
                        toastMessage = msg
                    )
                    refreshRealStorageMetadata(uiState.source)
                }
            }.start()
            return
        }

        val targetList = uiState.currentRawItems().toMutableList()
        val inserted = mutableListOf<FileNode>()

        for (rawNode in clip.items) {
            val cloned = rawNode.deepClone()
            val dedupedName = deduplicateName(cloned.name, cloned.isFolder, targetList)
            val finalNode = cloned.copy(name = dedupedName)
            targetList.add(0, finalNode)
            inserted.add(finalNode)
        }

        var nextTrees = replaceItemsAt(uiState.trees, uiState.source, uiState.folderStack, targetList)
        var movedNote = ""
        if (clip.mode == ClipboardMode.MOVE) {
            val originList = getItemsAt(nextTrees, clip.fromSource, clip.fromStack)
                .filterNot { it.id in clip.sourceItemIds }
            nextTrees = replaceItemsAt(nextTrees, clip.fromSource, clip.fromStack, originList)
            movedNote = " (đã di chuyển)"
        }

        val addedBytes = inserted.sumOf { if (it.isFolder) 4096L else parseSizeToBytes(it.size) }
        val meta = uiState.currentMetadata()
        val updatedMeta = meta.copy(
            usedBytes = (meta.usedBytes + addedBytes).coerceAtMost((meta.totalBytes * 0.96).toLong())
        )

        uiState = uiState.copy(
            trees = nextTrees,
            storageMeta = uiState.storageMeta + (uiState.source to updatedMeta),
            clipboard = if (clip.mode == ClipboardMode.MOVE) null else clip,
            longPressTarget = null,
            toastMessage = "Đã dán ${inserted.size} mục$movedNote."
        )
    }

    fun openCreateModal(kind: String) {
        uiState = uiState.copy(createModalKind = kind)
    }

    fun closeCreateModal() {
        uiState = uiState.copy(createModalKind = null)
    }

    fun confirmCreate(rawName: String, textBody: String) {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            showToast("Vui lòng nhập tên hợp lệ.")
            return
        }
        val currentList = uiState.currentRawItems().toMutableList()
        val isTxt = uiState.createModalKind == "txt"
        val finalName = if (isTxt) {
            val withExt = if (trimmed.contains('.')) trimmed else "$trimmed.txt"
            deduplicateName(withExt, false, currentList)
        } else {
            deduplicateName(trimmed, true, currentList)
        }

        val newNode = if (isTxt) {
            FileNode(
                name = finalName,
                isFolder = false,
                size = formatSize(textBody.toByteArray().size.toLong()),
                notes = "Tệp văn bản mới",
                textContent = textBody
            )
        } else {
            FileNode(name = finalName, isFolder = true)
        }
        currentList.add(0, newNode)
        val nextTrees = replaceItemsAt(uiState.trees, uiState.source, uiState.folderStack, currentList)
        uiState = uiState.copy(
            trees = nextTrees,
            createModalKind = null,
            toastMessage = if (isTxt) "Đã tạo tệp \"$finalName\"." else "Đã tạo thư mục \"$finalName\"."
        )
    }

    fun openInfoModal(item: FileNode) {
        uiState = uiState.copy(infoModalTarget = item, longPressTarget = null)
    }

    fun closeInfoModal() {
        uiState = uiState.copy(infoModalTarget = null)
    }

    fun saveItemInfo(targetId: String, newName: String, newNotes: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            showToast("Tên không được để trống.")
            return
        }
        val targetItem = uiState.currentRawItems().find { it.id == targetId }
        if (targetItem != null && isRealLocalSource()) {
            val currentPath = targetItem.realFilePath ?: "${uiState.currentPath()}/${targetItem.name}"
            val oldFile = File(currentPath)
            if (oldFile.exists() && oldFile.name != trimmed) {
                val parentDir = oldFile.parentFile ?: File(uiState.currentPath())
                val newFile = File(parentDir, trimmed)
                if (newFile.exists()) {
                    showToast("Đã tồn tại tệp hoặc thư mục tên \"$trimmed\".")
                    return
                }
                if (!oldFile.renameTo(newFile)) {
                    showToast("Không thể đổi tên tệp trên bộ nhớ thiết bị.")
                    return
                }
            }
        }

        val currentList = uiState.currentRawItems().map { item ->
            if (item.id == targetId) {
                val updatedRealPath = if (isRealLocalSource()) {
                    val baseReal = item.realFilePath ?: "${uiState.currentPath()}/${item.name}"
                    val parent = File(baseReal).parent
                    if (parent != null) "$parent/$trimmed" else item.realFilePath
                } else {
                    item.realFilePath
                }
                item.copy(
                    name = trimmed,
                    notes = newNotes.trim(),
                    modified = todayShort(),
                    realFilePath = updatedRealPath
                )
            } else item
        }
        val nextTrees = replaceItemsAt(uiState.trees, uiState.source, uiState.folderStack, currentList)
        uiState = uiState.copy(
            trees = nextTrees,
            infoModalTarget = null,
            selectedIds = emptySet(),
            selectMode = false,
            toastMessage = "Đã lưu thông tin mục."
        )
    }

    fun closeMediaModal() {
        uiState = uiState.copy(mediaModalTarget = null)
    }

    fun navigateMedia(delta: Int) {
        val current = uiState.mediaModalTarget ?: return
        val mediaItems = uiState.visibleSortedItems().filter {
            it.detectKind() in setOf(ItemKind.IMAGE, ItemKind.VIDEO, ItemKind.AUDIO)
        }
        val idx = mediaItems.indexOfFirst { it.id == current.id }
        val nextIdx = idx + delta
        if (idx >= 0 && nextIdx in mediaItems.indices) {
            uiState = uiState.copy(mediaModalTarget = mediaItems[nextIdx])
        }
    }

    fun openDocModal(item: FileNode) {
        uiState = uiState.copy(docModalTarget = item, infoModalTarget = null)
    }

    fun closeDocModal() {
        uiState = uiState.copy(docModalTarget = null)
    }

    fun saveDocText(itemId: String, newContent: String) {
        val currentList = uiState.currentRawItems().map { item ->
            if (item.id == itemId) {
                item.copy(
                    textContent = newContent,
                    size = formatSize(newContent.toByteArray().size.toLong()),
                    modified = todayShort()
                )
            } else item
        }
        val nextTrees = replaceItemsAt(uiState.trees, uiState.source, uiState.folderStack, currentList)
        val updatedItem = currentList.find { it.id == itemId }
        uiState = uiState.copy(
            trees = nextTrees,
            docModalTarget = updatedItem,
            toastMessage = "Đã lưu \"${updatedItem?.name ?: "tài liệu"}\"."
        )
    }

    fun closeArchiveModal() {
        uiState = uiState.copy(archiveModalTarget = null)
    }

    fun extractArchive(
        archive: FileNode,
        entries: List<ArchiveEntryItem>?,
        intoSubfolder: Boolean,
        extractAll: Boolean = false
    ) {
        val archiveList = archive.archiveEntries.orEmpty()
        val effectiveEntries = if (extractAll && archiveList.isNotEmpty()) {
            archiveList
        } else {
            entries.orEmpty()
        }

        if (!extractAll && effectiveEntries.isEmpty()) {
            showToast("Vui lòng chọn ít nhất một mục để giải nén.")
            return
        }

        if (isRealLocalSource()) {
            val archiveFile = resolveArchiveFile(archive)
            if (archiveFile == null || !archiveFile.exists()) {
                showToast("Không tìm thấy tệp nén trên thiết bị.")
                return
            }
            val isZip = archive.name.endsWith(".zip", ignoreCase = true)
            val isGzipTar = isTarGzipName(archive.name)
            val isTar = isGzipTar || archive.name.endsWith(".tar", ignoreCase = true)
            if (!isZip && !isTar) {
                showToast("Hiện hỗ trợ giải nén thật cho .zip, .tar, .tar.gz và .tgz trên bộ nhớ thiết bị.")
                return
            }

            val parentDir = File(uiState.currentPath())
            val existingNames = (parentDir.list()?.map { it.lowercase(Locale.ROOT) } ?: emptyList()).toSet()
            val baseSubName = archive.name.replace(
                Regex("""\.(tar\.gz|zip|tar|tgz|7z|rar)$""", RegexOption.IGNORE_CASE),
                ""
            ) + "_giai_nen"
            var finalSubName = baseSubName
            var subIdx = 1
            while (finalSubName.lowercase(Locale.ROOT) in existingNames) {
                finalSubName = "$baseSubName ($subIdx)"
                subIdx++
            }

            val baseDir = if (intoSubfolder) {
                File(parentDir, finalSubName).apply { mkdirs() }
            } else {
                parentDir
            }

            if (!baseDir.exists() || !baseDir.canWrite()) {
                showToast("Không có quyền ghi vào thư mục đích (${baseDir.absolutePath}).")
                return
            }

            val selectedNames = if (extractAll) {
                emptySet()
            } else {
                effectiveEntries.map { it.name.replace('\\', '/').trim('/') }.toSet()
            }

            uiState = uiState.copy(
                archiveModalTarget = null,
                toastMessage = "Đang giải nén \"${archive.name}\"..."
            )

            Thread {
                var extractedCount = 0
                var errorReason: String? = null
                val ok = try {
                    if (isZip) {
                        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val cleanEntryName = entry.name.replace('\\', '/').trim('/')
                                val matchesSelection = cleanEntryName.isNotEmpty() && (
                                    extractAll ||
                                        cleanEntryName in selectedNames ||
                                        selectedNames.any { sel -> cleanEntryName.startsWith("$sel/") }
                                    )

                                if (matchesSelection) {
                                    val outFile = File(baseDir, cleanEntryName)
                                    val canonicalBase = baseDir.canonicalPath
                                    val canonicalOut = outFile.canonicalPath
                                    if (canonicalOut.startsWith("$canonicalBase${File.separator}") || canonicalOut == canonicalBase) {
                                        if (entry.isDirectory) {
                                            outFile.mkdirs()
                                            extractedCount++
                                        } else {
                                            outFile.parentFile?.mkdirs()
                                            BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                                                zis.copyTo(bos)
                                            }
                                            extractedCount++
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    } else {
                        extractedCount = extractTarFile(archiveFile, baseDir, selectedNames, extractAll, isGzip = isGzipTar)
                    }
                    extractedCount > 0
                } catch (se: SecurityException) {
                    errorReason = "Bị từ chối quyền ghi bộ nhớ (${se.message ?: "SecurityException"})."
                    false
                } catch (e: Exception) {
                    errorReason = "Lỗi giải nén: ${e.message ?: e.javaClass.simpleName}"
                    false
                }

                val resultMsg = when {
                    ok && intoSubfolder -> "Đã giải nén $extractedCount mục vào thư mục con \"$finalSubName\"."
                    ok -> "Đã giải nén $extractedCount mục vào thư mục hiện tại."
                    errorReason != null -> errorReason!!
                    else -> "Không có mục nào được trích xuất (tệp trống hoặc sai cấu trúc lưu trữ)."
                }

                mainHandler.post {
                    uiState = uiState.copy(toastMessage = resultMsg)
                }
            }.start()
            return
        }

        val currentList = uiState.currentRawItems().toMutableList()
        val extractedNodes = effectiveEntries.map { en ->
            FileNode(
                name = en.name.substringAfterLast('/').ifBlank { "tep_giai_nen.txt" },
                isFolder = en.isFolder,
                size = en.size.ifBlank { "1,2 KB" },
                notes = "Giải nén từ ${archive.name}",
                textContent = "Nội dung trích xuất từ ${archive.name}: ${en.name}"
            )
        }

        if (intoSubfolder) {
            val baseName = archive.name.replace(
                Regex("""\.(tar\.gz|zip|tar|tgz|7z|rar)$""", RegexOption.IGNORE_CASE),
                ""
            ) + "_giai_nen"
            val folderName = deduplicateName(baseName, true, currentList)
            currentList.add(0, FileNode(name = folderName, isFolder = true, children = extractedNodes))
        } else {
            extractedNodes.asReversed().forEach { node ->
                val name = deduplicateName(node.name, node.isFolder, currentList)
                currentList.add(0, node.copy(name = name))
            }
        }
        val nextTrees = replaceItemsAt(uiState.trees, uiState.source, uiState.folderStack, currentList)
        uiState = uiState.copy(
            trees = nextTrees,
            archiveModalTarget = null,
            toastMessage = if (intoSubfolder) {
                "Đã giải nén ${extractedNodes.size} mục vào thư mục con."
            } else {
                "Đã giải nén ${extractedNodes.size} mục vào thư mục hiện tại."
            }
        )
    }

    fun openCompressModal() {
        uiState = uiState.copy(showCompressModal = true, longPressTarget = null)
    }

    fun closeCompressModal() {
        uiState = uiState.copy(showCompressModal = false)
    }

    fun confirmCompress(baseNameInput: String, ext: String) {
        val cur = uiState.currentRawItems().toMutableList()
        val chosen = if (uiState.selectedIds.isNotEmpty()) {
            cur.filter { it.id in uiState.selectedIds }
        } else {
            cur.take(2)
        }
        if (chosen.isEmpty()) {
            showToast("Không có mục nào để nén.")
            return
        }
        val cleanBase = baseNameInput.trim()
            .replace(Regex("""\.(tar\.gz|zip|tar|tgz|7z|rar)$""", RegexOption.IGNORE_CASE), "")
            .ifBlank { "luu_tru_${uiState.source.id}" }

        if (isRealLocalSource()) {
            val targetDir = File(uiState.currentPath())
            if (!targetDir.exists() || !targetDir.canWrite()) {
                showToast("Không thể ghi tệp nén vào thư mục hiện tại.")
                return
            }
            if (!ext.equals("zip", ignoreCase = true)) {
                showToast("Định dạng .$ext chưa hỗ trợ ghi trực tiếp trên bộ nhớ thật, hãy chọn .zip.")
                return
            }
            val existingNames = (targetDir.list()?.map { it.lowercase(Locale.ROOT) } ?: emptyList()).toSet()
            var finalFileName = "$cleanBase.$ext"
            var idx = 1
            while (finalFileName.lowercase(Locale.ROOT) in existingNames) {
                finalFileName = "$cleanBase ($idx).$ext"
                idx++
            }
            val outFile = File(targetDir, finalFileName)
            val basePath = uiState.currentPath()

            uiState = uiState.copy(
                showCompressModal = false,
                selectedIds = emptySet(),
                selectMode = false,
                toastMessage = "Đang nén thành \"$finalFileName\"..."
            )

            Thread {
                val created = runCatching {
                    ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
                        fun addToZip(src: File, entryPrefix: String) {
                            if (!src.exists()) return
                            if (src.isDirectory) {
                                val dirEntryName = if (entryPrefix.endsWith("/")) entryPrefix else "$entryPrefix/"
                                zos.putNextEntry(ZipEntry(dirEntryName))
                                zos.closeEntry()
                                src.listFiles()?.forEach { child ->
                                    addToZip(child, "$dirEntryName${child.name}")
                                }
                            } else {
                                zos.putNextEntry(ZipEntry(entryPrefix))
                                src.inputStream().use { input -> input.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                        for (node in chosen) {
                            val srcFile = File(node.realFilePath ?: "$basePath/${node.name}")
                            if (srcFile.exists()) {
                                addToZip(srcFile, srcFile.name)
                            }
                        }
                    }
                    outFile.exists()
                }.getOrDefault(false)

                val msg = if (created) {
                    "Đã nén thành \"$finalFileName\"."
                } else {
                    "Nén thất bại. Kiểm tra quyền bộ nhớ hoặc dung lượng trống."
                }
                mainHandler.post {
                    uiState = uiState.copy(toastMessage = msg)
                }
            }.start()
            return
        }

        val fileName = deduplicateName("$cleanBase.$ext", false, cur)
        val archiveNode = FileNode(
            name = fileName,
            isFolder = false,
            size = "4,4 MB",
            notes = "Nén ${chosen.size} mục (.$ext)",
            archiveEntries = chosen.map { ArchiveEntryItem(it.name, it.size.ifBlank { "1,0 MB" }, it.isFolder) }
        )
        cur.add(0, archiveNode)
        val nextTrees = replaceItemsAt(uiState.trees, uiState.source, uiState.folderStack, cur)
        uiState = uiState.copy(
            trees = nextTrees,
            showCompressModal = false,
            selectedIds = emptySet(),
            selectMode = false,
            toastMessage = "Đã nén thành \"$fileName\"."
        )
    }

    fun requestDeleteSelected() {
        val ids = uiState.selectedIds.toList()
        if (ids.isEmpty()) {
            showToast("Vui lòng chọn mục cần xóa.")
            return
        }
        uiState = uiState.copy(longPressTarget = null)
        if (uiState.settings.confirmDelete) {
            uiState = uiState.copy(pendingDeleteIds = ids)
        } else {
            deleteItems(ids)
        }
    }

    fun cancelDelete() {
        uiState = uiState.copy(pendingDeleteIds = emptyList())
    }

    fun confirmDelete() {
        deleteItems(uiState.pendingDeleteIds)
    }

    private fun deleteItems(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        var failedCount = 0

        if (isRealLocalSource()) {
            val targets = uiState.currentRawItems().filter { it.id in idSet }
            for (item in targets) {
                val path = item.realFilePath ?: "${uiState.currentPath()}/${item.name}"
                val file = File(path)
                if (file.exists()) {
                    val deleted = file.deleteRecursively()
                    if (!deleted) failedCount++
                }
            }
        }

        val kept = uiState.currentRawItems().filterNot { it.id in idSet }
        val nextTrees = replaceItemsAt(uiState.trees, uiState.source, uiState.folderStack, kept)
        val msg = when {
            failedCount == 0 -> "Đã xóa ${ids.size} mục."
            failedCount < ids.size -> "Đã xóa một phần (${ids.size - failedCount}/${ids.size} mục); kiểm tra quyền bộ nhớ."
            else -> "Không thể xóa tệp trên thiết bị. Hãy cấp quyền Quản lý tất cả tệp."
        }

        uiState = uiState.copy(
            trees = nextTrees,
            pendingDeleteIds = emptyList(),
            selectedIds = emptySet(),
            selectMode = false,
            toastMessage = msg
        )
    }

    fun setShowAllSources(show: Boolean) {
        uiState = uiState.copy(showAllSourcesModal = show)
    }

    fun setShowStorageManager(show: Boolean) {
        uiState = uiState.copy(showStorageManagerModal = show)
    }

    fun openNetworkModal(source: StorageSource) {
        uiState = uiState.copy(networkModalSource = source, showStorageManagerModal = false)
    }

    fun closeNetworkModal() {
        uiState = uiState.copy(networkModalSource = null)
    }

    fun saveNetworkConfig(
        source: StorageSource,
        host: String,
        path: String,
        port: String,
        mode: String,
        user: String,
        encoding: String
    ) {
        val base = uiState.storageMeta[source] ?: SeedDataRepository.defaultMetadataMap(appContext).getValue(source)
        val finalHost = host.trim().ifBlank { source.defaultHost }
        val finalPath = path.trim().ifBlank { source.defaultPath }
        val finalPort = port.trim().ifBlank { source.defaultPort }
        val updatedMeta = base.copy(
            host = finalHost,
            path = finalPath,
            port = finalPort,
            mode = mode,
            user = user.trim().ifBlank { "admin" },
            encoding = encoding,
            shortTag = "${source.label} • $finalHost${if (finalPort.isNotEmpty()) ":$finalPort" else ""}"
        )
        val newProfile = NetworkProfile(
            name = "${source.label}_$finalHost",
            host = finalHost,
            path = finalPath,
            port = finalPort,
            mode = mode,
            user = updatedMeta.user,
            encoding = encoding
        )
        uiState = uiState.copy(
            storageMeta = uiState.storageMeta + (source to updatedMeta),
            profiles = uiState.profiles + (source to listOf(newProfile)),
            networkModalSource = null
        )
        setSource(source, recordHistory = true)
        showToast("Đã kết nối ${source.label} ($finalHost${if (finalPort.isNotEmpty()) ":$finalPort" else ""}).")
    }

    fun setShowSettings(show: Boolean) {
        uiState = uiState.copy(showSettingsModal = show)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        uiState = uiState.copy(settings = transform(uiState.settings))
        if (uiState.settings.useRealDeviceStorage) {
            refreshRealStorageMetadata(uiState.source)
        }
    }

    fun restoreDemoData() {
        val keptTheme = uiState.settings.themeMode
        val keptSource = uiState.source
        uiState = FileManagerUiState(
            source = keptSource,
            settings = AppSettings(themeMode = keptTheme),
            toastMessage = "Đã khôi phục dữ liệu mẫu cho cả 6 nguồn lưu trữ."
        )
    }

    private fun getItemsAt(
        trees: Map<StorageSource, List<FileNode>>,
        source: StorageSource,
        stack: List<FolderStep>
    ): List<FileNode> {
        var list = trees[source].orEmpty()
        for (step in stack) {
            val match = list.find { it.isFolder && (it.id == step.id || it.name == step.name) } ?: return emptyList()
            list = match.children
        }
        return list
    }

    private fun replaceItemsAt(
        trees: Map<StorageSource, List<FileNode>>,
        source: StorageSource,
        stack: List<FolderStep>,
        newItems: List<FileNode>
    ): Map<StorageSource, List<FileNode>> {
        val rootList = trees[source].orEmpty()
        val updatedRoot = updateChildrenRecursive(rootList, stack, 0, newItems)
        return trees + (source to updatedRoot)
    }

    private fun updateChildrenRecursive(
        currentLevel: List<FileNode>,
        stack: List<FolderStep>,
        depth: Int,
        newItems: List<FileNode>
    ): List<FileNode> {
        if (depth >= stack.size) return newItems
        val targetStep = stack[depth]
        return currentLevel.map { node ->
            if (node.isFolder && (node.id == targetStep.id || node.name == targetStep.name)) {
                node.copy(children = updateChildrenRecursive(node.children, stack, depth + 1, newItems))
            } else {
                node
            }
        }
    }

    private fun deduplicateName(desired: String, isFolder: Boolean, existing: List<FileNode>): String {
        val used = existing.map { it.name.lowercase(Locale.ROOT) }.toSet()
        if (desired.lowercase(Locale.ROOT) !in used) return desired
        if (isFolder) {
            var idx = 1
            while ("$desired ($idx)".lowercase(Locale.ROOT) in used) idx++
            return "$desired ($idx)"
        }
        val match = Regex("""(\.tar\.gz|\.[^.]+)$""", RegexOption.IGNORE_CASE).find(desired)
        if (match != null) {
            val ext = match.value
            val stem = desired.removeSuffix(ext)
            var idx = 1
            while ("$stem ($idx)$ext".lowercase(Locale.ROOT) in used) idx++
            return "$stem ($idx)$ext"
        }
        var idx = 1
        while ("$desired ($idx)".lowercase(Locale.ROOT) in used) idx++
        return "$desired ($idx)"
    }
}
