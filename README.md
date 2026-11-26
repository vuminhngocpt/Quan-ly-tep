# Quản lý tệp (Android - Kotlin & Jetpack Compose)

Ứng dụng Quản lý tệp trên nền tảng Android viết bằng **Kotlin** và **Jetpack Compose (Material 3)**, chuyển đổi đầy đủ giao diện và tính năng sang ứng dụng Android hoàn chỉnh sẵn sàng mở bằng Android Studio hoặc build tự động trên GitHub Actions.

- **Phát triển:** Vũ Minh Ngọc
- **Email:** vuminhngocpt@gmail.com
- **GitHub:** [vuminhngocpt](https://github.com/vuminhngocpt)

## Tính năng chính
- **6 nguồn lưu trữ:** Bộ nhớ máy (`/storage/emulated/0`), Thẻ nhớ (`/storage/sdcard1`), USB OTG (`/storage/usbotg`), NAS, SMB và FTP kèm thanh dung lượng và cấu hình kết nối mạng.
- **Điều hướng & tìm kiếm:** Lịch sử Lùi/Tiến, Breadcrumb thư mục, lọc theo loại tệp (Tất cả, Thư mục, Ảnh, Video, Âm thanh, Tài liệu, Nén), tìm kiếm tức thì và chuyển đổi Danh sách / Lưới.
- **Giữ để chọn (Long-press) & Chọn nhiều mục:** Nhấn giữ mục bất kỳ để mở bảng thao tác nhanh hoặc bật chế độ chọn nhiều mục (Sao chép, Di chuyển, Dán, Nén, Đổi tên, Xóa).
- **Trình xem & biên tập tích hợp:**
  - Trình xem/chỉnh sửa tài liệu (`.txt`, `.md`, `.json`, `.csv`, `.log`, `.pdf`) có tìm từ khóa, phóng to/thu nhỏ, đánh số dòng và sao chép nội dung.
  - Trình xem tệp nén (`.zip`, `.tar`, `.tar.gz`, `.tgz`, `.7z`, `.rar`) hỗ trợ lọc mục, giải nén và nén tệp/thư mục.
  - Trình xem nhanh Ảnh, Video và Âm thanh.
- **Giao diện & Cài đặt:** Chế độ Tối (`#000000`), Sáng, Tự động theo hệ thống; sắp xếp theo Tên / Loại / Ngày; chế độ hiển thị gọn (Compact); khôi phục dữ liệu mẫu.
- **Tích hợp hệ thống tệp thật Android:** Hỗ trợ cấp quyền `MANAGE_EXTERNAL_STORAGE` / `READ_MEDIA_*` và tùy chọn duyệt tệp thực tế trên thiết bị bên cạnh dữ liệu mẫu.

## Cách build dự án
1. Giải nén và mở thư mục dự án bằng **Android Studio** (Jellyfish / Koala / Ladybug trở lên, JDK 17).
2. Chờ Gradle đồng bộ các thư viện Jetpack Compose.
3. Nhấn **Run** để cài lên thiết bị/máy ảo hoặc đẩy lên GitHub (đã có sẵn workflow `.github/workflows/android-build.yml` tự động build file APK Debug).
