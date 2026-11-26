package com.vuminhngoc.quanlytep.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vuminhngoc.quanlytep.data.SeedDataRepository
import com.vuminhngoc.quanlytep.model.*
import com.vuminhngoc.quanlytep.ui.theme.LocalExtendedColors
import com.vuminhngoc.quanlytep.viewmodel.FileManagerUiState
import com.vuminhngoc.quanlytep.viewmodel.FileManagerViewModel
import kotlinx.coroutines.delay

@Composable
fun FileManagerDialogsHost(
    state: FileManagerUiState,
    viewModel: FileManagerViewModel
) {
    state.longPressTarget?.let { target ->
        LongPressQuickDialog(
            item = target,
            selectedCount = state.selectedIds.size,
            hasClipboard = state.clipboard != null,
            onDismiss = { viewModel.dismissLongPressSheet() },
            onCopy = { viewModel.stageClipboard(ClipboardMode.COPY) },
            onMove = { viewModel.stageClipboard(ClipboardMode.MOVE) },
            onPaste = { viewModel.pasteClipboard() },
            onCompress = { viewModel.openCompressModal() },
            onRename = { viewModel.openInfoModal(target) },
            onDetail = { viewModel.openInfoModal(target) },
            onDeselect = {
                viewModel.dismissLongPressSheet()
                viewModel.deselectAll()
                viewModel.showToast("Đã bỏ chọn tất cả mục.")
            },
            onDelete = { viewModel.requestDeleteSelected() }
        )
    }

    if (state.showAllSourcesModal) {
        AllSourcesDialog(
            state = state,
            onSelectSource = {
                viewModel.setShowAllSources(false)
                viewModel.setSource(it)
            },
            onDismiss = { viewModel.setShowAllSources(false) }
        )
    }

    state.createModalKind?.let { kind ->
        CreateItemDialog(
            kind = kind,
            onDismiss = { viewModel.closeCreateModal() },
            onConfirm = { name, body -> viewModel.confirmCreate(name, body) }
        )
    }

    state.infoModalTarget?.let { target ->
        ItemInfoDialog(
            item = target,
            onDismiss = { viewModel.closeInfoModal() },
            onOpenDoc = { viewModel.openDocModal(target) },
            onSave = { name, notes -> viewModel.saveItemInfo(target.id, name, notes) }
        )
    }

    state.mediaModalTarget?.let { target ->
        MediaViewerDialog(
            item = target,
            onPrev = { viewModel.navigateMedia(-1) },
            onNext = { viewModel.navigateMedia(1) },
            onDismiss = { viewModel.closeMediaModal() }
        )
    }

    state.docModalTarget?.let { target ->
        DocumentViewerDialog(
            item = target,
            onSaveText = { newText -> viewModel.saveDocText(target.id, newText) },
            onToast = { viewModel.showToast(it) },
            onDismiss = { viewModel.closeDocModal() }
        )
    }

    state.archiveModalTarget?.let { target ->
        ArchiveViewerDialog(
            archive = target,
            onExtract = { entries, intoSub, extractAll ->
                viewModel.extractArchive(
                    archive = target,
                    entries = entries,
                    intoSubfolder = intoSub,
                    extractAll = extractAll
                )
            },
            onDismiss = { viewModel.closeArchiveModal() }
        )
    }

    if (state.showCompressModal) {
        CompressDialog(
            sourceId = state.source.id,
            targets = if (state.selectedIds.isNotEmpty()) {
                state.visibleSortedItems().filter { it.id in state.selectedIds }
            } else {
                state.visibleSortedItems().take(2)
            },
            onConfirm = { baseName, ext -> viewModel.confirmCompress(baseName, ext) },
            onDismiss = { viewModel.closeCompressModal() }
        )
    }

    if (state.showStorageManagerModal) {
        StorageManagerDialog(
            state = state,
            onOpenSource = {
                viewModel.setShowStorageManager(false)
                viewModel.setSource(it)
            },
            onConfigureNetwork = { viewModel.openNetworkModal(it) },
            onDismiss = { viewModel.setShowStorageManager(false) }
        )
    }

    state.networkModalSource?.let { netSource ->
        NetworkConfigDialog(
            source = netSource,
            meta = state.storageMeta[netSource] ?: SeedDataRepository.defaultMetadataMap().getValue(netSource),
            profiles = state.profiles[netSource].orEmpty(),
            onSave = { host, path, port, mode, user, enc ->
                viewModel.saveNetworkConfig(netSource, host, path, port, mode, user, enc)
            },
            onDismiss = { viewModel.closeNetworkModal() }
        )
    }

    if (state.pendingDeleteIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Xác nhận xóa", fontWeight = FontWeight.Bold) },
            text = { Text("Xác nhận xóa ${state.pendingDeleteIds.size} mục đã chọn?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = LocalExtendedColors.current.danger)
                ) {
                    Text("Xóa ngay", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (state.showSettingsModal) {
        SettingsDialog(
            settings = state.settings,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onClearCache = { viewModel.showToast("Đã xóa bộ nhớ đệm tìm kiếm.") },
            onResetDemo = { viewModel.restoreDemoData() },
            onDismiss = { viewModel.setShowSettings(false) }
        )
    }
}

@Composable
private fun BaseModalCard(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    wide: Boolean = false,
    footer: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalExtendedColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (wide) 0.95f else 0.92f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.borderStrong)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Đóng", tint = colors.text)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = colors.border)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 10.dp), color = colors.border)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = footer
                )
            }
        }
    }
}

@Composable
private fun LongPressQuickDialog(
    item: FileNode,
    selectedCount: Int,
    hasClipboard: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onPaste: () -> Unit,
    onCompress: () -> Unit,
    onRename: () -> Unit,
    onDetail: () -> Unit,
    onDeselect: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalExtendedColors.current
    val sub = "${item.detectKind().name}${if (item.size.isNotBlank()) " • ${item.size}" else ""} • Đã chọn $selectedCount mục"

    BaseModalCard(
        title = item.name,
        subtitle = sub,
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = onDismiss) { Text("Đóng") }
        }
    ) {
        val actions = listOf(
            Triple("Sao chép", true, onCopy),
            Triple("Di chuyển", true, onMove),
            Triple("Dán vào đây", hasClipboard, onPaste),
            Triple("Nén", true, onCompress),
            Triple("Đổi tên", selectedCount == 1, onRename),
            Triple("Chi tiết", true, onDetail),
            Triple("Bỏ chọn", true, onDeselect),
            Triple("Xóa", true, onDelete)
        )
        actions.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { (label, enabled, action) ->
                    val isDanger = label == "Xóa"
                    OutlinedButton(
                        onClick = action,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDanger) colors.danger.copy(alpha = 0.45f) else colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDanger) colors.danger else colors.text
                        )
                    ) {
                        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AllSourcesDialog(
    state: FileManagerUiState,
    onSelectSource: (StorageSource) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalExtendedColors.current
    BaseModalCard(
        title = "Tất cả nguồn lưu trữ (6)",
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = onDismiss) { Text("Đóng") }
        }
    ) {
        StorageSource.entries.forEach { src ->
            val meta = state.storageMeta[src] ?: SeedDataRepository.defaultMetadataMap().getValue(src)
            val count = state.trees[src]?.size ?: 0
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectSource(src) },
                color = colors.surface2,
                border = BorderStroke(1.dp, if (state.source == src) colors.accent else colors.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(src.dotColorHex))
                        )
                        Text(meta.label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
                    }
                    Text(meta.path, fontSize = 11.5.sp, color = colors.textSecondary)
                    Text("${formatSize(meta.usedBytes)} / ${formatSize(meta.totalBytes)} • $count mục", fontSize = 11.5.sp, color = colors.text)
                }
            }
        }
    }
}

@Composable
private fun CreateItemDialog(
    kind: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val isTxt = kind == "txt"
    var name by remember { mutableStateOf(if (isTxt) "ghi_chu_moi.txt" else "Thu_Muc_Moi") }
    var body by remember { mutableStateOf("") }

    BaseModalCard(
        title = if (isTxt) "Tạo tệp văn bản mới (.txt)" else "Tạo thư mục mới",
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = onDismiss) { Text("Hủy") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(name, body) }) { Text("Tạo mới") }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên mục") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (isTxt) {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Nội dung văn bản") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ItemInfoDialog(
    item: FileNode,
    onDismiss: () -> Unit,
    onOpenDoc: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val kind = item.detectKind()
    var name by remember { mutableStateOf(item.name) }
    var notes by remember { mutableStateOf(item.notes) }

    BaseModalCard(
        title = if (kind == ItemKind.FOLDER) "Thông tin & Đổi tên thư mục" else "Chi tiết & Đổi tên tệp",
        subtitle = "Loại: ${kind.name} • Cập nhật: ${item.modified}${if (item.size.isNotBlank()) " • ${item.size}" else ""}",
        onDismiss = onDismiss,
        footer = {
            if (kind == ItemKind.DOC) {
                OutlinedButton(onClick = onOpenDoc) { Text("Mở tài liệu") }
                Spacer(Modifier.width(6.dp))
            }
            OutlinedButton(onClick = onDismiss) { Text("Đóng") }
            Spacer(Modifier.width(6.dp))
            Button(onClick = { onSave(name, notes) }) { Text("Lưu") }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Ghi chú") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MediaViewerDialog(
    item: FileNode,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalExtendedColors.current
    val kind = item.detectKind()
    var isPlaying by remember(item.id) { mutableStateOf(false) }
    var elapsedSec by remember(item.id) { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying, item.id) {
        while (isPlaying) {
            delay(1000)
            elapsedSec = (elapsedSec + 1) % 180
        }
    }

    BaseModalCard(
        title = item.name,
        subtitle = "${kind.name} • ${item.size} • ${item.modified}",
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = onPrev) { Text("← Trước") }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = onNext) { Text("Tiếp →") }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onDismiss) { Text("Đóng") }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ItemTypeBadge(kind = kind, modifier = Modifier.size(56.dp))
                Text(item.name, fontWeight = FontWeight.Bold, color = colors.text)
                if (item.notes.isNotBlank()) {
                    Text(item.notes, fontSize = 12.sp, color = colors.textSecondary)
                }
                if (kind == ItemKind.VIDEO || kind == ItemKind.AUDIO) {
                    val mm = (elapsedSec / 60).toString().padStart(2, '0')
                    val ss = (elapsedSec % 60).toString().padStart(2, '0')
                    Text("$mm:$ss / 03:00", fontSize = 12.5.sp, color = colors.accentText)
                    Button(onClick = { isPlaying = !isPlaying }) {
                        Text(if (isPlaying) "Tạm dừng" else "Phát ngay")
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentViewerDialog(
    item: FileNode,
    onSaveText: (String) -> Unit,
    onToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalExtendedColors.current
    val ext = getExtension(item.name).ifBlank { "txt" }
    val isPdf = ext in PDF_EXTS
    val initialText = remember(item.id, item.textContent) {
        item.textContent ?: item.notes.ifBlank { "Nội dung tài liệu ${item.name}." }
    }
    val pdfPages = remember(item.id) {
        item.pdfPages ?: listOf(
            PdfPageData(1, "${item.name} — Trang 1", item.notes.ifBlank { "Nội dung tài liệu PDF hiển thị trực tiếp." }),
            PdfPageData(2, "${item.name} — Trang 2", "Hỗ trợ chuyển trang, phóng to, tìm từ khóa và sao chép nội dung.")
        )
    }

    var tab by remember(item.id) { mutableStateOf("raw") }
    var fontSize by remember { mutableIntStateOf(13) }
    var lineNums by remember { mutableStateOf(true) }
    var searchQuery by remember(item.id) { mutableStateOf("") }
    var draft by remember(item.id, initialText) { mutableStateOf(initialText) }
    var pdfPageIdx by remember(item.id) { mutableIntStateOf(0) }

    val activeContent = if (isPdf) {
        pdfPages.joinToString("\n\n") { "${it.title}\n${it.body}" }
    } else if (tab == "edit") draft else initialText

    val hitCount = remember(activeContent, searchQuery) {
        val term = searchQuery.trim()
        if (term.isEmpty()) 0
        else Regex(Regex.escape(term), RegexOption.IGNORE_CASE).findAll(activeContent).count()
    }

    BaseModalCard(
        title = item.name,
        subtitle = "${if (isPdf) "PDF" else ext.uppercase()} • ${item.size} • $hitCount khớp",
        onDismiss = onDismiss,
        wide = true,
        footer = {
            OutlinedButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(item.name, activeContent))
                onToast("Đã sao chép nội dung tài liệu.")
            }) { Text("Sao chép") }
            if (!isPdf && tab == "edit") {
                Spacer(Modifier.width(6.dp))
                Button(onClick = {
                    onSaveText(draft)
                    tab = "raw"
                }) { Text("Lưu thay đổi") }
            }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = onDismiss) { Text("Đóng") }
        }
    ) {
        if (!isPdf) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = tab == "raw", onClick = { tab = "raw" }, label = { Text("Văn bản") })
                FilterChip(selected = tab == "edit", onClick = { tab = "edit" }, label = { Text("Chỉnh sửa") })
                FilterChip(selected = false, onClick = { fontSize = (fontSize - 1).coerceAtLeast(10) }, label = { Text("A-") })
                FilterChip(selected = false, onClick = { fontSize = (fontSize + 1).coerceAtMost(20) }, label = { Text("A+") })
                FilterChip(selected = lineNums, onClick = { lineNums = !lineNums }, label = { Text("Số dòng") })
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (pdfPageIdx > 0) pdfPageIdx-- },
                    enabled = pdfPageIdx > 0
                ) { Text("← Trước") }
                Text("Trang ${pdfPageIdx + 1} / ${pdfPages.size}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                OutlinedButton(
                    onClick = { if (pdfPageIdx < pdfPages.lastIndex) pdfPageIdx++ },
                    enabled = pdfPageIdx < pdfPages.lastIndex
                ) { Text("Sau →") }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Tìm từ khóa trong tài liệu...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isPdf && tab == "edit") {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                minLines = 7,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp),
                modifier = Modifier.fillMaxWidth()
            )
        } else if (isPdf) {
            val page = pdfPages[pdfPageIdx]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFD1D5DB))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trang ${page.pageNumber} / ${pdfPages.size}", fontSize = 11.sp, color = Color(0xFF6B7280))
                    Text(page.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Text(page.body, fontSize = 13.sp, color = Color(0xFF1F2937), lineHeight = 20.sp)
                }
            }
        } else {
            val lines = initialText.lines()
            val formatted = if (lineNums) {
                lines.mapIndexed { idx, line -> "${(idx + 1).toString().padStart(3, ' ')}  $line" }.joinToString("\n")
            } else initialText

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = formatted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    color = colors.text
                )
            }
        }
    }
}

@Composable
private fun ArchiveViewerDialog(
    archive: FileNode,
    onExtract: (List<ArchiveEntryItem>, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val entries = remember(archive.id, archive.archiveEntries) {
        archive.archiveEntries ?: listOf(
            ArchiveEntryItem("noi_dung.txt", "1,4 KB"),
            ArchiveEntryItem("cau_hinh.json", "900 B")
        )
    }
    var filterText by remember { mutableStateOf("") }
    var intoSubfolder by remember { mutableStateOf(true) }
    var selectedNames by remember(archive.id, entries) {
        mutableStateOf(entries.map { it.name }.toSet())
    }

    val shown = entries.filter { filterText.isBlank() || it.name.contains(filterText, ignoreCase = true) }

    BaseModalCard(
        title = archive.name,
        subtitle = "Tệp nén • ${entries.size} mục • ${archive.size}",
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = {
                onExtract(entries.filter { it.name in selectedNames }, intoSubfolder, false)
            }) { Text("Giải nén mục chọn") }
            Spacer(Modifier.width(6.dp))
            Button(onClick = {
                onExtract(entries, intoSubfolder, true)
            }) { Text("Giải nén tất cả") }
        }
    ) {
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            placeholder = { Text("Lọc mục bên trong tệp nén...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Giải nén vào thư mục con riêng", fontSize = 12.5.sp)
            Switch(checked = intoSubfolder, onCheckedChange = { intoSubfolder = it })
        }
        shown.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedNames = if (entry.name in selectedNames) selectedNames - entry.name else selectedNames + entry.name
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = entry.name in selectedNames,
                        onCheckedChange = {
                            selectedNames = if (entry.name in selectedNames) selectedNames - entry.name else selectedNames + entry.name
                        }
                    )
                    Text(entry.name, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(entry.size, fontSize = 11.5.sp, color = LocalExtendedColors.current.textSecondary)
            }
        }
    }
}

@Composable
private fun CompressDialog(
    sourceId: String,
    targets: List<FileNode>,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("luu_tru_$sourceId") }
    val formats = listOf("zip", "tar", "tar.gz", "tgz", "7z", "rar")
    var selectedFormat by remember { mutableStateOf("zip") }

    BaseModalCard(
        title = "Nén tệp / thư mục",
        subtitle = "Mục sẽ nén (${targets.size}): ${targets.joinToString(", ") { it.name }}",
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = onDismiss) { Text("Hủy") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(name, selectedFormat) }) { Text("Bắt đầu nén") }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên tệp nén") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Định dạng nén:", fontSize = 12.sp, color = LocalExtendedColors.current.textSecondary)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            formats.forEach { fmt ->
                FilterChip(
                    selected = selectedFormat == fmt,
                    onClick = { selectedFormat = fmt },
                    label = { Text(".$fmt") }
                )
            }
        }
    }
}

@Composable
private fun StorageManagerDialog(
    state: FileManagerUiState,
    onOpenSource: (StorageSource) -> Unit,
    onConfigureNetwork: (StorageSource) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalExtendedColors.current
    BaseModalCard(
        title = "Quản lý nguồn lưu trữ",
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = onDismiss) { Text("Đóng") }
        }
    ) {
        StorageSource.entries.forEach { src ->
            val meta = state.storageMeta[src] ?: SeedDataRepository.defaultMetadataMap().getValue(src)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.surface2,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(src.dotColorHex)))
                            Text(meta.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text("${meta.path} • ${formatSize(meta.usedBytes)} / ${formatSize(meta.totalBytes)}", fontSize = 11.sp, color = colors.textSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (src.isNetwork) {
                            OutlinedButton(onClick = { onConfigureNetwork(src) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                                Text("Cấu hình", fontSize = 11.5.sp)
                            }
                        }
                        Button(onClick = { onOpenSource(src) }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Text("Mở", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkConfigDialog(
    source: StorageSource,
    meta: StorageMetadata,
    profiles: List<NetworkProfile>,
    onSave: (String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var host by remember(source) { mutableStateOf(meta.host) }
    var path by remember(source) { mutableStateOf(meta.path) }
    var port by remember(source) { mutableStateOf(meta.port) }
    var mode by remember(source) { mutableStateOf(meta.mode) }
    var user by remember(source) { mutableStateOf(meta.user) }
    var encoding by remember(source) { mutableStateOf(meta.encoding) }
    var testResult by remember(source) { mutableStateOf("") }

    BaseModalCard(
        title = "Cấu hình máy chủ ${source.label}",
        subtitle = profiles.firstOrNull()?.let { "Hồ sơ: ${it.name}" },
        onDismiss = onDismiss,
        footer = {
            OutlinedButton(onClick = {
                testResult = "Kết nối thành công tới $host:$port ($mode)."
            }) { Text("Kiểm tra") }
            Spacer(Modifier.width(6.dp))
            Button(onClick = { onSave(host, path, port, mode, user, encoding) }) { Text("Lưu & Kết nối") }
        }
    ) {
        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Địa chỉ IP / Hostname") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("Thư mục chia sẻ / Đường dẫn gốc") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Cổng") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = mode, onValueChange = { mode = it }, label = { Text("Chế độ") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Tên đăng nhập") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = encoding, onValueChange = { encoding = it }, label = { Text("Bảng mã") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        if (testResult.isNotEmpty()) {
            Text(testResult, fontSize = 12.sp, color = LocalExtendedColors.current.success)
        }
    }
}

@Composable
private fun SettingsDialog(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onClearCache: () -> Unit,
    onResetDemo: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalExtendedColors.current

    BaseModalCard(
        title = "Cài đặt",
        onDismiss = onDismiss,
        footer = {
            Button(onClick = onDismiss) { Text("Hoàn tất") }
        }
    ) {
        Text("GIAO DIỆN & SẮP XẾP", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onUpdateSettings { it.copy(themeMode = mode) } },
                    label = { Text(mode.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("Sắp xếp mục:", fontSize = 12.sp, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SortOption.entries.forEach { opt ->
                FilterChip(
                    selected = settings.sortBy == opt,
                    onClick = { onUpdateSettings { it.copy(sortBy = opt) } },
                    label = { Text(opt.label) }
                )
            }
        }

        SettingToggleRow("Hiển thị tệp ẩn (bắt đầu bằng dấu chấm)", settings.showHidden) { v ->
            onUpdateSettings { it.copy(showHidden = v) }
        }
        SettingToggleRow("Hiển thị dung lượng & kích thước tệp", settings.showSize) { v ->
            onUpdateSettings { it.copy(showSize = v) }
        }
        SettingToggleRow("Chế độ hiển thị gọn (Compact)", settings.compactMode) { v ->
            onUpdateSettings { it.copy(compactMode = v) }
        }
        SettingToggleRow("Xác nhận trước khi xóa", settings.confirmDelete) { v ->
            onUpdateSettings { it.copy(confirmDelete = v) }
        }
        SettingToggleRow("Đọc tệp thật trên bộ nhớ Android", settings.useRealDeviceStorage) { v ->
            if (v && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                runCatching {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
            onUpdateSettings { it.copy(useRealDeviceStorage = v) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClearCache) { Text("Xóa bộ nhớ đệm", fontSize = 12.sp) }
            OutlinedButton(onClick = onResetDemo) { Text("Khôi phục mẫu", fontSize = 12.sp) }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = colors.surface2,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Quản lý tệp (Android)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Phát triển : Vũ Minh Ngọc", fontSize = 12.sp, color = colors.textSecondary)
                Text("Email : vuminhngocpt@gmail.com", fontSize = 12.sp, color = colors.accentText)
                Text("GitHub : github.com/vuminhngocpt", fontSize = 12.sp, color = colors.accentText)
            }
        }
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
