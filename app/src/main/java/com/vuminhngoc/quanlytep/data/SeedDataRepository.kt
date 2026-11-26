package com.vuminhngoc.quanlytep.data

import android.os.Environment
import android.os.StatFs
import com.vuminhngoc.quanlytep.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SeedDataRepository {

    private val volumeNameRegex = Regex("""^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$""")

    fun findMountedRemovableVolumes(): List<File> {
        val storageRoot = File("/storage")
        return storageRoot.listFiles()
            ?.filter { dir ->
                dir.isDirectory &&
                    dir.name !in setOf("emulated", "self", "sdcard0") &&
                    volumeNameRegex.matches(dir.name) &&
                    dir.canRead()
            }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            .orEmpty()
    }

    fun resolveSourcePath(source: StorageSource): String {
        return when (source) {
            StorageSource.DEVICE -> {
                val primary = runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrNull()
                if (!primary.isNullOrBlank() && File(primary).exists()) primary else source.defaultPath
            }
            StorageSource.SD -> {
                if (File(source.defaultPath).exists()) {
                    source.defaultPath
                } else {
                    findMountedRemovableVolumes().firstOrNull()?.absolutePath ?: source.defaultPath
                }
            }
            StorageSource.OTG -> {
                if (File(source.defaultPath).exists()) {
                    source.defaultPath
                } else {
                    findMountedRemovableVolumes().getOrNull(1)?.absolutePath ?: source.defaultPath
                }
            }
            else -> source.defaultPath
        }
    }

    fun defaultMetadataMap(): Map<StorageSource, StorageMetadata> {
        return StorageSource.entries.associateWith { src ->
            var used = src.defaultUsedBytes
            var total = src.defaultTotalBytes
            val resolvedPath = resolveSourcePath(src)

            if (src in setOf(StorageSource.DEVICE, StorageSource.SD, StorageSource.OTG)) {
                val dir = File(resolvedPath)
                if (dir.exists()) {
                    runCatching {
                        val stat = StatFs(dir.absolutePath)
                        val realTotal = stat.totalBytes
                        val realFree = stat.availableBytes
                        if (realTotal > 0L) {
                            total = realTotal
                            used = (realTotal - realFree).coerceIn(0L, realTotal)
                        }
                    }
                }
            }

            StorageMetadata(
                source = src,
                label = src.label,
                shortTag = src.defaultTag,
                path = resolvedPath,
                usedBytes = used,
                totalBytes = total,
                host = src.defaultHost,
                port = src.defaultPort,
                mode = src.defaultMode,
                user = src.defaultUser
            )
        }
    }

    fun defaultProfilesMap(): Map<StorageSource, List<NetworkProfile>> {
        return mapOf(
            StorageSource.NAS to listOf(
                NetworkProfile("NAS_Chinh", "192.168.1.50", "192.168.1.50/volume1", "5005", "SMB3 + WebDAV", "admin")
            ),
            StorageSource.SMB to listOf(
                NetworkProfile("SMB_Share", "192.168.1.50", "//192.168.1.50/share", "445", "SMBv3", "workgroup_user")
            ),
            StorageSource.FTP to listOf(
                NetworkProfile("FTP_Server", "192.168.1.21", "ftp://192.168.1.21:21", "21", "Passive (PASV)", "ftp_user")
            )
        )
    }

    fun buildAllSeedTrees(): Map<StorageSource, List<FileNode>> {
        return StorageSource.entries.associateWith { buildSeedForSource(it) }
    }

    fun buildSeedForSource(source: StorageSource): List<FileNode> = when (source) {
        StorageSource.DEVICE -> listOf(
            FileNode(
                id = "dev_f_dcim",
                name = "DCIM",
                isFolder = true,
                modified = "26/11/26",
                children = listOf(
                    FileNode("dev_p_1", "Anh_Thanh_Pho.jpg", false, "26/11/26", "3,6 MB", "Ảnh lưu trên Bộ nhớ máy"),
                    FileNode("dev_v_1", "Clip_Gioi_Thieu.mp4", false, "25/11/26", "42,0 MB", "Video độ phân giải Full HD")
                )
            ),
            FileNode(
                id = "dev_f_docs",
                name = "Documents",
                isFolder = true,
                modified = "26/11/26",
                children = listOf(
                    FileNode(
                        id = "dev_pdf_1",
                        name = "Tai_Lieu_Huong_Dan.pdf",
                        isFolder = false,
                        modified = "26/11/26",
                        size = "1,5 MB",
                        notes = "Tài liệu hướng dẫn sử dụng Quản lý tệp",
                        pdfPages = listOf(
                            PdfPageData(
                                1,
                                "1. Tổng quan ứng dụng Quản lý tệp",
                                "Quản lý tệp hỗ trợ duyệt và thao tác trực tiếp trên Bộ nhớ máy, Thẻ nhớ, USB OTG, NAS, SMB và FTP.\n\nGiữ vào mục bất kỳ để mở bảng tùy chọn hoặc chọn nhiều mục để Sao chép, Di chuyển, Dán, Nén, Đổi tên và Xóa."
                            ),
                            PdfPageData(
                                2,
                                "2. Nén, giải nén & đa phương tiện",
                                "Hỗ trợ xem danh sách mục bên trong, giải nén và nén các định dạng .zip, .tar, .tar.gz, .tgz, .7z và .rar."
                            )
                        )
                    ),
                    FileNode(
                        id = "dev_txt_1",
                        name = "ghi_chu_may.txt",
                        isFolder = false,
                        modified = "26/11/26",
                        size = "1,4 KB",
                        notes = "Ghi chú cá nhân",
                        textContent = "Ghi chú Bộ nhớ máy:\n- Đường dẫn: /storage/emulated/0\n- Hệ thống tệp: UFS 4.0\n- Trạng thái: Hoạt động ổn định."
                    )
                )
            ),
            FileNode(
                id = "dev_md_1",
                name = "ke_hoach_tuan.md",
                isFolder = false,
                modified = "26/11/26",
                size = "2,1 KB",
                notes = "Kế hoạch công việc Markdown",
                textContent = "# Kế Hoạch Công Việc\n\n- Kiểm tra đồng bộ dữ liệu NAS & SMB\n- Sao lưu thư mục tài liệu sang USB OTG\n- Dọn dẹp tệp nén cũ"
            ),
            FileNode(
                id = "dev_json_1",
                name = "cau_hinh_he_thong.json",
                isFolder = false,
                modified = "25/11/26",
                size = "960 B",
                notes = "Cấu hình JSON mẫu",
                textContent = "{\n  \"app\": \"Quản lý tệp\",\n  \"version\": \"v22\",\n  \"platform\": \"Android Kotlin\",\n  \"active\": true\n}"
            ),
            FileNode(
                id = "dev_csv_1",
                name = "thong_ke_dung_luong.csv",
                isFolder = false,
                modified = "25/11/26",
                size = "580 B",
                notes = "Bảng thống kê dung lượng",
                textContent = "Nguon,Da_dung,Tong_dung_luong\nBo nho may,98.4 GB,256.0 GB\nThe nho,18.4 GB,32.0 GB\nUSB OTG,12.6 GB,64.0 GB"
            ),
            FileNode(
                id = "dev_zip_1",
                name = "du_lieu_mau.zip",
                isFolder = false,
                modified = "25/11/26",
                size = "5,8 MB",
                notes = "Gói lưu trữ ZIP mẫu",
                archiveEntries = listOf(
                    ArchiveEntryItem("readme.txt", "1,1 KB"),
                    ArchiveEntryItem("bao_cao.csv", "2,3 KB")
                )
            ),
            FileNode(
                id = "dev_aud_1",
                name = "Ban_Nhac_Thu_Gian.mp3",
                isFolder = false,
                modified = "24/11/26",
                size = "6,2 MB",
                notes = "Tệp âm thanh MP3 mẫu"
            )
        )

        StorageSource.SD -> listOf(
            FileNode(
                id = "sd_f_cam",
                name = "Camera_SD",
                isFolder = true,
                modified = "25/11/26",
                children = listOf(
                    FileNode("sd_p_1", "Nui_Rung_Sapa.jpg", false, "25/11/26", "4,1 MB", "Ảnh chụp lưu trên thẻ nhớ")
                )
            ),
            FileNode("sd_f_bak", "Backup_SD", true, "24/11/26"),
            FileNode(
                id = "sd_txt_1",
                name = "ghi_chu_the_nho.txt",
                isFolder = false,
                modified = "25/11/26",
                size = "920 B",
                notes = "Thông tin thẻ nhớ",
                textContent = "Thẻ nhớ SDXC UHS-I tại /storage/sdcard1.\nDung lượng: 18,4 GB / 32,0 GB."
            ),
            FileNode(
                id = "sd_csv_1",
                name = "danh_muc_sd.csv",
                isFolder = false,
                modified = "24/11/26",
                size = "490 B",
                notes = "Danh mục thẻ nhớ",
                textContent = "Thu_muc,So_muc\nCamera_SD,1\nBackup_SD,0"
            ),
            FileNode("sd_aud_1", "Audio_Ghi_Am.mp3", false, "23/11/26", "5,1 MB", "Bản ghi âm trên thẻ nhớ"),
            FileNode(
                id = "sd_7z_1",
                name = "sao_luu_sd.7z",
                isFolder = false,
                modified = "24/11/26",
                size = "14,6 MB",
                notes = "Tệp nén 7z trên thẻ nhớ",
                archiveEntries = listOf(
                    ArchiveEntryItem("photo_01.jpg", "4,1 MB"),
                    ArchiveEntryItem("log.txt", "800 B")
                )
            )
        )

        StorageSource.OTG -> listOf(
            FileNode(
                id = "otg_f_files",
                name = "OTG_Storage",
                isFolder = true,
                modified = "25/11/26",
                children = listOf(
                    FileNode(
                        id = "otg_log_1",
                        name = "otg_mount.log",
                        isFolder = false,
                        modified = "25/11/26",
                        size = "1,3 KB",
                        notes = "Nhật ký gắn USB OTG",
                        textContent = "[INFO] Mounted /storage/usbotg (exFAT)\n[OK] Transfer speed verified."
                    )
                )
            ),
            FileNode("otg_f_iso", "Bo_Cai_Dat", true, "23/11/26"),
            FileNode(
                id = "otg_txt_1",
                name = "huong_dan_otg.txt",
                isFolder = false,
                modified = "25/11/26",
                size = "1,0 KB",
                notes = "Ghi chú USB OTG",
                textContent = "USB OTG exFAT • USB 3.2 sẵn sàng trao đổi dữ liệu."
            ),
            FileNode("otg_p_1", "So_Do_He_Thong.png", false, "24/11/26", "1,7 MB", "Ảnh sơ đồ"),
            FileNode("otg_v_1", "Video_Huong_Dan.mp4", false, "23/11/26", "24,8 MB", "Video trên USB OTG"),
            FileNode(
                id = "otg_tgz_1",
                name = "goi_otg.tar.gz",
                isFolder = false,
                modified = "24/11/26",
                size = "11,4 MB",
                notes = "Tệp nén TAR.GZ",
                archiveEntries = listOf(
                    ArchiveEntryItem("otg/info.txt", "1,0 KB"),
                    ArchiveEntryItem("otg/data.json", "2,2 KB")
                )
            )
        )

        StorageSource.NAS -> listOf(
            FileNode(
                id = "nas_f_media",
                name = "Media_4K",
                isFolder = true,
                modified = "26/11/26",
                children = listOf(
                    FileNode("nas_v_1", "Phim_4K_Demo.mp4", false, "26/11/26", "118,0 MB", "Video lưu trên NAS")
                )
            ),
            FileNode("nas_f_shared", "Tai_Lieu_NAS", true, "25/11/26"),
            FileNode("nas_pdf_1", "Bao_Cao_NAS.pdf", false, "25/11/26", "1,1 MB", "Báo cáo lưu trữ NAS"),
            FileNode(
                id = "nas_md_1",
                name = "nas_readme.md",
                isFolder = false,
                modified = "26/11/26",
                size = "1,5 KB",
                notes = "Thông tin máy chủ NAS",
                textContent = "# Máy Chủ NAS\n\n- Địa chỉ: `192.168.1.50/volume1`"
            ),
            FileNode("nas_p_1", "May_Chu_NAS.jpg", false, "25/11/26", "2,8 MB", "Ảnh máy chủ NAS"),
            FileNode(
                id = "nas_7z_1",
                name = "backup_nas.7z",
                isFolder = false,
                modified = "26/11/26",
                size = "28,5 MB",
                notes = "Tệp nén 7z trên NAS",
                archiveEntries = listOf(
                    ArchiveEntryItem("nas/config.json", "6,4 KB"),
                    ArchiveEntryItem("nas/status.log", "3,1 KB")
                )
            )
        )

        StorageSource.SMB -> listOf(
            FileNode(
                id = "smb_f_work",
                name = "Workgroup_Share",
                isFolder = true,
                modified = "26/11/26",
                children = listOf(
                    FileNode(
                        id = "smb_txt_1",
                        name = "smb_huong_dan.txt",
                        isFolder = false,
                        modified = "26/11/26",
                        size = "1,1 KB",
                        notes = "Hướng dẫn thư mục chia sẻ SMB",
                        textContent = "Thư mục chia sẻ SMB tại //192.168.1.50/share."
                    )
                )
            ),
            FileNode("smb_f_team", "Du_An_SMB", true, "25/11/26"),
            FileNode(
                id = "smb_json_1",
                name = "smb_ket_noi.json",
                isFolder = false,
                modified = "25/11/26",
                size = "740 B",
                notes = "Thông số kết nối SMB",
                textContent = "{\n  \"protocol\": \"SMBv3\",\n  \"host\": \"192.168.1.50\",\n  \"share\": \"//192.168.1.50/share\"\n}"
            ),
            FileNode("smb_aud_1", "Nhac_Cho_SMB.mp3", false, "24/11/26", "3,9 MB", "Tệp âm thanh trên SMB"),
            FileNode(
                id = "smb_rar_1",
                name = "goi_smb.rar",
                isFolder = false,
                modified = "25/11/26",
                size = "8,7 MB",
                notes = "Tệp nén RAR trên SMB",
                archiveEntries = listOf(ArchiveEntryItem("smb/notes.txt", "1,8 KB"))
            )
        )

        StorageSource.FTP -> listOf(
            FileNode(
                id = "ftp_f_pub",
                name = "Public_FTP",
                isFolder = true,
                modified = "26/11/26",
                children = listOf(
                    FileNode(
                        id = "ftp_txt_1",
                        name = "ftp_readme.txt",
                        isFolder = false,
                        modified = "26/11/26",
                        size = "980 B",
                        notes = "Tệp văn bản trên máy chủ FTP",
                        textContent = "Máy chủ FTP tại ftp://192.168.1.21:21.\nChế độ: Passive (PASV)."
                    )
                )
            ),
            FileNode("ftp_f_up", "Uploads", true, "25/11/26"),
            FileNode(
                id = "ftp_log_1",
                name = "ftp_session.log",
                isFolder = false,
                modified = "26/11/26",
                size = "1,2 KB",
                notes = "Nhật ký phiên FTP",
                textContent = "[220] FTP Server Ready\n[PASV] Entering Passive Mode"
            ),
            FileNode(
                id = "ftp_csv_1",
                name = "ftp_danh_muc.csv",
                isFolder = false,
                modified = "24/11/26",
                size = "510 B",
                notes = "Danh mục tệp FTP",
                textContent = "Thu_muc,Quyen\nPublic_FTP,Doc\nUploads,Ghi"
            ),
            FileNode("ftp_p_1", "Anh_FTP.png", false, "24/11/26", "1,6 MB", "Ảnh trên máy chủ FTP"),
            FileNode(
                id = "ftp_tar_1",
                name = "luu_tru_ftp.tar",
                isFolder = false,
                modified = "25/11/26",
                size = "16,2 MB",
                notes = "Tệp nén TAR trên FTP",
                archiveEntries = listOf(ArchiveEntryItem("ftp/index.txt", "1,5 KB"))
            )
        )
    }

    fun listRealAndroidDirectory(dirPath: String): List<FileNode> {
        val dir = File(dirPath)
        val rawFiles = dir.listFiles() ?: return emptyList()
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        return rawFiles.map { file ->
            val isDir = file.isDirectory
            val count = if (isDir) (file.listFiles()?.size ?: 0) else 0
            val ext = if (!isDir) getExtension(file.name) else ""
            val readableText = if (!isDir && ext in DOC_EXTS && ext != "pdf" && file.length() in 0L..1_048_576L) {
                runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            } else {
                null
            }
            FileNode(
                id = "real_${file.absolutePath.hashCode()}_${file.name}",
                name = file.name,
                isFolder = isDir,
                modified = sdf.format(Date(file.lastModified())),
                size = if (isDir) "$count mục" else formatSize(file.length()),
                notes = file.absolutePath,
                textContent = readableText,
                realFilePath = file.absolutePath
            )
        }
    }
}
