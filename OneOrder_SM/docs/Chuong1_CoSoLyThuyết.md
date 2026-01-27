# CHƯƠNG 1. CƠ SỞ LÝ THUYẾT

Trong chương này, báo cáo sẽ giới thiệu về cơ sở lý thuyết và những nền tảng công nghệ để xây dựng Hệ thống Quản lý Nhà hàng và Gọi món theo mô hình Multi-tenant OneOrder, tập trung vào ba khía cạnh: Thứ nhất, giới thiệu về hệ thống quản lý nhà hàng và kiến trúc Multi-tenant. Thứ hai, giới thiệu về các công nghệ phát triển ứng dụng di động trên nền tảng Android. Thứ ba, giới thiệu về các kiến trúc phần mềm và design patterns được sử dụng để đảm bảo chất lượng và hiệu năng của hệ thống.

## 1.1. Hệ thống quản lý nhà hàng

### 1.1.1. Giới thiệu chung về hệ thống quản lý nhà hàng

Hệ thống quản lý nhà hàng (Restaurant Management System - RMS) là một giải pháp phần mềm được thiết kế để tự động hóa và tối ưu hóa các hoạt động vận hành của nhà hàng. Một hệ thống RMS hiện đại thường bao gồm các chức năng cốt lõi như: quản lý đơn hàng, quản lý thực đơn, quản lý bàn ăn, quản lý nhân viên, quản lý kho, và báo cáo thống kê doanh thu.

Trước khi có sự xuất hiện của các hệ thống RMS, việc quản lý nhà hàng chủ yếu dựa vào phương pháp thủ công với giấy tờ và ghi chép. Nhân viên phục vụ ghi nhận đơn hàng trên giấy, sau đó chuyển cho bếp để chuẩn bị. Việc này không chỉ tốn thời gian mà còn dễ xảy ra sai sót trong quá trình truyền đạt thông tin, đặc biệt trong giờ cao điểm khi lượng khách đông. Hơn nữa, việc theo dõi doanh thu, quản lý tồn kho và lập báo cáo tài chính thủ công đòi hỏi nhiều công sức và dễ dẫn đến thiếu chính xác.

Với sự phát triển của công nghệ thông tin, các hệ thống RMS hiện đại đã giải quyết được nhiều vấn đề của phương pháp truyền thống. Việc số hóa quy trình đặt món giúp giảm thiểu sai sót, tăng tốc độ phục vụ, và cải thiện trải nghiệm khách hàng. Các báo cáo thống kê tự động giúp chủ nhà hàng có cái nhìn tổng quan về tình hình kinh doanh, từ đó đưa ra các quyết định chiến lược phù hợp. Ngoài ra, khả năng tích hợp với các hệ thống thanh toán điện tử, quản lý kho, và CRM giúp tối ưu hóa toàn bộ chuỗi giá trị của nhà hàng.

### 1.1.2. Các mô hình triển khai hiện tại

Hiện nay, có ba mô hình triển khai chính cho hệ thống quản lý nhà hàng:

**Mô hình On-Premises (Tại chỗ)**: Trong mô hình này, toàn bộ phần mềm và dữ liệu được lưu trữ trên máy chủ đặt tại nhà hàng. Mô hình này cho phép nhà hàng có toàn quyền kiểm soát dữ liệu và hệ thống, phù hợp với các nhà hàng có yêu cầu bảo mật cao hoặc không có kết nối internet ổn định. Tuy nhiên, chi phí đầu tư ban đầu cho phần cứng và chi phí bảo trì, nâng cấp hệ thống thường rất cao.

**Mô hình Cloud-Based (Đám mây)**: Hệ thống được triển khai trên nền tảng đám mây, dữ liệu được lưu trữ trên các máy chủ từ xa. Mô hình này có ưu điểm là chi phí đầu tư thấp, dễ dàng mở rộng, và có thể truy cập từ bất kỳ đâu có kết nối internet. Nhà hàng chỉ cần trả phí thuê bao theo tháng hoặc theo số lượng người dùng. Tuy nhiên, mô hình này phụ thuộc vào kết nối internet và có thể gặp vấn đề về bảo mật dữ liệu nếu nhà cung cấp không đáng tin cậy.

**Mô hình Hybrid (Kết hợp)**: Mô hình này kết hợp ưu điểm của cả hai mô hình trên, trong đó một số dữ liệu quan trọng được lưu trữ tại chỗ, còn các dữ liệu khác được đồng bộ lên đám mây. Điều này giúp đảm bảo hệ thống vẫn hoạt động được khi mất kết nối internet, đồng thời tận dụng được lợi ích của đám mây như khả năng truy cập từ xa và sao lưu tự động.

## 1.2. Kiến trúc Multi-tenant

### 1.2.1. Giới thiệu tổng quan

Multi-tenant (Đa thuê bao) là một kiến trúc phần mềm trong đó một phiên bản duy nhất của ứng dụng phục vụ cho nhiều khách hàng (tenant) khác nhau. Mỗi tenant là một nhóm người dùng chia sẻ quyền truy cập chung vào phiên bản phần mềm đó với các đặc quyền cụ thể. Trong kiến trúc này, dữ liệu của mỗi tenant được cô lập và bảo vệ, đảm bảo rằng các tenant không thể truy cập hoặc xem dữ liệu của nhau.

Có ba mô hình chính để triển khai kiến trúc Multi-tenant:

**Database riêng biệt (Separate Database)**: Mỗi tenant có một cơ sở dữ liệu riêng. Mô hình này cung cấp mức độ cô lập dữ liệu cao nhất và dễ dàng tuân thủ các quy định về bảo mật. Tuy nhiên, chi phí vận hành và bảo trì cao do phải quản lý nhiều cơ sở dữ liệu.

**Schema riêng biệt (Separate Schema)**: Tất cả các tenant chia sẻ cùng một cơ sở dữ liệu nhưng mỗi tenant có schema riêng. Mô hình này cân bằng giữa chi phí và mức độ cô lập, phù hợp cho các ứng dụng có số lượng tenant vừa phải.

**Bảng chia sẻ (Shared Schema)**: Tất cả các tenant chia sẻ cùng cơ sở dữ liệu và schema. Dữ liệu được phân biệt thông qua một cột định danh tenant (tenant_id) trong mỗi bảng. Mô hình này có chi phí thấp nhất và dễ mở rộng, nhưng đòi hỏi cơ chế bảo mật chặt chẽ để đảm bảo cô lập dữ liệu.

Trong hệ thống OneOrder, em sử dụng mô hình **Shared Schema** kết hợp với cơ chế **Row-Level Security (RLS)** của PostgreSQL để đảm bảo cô lập dữ liệu giữa các tenant một cách hiệu quả và tiết kiệm chi phí.

### 1.2.2. Ưu điểm của kiến trúc Multi-tenant

Kiến trúc Multi-tenant mang lại nhiều lợi ích đáng kể so với mô hình Single-tenant truyền thống:

**Tiết kiệm chi phí**: Việc chia sẻ tài nguyên hạ tầng giữa nhiều tenant giúp giảm đáng kể chi phí phần cứng, phần mềm và vận hành. Thay vì phải triển khai và bảo trì nhiều hệ thống độc lập, nhà cung cấp chỉ cần quản lý một phiên bản duy nhất.

**Dễ dàng bảo trì và nâng cấp**: Khi có cập nhật hoặc sửa lỗi, chỉ cần triển khai một lần cho tất cả các tenant. Điều này giúp đảm bảo tất cả khách hàng luôn sử dụng phiên bản mới nhất của phần mềm.

**Khả năng mở rộng cao**: Hệ thống có thể dễ dàng thêm tenant mới mà không cần thay đổi cơ sở hạ tầng. Tài nguyên được phân bổ động dựa trên nhu cầu thực tế của từng tenant.

**Tối ưu hóa tài nguyên**: Các tenant có thể chia sẻ tài nguyên trong thời gian khác nhau (ví dụ: nhà hàng A đông khách vào buổi trưa, nhà hàng B đông khách vào buổi tối), giúp tận dụng tối đa tài nguyên hệ thống.

**Quản lý tập trung**: Nhà cung cấp có thể giám sát, quản lý và hỗ trợ tất cả các tenant từ một điểm trung tâm, giúp nâng cao chất lượng dịch vụ.

## 1.3. Nền tảng Android và Kotlin

### 1.3.1. Android SDK

Android là hệ điều hành di động mã nguồn mở được phát triển bởi Google, dựa trên nhân Linux. Với thị phần chiếm hơn 70% thị trường smartphone toàn cầu, Android là nền tảng phổ biến nhất cho phát triển ứng dụng di động. Android SDK (Software Development Kit) cung cấp đầy đủ các công cụ, thư viện và API cần thiết để phát triển ứng dụng Android.

Android SDK bao gồm các thành phần chính:

**Android API Libraries**: Tập hợp các thư viện cung cấp chức năng cốt lõi như quản lý giao diện người dùng, truy cập cơ sở dữ liệu, kết nối mạng, xử lý đa phương tiện, và tích hợp với các dịch vụ của Google.

**Android Debug Bridge (ADB)**: Công cụ dòng lệnh cho phép giao tiếp với thiết bị Android, hỗ trợ cài đặt ứng dụng, debug, và truy cập shell của thiết bị.

**Android Emulator**: Máy ảo cho phép chạy và kiểm thử ứng dụng trên máy tính mà không cần thiết bị vật lý.

**Build Tools**: Các công cụ biên dịch mã nguồn thành file APK hoặc AAB để cài đặt trên thiết bị.

Android cũng cung cấp kiến trúc ứng dụng rõ ràng với các thành phần chính như Activity (màn hình), Service (dịch vụ chạy nền), Broadcast Receiver (nhận sự kiện hệ thống), và Content Provider (chia sẻ dữ liệu giữa các ứng dụng).

### 1.3.2. Ngôn ngữ lập trình Kotlin

Kotlin là ngôn ngữ lập trình hiện đại, được JetBrains phát triển và chính thức được Google công nhận là ngôn ngữ ưu tiên cho phát triển Android từ năm 2019. Kotlin được thiết kế để tương thích 100% với Java, cho phép sử dụng tất cả các thư viện Java hiện có, đồng thời cung cấp nhiều tính năng mới giúp code ngắn gọn, an toàn và dễ bảo trì hơn.

Các đặc điểm nổi bật của Kotlin:

**Null Safety**: Kotlin phân biệt rõ ràng giữa kiểu dữ liệu nullable và non-nullable ngay từ khi biên dịch, giúp tránh lỗi NullPointerException - một trong những lỗi phổ biến nhất trong Java.

**Concise Syntax**: Cú pháp ngắn gọn hơn Java đáng kể. Ví dụ, data class trong Kotlin tự động tạo các phương thức equals(), hashCode(), toString() mà không cần viết code thủ công.

**Coroutines**: Hỗ trợ lập trình bất đồng bộ một cách đơn giản và hiệu quả, giúp xử lý các tác vụ nặng mà không làm đơ giao diện người dùng.

**Extension Functions**: Cho phép mở rộng chức năng của các class có sẵn mà không cần kế thừa, giúp code linh hoạt và dễ đọc hơn.

**Smart Casts**: Tự động ép kiểu sau khi kiểm tra, giảm thiểu code boilerplate.

Trong dự án OneOrder, Kotlin được sử dụng để phát triển toàn bộ logic nghiệp vụ của cả hai ứng dụng, tận dụng các tính năng hiện đại của ngôn ngữ để đảm bảo code chất lượng cao và dễ bảo trì.

## 1.4. Jetpack Compose

Jetpack Compose là framework UI declarative (khai báo) hiện đại của Android, được Google giới thiệu chính thức vào năm 2021. Khác với cách tiếp cận truyền thống sử dụng XML để định nghĩa giao diện, Compose cho phép xây dựng UI hoàn toàn bằng code Kotlin, theo phong cách declarative tương tự như React hoặc SwiftUI.

Trong mô hình declarative, thay vì mô tả **cách thức** thay đổi UI (imperative), lập trình viên chỉ cần mô tả **trạng thái** của UI tại một thời điểm. Khi trạng thái thay đổi, Compose tự động cập nhật lại UI tương ứng. Điều này giúp giảm thiểu lỗi và làm cho code dễ hiểu, dễ bảo trì hơn.

Các ưu điểm chính của Jetpack Compose:

**Less Code**: Giảm đáng kể lượng code cần viết so với cách tiếp cận XML truyền thống. Một màn hình phức tạp có thể được xây dựng với ít code hơn 30-40%.

**Intuitive**: API trực quan, dễ học và dễ sử dụng. Các composable function có thể được tái sử dụng và kết hợp linh hoạt.

**Accelerates Development**: Tăng tốc độ phát triển nhờ preview tức thì trong Android Studio, không cần build và chạy ứng dụng để xem thay đổi UI.

**Powerful**: Hỗ trợ đầy đủ các tính năng UI hiện đại như animations, theming, accessibility, và tích hợp tốt với các thư viện Jetpack khác.

Trong hệ thống OneOrder, Jetpack Compose được sử dụng để xây dựng toàn bộ giao diện người dùng của cả hai ứng dụng, giúp tạo ra UI đẹp mắt, mượt mà và nhất quán.

## 1.5. Supabase - Backend as a Service

### 1.5.1. PostgreSQL Database

Supabase là một nền tảng Backend-as-a-Service (BaaS) mã nguồn mở, được xây dựng trên nền tảng PostgreSQL - một trong những hệ quản trị cơ sở dữ liệu quan hệ mạnh mẽ và phổ biến nhất hiện nay. Supabase cung cấp một giải pháp thay thế mã nguồn mở cho Firebase của Google, với đầy đủ các tính năng cần thiết cho phát triển ứng dụng hiện đại.

PostgreSQL là một hệ quản trị cơ sở dữ liệu quan hệ đối tượng (ORDBMS) mã nguồn mở, nổi tiếng với độ tin cậy, tính năng phong phú và hiệu năng cao. PostgreSQL hỗ trợ đầy đủ các tính năng ACID (Atomicity, Consistency, Isolation, Durability), đảm bảo tính toàn vẹn dữ liệu trong mọi tình huống.

Các tính năng nổi bật của PostgreSQL:

**Advanced Data Types**: Hỗ trợ nhiều kiểu dữ liệu phong phú như JSON, JSONB, Array, UUID, và các kiểu dữ liệu tùy chỉnh.

**Full-Text Search**: Tìm kiếm toàn văn bản mạnh mẽ, hỗ trợ nhiều ngôn ngữ.

**Foreign Keys và Constraints**: Đảm bảo tính toàn vẹn tham chiếu giữa các bảng.

**Transactions**: Hỗ trợ giao dịch phức tạp với các mức cô lập khác nhau.

**Extensions**: Có thể mở rộng chức năng thông qua các extension như PostGIS (xử lý dữ liệu địa lý), pg_trgm (tìm kiếm mờ), v.v.

### 1.5.2. Authentication

Supabase cung cấp hệ thống xác thực người dùng đầy đủ và bảo mật, hỗ trợ nhiều phương thức đăng nhập khác nhau:

**Email/Password Authentication**: Phương thức truyền thống với email và mật khẩu, bao gồm các tính năng như xác thực email, đặt lại mật khẩu, và thay đổi email.

**OAuth Providers**: Hỗ trợ đăng nhập qua các nhà cung cấp bên thứ ba như Google, Facebook, GitHub, Twitter, v.v.

**Magic Links**: Đăng nhập không cần mật khẩu thông qua link được gửi qua email.

**Phone Authentication**: Xác thực qua số điện thoại với OTP.

Supabase sử dụng JWT (JSON Web Tokens) để quản lý phiên đăng nhập, cho phép xác thực stateless và dễ dàng mở rộng. Mỗi request từ client đều kèm theo JWT token, server sẽ xác thực token này để xác định người dùng và quyền truy cập.

### 1.5.3. Realtime Database

Một trong những tính năng nổi bật của Supabase là khả năng cập nhật dữ liệu theo thời gian thực (Realtime). Tính năng này được xây dựng dựa trên PostgreSQL's logical replication, cho phép client lắng nghe các thay đổi trong cơ sở dữ liệu và nhận cập nhật ngay lập tức.

Realtime hoạt động thông qua WebSocket, cung cấp kết nối hai chiều giữa client và server. Khi có bất kỳ thay đổi nào trong database (INSERT, UPDATE, DELETE), Supabase sẽ tự động gửi thông báo đến tất cả các client đang subscribe vào bảng đó.

Các use case phổ biến của Realtime:

**Live Updates**: Cập nhật danh sách đơn hàng mới cho nhân viên nhà hàng ngay khi khách đặt món.

**Collaborative Editing**: Nhiều người dùng cùng chỉnh sửa dữ liệu và thấy thay đổi của nhau ngay lập tức.

**Notifications**: Gửi thông báo real-time khi có sự kiện quan trọng xảy ra.

**Live Dashboard**: Cập nhật biểu đồ và số liệu thống kê theo thời gian thực.

Trong hệ thống OneOrder, Realtime được sử dụng để đồng bộ đơn hàng mới từ ứng dụng khách hàng sang ứng dụng nhân viên, giúp nhân viên nhận được thông báo ngay lập tức và xử lý đơn hàng kịp thời.

### 1.5.4. Storage

Supabase Storage cung cấp giải pháp lưu trữ file đơn giản và bảo mật, được xây dựng trên S3-compatible object storage. Storage cho phép upload, download, và quản lý các file như hình ảnh, video, documents một cách dễ dàng.

Các tính năng chính:

**Buckets**: Tổ chức file thành các bucket (thùng chứa) riêng biệt, mỗi bucket có thể có policy bảo mật riêng.

**Access Control**: Kiểm soát quyền truy cập file thông qua Row-Level Security policies, tương tự như database.

**Image Transformation**: Tự động resize, crop, và optimize hình ảnh on-the-fly thông qua URL parameters.

**CDN Integration**: Tích hợp với CDN để phân phối file nhanh chóng trên toàn cầu.

Trong OneOrder, Storage được sử dụng để lưu trữ hình ảnh món ăn, logo nhà hàng, và các file khác, giúp giảm tải cho database và tăng tốc độ tải trang.

## 1.6. Kiến trúc MVVM

MVVM (Model-View-ViewModel) là một kiến trúc phần mềm được thiết kế để tách biệt logic nghiệp vụ khỏi giao diện người dùng, giúp code dễ kiểm thử, bảo trì và mở rộng. MVVM đặc biệt phù hợp với các framework UI declarative như Jetpack Compose.

MVVM bao gồm ba thành phần chính:

**Model**: Đại diện cho dữ liệu và logic nghiệp vụ của ứng dụng. Model có thể là các data class, repository, hoặc các service tương tác với API và database. Model không biết gì về View và ViewModel.

**View**: Là giao diện người dùng, hiển thị dữ liệu và nhận tương tác từ người dùng. Trong Jetpack Compose, View là các composable function. View quan sát (observe) ViewModel và tự động cập nhật khi dữ liệu thay đổi.

**ViewModel**: Là cầu nối giữa Model và View, chứa UI state và xử lý logic presentation. ViewModel nhận sự kiện từ View, xử lý logic nghiệp vụ (có thể gọi đến Model), và cập nhật UI state. ViewModel tồn tại độc lập với vòng đời của View, giúp dữ liệu được giữ lại khi xoay màn hình.

Ưu điểm của MVVM:

**Separation of Concerns**: Tách biệt rõ ràng giữa UI và logic nghiệp vụ, giúp code dễ đọc và bảo trì.

**Testability**: ViewModel có thể được kiểm thử độc lập mà không cần UI, giúp viết unit test dễ dàng hơn.

**Reusability**: ViewModel có thể được tái sử dụng cho nhiều View khác nhau.

**Lifecycle Awareness**: ViewModel tự động xử lý vòng đời của Android component, tránh memory leak.

## 1.7. Clean Architecture

Clean Architecture là một triết lý thiết kế phần mềm được đề xuất bởi Robert C. Martin (Uncle Bob), nhấn mạnh vào việc tách biệt các mối quan tâm (separation of concerns) và độc lập với framework, UI, database, và các thành phần bên ngoài. Mục tiêu của Clean Architecture là tạo ra hệ thống dễ hiểu, linh hoạt, và dễ bảo trì.

Clean Architecture chia ứng dụng thành các tầng (layer) đồng tâm, trong đó các tầng bên trong không phụ thuộc vào các tầng bên ngoài:

**Domain Layer (Tầng nghiệp vụ)**: Là tầng trung tâm, chứa các business logic và entity. Tầng này hoàn toàn độc lập, không phụ thuộc vào bất kỳ framework hay thư viện nào. Domain layer bao gồm:
- **Entities**: Các đối tượng nghiệp vụ cốt lõi (ví dụ: Order, MenuItem, Table).
- **Use Cases**: Các ca sử dụng đại diện cho các hành động nghiệp vụ (ví dụ: PlaceOrderUseCase, UpdateOrderStatusUseCase).

**Data Layer (Tầng dữ liệu)**: Chịu trách nhiệm truy cập dữ liệu từ các nguồn khác nhau như API, database, hoặc cache. Data layer bao gồm:
- **Repository Implementation**: Triển khai các interface được định nghĩa trong Domain layer.
- **Data Sources**: Remote data source (API), local data source (database, cache).
- **Data Models**: Các model dùng để serialize/deserialize dữ liệu từ API hoặc database.

**Presentation Layer (Tầng hiển thị)**: Chứa UI và ViewModel. Tầng này phụ thuộc vào Domain layer thông qua các use case.

Nguyên tắc Dependency Rule: Các phụ thuộc chỉ được chỉ từ ngoài vào trong. Tầng bên trong không được biết gì về tầng bên ngoài. Điều này đảm bảo rằng business logic không bị ảnh hưởng bởi các thay đổi về UI hay database.

Lợi ích của Clean Architecture:

**Independence**: Mỗi tầng độc lập, có thể thay đổi hoặc thay thế mà không ảnh hưởng đến các tầng khác.

**Testability**: Dễ dàng viết unit test cho từng tầng một cách độc lập.

**Flexibility**: Dễ dàng thay đổi framework, database, hoặc UI mà không cần sửa đổi business logic.

**Maintainability**: Code có cấu trúc rõ ràng, dễ hiểu và dễ bảo trì.

## 1.8. Row-Level Security (RLS)

Row-Level Security (RLS) là một tính năng bảo mật mạnh mẽ của PostgreSQL, cho phép kiểm soát quyền truy cập ở mức độ hàng (row) trong bảng dữ liệu. Với RLS, có thể định nghĩa các policy xác định hàng nào người dùng có thể xem, thêm, sửa, hoặc xóa dựa trên các điều kiện cụ thể.

RLS hoạt động bằng cách tự động thêm các điều kiện WHERE vào mọi truy vấn SQL, đảm bảo người dùng chỉ có thể truy cập dữ liệu được phép. Điều này được thực hiện ở tầng database, không phụ thuộc vào application code, giúp tăng cường bảo mật và giảm thiểu lỗi.

Trong kiến trúc Multi-tenant, RLS đóng vai trò quan trọng trong việc cô lập dữ liệu giữa các tenant. Mỗi bảng dữ liệu có một cột `tenant_id` để xác định dữ liệu thuộc về tenant nào. RLS policy sẽ đảm bảo rằng mỗi truy vấn chỉ trả về dữ liệu có `tenant_id` khớp với tenant của người dùng hiện tại.

Ví dụ về RLS policy trong hệ thống OneOrder:

```sql
-- Tạo policy cho bảng orders
CREATE POLICY tenant_isolation_policy ON orders
  USING (tenant_id = auth.jwt() ->> 'tenant_id');
```

Policy này đảm bảo rằng người dùng chỉ có thể xem các đơn hàng thuộc về tenant của họ. Hàm `auth.jwt()` lấy thông tin từ JWT token của người dùng hiện tại.

Lợi ích của RLS:

**Security at Database Level**: Bảo mật được thực thi ngay tại tầng database, không phụ thuộc vào application code.

**Simplified Application Code**: Không cần viết logic filter dữ liệu theo tenant trong mỗi query, database tự động xử lý.

**Reduced Risk**: Giảm thiểu rủi ro lộ lọt dữ liệu do lỗi lập trình.

**Performance**: PostgreSQL optimize RLS policy hiệu quả, không ảnh hưởng đáng kể đến hiệu năng.

## 1.9. Design Patterns

### 1.9.1. Circuit Breaker

Circuit Breaker là một design pattern được sử dụng để xử lý lỗi trong các hệ thống phân tán, đặc biệt khi gọi đến các dịch vụ bên ngoài hoặc remote API. Pattern này hoạt động tương tự như cầu dao điện trong hệ thống điện: khi phát hiện lỗi quá nhiều, nó sẽ "ngắt mạch" để ngăn chặn các request tiếp tục gửi đến dịch vụ đang gặp sự cố.

Circuit Breaker có ba trạng thái:

**Closed (Đóng)**: Trạng thái bình thường, tất cả request được chuyển tiếp đến dịch vụ. Nếu số lượng lỗi vượt quá ngưỡng cho phép, chuyển sang trạng thái Open.

**Open (Mở)**: Circuit breaker chặn tất cả request, trả về lỗi ngay lập tức mà không gọi đến dịch vụ. Sau một khoảng thời gian timeout, chuyển sang trạng thái Half-Open.

**Half-Open (Nửa mở)**: Cho phép một số request thử nghiệm đi qua. Nếu thành công, chuyển về Closed. Nếu thất bại, quay lại Open.

Lợi ích của Circuit Breaker:

- Ngăn chặn cascading failure (lỗi lan truyền) trong hệ thống phân tán
- Giảm tải cho dịch vụ đang gặp sự cố, cho phép nó phục hồi
- Cải thiện trải nghiệm người dùng bằng cách fail fast thay vì chờ timeout
- Cung cấp fallback mechanism khi dịch vụ không khả dụng

### 1.9.2. Retry with Exponential Backoff

Retry with Exponential Backoff là một pattern xử lý lỗi tạm thời (transient failure) bằng cách tự động thử lại request sau khi thất bại, với thời gian chờ tăng dần theo cấp số nhân.

Thuật toán hoạt động như sau:
1. Thực hiện request lần đầu
2. Nếu thất bại, chờ một khoảng thời gian ngắn (ví dụ: 1 giây)
3. Thử lại lần 2, nếu thất bại, chờ gấp đôi thời gian (2 giây)
4. Tiếp tục tăng thời gian chờ theo cấp số nhân (4s, 8s, 16s...)
5. Dừng lại sau một số lần thử tối đa

Thêm vào đó, thường có một yếu tố ngẫu nhiên (jitter) để tránh tình trạng nhiều client cùng retry vào một thời điểm, gây quá tải cho server.

Pattern này đặc biệt hữu ích cho các lỗi tạm thời như:
- Network timeout
- Database connection pool exhausted
- Rate limiting
- Server tạm thời quá tải

### 1.9.3. Idempotency

Idempotency là một tính chất quan trọng trong thiết kế API, đảm bảo rằng thực hiện cùng một thao tác nhiều lần cho kết quả giống như thực hiện một lần. Điều này đặc biệt quan trọng trong các hệ thống phân tán, nơi network có thể không ổn định và request có thể bị gửi lại nhiều lần.

Trong hệ thống OneOrder, idempotency được triển khai cho chức năng đặt món:

1. Client tạo một `idempotency_key` duy nhất (UUID) cho mỗi đơn hàng
2. Gửi request kèm theo `idempotency_key`
3. Server kiểm tra xem `idempotency_key` đã tồn tại chưa
4. Nếu đã tồn tại, trả về kết quả của request trước đó
5. Nếu chưa, xử lý request và lưu `idempotency_key` cùng kết quả

Điều này đảm bảo rằng nếu người dùng vô tình nhấn nút "Đặt món" nhiều lần, hoặc network retry tự động, hệ thống sẽ không tạo ra nhiều đơn hàng trùng lặp.

### 1.9.4. Queue-based Load Leveling

Queue-based Load Leveling là một pattern sử dụng hàng đợi (queue) làm buffer giữa client và service, giúp cân bằng tải và xử lý burst traffic (lưu lượng tăng đột ngột).

Thay vì xử lý request đồng bộ ngay lập tức, pattern này hoạt động như sau:

1. Client gửi request vào queue
2. Trả về acknowledgment ngay lập tức cho client
3. Worker processes lấy request từ queue và xử lý với tốc độ ổn định
4. Kết quả được gửi lại cho client qua callback hoặc polling

Lợi ích:
- **Smooth out traffic spikes**: Hệ thống có thể xử lý burst traffic mà không bị quá tải
- **Decouple components**: Client và service không cần biết về nhau
- **Reliability**: Request không bị mất ngay cả khi service tạm thời down
- **Scalability**: Có thể thêm worker để tăng throughput

Trong OneOrder, pattern này có thể được áp dụng cho việc xử lý đơn hàng trong giờ cao điểm, đảm bảo hệ thống vẫn phản hồi nhanh cho khách hàng ngay cả khi có nhiều đơn hàng cùng lúc.

## 1.10. Tổng kết chương

Chương 1 đã trình bày về cơ sở lý thuyết và các công nghệ được sử dụng để xây dựng Hệ thống Quản lý Nhà hàng và Gọi món theo mô hình Multi-tenant OneOrder. Các nội dung chính bao gồm:

- Giới thiệu về hệ thống quản lý nhà hàng và các mô hình triển khai hiện tại
- Kiến trúc Multi-tenant và lợi ích của nó trong việc phục vụ nhiều nhà hàng trên cùng một nền tảng
- Nền tảng Android và ngôn ngữ Kotlin - công nghệ cốt lõi để phát triển ứng dụng di động
- Jetpack Compose - framework UI declarative hiện đại
- Supabase - nền tảng BaaS cung cấp database, authentication, realtime, và storage
- Kiến trúc MVVM và Clean Architecture - đảm bảo code có cấu trúc tốt, dễ bảo trì và kiểm thử
- Row-Level Security - cơ chế bảo mật quan trọng để cô lập dữ liệu giữa các tenant
- Các Design Patterns (Circuit Breaker, Retry, Idempotency, Queue-based Load Leveling) - đảm bảo hệ thống hoạt động ổn định và tin cậy

Từ những kiến thức nền tảng này, chương tiếp theo sẽ phân tích chi tiết các yêu cầu của hệ thống OneOrder, xác định rõ bài toán cần giải quyết và đưa ra các ca sử dụng cụ thể cho từng nhóm đối tượng người dùng.
