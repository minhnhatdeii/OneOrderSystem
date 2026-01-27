# Kế hoạch Triển khai Báo cáo Môn Dự án Công nghệ
## Dự án: OneOrder (Hệ thống quản lý đặt món Multi-tenant)

**Mục tiêu:** Hoàn thành báo cáo môn học với chất lượng cao, đảm bảo đầy đủ các nội dung yêu cầu và phản ánh đúng thực tế triển khai của dự án OneOrder.

**Cấu trúc báo cáo dự kiến:**
1.  Lời cảm ơn
2.  Tóm tắt (Abstract)
3.  Lời cam đoan
4.  Mục lục
5.  Danh mục hình vẽ
6.  Danh mục bảng biểu
7.  Danh mục cụm từ viết tắt
8.  Mở đầu
9.  Chương 1: Cơ sở lý thuyết
10. Chương 2: Phân tích yêu cầu
11. Chương 3: Thiết kế hệ thống
12. Chương 4: Triển khai, cài đặt và kiểm thử
13. Kết luận
14. Tài liệu tham khảo

---

## 📑 MỤC LỤC

**LỊ CẢM ƠN** ......................................................... i

**TÓM TẮT** ............................................................ ii

**LỜI CAM ĐOAN** ........................................................ iii

**MỤC LỤC** ............................................................ iv

**DANH MỤC HÌNH VẼ** .................................................... vii

**DANH MỤC BẢNG BIỂU** .................................................. ix

**DANH MỤC CỤM TỪ VIẾT TẮT** ............................................ x

**MỞ ĐẦU** ............................................................. 1

### CHƯƠNG 1. CƠ SỞ LÝ THUYẾT ........................................... 3

1.1. Hệ thống quản lý nhà hàng .......................................... 3
   - 1.1.1. Giới thiệu chung về hệ thống quản lý nhà hàng ............... 3
   - 1.1.2. Các mô hình triển khai hiện tại ............................. 4

1.2. Kiến trúc Multi-tenant ............................................. 5
   - 1.2.1. Giới thiệu tổng quan ........................................ 5
   - 1.2.2. Ưu điểm của kiến trúc Multi-tenant .......................... 6

1.3. Nền tảng Android và Kotlin ......................................... 7
   - 1.3.1. Android SDK ................................................. 7
   - 1.3.2. Ngôn ngữ lập trình Kotlin ................................... 8

1.4. Jetpack Compose .................................................... 9

1.5. Supabase - Backend as a Service .................................... 10
   - 1.5.1. PostgreSQL Database ......................................... 10
   - 1.5.2. Authentication .............................................. 11
   - 1.5.3. Realtime Database ........................................... 11
   - 1.5.4. Storage ..................................................... 12

1.6. Kiến trúc MVVM ..................................................... 12

1.7. Clean Architecture ................................................. 13

1.8. Row-Level Security (RLS) ........................................... 14

1.9. Design Patterns .................................................... 15
   - 1.9.1. Circuit Breaker ............................................. 15
   - 1.9.2. Retry with Exponential Backoff .............................. 16
   - 1.9.3. Idempotency ................................................. 16
   - 1.9.4. Queue-based Load Leveling ................................... 17

1.10. Tổng kết chương ................................................... 17

### CHƯƠNG 2. PHÂN TÍCH YÊU CẦU ......................................... 18

2.1. Xác định bài toán và đối tượng sử dụng ............................. 18

2.2. Phân tích các yêu cầu của hệ thống ................................. 19
   - 2.2.1. Yêu cầu chức năng cho khách hàng ............................ 19
   - 2.2.2. Yêu cầu chức năng cho nhân viên ............................. 20
   - 2.2.3. Yêu cầu chức năng cho quản lý ............................... 21
   - 2.2.4. Các yêu cầu phi chức năng ................................... 22

2.3. Phân tích và đặc tả ca sử dụng ..................................... 23
   - 2.3.1. Đặc tả ca sử dụng: Quét QR và xem thực đơn .................. 24
   - 2.3.2. Đặc tả ca sử dụng: Đặt món ................................... 25
   - 2.3.3. Đặc tả ca sử dụng: Quản lý đơn hàng (Staff) ................. 26
   - 2.3.4. Đặc tả ca sử dụng: Quản lý thực đơn (Manager) ............... 27
   - 2.3.5. Đặc tả ca sử dụng: Quản lý bàn (Manager) .................... 28
   - 2.3.6. Đặc tả ca sử dụng: Quản lý nhân viên (Manager) .............. 29
   - 2.3.7. Đặc tả ca sử dụng: Xem thống kê doanh thu ................... 30

2.4. Tổng kết chương .................................................... 31

### CHƯƠNG 3. THIẾT KẾ HỆ THỐNG ......................................... 32

3.1. Tổng quan thiết kế hệ thống ........................................ 32

3.2. Mô tả kiến trúc tổng thể ........................................... 33
   - 3.2.1. Kiến trúc Multi-tenant ...................................... 33
   - 3.2.2. Kiến trúc ứng dụng di động .................................. 34

3.3. Thiết kế cơ sở dữ liệu ............................................. 35
   - 3.3.1. Mô hình ERD ................................................. 35
   - 3.3.2. Các bảng dữ liệu chính ...................................... 36
   - 3.3.3. Row-Level Security Policies ................................. 38

3.4. Thiết kế luồng nghiệp vụ ........................................... 39
   - 3.4.1. Luồng đăng ký nhà hàng ...................................... 39
   - 3.4.2. Luồng quét QR và đặt món .................................... 40
   - 3.4.3. Luồng xử lý đơn hàng ........................................ 41
   - 3.4.4. Luồng quản lý thực đơn ...................................... 42
   - 3.4.5. Luồng quản lý bàn ........................................... 43
   - 3.4.6. Luồng thống kê doanh thu .................................... 44

3.5. Thiết kế giao diện người dùng ...................................... 45
   - 3.5.1. Giao diện ứng dụng khách hàng ............................... 45
   - 3.5.2. Giao diện ứng dụng nhân viên/quản lý ........................ 46

3.6. Tổng kết chương .................................................... 47

### CHƯƠNG 4. TRIỂN KHAI, CÀI ĐẶT VÀ KIỂM THỬ .......................... 48

4.1. Môi trường triển khai .............................................. 48
   - 4.1.1. Cấu hình phần cứng .......................................... 48
   - 4.1.2. Cấu hình phần mềm ........................................... 49
   - 4.1.3. Các thư viện sử dụng ........................................ 50

4.2. Cấu trúc mã nguồn .................................................. 51
   - 4.2.1. Cấu trúc dự án Android ...................................... 51
   - 4.2.2. Tổ chức theo Clean Architecture ............................. 52

4.3. Triển khai các chức năng chính ..................................... 53
   - 4.3.1. Triển khai xác thực và phân quyền ........................... 53
   - 4.3.2. Triển khai quản lý đơn hàng realtime ........................ 54
   - 4.3.3. Triển khai quản lý thực đơn ................................. 55
   - 4.3.4. Triển khai quản lý bàn và QR code ........................... 56
   - 4.3.5. Triển khai thống kê và báo cáo .............................. 57

4.4. Kết quả giao diện .................................................. 58
   - 4.4.1. Giao diện ứng dụng khách hàng ............................... 58
   - 4.4.2. Giao diện ứng dụng Staff/Manager ............................ 60

4.5. Kiểm thử hệ thống .................................................. 62
   - 4.5.1. Kiểm thử chức năng .......................................... 62
   - 4.5.2. Kiểm thử hiệu năng .......................................... 64
   - 4.5.3. Kiểm thử bảo mật ............................................ 65

4.6. Đánh giá kết quả ................................................... 66

4.7. Tổng kết chương .................................................... 67

**KẾT LUẬN** ........................................................... 68

**TÀI LIỆU THAM KHẢO** ................................................. 70

---

## � MỞ ĐẦU

### 1. Đặt vấn đề

Trong bối cảnh chuyển đổi số đang diễn ra mạnh mẽ tại Việt Nam, ngành dịch vụ ăn uống (F&B) đang đối mặt với nhiều thách thức trong việc nâng cao chất lượng phục vụ và tối ưu hóa quy trình vận hành. Theo báo cáo của Hiệp hội Nhà hàng Việt Nam, hơn 70% các nhà hàng vừa và nhỏ vẫn đang sử dụng phương pháp quản lý truyền thống với giấy tờ, ghi chép thủ công, dẫn đến nhiều vấn đề như: sai sót trong việc ghi nhận đơn hàng, khó khăn trong việc theo dõi doanh thu, thất thoát hàng hóa, và trải nghiệm khách hàng chưa được tối ưu.

Cụ thể, các phương pháp truyền thống như gọi món qua nhân viên, ghi chép đơn hàng bằng giấy, thanh toán thủ công không chỉ tốn thời gian mà còn dễ xảy ra nhầm lẫn trong quá trình truyền đạt thông tin từ khách hàng đến bếp. Điều này không những ảnh hưởng đến trải nghiệm của khách hàng mà còn làm giảm hiệu quả vận hành của nhà hàng, đặc biệt trong giờ cao điểm khi lượng khách đông. Hơn nữa, việc quản lý thực đơn, theo dõi tồn kho, và thống kê doanh thu thủ công đòi hỏi nhiều công sức và dễ dẫn đến sai sót trong báo cáo tài chính.

Bên cạnh đó, đối với các doanh nghiệp có nhiều chi nhánh nhà hàng, việc quản lý tập trung và đồng bộ dữ liệu giữa các chi nhánh trở thành một thách thức lớn. Các hệ thống quản lý nhà hàng truyền thống thường được xây dựng riêng lẻ cho từng chi nhánh, dẫn đến tình trạng dữ liệu phân tán, khó khăn trong việc tổng hợp báo cáo và ra quyết định kinh doanh. Việc triển khai và bảo trì nhiều hệ thống độc lập cũng đòi hỏi chi phí cao về nhân lực và hạ tầng công nghệ.

Trong xu hướng chung của các hệ thống hiện đại về việc tối ưu hóa chi phí và tăng khả năng mở rộng, kiến trúc Multi-tenant trở thành một giải pháp phổ biến. Kiến trúc này cho phép nhiều tổ chức (tenant) khác nhau sử dụng chung một nền tảng công nghệ nhưng vẫn đảm bảo sự độc lập và bảo mật dữ liệu tuyệt đối giữa các tenant. Tuy nhiên, việc xây dựng một hệ thống Multi-tenant đòi hỏi kiến thức sâu về bảo mật, quản lý dữ liệu phân tán, và thiết kế kiến trúc phần mềm.

### 2. Giải pháp đề xuất

Nhận thấy những hạn chế và nhu cầu thực tế của ngành F&B, em đã nghiên cứu và phát triển **"Hệ thống Quản lý Nhà hàng và Gọi món Phi tập trung theo Mô hình Multi-tenant OneOrder"** - một giải pháp toàn diện nhằm số hóa quy trình hoạt động của nhà hàng, từ việc gọi món, quản lý thực đơn, theo dõi đơn hàng đến thống kê doanh thu.

Hệ thống OneOrder được xây dựng dựa trên **kiến trúc Multi-tenant**, cho phép nhiều nhà hàng khác nhau đăng ký và sử dụng chung một nền tảng công nghệ. Mỗi nhà hàng (tenant) sẽ có dữ liệu riêng biệt, được bảo vệ bởi cơ chế **Row-Level Security (RLS)** của PostgreSQL, đảm bảo không có sự xâm phạm dữ liệu giữa các tenant. Hệ thống bao gồm hai ứng dụng di động chính được phát triển trên nền tảng Android:

**OneOrder (Customer App)** - Ứng dụng dành cho khách hàng, cung cấp trải nghiệm đặt món hiện đại và tiện lợi. Khách hàng chỉ cần quét mã QR được đặt tại mỗi bàn để tự động nhận diện nhà hàng và bàn ăn, sau đó có thể xem thực đơn đầy đủ với hình ảnh, mô tả chi tiết và giá cả. Việc đặt món được thực hiện hoàn toàn tự động, giúp giảm thiểu thời gian chờ đợi và sai sót trong quá trình truyền đạt thông tin.

**OneOrder_SM (Staff/Manager App)** - Ứng dụng dành cho nhân viên và quản lý nhà hàng, hỗ trợ toàn diện các hoạt động vận hành. Nhân viên có thể theo dõi đơn hàng theo thời gian thực, cập nhật trạng thái từ "Đang chờ" → "Đang chuẩn bị" → "Đã phục vụ" → "Đã thanh toán". Quản lý nhà hàng có thể quản lý thực đơn (thêm/sửa/xóa món ăn, cập nhật giá), quản lý bàn ăn, quản lý nhân viên, và xem các báo cáo thống kê doanh thu chi tiết theo ngày, tuần, tháng.

Về mặt công nghệ, hệ thống được xây dựng trên nền tảng **Android** sử dụng ngôn ngữ lập trình **Kotlin** - ngôn ngữ hiện đại, an toàn và được Google khuyến nghị cho phát triển Android. Giao diện người dùng được xây dựng bằng **Jetpack Compose** - framework UI declarative mới nhất của Android, giúp tạo ra giao diện đẹp mắt, mượt mà và dễ bảo trì.

Backend được xây dựng trên **Supabase** - một nền tảng Backend-as-a-Service (BaaS) mã nguồn mở, cung cấp đầy đủ các dịch vụ cần thiết như PostgreSQL database, xác thực người dùng (Authentication), lưu trữ file (Storage), và đặc biệt là khả năng cập nhật dữ liệu theo thời gian thực (Realtime). Việc sử dụng Supabase giúp giảm thiểu thời gian phát triển backend và tập trung vào logic nghiệp vụ.

Kiến trúc ứng dụng tuân theo mô hình **MVVM (Model-View-ViewModel)** kết hợp với **Clean Architecture**, phân tách rõ ràng các tầng Data, Domain, và Presentation. Điều này đảm bảo code dễ đọc, dễ bảo trì, dễ mở rộng và dễ kiểm thử. Để đảm bảo hiệu năng và độ tin cậy cao, hệ thống đã triển khai các design patterns quan trọng như **Circuit Breaker**, **Retry with Exponential Backoff**, **Idempotency**, và **Queue-based Load Leveling**.

### 3. Kết quả đạt được

Báo cáo này sẽ tập trung vào phần phát triển kiến trúc và hoạt động của cả hai ứng dụng trong hệ thống OneOrder. Phiên bản đầu tiên của hệ thống đã được triển khai thành công với đầy đủ các chức năng cốt lõi cho ba nhóm đối tượng sử dụng chính: khách hàng, nhân viên và quản lý nhà hàng.

Đối với **khách hàng**, hệ thống cho phép quét mã QR tại bàn để tự động nhận diện nhà hàng và vị trí, xem thực đơn đầy đủ với hình ảnh và giá cả, đặt món trực tiếp từ điện thoại, và theo dõi trạng thái đơn hàng theo thời gian thực.

Đối với **nhân viên**, hệ thống cung cấp khả năng nhận thông báo đơn hàng mới, quản lý và cập nhật trạng thái đơn hàng theo quy trình (Pending → Preparing → Served → Paid), cũng như quản lý trạng thái bàn ăn.

Đối với **quản lý nhà hàng**, hệ thống hỗ trợ đầy đủ các chức năng quản lý thực đơn, quản lý bàn ăn (bao gồm tạo mã QR), quản lý nhân viên với phân quyền rõ ràng, và xem thống kê doanh thu chi tiết theo thời gian với biểu đồ trực quan.

Hệ thống đã được kiểm thử kỹ lưỡng về mặt chức năng, hiệu năng và bảo mật. Kết quả cho thấy hệ thống hoạt động ổn định, đáp ứng tốt yêu cầu về thời gian phản hồi, và đảm bảo tính bảo mật cao thông qua cơ chế Row-Level Security. Khả năng mở rộng của hệ thống cũng được chứng minh thông qua việc hỗ trợ nhiều tenant cùng lúc mà không ảnh hưởng đến hiệu năng.


### 4. Cấu trúc báo cáo

Sau phần Mở đầu giới thiệu về vấn đề nghiên cứu, báo cáo sẽ được trình bày với 04 chương nội dung:

**Chương 1: Cơ sở lý thuyết** – Giới thiệu về các công nghệ và kiến trúc sử dụng trong hệ thống, bao gồm kiến trúc Multi-tenant, nền tảng Android, Kotlin, Jetpack Compose, Supabase, MVVM, Clean Architecture, Row-Level Security, và các Design Patterns quan trọng.

**Chương 2: Phân tích yêu cầu** – Mô tả chi tiết bài toán, phân tích các yêu cầu chức năng và phi chức năng của hệ thống cho từng đối tượng người dùng (khách hàng, nhân viên, quản lý), từ đó đưa ra các ca sử dụng cụ thể với biểu đồ Use Case và đặc tả chi tiết.

**Chương 3: Thiết kế hệ thống** – Trình bày thiết kế kiến trúc tổng thể của hệ thống Multi-tenant, thiết kế cơ sở dữ liệu với mô hình ERD và các bảng dữ liệu chính, thiết kế các luồng nghiệp vụ cho từng chức năng, và thiết kế giao diện người dùng cho cả hai ứng dụng.

**Chương 4: Triển khai, cài đặt và kiểm thử** – Tập trung vào việc trình bày môi trường triển khai, cấu trúc mã nguồn theo Clean Architecture, cách triển khai các chức năng chính, kết quả giao diện thực tế của ứng dụng, và các kết quả kiểm thử về chức năng, hiệu năng, và bảo mật.

Phần **Kết luận** sẽ tổng kết lại những vấn đề được trình bày trong 04 chương nội dung kể trên, những kết quả đạt được của báo cáo cũng như hướng nghiên cứu và phát triển tiếp theo để hoàn thiện hệ thống.

---

## �📄 TÓM TẮT

**Tóm tắt:** Ngày nay, với sự phát triển của công nghệ thông tin, đặc biệt là ứng dụng di động, ngành dịch vụ ăn uống (F&B) đang đối mặt với nhiều thách thức trong việc quản lý và phục vụ khách hàng. Một trong những khía cạnh quan trọng của rất nhiều nhà hàng hiện nay là yêu cầu về việc số hóa quy trình đặt món, quản lý đơn hàng cũng như theo dõi doanh thu. Các phương pháp truyền thống như gọi món qua nhân viên, thanh toán thủ công, và quản lý đơn hàng bằng giấy tờ không chỉ tốn thời gian mà còn dễ xảy ra sai sót. Từ đó, các giải pháp quản lý nhà hàng thông minh được phát triển nhằm giải quyết vấn đề trên. Hiện nay, một số doanh nghiệp có nhiều chi nhánh nhà hàng với các yêu cầu quản lý tập trung đặc thù khiến họ cần một hệ thống có khả năng phục vụ nhiều nhà hàng khác nhau trên cùng một nền tảng. Tuy nhiên, việc tự triển khai và vận hành các hệ thống như vậy đòi hỏi nhiều công sức cũng như những chi phí rất lớn trong quá trình phát triển và bảo trì. Một giải pháp có thể được sử dụng đó là xây dựng hệ thống theo mô hình Multi-tenant, cho phép nhiều nhà hàng sử dụng chung một nền tảng nhưng vẫn đảm bảo tính độc lập và bảo mật dữ liệu. Báo cáo này sẽ tập trung giới thiệu giải pháp xây dựng một Hệ thống Quản lý Nhà hàng và Gọi món theo mô hình Multi-tenant bao gồm các yêu cầu của hệ thống, các công nghệ sử dụng để xây dựng (Android, Kotlin, Jetpack Compose, Supabase), phát triển cũng như quản lý dữ liệu một cách hiệu quả thông qua kiến trúc MVVM và Clean Architecture.

**Từ khóa:** Quản lý nhà hàng, Multi-tenant, Android, Kotlin, Jetpack Compose, Supabase, MVVM, Clean Architecture, Row-Level Security, Realtime Database.

---

## 📅 Lộ trình thực hiện (5 Milestones)

Để đảm bảo tiến độ và chất lượng, quá trình viết báo cáo được chia thành 5 giai đoạn chính (Milestones), với khối lượng công việc được phân bổ đồng đều.

### 🚩 Milestone 1: Khởi động & Cơ sở lý thuyết
**Mục tiêu:** Xây dựng khung sườn báo cáo và hoàn thành nền tảng lý thuyết.

*   **Công việc:**
    *   [ ] **Xây dựng đề cương chi tiết:** Xác định rõ các mục lục con trong từng chương.
    *   [ ] **Viết phần "Mở đầu":**
        *   Trình bày lý do chọn đề tài (Tính cấp thiết, xu hướng Chuyển đổi số F&B).
        *   Mục tiêu và phạm vi của đề tài (Xây dựng mobile app cho Staff/Manager, Backend Supabase).
    *   [ ] **Viết "Chương 1: Cơ sở lý thuyết":**
        *   Nghiên cứu và tổng hợp tài liệu về **Android & Jetpack Compose**.
        *   Tìm hiểu về **Supabase** (Auth, Database, Realtime, Storage).
        *   Lý thuyết về kiến trúc **MVVM** và **Clean Architecture**.

### 🚩 Milestone 2: Phân tích yêu cầu hệ thống
**Mục tiêu:** Xác định rõ bài toán và các yêu cầu chức năng/phi chức năng.

*   **Công việc:**
    *   [ ] **Viết "Chương 2: Phân tích yêu cầu":**
        *   Mô tả bài toán nghiệp vụ hiện tại của nhà hàng và giải pháp đề xuất.
        *   Xác định các tác nhân (Actors): Khách hàng, Nhân viên, Quản lý, Admin hệ thống.
    *   [ ] **Biểu đồ Use Case:** Vẽ và giải thích biểu đồ Use Case tổng quát và chi tiết.
    *   [ ] **Đặc tả Use Case:** Viết kịch bản chi tiết (Scenario) cho các chức năng cốt lõi (Đặt món, Quản lý Menu, Thống kê).
    *   [ ] **Yêu cầu phi chức năng:** Hiệu năng, bảo mật, tính sẵn sàng.

### 🚩 Milestone 3: Thiết kế hệ thống
**Mục tiêu:** Thiết kế kiến trúc, cơ sở dữ liệu và giao diện người dùng.

*   **Công việc:**
    *   [ ] **Viết "Chương 3: Thiết kế hệ thống":**
        *   **Thiết kế kiến trúc:** Sơ đồ kiến trúc tổng thể (Mobile App <-> Supabase), mô hình phân lớp trong App.
        *   **Thiết kế Cơ sở dữ liệu:**
            *   Vẽ biểu đồ ERD (Entity Relationship Diagram).
            *   Mô tả chi tiết các bảng (Tables) và mối quan hệ (Relationships) trong PostgreSQL.
        *   **Thiết kế Giao diện (UI/UX):**
            *   Mô tả luồng người dùng (User Flow).
            *   Đưa vào thiết kế màn hình (Wireframes hoặc Mockups) cho các chức năng chính.

### 🚩 Milestone 4: Triển khai & Kiểm thử
**Mục tiêu:** Mô tả quá trình hiện thực hóa ứng dụng và kiểm chứng kết quả.

*   **Công việc:**
    *   [ ] **Viết "Chương 4: Triển khai, cài đặt và kiểm thử":**
        *   **Môi trường triển khai:** Cấu hình phần cứng, phần mềm, các thư viện sử dụng.
        *   **Hiện thực chức năng:** Trình bày cách cài đặt các tính năng quan trọng (kèm các đoạn code snippets nổi bật, ví dụ: xử lý Realtime orders, logic MVVM).
        *   **Kết quả giao diện:** Chụp ảnh màn hình thực tế của ứng dụng đã chạy.
    *   [ ] **Kiểm thử (Testing):**
        *   Xây dựng các kịch bản kiểm thử (Test Cases).
        *   Trình bày kết quả kiểm thử (Test Results) - Chức năng nào Đạt/Không đạt.

### 🚩 Milestone 5: Hoàn thiện & Tổng kết
**Mục tiêu:** Tổng hợp, đánh giá và hoàn chỉnh hình thức báo cáo.

*   **Công việc:**
    *   [ ] **Viết "Kết luận":**
        *   Tóm tắt kết quả đạt được so với mục tiêu ban đầu.
        *   Hạn chế còn tồn tại.
        *   Hướng phát triển trong tương lai (Mở rộng tính năng, AI, v.v.).
    *   [ ] **Viết các phần còn lại:**
        *   Lời cảm ơn.
        *   Lời cam đoan.
        *   Tóm tắt (Abstract - Tiếng Việt/Tiếng Anh).
    *   [ ] **Tổng hợp "Tài liệu tham khảo":** Liệt kê các nguồn tài liệu, sách, trang web đã tham khảo theo chuẩn APA hoặc IEEE.
    *   [ ] **Rà soát lần cuối:**
        *   Kiểm tra lỗi chính tả, ngữ pháp.
        *   Đánh số trang, mục lục tự động, danh mục hình ảnh/bảng biểu.
        *   Định dạng văn bản (Font, margins) theo quy định của nhà trường.
