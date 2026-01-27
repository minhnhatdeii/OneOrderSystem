# 🎤 BÀI THUYẾT TRÌNH HỆ THỐNG ONEORDER
## *Hệ thống Quản lý Nhà hàng và Gọi món theo mô hình Multi-tenant*

---

# MỞ ĐẦU

> "Xin chào thầy/cô và các bạn. Hôm nay em xin trình bày đề tài **Xây dựng Hệ thống Quản lý Nhà hàng và Gọi món theo mô hình Multi-tenant OneOrder**."
>
> "Bài thuyết trình gồm 4 phần:
> 1. Giới thiệu bài toán
> 2. Kiến trúc tổng thể hệ thống
> 3. Các Pattern tối ưu cho ứng dụng
> 4. Demo ứng dụng"

---

# PHẦN 1: GIỚI THIỆU BÀI TOÁN

---

## SLIDE 1.1: Vấn đề thực tế

> "Trước tiên, em xin đặt câu hỏi: **Khi đến nhà hàng, các bạn đã bao giờ gặp tình huống** muốn gọi món nhưng phải chờ nhân viên đến, hoặc đặt món rồi nhưng không biết bao giờ mới được phục vụ?"

**[SLIDE hiển thị]**
```
❌ VẤN ĐỀ NHÀ HÀNG TRUYỀN THỐNG:
• Ghi order bằng giấy → Sai sót, nhầm món
• Truyền đạt thủ công → Chậm trễ giờ cao điểm
• Không theo dõi được trạng thái đơn hàng
• Thống kê doanh thu bằng Excel → Thiếu chính xác
```

> "Đây là những vấn đề rất phổ biến. Từ góc nhìn quản lý, việc ghi chép thủ công tốn thời gian và dễ sai sót, đặc biệt trong giờ cao điểm."

---

## SLIDE 1.2: Nhu cầu chuyển đổi số

> "Chính vì vậy, ngành F&B đang có xu hướng **chuyển đổi số mạnh mẽ**. Khách hàng muốn tự đặt món nhanh chóng. Nhà hàng muốn quản lý tập trung, theo dõi realtime."

**[SLIDE hiển thị]**
```
✅ NHU CẦU HIỆN NAY:
• Khách hàng: Đặt món nhanh, không chờ đợi
• Nhà hàng: Quản lý đơn hàng realtime
• Chuỗi nhà hàng: Một hệ thống cho nhiều chi nhánh
```

> "Và đặc biệt, với các **chuỗi nhà hàng**, họ cần một hệ thống có thể phục vụ nhiều cơ sở trên cùng một nền tảng. Đây chính là bài toán **Multi-tenant** mà em sẽ giải quyết trong đề tài này."

---

## SLIDE 1.3: Giải pháp OneOrder

> "Từ những vấn đề trên, em đã xây dựng hệ thống **OneOrder** với các mục tiêu sau:"

**[SLIDE hiển thị]**
```
🎯 MỤC TIÊU ONEORDER:

┌─────────────────────┬──────────────────────────────────────┐
│ Vấn đề              │ Giải pháp                            │
├─────────────────────┼──────────────────────────────────────┤
│ Nhiều nhà hàng      │ Multi-tenant với Row-Level Security  │
│ Đặt món chờ lâu     │ Quét QR → Xem menu → Đặt ngay        │
│ Không biết tiến độ  │ Realtime cập nhật qua WebSocket      │
│ Bảo mật dữ liệu     │ Cô lập 100% giữa các tenant          │
└─────────────────────┴──────────────────────────────────────┘
```

> "Điểm khác biệt của OneOrder so với các ứng dụng thông thường là **khả năng phục vụ nhiều nhà hàng** trên cùng một hệ thống, nhưng vẫn **đảm bảo dữ liệu cô lập hoàn toàn**."

---

## SLIDE 1.4: Đối tượng sử dụng

> "Hệ thống phục vụ **3 nhóm đối tượng** chính:"

**[SLIDE hiển thị]**
```
👤 KHÁCH HÀNG           👨‍🍳 NHÂN VIÊN           👔 QUẢN LÝ
═══════════════         ═══════════════        ═══════════════
• Quét QR bàn           • Nhận đơn realtime    • Quản lý menu
• Xem thực đơn          • Cập nhật trạng thái  • Quản lý bàn + QR
• Đặt món               • Quản lý bàn          • Quản lý nhân viên
• Theo dõi đơn          • Thanh toán           • Xem thống kê
```

> "Mỗi nhóm có một luồng nghiệp vụ riêng, và em đã phân tích chi tiết hơn 15 Use Cases trong báo cáo."

---

# PHẦN 2: KIẾN TRÚC TỔNG THỂ HỆ THỐNG

> "Bây giờ em xin trình bày về **kiến trúc hệ thống**. Đây là phần quan trọng nhất để hiểu cách OneOrder giải quyết bài toán Multi-tenant."

---

## SLIDE 2.1: Kiến trúc Multi-tenant

> "Đầu tiên, em xin giải thích **Multi-tenant là gì**. Tưởng tượng OneOrder như một **tòa chung cư** - nhiều gia đình sống chung nhưng mỗi nhà có khóa riêng, không ai vào được nhà của nhau."

**[SLIDE hiển thị]**
```
KIẾN TRÚC MULTI-TENANT: Shared Database + RLS

┌────────────────────────────────────────────────────────────┐
│                   SUPABASE POSTGRESQL                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                 SHARED DATABASE                       │  │
│  │                                                       │  │
│  │  ┌──────────────┬──────────────┬──────────────────┐  │  │
│  │  │ Nhà hàng A   │ Nhà hàng B   │ Nhà hàng C       │  │  │
│  │  │ tenant_id=1  │ tenant_id=2  │ tenant_id=3      │  │  │
│  │  └──────────────┴──────────────┴──────────────────┘  │  │
│  │                          │                            │  │
│  │                          ▼                            │  │
│  │           ┌─────────────────────────────┐             │  │
│  │           │    ROW-LEVEL SECURITY       │             │  │
│  │           │  "CHỈ THẤY DỮ LIỆU CỦA MÌNH"│             │  │
│  │           └─────────────────────────────┘             │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

> "Em chọn mô hình **Shared Database, Shared Schema** - tất cả tenant dùng chung một database, nhưng **Row-Level Security** của PostgreSQL sẽ tự động lọc dữ liệu. Nhà hàng A chỉ thấy dữ liệu của A, không bao giờ thấy được dữ liệu của B."

---

## SLIDE 2.2: Ưu điểm Multi-tenant

> "Tại sao em lại chọn kiến trúc này?"

**[SLIDE hiển thị]**
```
✅ ƯU ĐIỂM CỦA MÔ HÌNH SHARED DATABASE + RLS:

1. CHI PHÍ THẤP
   → 1 database phục vụ hàng trăm nhà hàng
   → Không cần tạo database riêng cho mỗi tenant

2. DỄ BẢO TRÌ
   → Cập nhật 1 lần, áp dụng cho tất cả
   → Sửa lỗi nhanh chóng

3. BẢO MẬT MẠNH MẼ
   → RLS hoạt động ở tầng database
   → Không phụ thuộc vào code ứng dụng
   → Kiểm thử cho thấy: 100% cô lập dữ liệu ✓
```

> "So với việc tạo database riêng cho mỗi nhà hàng, cách này **tiết kiệm chi phí đáng kể** và **dễ bảo trì hơn**. Quan trọng là bảo mật được thực thi ngay tại tầng database, không phụ thuộc vào code."

---

## SLIDE 2.3: Kiến trúc ứng dụng Mobile

> "Tiếp theo là kiến trúc của ứng dụng Android. Em áp dụng **Clean Architecture** kết hợp **MVVM**."

**[SLIDE hiển thị]**
```
CLEAN ARCHITECTURE + MVVM

┌───────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                       │
│  ┌────────────────┐    ┌────────────────────────────────┐ │
│  │ Jetpack Compose│ ←→ │ ViewModel + StateFlow          │ │
│  │     (UI)       │    │ (Quản lý trạng thái màn hình)  │ │
│  └────────────────┘    └────────────────────────────────┘ │
├───────────────────────────────────────────────────────────┤
│                     DOMAIN LAYER                           │
│  ┌───────────────────────────────────────────────────────┐│
│  │ Use Cases + Entities + Repository Interfaces          ││
│  │ (Logic nghiệp vụ thuần túy, không phụ thuộc framework)││
│  └───────────────────────────────────────────────────────┘│
├───────────────────────────────────────────────────────────┤
│                       DATA LAYER                           │
│  ┌────────────────┐    ┌────────────────────────────────┐ │
│  │ Supabase SDK   │ ←→ │ Room Database                  │ │
│  │   (Remote)     │    │    (Local Cache)               │ │
│  └────────────────┘    └────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

> "Kiến trúc này chia ứng dụng thành **3 tầng rõ ràng**:
> - **Presentation**: Giao diện và ViewModel
> - **Domain**: Logic nghiệp vụ, hoàn toàn độc lập
> - **Data**: Truy cập dữ liệu từ Supabase và Room"

---

## SLIDE 2.4: Ưu điểm Clean Architecture

> "Tại sao em chọn Clean Architecture?"

**[SLIDE hiển thị]**
```
✅ ƯU ĐIỂM TRONG HỆ THỐNG ONEORDER:

1. TÁCH BIỆT TRÁCH NHIỆM
   → UI chỉ lo hiển thị
   → Logic nghiệp vụ nằm riêng trong Domain
   → Data layer có thể đổi từ Supabase sang Firebase mà không ảnh hưởng

2. DỄ KIỂM THỬ
   → Mock Repository để test ViewModel
   → Test Domain layer độc lập

3. LINH HOẠT ĐỔI NGUỒN DỮ LIỆU
   → Ví dụ: Khi mất mạng, tự động lấy từ Room (offline)
   → Khi có mạng, sync với Supabase (online)
```

> "Trong OneOrder, em đã áp dụng điều này khi xây dựng module thống kê - dữ liệu có thể được cache trong Room để hiển thị ngay cả khi offline."

---

## SLIDE 2.5: Stack Công nghệ

> "Đây là các công nghệ chính em sử dụng:"

**[SLIDE hiển thị]**
```
┌────────────────┬──────────────────┬──────────────────────────┐
│ Thành phần     │ Công nghệ        │ Lý do chọn               │
├────────────────┼──────────────────┼──────────────────────────┤
│ Language       │ Kotlin           │ Null-safety, Coroutines  │
│ UI             │ Jetpack Compose  │ Declarative, Material 3  │
│ DI             │ Hilt             │ Android-first DI         │
│ Backend        │ Supabase         │ Auth+DB+Realtime+Storage │
│ Database       │ PostgreSQL + RLS │ Multi-tenant isolation   │
│ Async          │ Coroutines+Flow  │ Reactive programming     │
│ Local Cache    │ Room             │ Offline support          │
│ Charts         │ Vico             │ Biểu đồ thống kê         │
│ QR             │ ZXing            │ Sinh/quét mã QR          │
└────────────────┴──────────────────┴──────────────────────────┘
```

> "Điểm đặc biệt là **Supabase** - một nền tảng Backend-as-a-Service cung cấp đủ 4 thứ em cần: Authentication, Database, Realtime và Storage. Quan trọng hơn, PostgreSQL của Supabase hỗ trợ RLS - đây chính là chìa khóa cho Multi-tenant."

---

## SLIDE 2.6: Luồng Realtime

> "Một trong những tính năng quan trọng nhất là **cập nhật đơn hàng realtime**. Khi khách đặt món, nhân viên nhận được thông báo **ngay lập tức**, không cần F5."

**[SLIDE hiển thị]**
```
LUỒNG REALTIME TRONG ONEORDER:

   📱 Khách hàng                          📱 Nhân viên
        │                                      ↑
        │ Đặt món                              │ Nhận thông báo
        ▼                                      │ (< 1 giây)
   ┌─────────┐      ┌───────────────┐    ┌─────────────┐
   │ INSERT  │ ──→  │   Supabase    │ ──→│  WebSocket  │
   │ orders  │      │   Realtime    │    │    Push     │
   └─────────┘      └───────────────┘    └─────────────┘
```

> "Công nghệ WebSocket giúp tạo kết nối hai chiều. Kết quả kiểm thử cho thấy **độ trễ dưới 1 giây** - tức là khách vừa nhấn đặt món, nhân viên đã thấy ngay."

---

# PHẦN 3: CÁC PATTERN TỐI ƯU CHO ỨNG DỤNG

> "Bây giờ em xin trình bày về các **Design Patterns** đã áp dụng. Đây là những pattern giúp hệ thống hoạt động **ổn định và tin cậy**."

---

## SLIDE 3.1: Row-Level Security (RLS)

> "Pattern đầu tiên và quan trọng nhất là **Row-Level Security**."

**[SLIDE hiển thị]**
```
VẤN ĐỀ: Làm sao cô lập dữ liệu giữa các nhà hàng?

GIẢI PHÁP: RLS - Bảo mật ở tầng Database

┌─────────────────────────────────────────────────────────┐
│                    CÁCH RLS HOẠT ĐỘNG                    │
│                                                          │
│  User đăng nhập → JWT chứa user_id                      │
│                      │                                   │
│                      ▼                                   │
│  Truy vấn menu_items → RLS kiểm tra:                    │
│                      │                                   │
│       ┌──────────────┴──────────────┐                   │
│       ▼                              ▼                   │
│  get_user_tenant_id()         tenant_id của record      │
│       │                              │                   │
│       └──────────┬───────────────────┘                  │
│                  ▼                                       │
│            BẰNG NHAU?                                    │
│           /         \                                    │
│         CÓ           KHÔNG                               │
│         ▼              ▼                                 │
│    ✓ Cho xem       ✗ Ẩn đi                              │
└─────────────────────────────────────────────────────────┘
```

> "Mỗi lần user truy vấn, database tự động kiểm tra `tenant_id`. Đây là **bảo mật ở tầng database**, nghĩa là **dù code app có lỗi, dữ liệu vẫn được bảo vệ**."

---

## SLIDE 3.2: RLS trong OneOrder

> "Em đã triển khai RLS như sau:"

**[SLIDE hiển thị]**
```sql
-- Helper Functions
get_user_tenant_id()    -- Lấy tenant của user hiện tại
is_tenant_manager()     -- Kiểm tra có phải manager không

-- Policy cho bảng menu_items
SELECT: tenant_id = get_user_tenant_id()
        → Nhân viên chỉ xem menu nhà hàng mình

INSERT: is_tenant_manager(tenant_id)
        → Chỉ Manager mới được thêm món

UPDATE: is_tenant_manager(tenant_id)
        → Chỉ Manager mới được sửa món

DELETE: is_tenant_manager(tenant_id)
        → Chỉ Manager mới được xóa món
```

> "Với policy này:
> - **Staff** có thể xem menu nhưng không thể sửa
> - **Manager** có toàn quyền với menu của tenant mình
> - **Nhà hàng A không thể xem hay sửa menu nhà hàng B**"

**Kết quả kiểm thử:** *100% cô lập dữ liệu, không có trường hợp xem được dữ liệu tenant khác.*

---

## SLIDE 3.3: Idempotency Pattern

> "Pattern thứ hai giải quyết một vấn đề thực tế: **Mạng không ổn định khi đặt món**."

**[SLIDE hiển thị]**
```
VẤN ĐỀ: Khách nhấn "Đặt món" → Mạng chập chờn
        → Request gửi 2 lần → Tạo 2 đơn hàng trùng!

GIẢI PHÁP: IDEMPOTENCY KEY

┌──────────────────────────────────────────────────────────┐
│  1. Client tạo UUID mới: "abc-123-xyz"                   │
│                                                          │
│  2. Gửi request kèm idempotency_key                      │
│                                                          │
│  3. Server kiểm tra:                                     │
│     ┌─────────────────────────────────────────────────┐  │
│     │ SELECT * FROM orders                             │  │
│     │ WHERE idempotency_key = 'abc-123-xyz'           │  │
│     └─────────────────────────────────────────────────┘  │
│                                                          │
│  4. Kết quả:                                             │
│     • Đã tồn tại → Trả về đơn cũ (không tạo mới)       │
│     • Chưa có    → Tạo đơn mới, lưu key                │
└──────────────────────────────────────────────────────────┘
```

> "Với pattern này, **dù người dùng nhấn nút đặt món 10 lần, hệ thống cũng chỉ tạo 1 đơn hàng**. Đây là điều rất quan trọng trong ứng dụng thương mại."

---

## SLIDE 3.4: Circuit Breaker Pattern

> "Pattern thứ ba xử lý tình huống **API lỗi liên tục**."

**[SLIDE hiển thị]**
```
VẤN ĐỀ: Server Supabase bị lỗi
        → App liên tục gọi API → Chậm, tốn pin, UX tệ

GIẢI PHÁP: CIRCUIT BREAKER - "CẦU DAO TỰ NGẮT"

        ┌─────────────┐
        │   CLOSED    │ ← Hoạt động bình thường
        │ (Đóng mạch) │
        └──────┬──────┘
               │ Lỗi vượt ngưỡng (VD: 5 lần liên tiếp)
               ▼
        ┌─────────────┐
        │    OPEN     │ ← Chặn ngay, không gọi API
        │ (Ngắt mạch) │   → Trả lỗi ngay lập tức
        └──────┬──────┘
               │ Sau 30 giây
               ▼
        ┌─────────────┐
        │ HALF-OPEN   │ ← Thử 1 request
        │(Nửa đóng)   │
        └──────┬──────┘
               │
       ┌───────┴────────┐
       ▼                ▼
    Thành công       Thất bại
       ↓                ↓
    CLOSED           OPEN
```

> "Giống như cầu dao điện tự động ngắt khi quá tải. Thay vì đợi timeout 30 giây cho mỗi request, hệ thống **fail fast** - báo lỗi ngay để người dùng biết, đồng thời cho server thời gian phục hồi."

---

## SLIDE 3.5: Retry with Exponential Backoff

> "Pattern cuối cùng xử lý **lỗi tạm thời**."

**[SLIDE hiển thị]**
```
VẤN ĐỀ: Lỗi ngắn hạn (mất kết nối tạm thời, rate limit)
        → Thử lại ngay có thể tiếp tục lỗi

GIẢI PHÁP: RETRY VỚI DELAY TĂNG DẦN

   Lần 1 fail → Chờ 1 giây  → Retry
   Lần 2 fail → Chờ 2 giây  → Retry
   Lần 3 fail → Chờ 4 giây  → Retry
   Lần 4 fail → Chờ 8 giây  → Retry
   ...
   Max 5 lần → Thông báo lỗi cho user

   + JITTER: Thêm random 0-500ms
     → Tránh nhiều client retry cùng lúc
     → Không làm server quá tải
```

> "Trong OneOrder, khi tải thống kê bị lỗi, hệ thống sẽ **tự động thử lại** với delay tăng dần. Người dùng không cần làm gì, hệ thống tự xử lý."

---

## SLIDE 3.6: Tổng hợp Patterns

> "Tổng kết lại, đây là cách các pattern phối hợp với nhau:"

**[SLIDE hiển thị]**
```
┌─────────────────────────────────────────────────────────────┐
│                    CÁC PATTERN ĐÃ ÁP DỤNG                   │
├──────────────────┬─────────────────┬────────────────────────┤
│ Pattern          │ Vấn đề          │ Kết quả trong OneOrder │
├──────────────────┼─────────────────┼────────────────────────┤
│ RLS              │ Cô lập tenant   │ 100% bảo mật dữ liệu   │
│ Idempotency      │ Duplicate order │ 0 đơn hàng trùng lặp   │
│ Circuit Breaker  │ API liên tục lỗi│ Fail fast, UX tốt hơn  │
│ Retry + Backoff  │ Lỗi tạm thời    │ Tự phục hồi không cần  │
│                  │                 │ user can thiệp         │
│ Repository       │ Coupling code   │ Dễ test, dễ maintain   │
│ MVVM             │ UI-Logic mix    │ Tách biệt, reactive    │
└──────────────────┴─────────────────┴────────────────────────┘
```

> "Các pattern này không đứng riêng lẻ mà **phối hợp với nhau**:
> - RLS bảo vệ dữ liệu ở tầng database
> - Idempotency đảm bảo tính toàn vẹn khi gọi API
> - Circuit Breaker + Retry giúp hệ thống chịu lỗi tốt hơn
> - Repository + MVVM giúp code dễ bảo trì"

---

# PHẦN 4: DEMO ỨNG DỤNG

> "Bây giờ em xin demo trực tiếp hệ thống. Em sẽ demo các luồng chính:"

---

## SLIDE 4.1: Các luồng demo

**[SLIDE hiển thị]**
```
DEMO 1: LUỒNG KHÁCH HÀNG (OneOrder App)
────────────────────────────────────────
1. Đăng nhập → Email/Password
2. Quét QR bàn → Camera scan
3. Xem thực đơn → Danh sách món theo category
4. Thêm giỏ hàng → Chọn số lượng, ghi chú
5. Đặt món → Submit order
6. Theo dõi trạng thái → Realtime update

DEMO 2: LUỒNG NHÂN VIÊN (OneOrder_SM App)
─────────────────────────────────────────
1. Đăng nhập Staff → Dashboard
2. Nhận thông báo đơn mới → Realtime
3. Xem chi tiết đơn → Danh sách món, bàn
4. Cập nhật trạng thái → PENDING → PREPARING → SERVED → PAID
```

*[Thực hiện demo trên thiết bị/emulator]*

---

## SLIDE 4.2: Demo Realtime

**[SLIDE hiển thị]**
```
DEMO REALTIME: MỞ 2 THIẾT BỊ SONG SONG

┌────────────────────┐      ┌────────────────────┐
│  📱 THIẾT BỊ 1     │      │  📱 THIẾT BỊ 2     │
│  (Khách hàng)      │      │  (Nhân viên)       │
├────────────────────┤      ├────────────────────┤
│                    │      │                    │
│  Nhấn "Đặt món"    │ ──►  │  Nhận thông báo   │
│                    │      │  "Đơn hàng mới!"   │
│                    │  < 1 giây                 │
└────────────────────┘      └────────────────────┘
```

*[Demo với 2 emulator: 1 Customer, 1 Staff - đặt món và xem Realtime notification]*

---

## SLIDE 4.3: Demo RLS Security

**[SLIDE hiển thị]**
```
DEMO BẢO MẬT: LOGIN 2 TÀI KHOẢN KHÁC TENANT

┌────────────────────┐      ┌────────────────────┐
│  👤 Nhà hàng A     │      │  👤 Nhà hàng B     │
├────────────────────┤      ├────────────────────┤
│  Xem menu          │      │  Xem menu          │
│  ↓                 │      │  ↓                 │
│  Thấy: Phở, Bún    │      │  Thấy: Pizza, Pasta│
│  (Menu nhà hàng A) │      │  (Menu nhà hàng B) │
│                    │      │                    │
│  ❌ KHÔNG THẤY     │      │  ❌ KHÔNG THẤY     │
│  Pizza, Pasta      │      │  Phở, Bún          │
└────────────────────┘      └────────────────────┘
```

*[Demo với 2 tài khoản staff của 2 tenant khác nhau]*

---

# KẾT LUẬN

> "Qua đề tài này, em đã:"

**[SLIDE hiển thị]**
```
✅ KẾT QUẢ ĐẠT ĐƯỢC:

• Xây dựng thành công hệ thống Multi-tenant với RLS
  → Kiểm thử: 100% cô lập dữ liệu

• Triển khai Realtime với độ trễ < 1 giây
  → Đơn hàng mới thông báo ngay lập tức

• Áp dụng các Design Patterns để tối ưu
  → Idempotency: Không trùng đơn
  → Circuit Breaker + Retry: Chịu lỗi tốt

• Kiểm thử 17 ca chức năng - tất cả PASS
```

---

## Hướng phát triển

**[SLIDE hiển thị]**
```
🚀 HƯỚNG PHÁT TRIỂN:

NGẮN HẠN:
• Tích hợp Firebase Cloud Messaging (Push notification)
• Cải thiện offline mode

TRUNG HẠN:
• Tích hợp thanh toán VNPay/MoMo
• Báo cáo PDF/Excel

DÀI HẠN:
• AI gợi ý món ăn
• Tích hợp GrabFood, ShopeeFood
```

> "Em xin kết thúc bài thuyết trình tại đây. Xin cảm ơn thầy/cô và các bạn đã lắng nghe. Em sẵn sàng trả lời các câu hỏi."

---

# 📋 CHECKLIST CHUẨN BỊ DEMO

```
□ 2 emulator/thiết bị Android (cho demo Realtime)
□ Tài khoản Customer đã tạo sẵn
□ Tài khoản Staff và Manager đã tạo sẵn
□ 2 tài khoản khác tenant (cho demo RLS)
□ Kết nối internet ổn định
□ Supabase project đang hoạt động
□ Có sẵn menu items và orders để demo
```

---

# ⏱️ THỜI GIAN GỢI Ý

| Phần | Nội dung | Thời gian |
|------|----------|-----------|
| Mở đầu | Giới thiệu đề tài | 1 phút |
| Phần 1 | Giới thiệu bài toán | 3-4 phút |
| Phần 2 | Kiến trúc tổng thể | 5-6 phút |
| Phần 3 | Các Pattern tối ưu | 5-6 phút |
| Phần 4 | Demo ứng dụng | 5-7 phút |
| Kết luận | Tổng kết + Q&A | 2-3 phút |
| **Tổng** | | **21-27 phút** |
