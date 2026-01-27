# KẾT LUẬN

Với sự phát triển mạnh mẽ của công nghệ di động và xu hướng chuyển đổi số trong ngành dịch vụ ăn uống (F&B), nhu cầu về các giải pháp quản lý nhà hàng thông minh ngày càng trở nên cấp thiết. Xuất phát từ bài toán xây dựng một hệ thống quản lý nhà hàng có khả năng phục vụ nhiều cơ sở kinh doanh trên cùng một nền tảng, kết hợp với các công nghệ hiện đại như Android, Kotlin, Jetpack Compose và Supabase, đề tài "Xây dựng Hệ thống Quản lý Nhà hàng và Gọi món theo mô hình Multi-tenant OneOrder" đã được nghiên cứu và phát triển.

## 1. Kết quả đạt được

Sau quá trình phân tích, thiết kế và triển khai, hệ thống OneOrder đã đạt được các kết quả sau:

**Về mặt chức năng:**

Hệ thống đã hoàn thành đầy đủ các chức năng cốt lõi cho ba đối tượng người dùng chính. Đối với **khách hàng**, ứng dụng OneOrder cho phép quét mã QR để nhận diện bàn ăn, xem thực đơn của nhà hàng với hình ảnh và mô tả chi tiết, thêm món vào giỏ hàng với ghi chú đặc biệt, đặt món và theo dõi trạng thái đơn hàng theo thời gian thực. Đối với **nhân viên**, ứng dụng OneOrder_SM cung cấp khả năng nhận thông báo đơn hàng mới ngay lập tức thông qua WebSocket, cập nhật trạng thái đơn hàng theo quy trình nghiệp vụ, và quản lý trạng thái các bàn ăn. Đối với **quản lý**, ứng dụng cho phép toàn quyền quản lý thực đơn bao gồm thêm, sửa, xóa danh mục và món ăn; quản lý bàn ăn với khả năng sinh và tải mã QR; quản lý nhân viên thông qua cơ chế lời mời; và xem thống kê doanh thu với biểu đồ trực quan.

**Về mặt kiến trúc:**

Hệ thống đã được xây dựng thành công theo mô hình Multi-tenant với chiến lược Shared Database, Shared Schema, sử dụng Row-Level Security (RLS) của PostgreSQL để đảm bảo cô lập dữ liệu hoàn toàn giữa các nhà hàng. Kiến trúc ứng dụng di động tuân theo Clean Architecture với ba tầng Presentation, Domain và Data, kết hợp mô hình MVVM cho việc quản lý trạng thái UI. Dependency Injection được triển khai bằng Hilt, đảm bảo tính module hóa và khả năng kiểm thử cao.

**Về mặt công nghệ:**

Dự án đã ứng dụng thành công các công nghệ hiện đại bao gồm Kotlin với Coroutines và Flow cho lập trình bất đồng bộ, Jetpack Compose cho giao diện người dùng khai báo, Supabase với đầy đủ các dịch vụ Authentication, Database, Realtime và Storage. Tính năng Realtime của Supabase cho phép cập nhật đơn hàng theo thời gian thực với độ trễ dưới một giây.

**Về mặt kiểm thử:**

Hệ thống đã được kiểm thử kỹ lưỡng với 17 ca kiểm thử chức năng, tất cả đều đạt yêu cầu. Kiểm thử hiệu năng cho thấy thời gian phản hồi trung bình dưới 1.5 giây cho các thao tác thông thường. Kiểm thử bảo mật xác nhận RLS policies hoạt động đúng, ngăn chặn hoàn toàn việc truy cập dữ liệu trái phép giữa các tenant.

## 2. Những kiến thức và kinh nghiệm thu được

Thông qua quá trình thực hiện đề tài, em đã tích lũy được nhiều kiến thức và kinh nghiệm quý báu trong lĩnh vực phát triển ứng dụng di động và backend:

- **Kiến trúc phần mềm:** Hiểu sâu về Clean Architecture và cách áp dụng vào dự án thực tế, biết cách phân tách trách nhiệm giữa các tầng để tạo ra mã nguồn dễ bảo trì và kiểm thử.

- **Multi-tenant Architecture:** Nắm vững các chiến lược triển khai multi-tenant và lý do lựa chọn phương pháp Shared Schema với RLS cho các ứng dụng có quy mô vừa và nhỏ.

- **Supabase và Backend-as-a-Service:** Có kinh nghiệm sử dụng Supabase như một giải pháp backend hoàn chỉnh, đặc biệt là các tính năng Row-Level Security, Realtime subscriptions và Storage policies.

- **Jetpack Compose:** Thành thạo việc xây dựng giao diện người dùng khai báo với Compose, quản lý state, navigation và theming.

- **Quy trình phát triển phần mềm:** Trải nghiệm toàn bộ quy trình từ phân tích yêu cầu, thiết kế, triển khai đến kiểm thử, giúp có cái nhìn tổng quan về vòng đời phát triển phần mềm.

Em hy vọng đề tài này có thể là nguồn tài liệu tham khảo hữu ích cho các bạn sinh viên có mong muốn tìm hiểu về phát triển ứng dụng Android với Kotlin, sử dụng Supabase làm backend, hoặc triển khai kiến trúc Multi-tenant trong các dự án thực tế.

## 3. Hạn chế còn tồn tại

Mặc dù đã đạt được các mục tiêu đề ra, hệ thống vẫn còn một số hạn chế cần được cải thiện:

- **Chế độ offline hạn chế:** Hiện tại, ứng dụng phụ thuộc nhiều vào kết nối mạng. Mặc dù đã có Room database cho việc cache thống kê, các chức năng khác vẫn yêu cầu kết nối internet.

- **Chưa có thông báo push:** Hệ thống chưa tích hợp Firebase Cloud Messaging để gửi thông báo push khi ứng dụng ở chế độ nền, có thể khiến nhân viên bỏ lỡ đơn hàng mới.

- **Chưa tích hợp thanh toán trực tuyến:** Quá trình thanh toán vẫn được xử lý thủ công bởi nhân viên, chưa hỗ trợ các phương thức thanh toán điện tử như VNPay, MoMo, hay quét QR ngân hàng.

- **Giới hạn của Supabase free tier:** Với gói miễn phí, có giới hạn về số lượng connections đồng thời và dung lượng storage, cần nâng cấp lên gói Pro cho môi trường production.

- **Chưa có báo cáo chi tiết:** Módule thống kê hiện chỉ hiển thị doanh thu cơ bản, chưa có khả năng xuất báo cáo PDF hay phân tích chuyên sâu theo sản phẩm, thời điểm.

## 4. Hướng phát triển trong tương lai

Để hoàn thiện và mở rộng hệ thống OneOrder, các hướng phát triển sau được đề xuất:

**Ngắn hạn:**
- Tích hợp Firebase Cloud Messaging để gửi thông báo push cho nhân viên khi có đơn hàng mới hoặc cập nhật trạng thái.
- Cải thiện chế độ offline bằng cách cache thực đơn và cho phép đặt món khi mất mạng, đồng bộ khi có kết nối trở lại.
- Thêm tính năng in hóa đơn qua máy in nhiệt Bluetooth.

**Trung hạn:**
- Tích hợp cổng thanh toán trực tuyến VNPay hoặc MoMo, cho phép khách hàng thanh toán trực tiếp qua ứng dụng.
- Phát triển module báo cáo nâng cao với khả năng xuất PDF/Excel, phân tích doanh thu theo món ăn, theo thời gian, so sánh giữa các kỳ.
- Thêm tính năng đánh giá và nhận xét từ khách hàng sau khi hoàn thành đơn hàng.

**Dài hạn:**
- Ứng dụng trí tuệ nhân tạo (AI) để gợi ý món ăn dựa trên lịch sử đặt hàng và sở thích của khách hàng.
- Phát triển phiên bản web cho quản lý để tiện thao tác trên màn hình lớn.
- Tích hợp với các nền tảng giao hàng như GrabFood, ShopeeFood để mở rộng kênh bán hàng.
- Xây dựng hệ thống loyalty points để tích điểm và ưu đãi cho khách hàng thân thiết.

---

# TÀI LIỆU THAM KHẢO

[1] Google. (2024). *Android Developers - Build apps for Android*. [Trực tuyến]. Có tại: https://developer.android.com/

[2] Google. (2024). *Jetpack Compose - Android's recommended modern toolkit for building native UI*. [Trực tuyến]. Có tại: https://developer.android.com/jetpack/compose

[3] Kotlin Foundation. (2024). *Kotlin Programming Language*. [Trực tuyến]. Có tại: https://kotlinlang.org/

[4] Supabase Inc. (2024). *Supabase - The Open Source Firebase Alternative*. [Trực tuyến]. Có tại: https://supabase.com/

[5] Supabase Inc. (2024). *Supabase Auth - User Authentication and Authorization*. [Trực tuyến]. Có tại: https://supabase.com/docs/guides/auth

[6] Supabase Inc. (2024). *Row Level Security in PostgreSQL*. [Trực tuyến]. Có tại: https://supabase.com/docs/guides/auth/row-level-security

[7] Supabase Inc. (2024). *Supabase Realtime - Listen to database changes*. [Trực tuyến]. Có tại: https://supabase.com/docs/guides/realtime

[8] PostgreSQL Global Development Group. (2024). *PostgreSQL: The World's Most Advanced Open Source Relational Database*. [Trực tuyến]. Có tại: https://www.postgresql.org/

[9] R. C. Martin. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.

[10] Google. (2024). *Guide to app architecture*. [Trực tuyến]. Có tại: https://developer.android.com/topic/architecture

[11] Google. (2024). *ViewModel Overview*. [Trực tuyến]. Có tại: https://developer.android.com/topic/libraries/architecture/viewmodel

[12] Google. (2024). *Kotlin coroutines on Android*. [Trực tuyến]. Có tại: https://developer.android.com/kotlin/coroutines

[13] Google. (2024). *Navigation with Compose*. [Trực tuyến]. Có tại: https://developer.android.com/jetpack/compose/navigation

[14] Google. (2024). *Dependency injection with Hilt*. [Trực tuyến]. Có tại: https://developer.android.com/training/dependency-injection/hilt-android

[15] Jan Tennert. (2024). *Supabase Kotlin Client - Multiplatform Kotlin client for Supabase*. [Trực tuyến]. Có tại: https://github.com/supabase-community/supabase-kt

[16] Coil Contributors. (2024). *Coil - An image loading library for Android backed by Kotlin Coroutines*. [Trực tuyến]. Có tại: https://coil-kt.github.io/coil/

[17] ZXing Authors. (2024). *ZXing ("Zebra Crossing") - Barcode scanning library for Java, Android*. [Trực tuyến]. Có tại: https://github.com/zxing/zxing

[18] Patryk Goworowski. (2024). *Vico - A light and extensible chart library for Android*. [Trực tuyến]. Có tại: https://github.com/patrykandpatrick/vico

[19] S. Millett và N. Tune. (2015). *Patterns, Principles, and Practices of Domain-Driven Design*. Wrox.

[20] E. Gamma, R. Helm, R. Johnson, và J. Vlissides. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley Professional.
