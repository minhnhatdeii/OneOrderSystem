# OneOrder_SM Implementation Plan
## Phân chia công việc qua 5 Milestone chính

---

## 📋 Tổng quan dự án

**Ứng dụng**: OneOrder_SM (Staff/Manager Application)  
**Mục đích**: Quản lý đơn hàng, thực đơn, bàn, nhân viên và thống kê cho nhà hàng  
**Tech Stack**: 
- Frontend: Kotlin + Jetpack Compose
- Backend: Supabase (PostgreSQL + Realtime + Auth + Storage)
- Architecture: MVVM + Clean Architecture

---

## 🎯 Milestone 1: Foundation & Authentication
**Thời gian ước tính**: 1-2 tuần  
**Mục tiêu**: Thiết lập cơ sở hạ tầng và hệ thống xác thực

### 1.1 Backend Setup (Supabase)
> **Lưu ý**: Sử dụng Supabase project hiện có từ OneOrder (cả 2 apps dùng chung backend)

- [ ] **Kết nối với Supabase project hiện có**
  - [ ] Lấy Supabase URL và anon key từ project OneOrder
  - [ ] Verify project settings và quotas
  - [ ] Backup database trước khi thay đổi
  
- [ ] **Verify Database Schema**
  - [ ] Kiểm tra bảng `profiles` đã có field `role` (customer, staff, manager)
  - [ ] Kiểm tra bảng `categories` và `menu_items`
  - [ ] Kiểm tra bảng `tables` với QR code support
  - [ ] Kiểm tra bảng `orders` và `order_items`
  - [ ] Kiểm tra bảng `idempotency_keys`
  - [ ] Nếu thiếu bảng nào, tạo theo schema trong `database_design.md`
  
- [ ] **Thiết lập RLS Policies cho Staff/Manager**
  - [ ] **Orders table**:
    - [ ] Staff/Manager: SELECT all orders
    - [ ] Staff/Manager: UPDATE order status
    - [ ] Customer: SELECT own orders only
  - [ ] **Menu tables** (categories, menu_items):
    - [ ] Manager: Full CRUD access
    - [ ] Staff: SELECT only
    - [ ] Customer: SELECT only (where is_available = true)
  - [ ] **Tables table**:
    - [ ] Manager: Full CRUD access
    - [ ] Staff: SELECT and UPDATE status
    - [ ] Customer: SELECT only
  - [ ] **Profiles table**:
    - [ ] Manager: SELECT all, UPDATE staff/manager profiles
    - [ ] Staff: SELECT own profile, UPDATE own profile
    - [ ] Customer: SELECT own profile only
    
- [ ] **Tạo/Verify Database Functions**
  - [ ] Function tự động tạo profile khi user đăng ký (nếu chưa có)
  - [ ] Trigger cập nhật `updated_at` timestamp
  - [ ] Function kiểm tra idempotency key
  - [ ] **Function mới cho Staff/Manager**:
    - [ ] `get_order_statistics(date_range)` - Thống kê orders
    - [ ] `get_revenue_by_date(start_date, end_date)` - Doanh thu
    - [ ] `get_popular_items(limit)` - Món phổ biến
    
- [ ] **Setup Storage Buckets** (nếu chưa có)
  - [ ] Bucket `menu-images` cho hình ảnh món ăn
  - [ ] Bucket `category-images` cho hình ảnh categories
  - [ ] Storage policies: Manager upload/delete, others read-only

### 1.2 Android Project Setup
- [ ] Tạo Android project với Kotlin
- [ ] Cấu hình Gradle dependencies
  - [ ] Jetpack Compose
  - [ ] Supabase Kotlin SDK
  - [ ] Hilt (Dependency Injection)
  - [ ] Navigation Compose
  - [ ] Coil (Image loading)
  - [ ] Kotlinx Serialization
- [ ] Thiết lập project structure
  ```
  app/
  ├── data/
  │   ├── model/
  │   ├── repository/
  │   └── remote/
  ├── domain/
  │   ├── model/
  │   ├── repository/
  │   └── usecase/
  ├── ui/
  │   ├── screens/
  │   ├── components/
  │   └── theme/
  └── di/
  ```
- [ ] Cấu hình Hilt modules
- [ ] Setup Supabase client singleton

### 1.3 Authentication Implementation
- [ ] **Data Layer**
  - [ ] Tạo `AuthRepository` interface
  - [ ] Implement `AuthRepositoryImpl` với Supabase Auth
  - [ ] Tạo data models: `User`, `Profile`
- [ ] **Domain Layer**
  - [ ] Tạo use cases:
    - [ ] `LoginUseCase`
    - [ ] `LogoutUseCase`
    - [ ] `GetCurrentUserUseCase`
    - [ ] `CheckAuthStateUseCase`
- [ ] **UI Layer**
  - [ ] Tạo `LoginScreen` với Compose
  - [ ] Tạo `LoginViewModel`
  - [ ] Implement form validation
  - [ ] Xử lý loading states và error handling
  - [ ] Implement role-based navigation (Staff vs Manager)

### 1.4 Navigation Setup
- [ ] Tạo `AppNavigation.kt`
- [ ] Define navigation routes
  ```kotlin
  sealed class Screen(val route: String) {
      object Login : Screen("login")
      object Dashboard : Screen("dashboard")
      object OrderManagement : Screen("orders")
      object MenuManagement : Screen("menu")
      object TableManagement : Screen("tables")
      object StaffManagement : Screen("staff")
      object Statistics : Screen("statistics")
  }
  ```
- [ ] Implement role-based navigation graph
- [ ] Setup deep linking (optional)

### 1.5 Testing & Validation
- [ ] Test đăng nhập với Staff account
- [ ] Test đăng nhập với Manager account
- [ ] Verify RLS policies hoạt động đúng
- [ ] Test navigation flow

---

## 🍽️ Milestone 2: Order Management System
**Thời gian ước tính**: 2-3 tuần  
**Mục tiêu**: Xây dựng hệ thống quản lý đơn hàng cho Staff

### 2.1 Backend Enhancement
- [ ] Tạo Supabase Edge Functions (nếu cần)
  - [ ] Function cập nhật order status
  - [ ] Function tính toán total amount
- [ ] Setup Realtime subscriptions cho orders table
- [ ] Implement idempotency pattern cho order updates

### 2.2 Data Layer - Orders
- [ ] **Models**
  - [ ] `Order` data model
  - [ ] `OrderItem` data model
  - [ ] `OrderStatus` enum
  - [ ] `PaymentStatus` enum
- [ ] **Repository**
  - [ ] `OrderRepository` interface
  - [ ] `OrderRepositoryImpl`
    - [ ] `getActiveOrders()`: Lấy danh sách orders đang active
    - [ ] `getOrderById(id)`: Lấy chi tiết order
    - [ ] `updateOrderStatus(id, status)`: Cập nhật trạng thái
    - [ ] `subscribeToOrders()`: Realtime subscription
    - [ ] `getOrderHistory(dateRange)`: Lấy lịch sử orders

### 2.3 Domain Layer - Orders
- [ ] **Use Cases**
  - [ ] `GetActiveOrdersUseCase`
  - [ ] `GetOrderDetailsUseCase`
  - [ ] `UpdateOrderStatusUseCase`
  - [ ] `SubscribeToOrdersUseCase`
  - [ ] `GetOrderHistoryUseCase`

### 2.4 UI Layer - Order Management
- [ ] **OrderListScreen**
  - [ ] Hiển thị danh sách orders theo trạng thái
  - [ ] Filter orders: Pending, Confirmed, Preparing, Served
  - [ ] Realtime updates khi có order mới
  - [ ] Pull-to-refresh
  - [ ] Search orders by table number
- [ ] **OrderDetailScreen**
  - [ ] Hiển thị chi tiết order items
  - [ ] Hiển thị customer info
  - [ ] Hiển thị table info
  - [ ] Buttons cập nhật status:
    - [ ] Confirm Order
    - [ ] Start Preparing
    - [ ] Mark as Served
    - [ ] Cancel Order
  - [ ] Hiển thị timestamp cho mỗi status change
- [ ] **ViewModels**
  - [ ] `OrderListViewModel`
  - [ ] `OrderDetailViewModel`

### 2.5 UI Components
- [ ] `OrderCard` component
- [ ] `OrderStatusBadge` component
- [ ] `OrderItemRow` component
- [ ] `StatusUpdateDialog` component

### 2.6 Performance Patterns Implementation
- [ ] **Queue-Based Load Leveling**
  - [ ] Setup queue mechanism cho order updates
  - [ ] Implement worker để process queue
- [ ] **Idempotency**
  - [ ] Implement idempotency key checking
  - [ ] Prevent duplicate status updates

### 2.7 Testing
- [ ] Unit tests cho repositories
- [ ] Unit tests cho use cases
- [ ] UI tests cho order screens
- [ ] Integration test: Order flow từ pending → served
- [ ] Test realtime updates
- [ ] Test idempotency pattern

---

## 📊 Milestone 3: Menu & Table Management
**Thời gian ước tính**: 2-3 tuần  
**Mục tiêu**: Quản lý thực đơn và bàn (Manager only)

### 3.1 Menu Management - Backend
- [ ] Setup Supabase Storage bucket cho menu images
- [ ] Tạo storage policies
  - [ ] Manager: Upload, update, delete images
  - [ ] Staff/Customer: Read only
- [ ] Tạo database functions
  - [ ] Function toggle `is_available` cho menu items
  - [ ] Function toggle `is_active` cho categories

### 3.2 Menu Management - Data Layer
- [ ] **Models**
  - [ ] `Category` model
  - [ ] `MenuItem` model
- [ ] **Repository**
  - [ ] `MenuRepository` interface
  - [ ] `MenuRepositoryImpl`
    - [ ] `getCategories()`: Lấy danh sách categories
    - [ ] `getMenuItems(categoryId)`: Lấy items theo category
    - [ ] `addMenuItem(item, image)`: Thêm món mới + upload ảnh
    - [ ] `updateMenuItem(item, image?)`: Cập nhật món
    - [ ] `deleteMenuItem(id)`: Xóa món
    - [ ] `toggleItemAvailability(id)`: Bật/tắt món
    - [ ] `addCategory(category, image)`: Thêm category
    - [ ] `updateCategory(category)`: Cập nhật category
    - [ ] `deleteCategory(id)`: Xóa category

### 3.3 Menu Management - Domain Layer
- [ ] **Use Cases**
  - [ ] `GetCategoriesUseCase`
  - [ ] `GetMenuItemsUseCase`
  - [ ] `AddMenuItemUseCase`
  - [ ] `UpdateMenuItemUseCase`
  - [ ] `DeleteMenuItemUseCase`
  - [ ] `ToggleItemAvailabilityUseCase`
  - [ ] Category management use cases

### 3.4 Menu Management - UI Layer
- [ ] **MenuManagementScreen**
  - [ ] Tab layout: Categories | Menu Items
  - [ ] Category list với images
  - [ ] Menu items list grouped by category
  - [ ] FAB để thêm item/category mới
  - [ ] Search và filter
- [ ] **AddEditMenuItemScreen**
  - [ ] Form input: Name, Description, Price, Category
  - [ ] Image picker và preview
  - [ ] Toggle availability switch
  - [ ] Validation
- [ ] **AddEditCategoryScreen**
  - [ ] Form input: Name
  - [ ] Image picker
  - [ ] Toggle active switch
- [ ] **ViewModels**
  - [ ] `MenuManagementViewModel`
  - [ ] `AddEditMenuItemViewModel`
  - [ ] `AddEditCategoryViewModel`

### 3.5 Table Management - Data Layer
- [ ] **Models**
  - [ ] `Table` model
  - [ ] `TableStatus` enum (free, occupied)
- [ ] **Repository**
  - [ ] `TableRepository` interface
  - [ ] `TableRepositoryImpl`
    - [ ] `getTables()`: Lấy danh sách bàn
    - [ ] `getTableById(id)`: Chi tiết bàn
    - [ ] `updateTableStatus(id, status)`: Cập nhật trạng thái
    - [ ] `addTable(table)`: Thêm bàn mới
    - [ ] `deleteTable(id)`: Xóa bàn
    - [ ] `generateQRCode(tableId)`: Generate QR code

### 3.6 Table Management - Domain Layer
- [ ] **Use Cases**
  - [ ] `GetTablesUseCase`
  - [ ] `UpdateTableStatusUseCase`
  - [ ] `AddTableUseCase`
  - [ ] `DeleteTableUseCase`
  - [ ] `GenerateQRCodeUseCase`

### 3.7 Table Management - UI Layer
- [ ] **TableManagementScreen**
  - [ ] Grid layout hiển thị bàn
  - [ ] Color coding: Free (green), Occupied (red)
  - [ ] Tap để toggle status
  - [ ] FAB để thêm bàn mới
- [ ] **AddEditTableScreen**
  - [ ] Input table number
  - [ ] Generate QR code button
  - [ ] Preview QR code
  - [ ] Save QR code to storage
- [ ] **TableDetailScreen**
  - [ ] Hiển thị QR code
  - [ ] Hiển thị current order (nếu occupied)
  - [ ] Button mark as free/occupied
- [ ] **ViewModels**
  - [ ] `TableManagementViewModel`
  - [ ] `AddEditTableViewModel`

### 3.8 UI Components
- [ ] `MenuItemCard` component
- [ ] `CategoryCard` component
- [ ] `TableCard` component
- [ ] `ImagePicker` component
- [ ] `QRCodeDisplay` component

### 3.9 Testing
- [ ] Test CRUD operations cho menu items
- [ ] Test CRUD operations cho categories
- [ ] Test image upload/delete
- [ ] Test table management
- [ ] Test QR code generation
- [ ] Verify Manager-only access

---

## 👥 Milestone 4: Staff Management & Dashboard
**Thời gian ước tính**: 2 tuần  
**Mục tiêu**: Quản lý nhân viên và dashboard thống kê (Manager only)

### 4.1 Staff Management - Backend
- [ ] Tạo Supabase Edge Function để tạo staff account
  - [ ] Function `createStaffAccount(email, password, role, fullName, phone)`
  - [ ] Automatically create profile entry
- [ ] Tạo function deactivate staff
- [ ] Setup email templates cho staff invitation

### 4.2 Staff Management - Data Layer
- [ ] **Models**
  - [ ] `StaffMember` model
  - [ ] `UserRole` enum (staff, manager)
- [ ] **Repository**
  - [ ] `StaffRepository` interface
  - [ ] `StaffRepositoryImpl`
    - [ ] `getStaffList()`: Lấy danh sách nhân viên
    - [ ] `getStaffById(id)`: Chi tiết nhân viên
    - [ ] `createStaff(staffData)`: Tạo tài khoản mới
    - [ ] `updateStaff(id, data)`: Cập nhật thông tin
    - [ ] `deactivateStaff(id)`: Vô hiệu hóa tài khoản
    - [ ] `reactivateStaff(id)`: Kích hoạt lại

### 4.3 Staff Management - Domain Layer
- [ ] **Use Cases**
  - [ ] `GetStaffListUseCase`
  - [ ] `GetStaffDetailsUseCase`
  - [ ] `CreateStaffUseCase`
  - [ ] `UpdateStaffUseCase`
  - [ ] `DeactivateStaffUseCase`

### 4.4 Staff Management - UI Layer
- [ ] **StaffManagementScreen**
  - [ ] List staff members với avatar, name, role
  - [ ] Filter: Active | Inactive
  - [ ] Search by name
  - [ ] FAB để thêm staff mới
- [ ] **AddEditStaffScreen**
  - [ ] Form: Full Name, Phone, Email, Role
  - [ ] Password generation (auto or manual)
  - [ ] Validation
- [ ] **StaffDetailScreen**
  - [ ] Hiển thị thông tin chi tiết
  - [ ] Edit button
  - [ ] Deactivate/Reactivate button
  - [ ] Activity log (optional)
- [ ] **ViewModels**
  - [ ] `StaffManagementViewModel`
  - [ ] `AddEditStaffViewModel`

### 4.5 Dashboard & Statistics - Backend
- [ ] Tạo database views cho statistics
  - [ ] View: Daily revenue
  - [ ] View: Orders by status
  - [ ] View: Popular menu items
  - [ ] View: Table occupancy rate
- [ ] Tạo Supabase functions
  - [ ] `getRevenueStats(startDate, endDate)`
  - [ ] `getOrderStats(startDate, endDate)`
  - [ ] `getPopularItems(limit)`

### 4.6 Dashboard - Data Layer
- [ ] **Models**
  - [ ] `DashboardStats` model
  - [ ] `RevenueData` model
  - [ ] `OrderStatsData` model
  - [ ] `PopularItem` model
- [ ] **Repository**
  - [ ] `StatisticsRepository` interface
  - [ ] `StatisticsRepositoryImpl`
    - [ ] `getDashboardStats(dateRange)`: Tổng quan
    - [ ] `getRevenueStats(dateRange)`: Doanh thu
    - [ ] `getOrderStats(dateRange)`: Thống kê orders
    - [ ] `getPopularItems(limit)`: Món phổ biến

### 4.7 Dashboard - Domain Layer
- [ ] **Use Cases**
  - [ ] `GetDashboardStatsUseCase`
  - [ ] `GetRevenueStatsUseCase`
  - [ ] `GetOrderStatsUseCase`
  - [ ] `GetPopularItemsUseCase`

### 4.8 Dashboard - UI Layer
- [ ] **DashboardScreen** (Manager Home)
  - [ ] Summary cards:
    - [ ] Today's revenue
    - [ ] Active orders count
    - [ ] Occupied tables count
    - [ ] Total staff count
  - [ ] Revenue chart (line/bar chart)
  - [ ] Order status distribution (pie chart)
  - [ ] Popular items list
  - [ ] Date range picker
  - [ ] Quick actions: View Orders, Manage Menu, etc.
- [ ] **ViewModel**
  - [ ] `DashboardViewModel`

### 4.9 UI Components & Charts
- [ ] `StatCard` component
- [ ] `LineChart` component (using library như MPAndroidChart hoặc Vico)
- [ ] `BarChart` component
- [ ] `PieChart` component
- [ ] `DateRangePicker` component

### 4.10 Performance - Bulkhead Pattern
- [ ] Implement separate connection pool cho reporting
- [ ] Use read replica cho statistics queries (nếu có)
- [ ] Cache dashboard data với expiration

### 4.11 Testing
- [ ] Test staff CRUD operations
- [ ] Test role-based access
- [ ] Test statistics calculations
- [ ] Test dashboard data loading
- [ ] Performance test cho heavy queries

---

## 🚀 Milestone 5: Polish, Performance & Deployment
**Thời gian ước tính**: 1-2 tuần  
**Mục tiêu**: Hoàn thiện, tối ưu hóa và triển khai

### 5.1 Performance Optimization
- [ ] **Circuit Breaker Pattern**
  - [ ] Implement circuit breaker cho external services
  - [ ] Fallback mechanisms
- [ ] **Retry with Exponential Backoff**
  - [ ] Implement retry logic cho network calls
  - [ ] Configure retry policies
- [ ] **Caching**
  - [ ] Cache menu data locally
  - [ ] Cache user profile
  - [ ] Implement cache invalidation strategy
- [ ] **Database Optimization**
  - [ ] Add indexes cho frequently queried columns
  - [ ] Optimize complex queries
  - [ ] Review and optimize RLS policies

### 5.2 Error Handling & Logging
- [ ] Implement global error handler
- [ ] Setup logging framework (Timber)
- [ ] Log critical operations
- [ ] User-friendly error messages
- [ ] Offline mode handling
- [ ] Network connectivity detection

### 5.3 UI/UX Polish
- [ ] **Theme & Design System**
  - [ ] Finalize color scheme
  - [ ] Typography system
  - [ ] Spacing system
  - [ ] Component library documentation
- [ ] **Animations**
  - [ ] Screen transitions
  - [ ] Loading animations
  - [ ] Success/error animations
- [ ] **Accessibility**
  - [ ] Content descriptions
  - [ ] Proper contrast ratios
  - [ ] Font scaling support
- [ ] **Dark Mode** (optional)
  - [ ] Implement dark theme
  - [ ] Theme switcher

### 5.4 Notifications & Realtime Features
- [ ] **Push Notifications** (optional)
  - [ ] Setup Firebase Cloud Messaging
  - [ ] Notify staff khi có order mới
  - [ ] Notify manager về daily summary
- [ ] **Realtime Updates**
  - [ ] Optimize Supabase realtime subscriptions
  - [ ] Handle connection drops
  - [ ] Reconnection logic

### 5.5 Security Hardening
- [ ] Review RLS policies
- [ ] Implement rate limiting (Supabase Edge Functions)
- [ ] Secure storage cho sensitive data
- [ ] Input validation và sanitization
- [ ] SQL injection prevention
- [ ] XSS prevention

### 5.6 Testing & QA
- [ ] **Unit Tests**
  - [ ] Repository tests (coverage > 80%)
  - [ ] Use case tests (coverage > 80%)
  - [ ] ViewModel tests (coverage > 80%)
- [ ] **Integration Tests**
  - [ ] End-to-end order flow
  - [ ] Menu management flow
  - [ ] Staff management flow
- [ ] **UI Tests**
  - [ ] Critical user journeys
  - [ ] Screenshot tests
- [ ] **Manual Testing**
  - [ ] Test trên nhiều devices
  - [ ] Test với slow network
  - [ ] Test offline scenarios
  - [ ] Stress testing với nhiều orders

### 5.7 Documentation
- [ ] **Code Documentation**
  - [ ] KDoc comments cho public APIs
  - [ ] README cho mỗi module
- [ ] **User Documentation**
  - [ ] Staff user guide
  - [ ] Manager user guide
  - [ ] Troubleshooting guide
- [ ] **Technical Documentation**
  - [ ] Architecture overview
  - [ ] API documentation
  - [ ] Database schema documentation
  - [ ] Deployment guide

### 5.8 Deployment Preparation
- [ ] **Build Configuration**
  - [ ] Setup build variants (debug, staging, production)
  - [ ] Configure ProGuard/R8
  - [ ] Setup signing configs
- [ ] **Environment Configuration**
  - [ ] Production Supabase project
  - [ ] Environment variables management
  - [ ] API keys management
- [ ] **Release Build**
  - [ ] Generate signed APK/AAB
  - [ ] Test release build
  - [ ] Prepare for Play Store submission

### 5.9 Monitoring & Analytics (Optional)
- [ ] Setup crash reporting (Firebase Crashlytics)
- [ ] Setup analytics (Firebase Analytics)
- [ ] Track key metrics:
  - [ ] Order completion rate
  - [ ] Average order processing time
  - [ ] App crashes
  - [ ] User engagement

### 5.10 Final Validation
- [ ] **Acceptance Testing**
  - [ ] Test với actual restaurant staff
  - [ ] Gather feedback
  - [ ] Fix critical issues
- [ ] **Performance Testing**
  - [ ] App startup time < 2s
  - [ ] Screen load time < 1s
  - [ ] Smooth scrolling (60fps)
- [ ] **Security Audit**
  - [ ] Review all security measures
  - [ ] Penetration testing (nếu có resources)

### 5.11 Launch Preparation
- [ ] Create demo data
- [ ] Prepare training materials
- [ ] Setup support channels
- [ ] Plan rollout strategy
- [ ] Backup and recovery plan

---

## 📈 Phụ lục: Ước tính thời gian tổng thể

| Milestone | Thời gian | Độ ưu tiên |
|-----------|-----------|------------|
| M1: Foundation & Authentication | 1-2 tuần | Critical |
| M2: Order Management | 2-3 tuần | Critical |
| M3: Menu & Table Management | 2-3 tuần | High |
| M4: Staff Management & Dashboard | 2 tuần | High |
| M5: Polish & Deployment | 1-2 tuần | Medium |
| **Tổng cộng** | **8-12 tuần** | |

---

## 🎯 Chiến lược triển khai

### Phương pháp Agile
- **Sprint length**: 1 tuần
- **Daily standups**: Theo dõi tiến độ
- **Sprint review**: Cuối mỗi tuần
- **Retrospective**: Cải thiện quy trình

### Phân công nhóm (ví dụ cho team 3-4 người)
- **Developer 1**: Backend (Supabase) + Data Layer
- **Developer 2**: Domain Layer + UI (Order Management)
- **Developer 3**: UI (Menu/Table/Staff Management)
- **Developer 4**: Testing + DevOps + Documentation

### Deliverables mỗi Milestone
- Working demo
- Unit tests
- Documentation updates
- Code review completed

---

## ⚠️ Rủi ro và giải pháp

| Rủi ro | Mức độ | Giải pháp |
|--------|--------|-----------|
| Supabase realtime lag | Medium | Implement fallback polling, optimize queries |
| Complex RLS policies | High | Thorough testing, use Supabase dashboard testing tools |
| Image upload performance | Medium | Compress images, use progressive loading |
| Concurrent order updates | High | Implement idempotency, optimistic locking |
| Staff training | Medium | Create comprehensive user guides, demo videos |

---

## 📚 Tài liệu tham khảo

- [Supabase Documentation](https://supabase.com/docs)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Backend Performance Patterns](./backend_patterns.md)
- [Database Design](./database_design.md)
- [Activity Diagrams](./activity_diagrams.md)

---

**Lưu ý**: Kế hoạch này có thể điều chỉnh dựa trên feedback thực tế và constraints của dự án. Nên review và update sau mỗi milestone.
