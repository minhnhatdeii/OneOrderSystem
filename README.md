# OneOrder System 

Chào mừng bạn đến với **OneOrder System** - Hệ thống quản lý và đặt món ăn toàn diện, được thiết kế để kết nối thực khách với các nhà hàng một cách liền mạch, đồng thời cung cấp công cụ quản lý mạnh mẽ cho chủ cửa hàng và nhân viên.

##  Tổng quan

OneOrder là một hệ sinh thái bao gồm 2 ứng dụng di động chính và một hệ thống backend mạnh mẽ, hướng tới việc hiện đại hóa trải nghiệm ăn uống và tối ưu hóa quy trình vận hành nhà hàng.

Hệ thống được chia thành 3 phần chính:
1. **OneOrder (Client App):** Ứng dụng dành cho thực khách (Người dùng cuối).
2. **OneOrder_SM (Store Management App):** Ứng dụng dành cho chủ cửa hàng và nhân viên.
3. **Backend (Supabase):** Hệ thống cơ sở dữ liệu và xử lý logic tập trung.

---

##  Các thành phần của hệ thống

### 1. OneOrder (Dành cho Thực khách)
Ứng dụng mang đến trải nghiệm khám phá ẩm thực trực quan và cá nhân hóa.
- **Khám phá & Tìm kiếm:** Dễ dàng tìm kiếm các món ăn và nhà hàng yêu thích.
- **Mạng xã hội ẩm thực (Food Feed):** Xem, tương tác (Like, Comment, Share) với các bài đăng khuyến mãi và món mới từ các nhà hàng bạn theo dõi.
- **Đặt món & Giỏ hàng:** Quá trình đặt món mượt mà, quản lý giỏ hàng thông minh và theo dõi trạng thái đơn hàng theo thời gian thực.
- **Hệ thống gợi ý (Recommendation):** Gợi ý món ăn thông minh dựa trên sở thích và lịch sử tương tác của người dùng.

### 2. OneOrder_SM (Dành cho Quản lý & Nhân viên)
Công cụ quản lý đa người dùng (Multi-tenant) giúp chủ cửa hàng và nhân viên vận hành quán ăn hiệu quả.
- **Quản lý Đơn hàng:** Nhận, xử lý và cập nhật trạng thái đơn hàng nhanh chóng.
- **Quản lý Thực đơn:** Thêm, sửa, xóa món ăn và cập nhật tình trạng hết/còn hàng.
- **Quản lý Nhân sự (Staff Management):** Phân quyền nhân viên, theo dõi chấm công và hiệu suất làm việc.
- **Dashboard & Thống kê:** Báo cáo doanh thu, lợi nhuận, và thống kê đơn hàng trực quan giúp chủ quán nắm bắt tình hình kinh doanh (Cập nhật real-time).
- **Quản lý Marketing (Food Feed):** Tạo và quản lý các bài đăng, chương trình khuyến mãi để tương tác trực tiếp với khách hàng.

### 3. Backend & Database
- Sử dụng **Supabase** (PostgreSQL) để quản lý dữ liệu với hiệu suất cao.
- Phân quyền dữ liệu bảo mật với **Row Level Security (RLS)**.
- Xử lý các logic phức tạp, đồng bộ hóa trạng thái real-time cho các thiết bị di động.

---

##  Công nghệ sử dụng
- **Nền tảng ứng dụng:** Android (Kotlin, Jetpack Compose)
- **Kiến trúc:** Clean Architecture, MVVM
- **Backend/Database:** Supabase, PostgreSQL
- **Tính năng mở rộng:** Tích hợp AI (Hệ thống gợi ý món ăn)

---

##  Trải nghiệm hệ thống

Để trải nghiệm ứng dụng, bạn có thể clone repository này và build dự án trên Android Studio:

1. **Clone repository:**
   ```bash
   git clone https://github.com/your-username/OneOrderSystem.git
   ```
2. **Mở dự án:**
   - Mở thư mục `OneOrder` trong Android Studio để trải nghiệm ứng dụng Khách hàng.
   - Mở thư mục `OneOrder_SM` trong Android Studio để trải nghiệm ứng dụng Quản lý nhà hàng.
3. **Cấu hình Backend:**
   - Đảm bảo bạn đã thêm các file cấu hình `local.properties` (bao gồm Supabase URL và API Key) được cung cấp riêng. (Các file nhạy cảm đã được loại bỏ khỏi mã nguồn để bảo mật).
4. **Build & Run:**
   - Chọn máy ảo (Emulator) hoặc thiết bị thật và nhấn `Run`.

---

##  Bảo mật và Quyền riêng tư
Repository này chỉ chứa **mã nguồn nguyên bản** của ứng dụng. Các tệp tin cấu hình nhạy cảm (API Keys, Keystore), dữ liệu huấn luyện AI, kịch bản phân tích và các tài liệu nghiên cứu liên quan đã được đưa vào `.gitignore` để đảm bảo an toàn và tính bảo mật của dự án.

---
*Dự án được phát triển với sự tâm huyết nhằm mang lại giải pháp công nghệ tối ưu cho ngành dịch vụ F&B.*
