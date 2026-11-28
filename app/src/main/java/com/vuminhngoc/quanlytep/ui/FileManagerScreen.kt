package com.vuminhngoc.quanlytep.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vuminhngoc.quanlytep.model.*
import com.vuminhngoc.quanlytep.ui.components.*
import com.vuminhngoc.quanlytep.ui.theme.LocalExtendedColors
import com.vuminhngoc.quanlytep.viewmodel.FileManagerViewModel
import kotlinx.coroutines.delay

@Composable
fun FileManagerScreen(viewModel: FileManagerViewModel) {
    val state = viewModel.uiState
    val colors = LocalExtendedColors.current
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            delay(2300)
            viewModel.clearToast()
        }
    }

    val items = state.visibleSortedItems()
    val meta = state.currentMetadata()
    val pct = ((meta.usedBytes.toDouble() / meta.totalBytes.coerceAtLeast(1L).toDouble()) * 100)
        .toInt()
        .coerceIn(1, 100)
    val freeBytes = (meta.totalBytes - meta.usedBytes).coerceAtLeast(0L)

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { viewModel.setShowStorageManager(true) },
                        shape = CircleShape,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(state.source.dotColorHex))
                            )
                            Text(meta.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                        }
                    }

                    Box {
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showOverflowMenu = true },
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.MoreHoriz, contentDescription = "Thêm tùy chọn", tint = colors.text)
                            }
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tạo thư mục mới") },
                                onClick = { showOverflowMenu = false; viewModel.openCreateModal("folder") }
                            )
                            DropdownMenuItem(
                                text = { Text("Tạo tệp văn bản mới (.txt)") },
                                onClick = { showOverflowMenu = false; viewModel.openCreateModal("txt") }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (state.clipboard != null) "Dán (${state.clipboard.items.size} mục)"
                                        else "Dán từ bộ nhớ tạm"
                                    )
                                },
                                onClick = { showOverflowMenu = false; viewModel.pasteClipboard() }
                            )
                            DropdownMenuItem(
                                text = { Text("Chọn nhiều mục") },
                                onClick = { showOverflowMenu = false; viewModel.setSelectMode(true) }
                            )
                            DropdownMenuItem(
                                text = { Text("Nén tệp / thư mục...") },
                                onClick = { showOverflowMenu = false; viewModel.openCompressModal() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Quản lý nguồn lưu trữ") },
                                onClick = { showOverflowMenu = false; viewModel.setShowStorageManager(true) }
                            )
                            DropdownMenuItem(
                                text = { Text("Cấu hình máy chủ NAS") },
                                onClick = { showOverflowMenu = false; viewModel.openNetworkModal(StorageSource.NAS) }
                            )
                            DropdownMenuItem(
                                text = { Text("Kết nối máy chủ SMB") },
                                onClick = { showOverflowMenu = false; viewModel.openNetworkModal(StorageSource.SMB) }
                            )
                            DropdownMenuItem(
                                text = { Text("Kết nối máy chủ FTP") },
                                onClick = { showOverflowMenu = false; viewModel.openNetworkModal(StorageSource.FTP) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Cài đặt") },
                                onClick = { showOverflowMenu = false; viewModel.setShowSettings(true) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "${formatSize(meta.usedBytes)} / ${formatSize(meta.totalBytes)} ($pct%) • Trống ${formatSize(freeBytes)}",
                                fontSize = 11.sp,
                                color = colors.text,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = meta.shortTag,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(CircleShape),
                        color = when {
                            pct >= 90 -> colors.danger
                            pct >= 75 -> colors.warning
                            else -> colors.accent
                        },
                        trackColor = colors.surface3
                    )
                }

                if (state.folderStack.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = meta.label,
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.navigateCrumb(-1) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        state.folderStack.forEachIndexed { idx, step ->
                            val isLast = idx == state.folderStack.lastIndex
                            Text("/", fontSize = 11.sp, color = colors.textMuted)
                            Text(
                                text = step.name,
                                fontSize = 11.5.sp,
                                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isLast) colors.accentText else colors.textSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isLast) colors.accentSoft else Color.Transparent)
                                    .clickable { viewModel.navigateCrumb(idx) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StorageSource.entries.forEach { src ->
                            val active = state.source == src
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.setSource(src) },
                                shape = CircleShape,
                                color = if (active) colors.accentSoft else colors.surface,
                                border = BorderStroke(1.dp, if (active) colors.accent else colors.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(src.dotColorHex))
                                    )
                                    Text(
                                        text = src.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) colors.text else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { viewModel.openCreateModal("folder") },
                        shape = CircleShape,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.text)
                            Text("Thư mục", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FileFilter.entries.forEach { flt ->
                        val active = state.filter == flt
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { viewModel.setFilter(flt) },
                            shape = CircleShape,
                            color = if (active) colors.accent else colors.surface,
                            border = BorderStroke(1.dp, if (active) colors.accent else colors.border)
                        ) {
                            Text(
                                text = flt.label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = colors.text, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            "Tìm kiếm tệp, thư mục, tài liệu, tệp nén...",
                                            color = colors.textMuted,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (state.searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Xóa tìm kiếm",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.setSearchQuery("") }
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleViewMode() },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.viewMode == ViewMode.LIST) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                                contentDescription = "Chế độ hiển thị",
                                tint = colors.text,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                val showMultiBar = state.selectMode || state.selectedIds.isNotEmpty() || state.clipboard != null
                AnimatedVisibility(visible = showMultiBar) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.accent)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = when {
                                    state.selectedIds.isNotEmpty() -> "Đã chọn ${state.selectedIds.size} mục"
                                    state.clipboard != null -> "Bộ nhớ tạm: ${state.clipboard.items.size} mục"
                                    else -> "Chế độ chọn"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                SmallBarChip("Tất cả") { viewModel.selectAllVisible() }
                                SmallBarChip("Bỏ chọn") { viewModel.deselectAll() }
                                SmallBarChip("Sao chép", enabled = state.selectedIds.isNotEmpty(), primary = true) {
                                    viewModel.stageClipboard(ClipboardMode.COPY)
                                }
                                SmallBarChip("Di chuyển", enabled = state.selectedIds.isNotEmpty()) {
                                    viewModel.stageClipboard(ClipboardMode.MOVE)
                                }
                                SmallBarChip("Dán", enabled = state.clipboard != null) {
                                    viewModel.pasteClipboard()
                                }
                                SmallBarChip("Nén", enabled = state.selectedIds.isNotEmpty()) {
                                    viewModel.openCompressModal()
                                }
                                SmallBarChip("Đổi tên", enabled = state.selectedIds.size == 1) {
                                    val found = items.find { it.id in state.selectedIds }
                                    if (found != null) viewModel.openInfoModal(found)
                                }
                                SmallBarChip("Xóa", enabled = state.selectedIds.isNotEmpty(), danger = true) {
                                    viewModel.requestDeleteSelected()
                                }
                                SmallBarChip("Xong") { viewModel.setSelectMode(false) }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.border)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = colors.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                IconButton(
                                    onClick = { viewModel.goHistory(-1) },
                                    enabled = state.canGoBack,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Quay lại",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.goHistory(1) },
                                    enabled = state.canGoForward,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = "Tiến tới",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (state.selectedIds.isNotEmpty()) {
                            "${items.size} mục • Đã chọn ${state.selectedIds.size}"
                        } else "${items.size} mục",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            if (items.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.borderStrong)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(34.dp))
                        Text("Không có mục phù hợp", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                        Text(
                            "Xóa bộ lọc tìm kiếm hoặc dán mục từ bộ nhớ tạm.",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.setFilter(FileFilter.ALL)
                            }) { Text("Xóa bộ lọc") }
                            if (state.clipboard != null) {
                                OutlinedButton(onClick = { viewModel.pasteClipboard() }) { Text("Dán") }
                            }
                        }
                    }
                }
            } else if (state.viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val selected = item.id in state.selectedIds
                        FileListRowCard(
                            item = item,
                            isSelected = selected,
                            showCheckbox = state.selectMode || selected,
                            showSize = state.settings.showSize,
                            compactMode = state.settings.compactMode,
                            onClick = { viewModel.onItemClick(item) },
                            onLongClick = { viewModel.onItemLongPress(item) },
                            onToggleSelect = { viewModel.toggleSelectId(item.id) },
                            onEditClick = { viewModel.openInfoModal(item) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val selected = item.id in state.selectedIds
                        FileGridTileCard(
                            item = item,
                            isSelected = selected,
                            showCheckbox = state.selectMode || selected,
                            showSize = state.settings.showSize,
                            onClick = { viewModel.onItemClick(item) },
                            onLongClick = { viewModel.onItemLongPress(item) },
                            onToggleSelect = { viewModel.toggleSelectId(item.id) },
                            onEditClick = { viewModel.openInfoModal(item) }
                        )
                    }
                }
            }

            state.toastMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    shape = CircleShape,
                    color = colors.surface3,
                    border = BorderStroke(1.dp, colors.borderStrong)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    FileManagerDialogsHost(state = state, viewModel = viewModel)
}

@Composable
private fun SmallBarChip(
    label: String,
    enabled: Boolean = true,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalExtendedColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = when {
            !enabled -> colors.surface.copy(alpha = 0.5f)
            primary -> colors.accent
            else -> colors.surface
        },
        border = BorderStroke(
            1.dp,
            when {
                primary && enabled -> colors.accent
                danger && enabled -> colors.danger.copy(alpha = 0.5f)
                else -> colors.border
            }
        )
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> colors.textMuted
                primary -> Color.White
                danger -> colors.danger
                else -> colors.text
            },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}
