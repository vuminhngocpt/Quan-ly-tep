package com.vuminhngoc.quanlytep.model

data class FileDetailInfo(
    val fileFormat: String = "",
    val exactBytes: Long = 0L,
    val formattedSize: String = "",
    val folderCount: Int = 0,
    val fileCount: Int = 0,
    val formattedTime: String = "",
    val fullPath: String = "",
    val isReadable: Boolean = true,
    val isWritable: Boolean = false,
    val isComputing: Boolean = false
)
