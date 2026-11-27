package com.vuminhngoc.quanlytep.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vuminhngoc.quanlytep.model.FileNode
import com.vuminhngoc.quanlytep.model.ItemKind
import com.vuminhngoc.quanlytep.ui.theme.LocalExtendedColors
import java.io.File

private fun folderCountLabel(item: FileNode): String {
    val trimmedSize = item.size.trim()
    if (trimmedSize.endsWith("mục")) return trimmedSize
    val realPath = item.realFilePath
    if (!realPath.isNullOrBlank()) {
        val dir = File(realPath)
        if (dir.exists() && dir.isDirectory) {
            val count = dir.listFiles()?.size ?: 0
            return "$count mục"
        }
    }
    return "${item.children.size} mục"
}

@Composable
fun ItemTypeBadge(kind: ItemKind, modifier: Modifier = Modifier) {
    val colors = LocalExtendedColors.current
    val (icon: ImageVector, tint: Color) = when (kind) {
        ItemKind.FOLDER -> Icons.Outlined.Folder to colors.accent
        ItemKind.IMAGE -> Icons.Outlined.Image to Color(0xFF38BDF8)
        ItemKind.VIDEO -> Icons.Outlined.Videocam to Color(0xFFF43F5E)
        ItemKind.AUDIO -> Icons.Outlined.MusicNote to Color(0xFFA855F7)
        ItemKind.ARCHIVE -> Icons.Outlined.FolderZip to Color(0xFFF59E0B)
        ItemKind.DOC -> Icons.Outlined.Description to Color(0xFF22C55E)
        ItemKind.FILE -> Icons.Outlined.InsertDriveFile to colors.textSecondary
    }

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = kind.name,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListRowCard(
    item: FileNode,
    isSelected: Boolean,
    showCheckbox: Boolean,
    showSize: Boolean,
    compactMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onEditClick: () -> Unit
) {
    val colors = LocalExtendedColors.current
    val kind = item.detectKind()
    val metaParts = buildList {
        add(item.modified.ifBlank { "26/11/26" })
        if (kind == ItemKind.FOLDER) {
            add(folderCountLabel(item))
        } else if (showSize && item.size.isNotBlank()) {
            add(item.size)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) colors.accentSoft else colors.surface,
        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (compactMode) 7.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showCheckbox) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                )
            }

            ItemTypeBadge(kind = kind)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = colors.text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metaParts.joinToString(" • "),
                    color = colors.textSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Thông tin & Đổi tên",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridTileCard(
    item: FileNode,
    isSelected: Boolean,
    showCheckbox: Boolean,
    showSize: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onEditClick: () -> Unit
) {
    val colors = LocalExtendedColors.current
    val kind = item.detectKind()
    val subText = if (kind == ItemKind.FOLDER) {
        folderCountLabel(item)
    } else if (showSize && item.size.isNotBlank()) {
        item.size
    } else {
        item.modified
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) colors.accentSoft else colors.surface,
        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemTypeBadge(kind = kind)
                if (showCheckbox) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(20.dp),
                        colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                    )
                } else {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Thông tin",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = item.name,
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subText,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
