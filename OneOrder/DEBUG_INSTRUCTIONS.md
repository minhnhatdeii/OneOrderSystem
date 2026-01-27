# Debug Instructions - QR Scanner Issue

## Bước 1: Rebuild ứng dụng
1. Mở Android Studio
2. Chọn **Build > Clean Project**
3. Chọn **Build > Rebuild Project**
4. Cài đặt lại ứng dụng lên thiết bị/emulator

## Bước 2: Xóa logs cũ và bắt đầu theo dõi
Mở terminal và chạy lệnh sau để xóa logs cũ:
```powershell
adb logcat -c
```

Sau đó bắt đầu theo dõi logs:
```powershell
adb logcat | Select-String -Pattern "HomeViewModel|QRScanner"
```

## Bước 3: Test QR Scanner
1. Mở ứng dụng OneOrder
2. Đăng nhập
3. Nhấn vào nút QR Scanner
4. Quét mã QR của nhà hàng
5. Quan sát màn hình và logs

## Bước 4: Thu thập logs
Sau khi test xong, lưu logs bằng lệnh:
```powershell
adb logcat -d | Select-String -Pattern "HomeViewModel|QRScanner" > qr_debug_logs.txt
```

## Logs cần kiểm tra

### Khi khởi động HomeScreen:
```
HomeViewModel: === HOME VIEW MODEL INITIALIZED ===
HomeViewModel: Instance: [số hash code]
HomeViewModel: === LOADING HOME DATA ===
HomeViewModel: No restaurant set, showing empty state
```

### Khi quét QR thành công:
```
QRScanner: === QR CODE DETECTED IN CAMERA ===
QRScanner: Raw value: oneorder://restaurant/xxx/table/yyy
QRScannerViewModel: === QR CODE SCANNED ===
QRScannerViewModel: === QR PARSING SUCCESS ===
QRScanner: === SCAN SUCCESS - NAVIGATING ===
```

### Khi set restaurant và table:
```
HomeViewModel: === SETTING RESTAURANT & TABLE ===
HomeViewModel: Instance: [số hash code - phải GIỐNG với init]
HomeViewModel: Restaurant ID: xxx, Table ID: yyy
HomeViewModel: Current state BEFORE: restaurant=null, table=null
HomeViewModel: Restaurant fetched: [Tên nhà hàng]
HomeViewModel: Table fetched: [Tên bàn]
HomeViewModel: State AFTER update: restaurant=[Tên nhà hàng], table=[Tên bàn]
HomeViewModel: Calling loadData()...
HomeViewModel: === LOADING HOME DATA ===
HomeViewModel: Fetching categories...
```

## Vấn đề có thể xảy ra:

### 1. ViewModel bị tạo lại (Instance hash code khác nhau)
Nếu hash code ở `INITIALIZED` khác với hash code ở `SETTING RESTAURANT`, có nghĩa là ViewModel bị tạo lại → cần fix navigation.

### 2. Restaurant/Table fetch thất bại
Nếu thấy logs:
```
HomeViewModel: Failed to load restaurant or table info
HomeViewModel: Restaurant result: false
```
→ Vấn đề ở repository hoặc database.

### 3. State bị mất sau khi set
Nếu `State AFTER update` vẫn là null → vấn đề ở state management.

## Gửi kết quả
Vui lòng gửi file `qr_debug_logs.txt` hoặc copy toàn bộ logs liên quan để tôi phân tích.
