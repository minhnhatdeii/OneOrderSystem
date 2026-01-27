ĐẠI HỌC QUỐC GIA HÀ NỘI
TRƯỜNG ĐẠI HỌC CÔNG NGHỆ 






BÁO CÁO BÀI TẬP LỚN
PHÂN TÍCH THIẾT KẾ ỨNG DỤNG DI ĐỘNG
Chủ đề: Ứng dụng nghe nhạc trực tuyến HarmonyHub

	
Lớp học phần	: INT3120_55
Giảng viên	: TS. Lê Khánh Trình
Nhóm thực hiện	: Lê Hoàng Lan (Nhóm trưởng)
Nguyễn Hữu Thế
Hoàng Minh Đức
Trương Sỹ Đạt
Bùi Minh Nhật



Hà Nội, tháng 12 năm 2024
Mục Lục
I. Xác định bài toán	4
II. Danh sách các thuật ngữ	5
III. Phân tích yêu cầu người dùng	7
3.1. Yêu cầu chức năng	7
3.2. Yêu cầu phi chức năng	8
IV. Thiết kế hệ thống	9
4.1. Biểu đồ Use-case	9
4.1.1. Xác định các tác nhân	9
4.1.2. Xác định các use-case	9
4.1.3. Biểu đồ use-case	15
4.2. Biểu đồ trình tự	17
4.2.1. Biểu đồ trình tự đăng nhập	17
4.2.2. Biểu đồ trình tự đăng ký	17
4.2.3. Biểu đồ trình tự tìm kiếm	18
4.2.4. Biểu đồ trình tự phát nhạc	18
4.2.5. Biểu đồ trình tự tách giọng hát và nhạc cụ	19
4.2.6. Biểu đồ trình tự tạo playlist	19
4.2.7. Biểu đồ trình tự tạo nghệ sĩ yêu thích (artist list)	19
4.3 Sơ đồ lớp	20
4.4 Thiết kế giao diện	21
4.4.1. Giao diện trang đăng ký	21
4.4.2. Giao diện trang đăng nhập	22
4.4.3. Giao diện trang chủ	23
4.4.4. Giao diện trang tìm kiếm	24
4.4.5. Giao diện trang thư viện	25
4.4.6. Giao diện trang nhạc yêu thích	26
4.4.7. Giao diện trang playlist	27
4.4.8. Giao diện trang tải xuống	28
4.4.9. Giao diện trang nghệ sĩ	29
4.4.10. Giao diện trang phát nhạc	30
4.4.11. Giao diện trang lịch sử phát	31
4.4.12. Giao diện trang hồ sơ	32
4.4.13. Giao diện trang danh sách bạn bè	33
4.4.14. Giao diện trang tách nhạc	34
V. Cài đặt và kiểm thử	35
1. Cài đặt	35
2. Kiểm thử	35
a. Các kịch bản kiểm thử	35
b. Danh sách các ca kiểm thử	35
c. Chi tiết các test case	37
VI. Đánh giá và kết luận	40
1. Ưu điểm	40
2. Nhược điểm	40
3. Định hướng phát triển	40


I. Xác định bài toán 
Trong thời đại công nghệ số và kết nối không biên giới hiện nay, nhu cầu thưởng thức âm nhạc đã trở thành một phần không thể thiếu trong cuộc sống của nhiều người. Âm nhạc không chỉ là phương tiện giải trí, mà còn là công cụ giúp giảm căng thẳng, kết nối con người và thúc đẩy cảm hứng. Trong bối cảnh này, việc phát triển một ứng dụng nghe nhạc trực tuyến không chỉ đáp ứng nhu cầu nghe nhạc mà còn mang đến trải nghiệm cá nhân hóa, tiện lợi và liền mạch cho người dùng.
Với sự gia tăng của các nền tảng số, ứng dụng nghe nhạc trực tuyến là giải pháp giúp người dùng dễ dàng truy cập vào một kho nhạc phong phú, cập nhật liên tục và dễ dàng khám phá những bài hát mới. Người dùng có thể thưởng thức âm nhạc mọi lúc, mọi nơi mà không cần tải về nhạc theo cách truyền thống. Điều này giúp họ tiết kiệm bộ nhớ thiết bị, đồng thời có cơ hội tiếp cận những xu hướng âm nhạc mới một cách nhanh chóng.
Một trong những tiện ích lớn nhất của ứng dụng nghe nhạc trực tuyến là khả năng cá nhân hóa trải nghiệm người dùng. Thông qua lịch sử nghe nhạc và sở thích cá nhân, ứng dụng sẽ gợi ý những bài hát, nghệ sĩ, hoặc playlist phù hợp với tâm trạng, sở thích và thói quen của từng người. Không cần phải tìm kiếm mất thời gian, người dùng có thể dễ dàng tiếp cận những bản nhạc yêu thích, hay thậm chí khám phá những bài hát mới lạ mà họ chưa từng nghe.
Ngoài ra, ứng dụng nghe nhạc trực tuyến còn mang đến sự linh hoạt và tiện lợi thông qua các tính năng hỗ trợ nghe nhạc offline, cho phép tải trước bài hát để phát mà không cần kết nối Internet. Đây là một giải pháp lý tưởng cho những ai di chuyển thường xuyên hoặc ở những nơi có kết nối mạng không ổn định.
Bên cạnh đó, ứng dụng còn hỗ trợ các tính năng chia sẻ playlist, giúp người dùng dễ dàng giới thiệu những bản nhạc yêu thích tới bạn bè và cộng đồng. Điều này không chỉ giúp lan tỏa niềm vui âm nhạc mà còn xây dựng một cộng đồng người yêu nhạc sôi nổi và gắn kết.
Tổng kết lại, với sự kết hợp giữa tiện ích, cá nhân hóa và khả năng kết nối, ứng dụng nghe nhạc trực tuyến không chỉ là công cụ giải trí, mà còn là người bạn đồng hành, mang lại những trải nghiệm âm nhạc thú vị, góp phần nâng cao chất lượng cuộc sống tinh thần. Qua tài liệu này, mọi người sẽ hiểu rõ hơn về ứng dụng cũng như cách thức hoạt động và các tính năng của hệ thống nghe nhạc trực tuyến, giúp ứng dụng trở thành một phần không thể thiếu trong đời sống hiện đại.
II. Danh sách các thuật ngữ 
Giải thích định nghĩa một số thuật ngữ sử dụng trong ứng dụng nghe nhạc trực tuyến trong mô tả ca sử dụng và các tài liệu khác.
 Actor (tác nhân): 
Là người sử dụng các dịch vụ của hệ thống, một tác nhân có thể là một người dùng thực hoặc các hệ thống máy tính khác có vai trò nào đó trong hoạt động của hệ thống. Như vậy, tác nhân thực hiện các use case. Một tác nhân có thể thực hiện nhiều use case và ngược lại một use case cũng có thể được thực hiện bởi nhiều tác nhân. Kí hiệu:

 Use case: 
thành phần chính của biểu đồ ca sử dụng, được biểu diễn bằng hình elip. Tên use case là thể hiện một chức năng của hệ thống.


Các mối quan hệ: 
+ Association: thường được dùng để mô tả mối quan hệ giữa Actor và Use Case và giữa các Use Case với nhau. Kí hiệu:
 

+ Extend: Một ca sử dụng có thể được định nghĩa như là một sự mở rộng tăng dần của một ca sử dụng cơ sở. Kí hiệu: 


+ Include: Một ca sử dụng có thể tích hợp hành vi của các ca sử dụng khác như là các phần trong hành vi tổng thể của nó. Biểu diễn một UC chứa hành vi được định nghĩa trong một UC khác. Đây là quan hệ giữa các Use Case với nhau, nó mô tả việc một Use Case lớn được chia ra thành các Use Case nhỏ để dễ cài đặt (module hóa) hoặc thể hiện sự dùng lại. Kí hiệu:


 
Biểu đồ lớp
- Một biểu đồ lớp chỉ ra cấu trúc tĩnh của các lớp trong hệ thống. Các lớp là đại diện cho các “đối tượng” được xử lý trong hệ thống. 
+ Một lớp có những thành phần sau: tên lớp, các thuộc tính, các phương thức. 
+ Giữa các lớp luôn có sự liên kết với nhau. Mối liên hệ ngữ nghĩa giữa hai hay nhiều lớp chỉ ra sự liên kết giữa các thể hiện của chúng. Mối quan hệ về mặt cấu trúc chỉ ra các đối tượng của lớp này có kết nối với các đối tượng của lớp khác. 

Biểu đồ tuần tự
- Biểu đồ tuần tự là biểu đồ dùng để xác định các trình tự diễn ra sự kiện của một nhóm đối tượng nào đó. Nó miêu tả chi tiết các thông điệp được gửi và nhận giữa các đối tượng đồng thời cũng chú trọng đến việc trình tự về mặt thời gian gửi và nhận các thông điệp đó

Mô hình cơ sở dữ liệu
Mô hình cơ sở dữ liệu là một loại mô hình dữ liệu xác định cấu trúc logic của cơ sở dữ liệu và xác định một cách cơ bản cách thức dữ liệu có thể được lưu trữ, sắp xếp và thao tác.

Khách hàng:
Là người sử dụng các tiện ích nghe nhạc… Cần có tài khoản sử dụng 

Nghệ sĩ:
Là người phát hành các bài hát các album cho ứng dụng.

Hệ quản trị cơ sở dữ liệu:
Là một phần mềm để quản lý dữ liệu thao tác và truy xuất để vận hành ứng dụng. Nhằm phục vụ yêu cầu người dùng 

Cơ sở dữ liệu:
Là một tập hợp các dữ liệu có tổ chức được quản lý bằng hệ quản trị cơ sở dữ liệu. Trong hệ thống có ba cơ sở dữ liệu bao gồm cơ sở dữ liệu người dùng, cơ sở dữ liệu chuyến đi, cơ sở dữ liệu thanh toán, cơ sở dữ liệu thông tin…

Hệ thống quản lý người dùng:
Quản lý và lưu trữ thông tin của người dùng, hệ thống này phải là một hệ thống bảo mật an toàn bởi vì sự tin tưởng sẽ là yếu tố tạo nên thương hiệu của ứng dụng

Hệ thống quản lý sản phẩm:
Quản lý và lưu trữ danh sách các sản phẩm được bán của các nghệ sĩ, nhóm nghệ sĩ để khách hàng từ đó có thể dễ dàng tìm kiếm và nghe nhạc.
III. Phân tích yêu cầu người dùng
3.1. Yêu cầu chức năng
Ứng dụng cần cung cấp cho người dùng một kho nhạc phong phú và luôn được cập nhật liên tục.
Ứng dụng cần gợi ý những bài hát, nghệ sĩ, hoặc playlist phù hợp với tâm trạng, sở thích và thói quen của từng người dùng
Ứng dụng cần có tính năng hỗ trợ nghe nhạc offline, cho phép người dùng tải trước bài hát để phát mà không cần kết nối Internet
Ứng dụng cần cung cấp tính năng chia sẻ playlist giúp người dùng dễ dàng giới thiệu những bản nhạc yêu thích tới bạn bè và cộng đồng
Đăng ký/đăng nhập: Người dùng tạo tài khoản cho lần đầu tiên sử dụng. Sau đó chỉ cần đăng nhập thông tin đã đăng ký trước đó để sử dụng dịch vụ.
Tìm kiếm nhạc: Cho phép người dùng tìm kiếm qua tên (tên bài hát hoặc tên Nghệ sĩ) hoặc qua lời bài hát, list nhạc theo chủ đề, theo album, theo Nghệ sĩ.
Trình phát nhạc: Phát nhạc cơ bản, các nút điều khiển: play, pause, next, previous; điều chỉnh âm lượng, tốc độ, hiển thị lời nhạc, thứ tự phát nhạc (random hay cố định).
Quản lý tài khoản:
Tạo danh sách phát nhạc yêu thích.
Tạo danh mục Nghệ sĩ yêu thích.
Cho phép xem danh sách phát nhạc, danh sách Nghệ sĩ yêu thích của bạn bè.

Thống kê sử dụng dịch vụ: Trong bao lâu, nghe những bài gì, những bài hát và những nghệ sĩ người dùng nghe nhiều nhất.
Cập nhật BXH xu hướng âm nhạc hàng ngày.
3.2. Yêu cầu phi chức năng
Tính khả dụng
Ứng dụng có giao diện dễ sử dụng và trực quan trên mọi loại điện thoại thông minh, phù hợp với mọi lứa tuổi, từ trẻ nhỏ đến người lớn tuổi. Ứng dụng hỗ trợ trên Android, yêu cầu người dùng có điện thoại thông minh và kết nối Internet để trải nghiệm dịch vụ.
Ngoài ra, ứng dụng cần hỗ trợ cả tiếng Anh và tiếng Việt để đảm bảo người dùng có thể thấy thoải mái và thuận tiện nhất khi sử dụng. 
Hiệu năng
Hệ thống cần phản hồi trong vòng 3 giây cho mỗi truy vấn của người dùng. Ngoài ra, hệ thống cần có khả năng xử lý ít nhất 500 yêu cầu/giờ.
Tính tin cậy
Ứng dụng có cơ chế giám sát và trung tâm hỗ trợ để xử lý các sự cố hoặc lỗi xảy ra trong quá trình sử dụng. Ngoài ra, ứng dụng cần sao lưu dữ liệu định kỳ, có khả năng phục hồi dữ liệu sau sự cố, cũng như có cơ chế thông báo lỗi và cách khắc phục cho người dùng một cách rõ ràng.
Tính bảo mật
Ứng dụng đề cao việc bảo mật để ngăn chặn hành vi truy cập trái phép và đảm bảo an toàn thông tin người dùng. Dữ liệu tài khoản cá nhân người dùng và lịch sử nghe nhạc của người dùng sẽ được mã hoá.
Tính sẵn sàng
Ứng dụng phải luôn sẵn sàng được sử dụng 24/7. Điều này đảm bảo người dùng có thể truy cập, sử dụng sản phẩm mọi lúc, không gặp gián đoạn đáng kể. 
IV. Thiết kế hệ thống
4.1. Biểu đồ Use-case
4.1.1. Xác định các tác nhân
Hệ thống có tác nhân chính: Người dùng
4.1.2. Xác định các use-case
Người dùng có các usecase sau:
Đăng ký, đăng nhập
Tìm kiếm các bài hát, album, nghệ sỹ yêu thích
Phát nhạc
Tạo playlist
Xem playlist, album, nghệ sỹ yêu thích của bạn bè, cộng đồng
Tách giọng hát và nhạc cụ.

Use case Đăng ký tài khoản
Mô tả
Usecase mô tả cách người dùng đăng ký tài khoản ứng dụng
Luồng cơ bản
- Người dùng chọn ‘Sign Up’
→ Hệ thống hiển thị mẫu đăng ký.
-Người dùng điền thông tin vào mẫu: Họ tên, Email, Mật khẩu.
-Hệ thống lưu trữ thông tin vào cơ sở dữ liệu.
- Hệ thống gửi mã kích hoạt tới email.
- Người dùng sử dụng mã kích hoạt tài khoản của mình
- Hệ thống kích hoạt tài khoản.
Luồng thay thế
- Người dùng không điền đầy đủ thông tin.
→ Hệ thống báo lỗi, yêu cầu đầy đủ thông tin.
- Người dùng xác nhận mật khẩu lần 2 không hợp lệ.
→ Yêu cầu khớp mật khẩu.
Yêu cầu đặc biệt
Không
Tiền điều kiện
Người dùng chưa có tài khoản
Hậu điều kiện
Tài khoản người dùng được tạo và xác thực khả dụng.
Điểm mở rộng
Không



Use case Đăng nhập
Mô tả
Usecase mô tả cách người dùng đăng nhập vào hệ thống
Luồng cơ bản
- Người dùng chọn Log in with a password
- Hệ thống hiển thị mẫu đăng nhập: Email/Tên đăng nhập, mật khẩu 
- Hệ thống xác thực tài khoản trên CSDL 
- Hệ thống chuyển hướng người dùng.
Luồng thay thế
- Login with password: Thông tin đăng nhập không chính xác
 → Hệ thống báo lỗi, yêu cầu nhập thông tin. 
- Người dùng chưa xác thực tài khoản thông qua email.
→ Hệ thống hiển thị thông báo yêu cầu người dùng xác thực email.
- Người dùng quên mật khẩu
→ Người dùng tùy chọn “Forgot password” → Người dùng nhập email của mình → Hệ thống gửi mật khẩu tạm thời đến email người dùng.
Yêu cầu đặc biệt
Hệ thống chỉ cho phép đăng nhập sai không quá 5 lần liên tiếp, nếu sai quá 5 lần liên tiếp thì chỉ cho phép đăng nhập sau 3 phút. 
Tiền điều kiện
- Người dùng đã có tài khoản được kích hoạt trên hệ thống.
Hậu điều kiện
Xác thực danh tính thành công, người dùng được chuyển đến màn hình chính và thao tác với các chức năng của ứng dụng.
Điểm mở rộng
Không



Use case Tìm kiếm nhạc yêu thích
Mô tả
Usecase mô tả cách người dùng thao tác hệ thống để tìm kiếm nhạc
Luồng cơ bản
- Tại thanh công cụ tìm kiếm, người dùng chọn vào và nhập vào tên bài hát/ tên nghệ sĩ/ album phát hành.
- Hệ thống tìm kiếm trên cơ sở dữ liệu và hiển thị thông tin tìm kiếm được.
Luồng thay thế
- Người dùng nhập tên bị thiếu, bị lỗi, bị sai, dẫn đến hệ thống không thể tìm kiếm trong cơ sở dữ liệu.
→ Hệ thống hiển thị: “Không tìm thấy kết quả trùng khớp”, yêu cầu người dùng nhập lại.
Yêu cầu đặc biệt
Không
Tiền điều kiện
Người dùng đã đăng nhập thành công vào hệ thống.
Hậu điều kiện
Hệ thống trả về kết quả liên quan đã tìm kiếm được.
Điểm mở rộng
Không




Use case Xem BXH xu hướng hàng ngày
Mô tả
Usecase mô tả cách về việc hệ thống cập nhật bảng xếp hạng xu hướng tới người dùng.
Luồng cơ bản
- Người dùng đã đăng nhập vào hệ thống, và hệ thống trả về giao diện trang chủ chính.
- Trang chủ chính bao gồm một bảng xếp hạng về xu hướng nghe nhạc theo ngày.
Luồng thay thế
- Trong quá trình hệ thống kết nối và lấy thông tin, sẽ hiển thị ký hiệu thông báo rằng hệ thống đang trong quá trình xử lý.
Yêu cầu đặc biệt
Không
Tiền điều kiện
- Hệ thống có kết nối ổn định tới internet.
- Người dùng đã đăng nhập thành công vào hệ thống.
Hậu điều kiện
Bảng xếp hạng xu hướng âm nhạc hàng ngày được hệ thống cập nhật và gửi tới người dùng.
Điểm mở rộng
Không



Use case Tạo danh sách phát nhạc
Mô tả
Usecase mô tả cách người thực hiện thao tác tạo danh sách phát nhạc yêu thích.
Luồng cơ bản
- Người dùng đã đăng nhập thành công vào hệ thống, và hệ thống trả về giao diện chính.
- Tại giao diện chính, người dùng chọn vào nút ấn: “Tạo danh sách của tôi” → Hệ thống hiển thị giao diện để người dùng nhập tên của danh sách.
- Người dùng nhập tên của danh sách phát nhạc và chọn hoàn thành.
Luồng thay thế
- Tên của danh sách nhạc không hợp lệ: Bị trùng, ký tự đặc biệt, để trống,...
→ Hệ thống báo lỗi và yêu cầu điều chỉnh đúng tên.
Yêu cầu đặc biệt
Không
Tiền điều kiện
- Hệ thống có kết nối ổn định tới internet.
- Người dùng đăng nhập thành công vào hệ thống.
Hậu điều kiện
Hệ thống hoàn thành việc tạo một danh sách phát nhạc vào cơ sở dữ liệu.
Điểm mở rộng
Không



Use case Quản lý danh sách phát nhạc
Mô tả
Usecase mô tả cách người thực hiện thao tác với danh sách phát nhạc.
Luồng cơ bản
- Người dùng chọn một thẻ bài hát, và tùy chọn thêm vào danh sách phát nhạc mong muốn.
- Người dùng lựa chọn xem các nội dung của một danh sách phát nhạc cụ thể, lựa chọn xóa bài hát hoặc thậm chí xóa cả danh sách phát nhạc.
Luồng thay thế
- Người dùng chưa tạo danh sách phát nhạc nào trước đó → Hệ thống trả về thông báo yêu cầu tạo danh sách phát để có thể thêm bài hát.
- Bài hát đã được thêm vào trước đó → Hệ thống hiển thị thông báo “Bài hát nãy đã có trong danh sách”.
Yêu cầu đặc biệt
Không
Tiền điều kiện
- Hệ thống kết nối ổn định tới internet.
- Người dùng đã đăng nhập vào hệ thống.
Hậu điều kiện
- Bài hát mới được cập nhật vào danh sách phát nhạc.
- Nếu lựa chọn xóa, thì bài hát bị xóa khỏi danh sách phát hoặc thậm chí là cả danh sách phát bị xóa (Khi người dùng tùy chọn xóa luôn Danh sách phát).
Điểm mở rộng
Không



Use case Phát nhạc
Mô tả
Usecase mô tả cách người thực hiện thao tác chính là phát nhạc.
Luồng cơ bản
- Người dùng nhấn vào thẻ bài hát (Thẻ bài hát có thể xuất hiện ở trang chính, tìm kiếm, danh sách phát, danh sách bạn bè,...)
Luồng thay thế
- Trường hợp kết nối không ổn định, hệ thống không thể phát nhạc và thông báo lỗi kết nối.
Yêu cầu đặc biệt
Không
Tiền điều kiện
- Người dùng đã đăng nhập vào hệ thống.
Hậu điều kiện
Hệ thống chuyển sang giao diện phát nhạc và phát bài hát tương ứng.
Điểm mở rộng
Các thao tác Dừng/Tiếp tục, Tăng giảm âm lượng, nút ấn chuyển bài/quay lại.



Use case Phân tích nhạc cụ
Mô tả
Người dùng thực hiện thao tác để phân tích nhạc cụ, và có thể kết hợp thêm nhạc cụ mới vào.
Luồng cơ bản
- Người dùng tùy chọn để chuyển đến giao diện Phân tích nhạc cụ, và tìm kiếm bài hát muốn phân tích.
Luồng thay thế
- Trong trường hợp API gọi tới chưa thể xử lý một cách nhanh chóng, hệ thống hiển thị thông báo: “Đang trong quá trình phân tích”.
Yêu cầu đặc biệt
Không
Tiền điều kiện
- Hệ thống kết nối ổn định tới internet.
- Người dùng đã đăng nhập vào hệ thống.
- Người dùng đã thành công mở giao diện phát nhạc của hệ thống đối với một bài hát cụ thể nào đó.
Hậu điều kiện
- Hệ thống hiển thị danh sách các nhạc cụ được phân tách từ bản nhạc và cho phép người dùng thêm bớt để tạo giai điệu mới.
Điểm mở rộng
Không



Use case Thao tác với bạn bè
Mô tả
Người dùng thao tác với danh mục bạn bè đã kết nối, cho phép hiển thị danh mục phát nhạc của bạn bè.
Luồng cơ bản
- Người dùng tùy chọn vào giao diện danh sách bạn bè và chọn một thẻ bạn bè bất kỳ.
Luồng thay thế
- Nếu kết nối internet không ổn định, hệ thống không thể kết nối tới thông tin phía bên bạn bè.
- Nếu người dùng không có kết nối tới bạn bè, hệ thống hiển thị trống và hiển thị thông báo đề xuất tìm kiếm bạn bè thông qua nhập vào email.
Yêu cầu đặc biệt
Không
Tiền điều kiện
- Hệ thống kết nối ổn định tới internet.
- Người dùng đã đăng nhập vào hệ thống.
- Người dùng đã kết nối tới bạn bè.
Hậu điều kiện
Hệ thống hiển thị danh sách phát nhạc yêu thích của bạn bè.
Điểm mở rộng
Các thao tác như Nghe nhạc, hay thêm một bài hát vào danh sách phát yêu thích của bản thân.


4.1.3. Biểu đồ use-case
Biểu đồ usecase của người dùng:

Tổng quan:






4.2. Biểu đồ trình tự
4.2.1. Biểu đồ trình tự đăng nhập

4.2.2. Biểu đồ trình tự đăng ký

4.2.3. Biểu đồ trình tự tìm kiếm

4.2.4. Biểu đồ trình tự phát nhạc

4.2.5. Biểu đồ trình tự tách giọng hát và nhạc cụ

4.2.6. Biểu đồ trình tự tạo playlist


4.2.7. Biểu đồ trình tự tạo nghệ sĩ yêu thích (artist list)

4.3 Sơ đồ lớp

4.4 Thiết kế giao diện
4.4.1. Giao diện trang đăng ký

4.4.2. Giao diện trang đăng nhập

4.4.3. Giao diện trang chủ


4.4.4. Giao diện trang tìm kiếm

4.4.5. Giao diện trang thư viện

4.4.6. Giao diện trang nhạc yêu thích


4.4.7. Giao diện trang playlist

4.4.8. Giao diện trang tải xuống

4.4.9. Giao diện trang nghệ sĩ

4.4.10. Giao diện trang phát nhạc

4.4.11. Giao diện trang lịch sử phát


4.4.12. Giao diện trang hồ sơ

4.4.13. Giao diện trang danh sách bạn bè



4.4.14. Giao diện trang tách nhạc

4.5 Cấu trúc hệ thống


Client: Một thiết bị Mobile đã được cài đặt ứng dụng HarmonyHub thông qua dịch vụ Firebase Distribution.
Firebase Authentication: Một dịch vụ được cung cấp từ phía Firebase, cho phép xác thực danh tính tài khoản. Ở đây, dịch vụ Firebase Authentication được ứng dụng, dùng cho xác thực danh tính từ phía Client và cấp quyền truy cập vào hệ thống.
Hệ thống được triển khai trên 3 tác vụ chính:
 Spotify API: Được cung cấp từ phía Spotify, cho phép hệ thống kết nối và trả về thông tin liên quan lĩnh vực âm nhạc: Bài hát, ca/ nhạc sĩ, bảng xếp hạng xu hướng,...
 Firestore Database: Một data được xây dựng cá nhân hóa với mỗi người dùng (Chi tiết ở 4.6). Ở đây database được thiết kế, với mỗi người dùng đặc trưng bởi một UID riêng sẽ được kết nối tới một database riêng của họ. Có chứa: Danh sách bạn bè, danh sách lời mời kết bạn, nhạc yêu thích, và các playlists.
 Musical Instrument Analysis API - SplitBeat: Tương tự, cũng là một API cho phép phân tích nhạc cụ có trong một bản nhạc. Từ đó, cho phép người dùng tùy chỉnh, thêm/ bớt một nhạc cụ nào đó, tạo ra sự đa dạng và cá nhân hóa, tùy theo sở thích mỗi người.

4.6 Cơ sở dữ liệu


CSDL của ứng dụng được lưu trên Firebase, một nền tảng đám mây của Google. Firebase cung cấp nhiều dịch vụ như cơ sở dữ liệu thời gian thực, lưu trữ đám mây, xác thực người dùng cho phép tích hợp nhiều phương thức đăng nhập khác nhau, phân tích dữ liệu và gửi thông báo đẩy, giúp tiết kiệm thời gian cho việc cài đặt cơ sở hạ tầng và thêm thời gian đầu tư cho những tính năng nâng cao trải nghiệm người dùng.

Firestore database quy chuẩn các cơ sở dữ liệu theo trình tự: collection/document/collection/document…
Mỗi Collection có thể có một hoặc nhiều Document.
Mỗi Document lưu trữ thông tin về các thuộc tính được định nghĩa.
Và một Document cũng có thể một hoặc nhiều các Collections khác.

CSDL của HarmonyHub gồm 4 bảng: User, Albums, Songs và Favorites.
Bảng User gồm các thuộc tính: 
uid (UID): ID của người dùng
userName: Tên đăng nhập của người dùng
email: Email của người dùng
friends: Danh sách uid của bạn bạn bè
waiting_queue: Danh sách uid của những người gửi tới lời mời kết bạn.
Và gồm 2 collections lần lượt là: ALBUMS, FAVORITES. Trong đó, ALBUMS chứa các Album và FAVORITES chứa các Song.
Bảng Album gồm các thuộc tính:
name: Tên album
songCount: Số bài hát trong album
Gồm 1 collection là SONGS, chứa các Song.
Bảng Song gồm các thuộc tính:
songURL: URL đến các bài hát
name: Tên bài hát
artist: Nghệ sỹ
image: Hình ảnh đại diện của bài hát.
V. Cài đặt và kiểm thử
Cài đặt
Nhóm đã triển khai thông qua Firebase Distribution, cho phép cài đặt và chạy trực tiếp trên các thiết bị mobile khác nhau một cách nhanh chóng, dễ dàng và tiện lợi.
Kiểm thử
Các kịch bản kiểm thử
Kiểm thử chức năng đăng ký, đăng nhập, đăng xuất
Kiểm thử chức năng xem thông tin người dùng
Kiểm thử chức năng phát nhạc
Kiểm thử chức năng tìm kiếm bài hát
Kiểm thử chức năng xem và cập nhật playlist
Kiểm thử chức năng xem và cập nhật bài hát yêu thích
Kiểm thử chức năng xem lịch sử nghe nhạc
Kiểm thử chức năng tải bài hát
Kiểm thử chức năng kết bạn
Kiểm thử chức năng tách và ghép nhạc
Danh sách các ca kiểm thử
ID
Mô tả
TC01
Đăng ký tài khoản thành công
TC02
Đăng ký với email đã đăng nhập
TC03
Thiếu thông tin đăng ký bắt buộc
TC04
Đăng nhập hợp lệ
TC05
Đăng nhập với mật khẩu sai
TC06
Đăng xuất thành công
TC07
Xem hồ sơ người dùng
TC08
Phát nhạc
TC09
Xem playlist
TC10
Tạo mới playlist
TC11
Thêm bài hát mới vào playlist
TC12
Xóa bài hát trong playlist
TC13
Xem bài hát yêu thích
TC14
Thêm bài hát yêu thích
TC15
Bỏ yêu thích bài hát
TC16
Tải xuống bài hát
TC17
Tìm kiếm bài hát theo tên
TC18
Xem lịch sử nghe nhạc
TC19
Thêm bạn bè
TC20
Xác nhận bạn bè
TC21
Xem playlist của bạn bè
TC22
Hủy kết bạn
TC23
Tách lời và giai điệu của hai bài hát
TC24
Phát từng thành phần của hai bài hát

Chi tiết các test case
Test case 1: Đăng ký tài khoản thành công
Mô tả: Xác minh người dùng có thể đăng ký tài khoản mới thành công.
Các bước:
Truy cập trang đăng ký.
Nhập thông tin người dùng: email, tên người dùng, mật khẩu, xác nhận mật khẩu.
Chọn nút đăng ký.
Kết quả mong đợi:
Hệ thống tạo thành công một tài khoản mới với thông tin người dùng cung cấp. 
Người dùng nhận được xác nhận đăng ký thông qua email. 
Người dùng được chuyển hướng đến trang đăng nhập.
Test case 2: Đăng ký với email đã đăng nhập
Mô tả: Kiểm tra cách hệ thống xử lý khi người dùng đăng ký với địa chỉ email đã được sử dụng trước đó.
Các bước:
Truy cập trang đăng ký.
Nhập thông tin người dùng, trong đó địa chỉ email đã sử dụng trước đó.
Chọn nút đăng ký.
Kết quả mong đợi:
Hệ thống phát hiện địa chỉ email đã tồn tại trong hệ thống và hiển thị thông báo lỗi.
Người dùng không thể đăng ký với email đã được sử dụng trước đó.
Test case 3: Thiếu thông tin đăng ký bắt buộc
Mô tả: Kiểm tra cách hệ thống xử lý khi người dùng đăng ký nhưng bỏ trống thông tin.
Các bước:
Truy cập trang đăng ký.
Bỏ trống một hoặc nhiều trường thông tin hệ thống yêu cầu nhập.
Chọn nút đăng ký.
Kết quả mong đợi:
Hệ thống hiển thị thông báo lỗi yêu cầu nhập đầy đủ thông tin.
Người dùng không thể đăng ký nếu không nhập đầy đủ thông tin.
Test case 4: Đăng nhập hợp lệ
Mô tả: Kiểm tra khả năng đăng nhập của người dùng bằng thông tin đăng nhập hợp lệ.
Các bước:
Truy cập trang đăng nhập.
Nhập thông tin đăng nhập hợp lệ: email và mật khẩu.
Chọn nút đăng nhập.
Kết quả mong đợi:
Hệ thống xác nhận thông tin đăng nhập.
Người dùng được chuyển hướng đến trang chủ ứng dụng và sử dụng các chức năng của ứng dụng.
Test case 5: Đăng nhập với mật khẩu sai
Mô tả: Kiểm tra cách hệ thống xử lý khi người dùng đăng nhập với mật khẩu sai.
Các bước:
Truy cập trang đăng nhập.
Nhập thông tin đăng nhập: email hợp lệ nhưng mật khẩu sai.
Chọn nút đăng nhập.
Kết quả mong đợi:
Hệ thống hiển thị thông báo lỗi nhập sai mật khẩu.
Người dùng không thể đăng nhập và được yêu cầu nhập chính xác mật khẩu.
Test case 6: Đăng xuất thành công
Mô tả: Kiểm tra khả năng đăng xuất khỏi hệ thống của ứng dụng.
Các bước:
Đăng nhập vào ứng dụng.
Chọn nút đăng xuất.
Kết quả mong đợi:
Hệ thống xử lý yêu cầu đăng xuất và chuyển hướng người dùng đến trang đăng nhập.
Test case 7: Xem hồ sơ người dùng
Mô tả: Kiểm tra khả năng hiển thị giao diện hồ sơ người dùng.
Các bước:
Đăng nhập vào ứng dụng.
Chọn nút hồ sơ để xem thông tin người dùng.
Kết quả mong đợi:
Hệ thống hiển thị chính xác thông tin cá nhân của người dùng.
Người dùng có thể xem thông tin cá nhân của mình.
Test case 8: Phát nhạc
Mô tả: Kiểm tra xem ứng dụng có phát được nhạc theo yêu cầu của người dùng không
Các bước:
Đăng nhập vào ứng dụng
Người dùng nhấn vào một bài hát phần album/playlist, hoặc tìm kiếm bài hát ở mục Tìm kiếm
Kết quả mong đợi:
Hệ thống: Chuyển về màn hình phát bài hát người dùng vừa tìm kiếm 
Người dùng có thể nghe được bài hát mình muốn
Test case 9: Xem playlist 
Mô tả: Kiểm tra xem ứng dụng có thể hiển thị các playlist mà người dùng đã tạo không, và mỗi playlist có hiển thị được các bài hát người dùng đã thêm không
Các bước:
Đăng nhập vào ứng dụng
Chọn nút Library để xem phần playlist, album yêu thích của người dùng
Click vào một playlist
Kết quả mong đợi:
Hệ thống:
Hiển thị các playlist của người dùng
Hiển thị các bài hát trong playlist khi ấn vào playlist 
Người dùng:
Xem được các playlist mình đã tạo 
Xem được các bài hát mình đã thêm vào một playlist bất kì
Test case 10: Tạo playlist:
Mô tả:Kiểm tra người dùng có thể tạo một playlist mới hay không
Các bước:
Người dùng đăng nhập vào hệ thống
Người dùng vào mục “Library”
Vào danh mục “ Danh sách phát”
Chọn tùy chọn Tạo playlist
Nhập tên playlist mới
Nhấn nút lưu
Kết quả mong đợi:
Hệ thống thêm và hiển thị playlist mới vào mục danh sách phát
Người dùng có thể xem các playlist mới trong danh sách phát

Test case 11: Thêm bài hát mới vào playlist
Mô tả: Kiểm tra các hệ thống xử lý khi người dùng thêm bài hát mới vào playlist
Các bước:
Đăng nhập vào ứng dụng.
Chọn thư viện, nhấn chọn playlist.
Chọn playlist cần thêm bài hát mới.
Nhấn chọn thêm và chọn bài hát cần thêm.
Nhấn xác nhận.
Kết quả mong đợi:
Hệ thống thêm và hiển thị bài hát mới vào danh sách phát người dùng yêu cầu.
Người dùng có thể xem và phát bài hát mới trong danh sách phát.
Test case 12: Xóa bài hát trong playlist
Mô tả: Kiểm tra người dùng có thể thao tác để xóa một hoặc nhiều bài hát từ một playlist cụ thể hay không.
Các bước:
Đăng nhập vào ứng dụng.
Chọn nút Library.
Chọn vào danh mục Playlists.
Chọn vào một Playlist cụ thể.
Xóa một bài hát ra khỏi Playlist
Kết quả mong đợi:
Hệ thống:
Hiển thị các playlists của người.
Hiển thị các bài hát hiện có của người dùng.
Hiển thị danh sách bài hát mới cập nhật sau khi người dùng thao tác xóa.
Người dùng:
Xem được danh sách các playlist đã tạo.
Xem được các bài hát hiện có trong một playlist cụ thể nào đó.
Xem được danh sách các bài hát (đã được cập nhật) sau khi thực hiện thao tác xóa.
Test case 13: Xem bài hát yêu thích
Mô tả: Kiểm tra xem hệ thống có hiển thị đúng những bài hát yêu thích đã được chọn hay không.
Các bước:
Người dùng đăng nhập vào hệ thống.
Chọn nút Library.
Chọn vào danh mục “Đã thích”
Kết quả mong đợi:
Hệ thống:
Hiển thị số lượng bài hát yêu thích và danh sách những bài hát đó.
Người dùng: Xem được những bài hát yêu thích của bản thân.
Test case 14: Thêm bài hát yêu thích
Mô tả: Kiểm tra xem liệu người dùng có thể thêm một bài hát mới vào danh sách yêu thích của mình hay không.
Các bước: 
Người dùng đăng nhập vào hệ thống.
Người dùng tùy chọn một thẻ bài hát bất kỳ ở cửa sổ “Search”.
Người dùng chọn thêm vào yêu thích.
Người dùng chọn “Library”
Người dùng chọn danh mục: “Đã thích”.
Kết quả mong đợi:
Hệ thống:
Cập nhật bài hát mới vào danh sách yêu thích của người dùng.
Người dùng: Xem được bài hát mình đã chọn ở danh mục: “Đã thích”.
Test case 15: Bỏ yêu thích bài hát
Mô tả: Kiểm tra người dùng có thể thao tác để bỏ yêu thích một bài hát không.
Các bước:
Đăng nhập vào ứng dụng.
Chọn nút Library.
Chọn vào danh mục Đã thích.
Chọn vào một bài hát cụ thể và nhấn nút tuỳ chỉnh
Xóa một bài hát ra danh sách yêu thích
 	 	Kết quả mong đợi:
Hệ thống:
Xoá dữ liệu bài hát đã huỷ yêu thích.
Hiển thị các bài hát yêu thích hiện có của người dùng.
Hiển thị danh sách bài hát mới cập nhật sau khi người dùng thao tác xóa.
Người dùng:
Xem được danh sách bài hát yêu thích.
Xem được danh sách các bài hát yêu thích (đã được cập nhật) sau khi thực hiện thao tác xóa.

Test case 16: Tải xuống bài hát
Mô tả: Kiểm tra xem người dùng có thể tải xuống một bài hát không
Các bước:
Người dùng đăng nhập vào hệ thống
Người dùng tùy chọn một thẻ bài hát bất kỳ ở cửa sổ “Search”.
Người dùng chọn tải xuống
Vào phần “Library” 
Chọn danh mục “Tải xuống”
 Kết quả mong đợi:
Hệ thống:
Hiển thị danh sách các bài hát theo kết quả tìm kiếm trong cửa sổ "Search".
Cung cấp tùy chọn "Tải xuống" cho từng bài hát.
Tải dữ liệu bài hát về thiết bị người dùng và lưu trữ trong thư mục "Tải xuống".
Cập nhật danh mục "Tải xuống" trong "Library" khi quá trình tải hoàn tất.

Người dùng:
Người dùng có thể xem được bài hát đã được tải xuống ở danh mục “Tải xuống”

Test case 17: Tìm kiếm bài hát theo tên
Mô tả: Kiểm tra người dùng có thể thao tác tìm kiếm bài hát mong muốn không.
Các bước:
Đăng nhập vào ứng dụng.
Chọn phần Search.
Chọn vào phần tìm kiếm bài hát.
Gõ tìm kiếm bài hát mong muốn
Hiển thị các bài hát thuộc phần tìm kiếm liên quan
      -	Kết quả mong đợi:
Hệ thống:
Hiển thị danh sách bài hát phù hợp với mong muốn tìm kiếm của người dùng
Người dùng:
Người dùng có thể thấy danh sách các bài hát mong muốn với từ khóa tìm kiếm tương ứng được yêu cầu

Test case 18: Xem lịch sử nghe nhạc
Mô tả: Kiểm tra người dùng có thể thao tác để xem lịch sử bài hát không.
Các bước:
Đăng nhập vào ứng dụng.
Chọn nút Library.
Trong mục Library chọn xem tất cả.
      -	Kết quả mong đợi:
Hệ thống:
Hiển thị các bài hát đã xem của người dùng.
Người dùng:
Xem được danh sách bài hát đã xem trong phần lịch sử phát.
Test case 19: Thêm bạn bè
Mô tả: Kiểm tra xem liệu người dùng có thể thực hiện thao tác gửi lời mời kết bạn đến một người dùng khác.
Các bước:
Người dùng đăng nhập vào hệ thống.
Người dùng chọn vào danh mục “Hồ sơ”
Người dùng tiếp tục chọn danh mục “Xem tất cả bạn bè”
Người dùng chọn vào biểu tượng “+” ở cửa sổ “Danh sách bạn bè”
Người dùng nhập vào email của người dùng khác.
Người dùng chọn “Ok”
Kết quả mong đợi:
Hệ thống:
Hệ thống hiển thị danh sách bạn bè hiện có.
Hệ thống hiển thị thông báo: “Gửi lời mời thành công”
Ở danh sách chờ của người dùng đối diện, sẽ được cập nhật thêm.
Người dùng:
Người dùng xem được danh sách bạn bè hiện có.
Xem được thông báo liệu có gửi lời mời đi thành công.
Phía người dùng đối diện: Thấy được danh sách chờ có cập nhật thêm.
Test case 20: Xác nhận bạn bè
Mô tả: Kiểm tra xem thao tác xác nhận bạn bè có được thực hiện một cách chính xác bởi hệ thống.
Các bước:
Người dùng đăng nhập vào hệ thống.
Người dùng chọn vào danh mục “Hồ sơ”
Người dùng tiếp tục chọn danh mục “Xem tất cả bạn bè”
Người dùng chọn vào biểu tượng hình chuông.
Người dùng chọn vào dấu “v” xác nhận chấp nhận lời mời kết bạn.
Kết quả mong đợi:
Hệ thống:
Hệ thống hiển thị danh sách bạn bè hiện có.
Hệ thống hiển thị danh sách các lời mời.
Hệ thống xóa đi lời mời.
Hệ thống cập nhật danh sách bạn bè hiện có.
Người dùng:	
Người dùng xem được danh sách bạn bè hiện có.
Người dùng xem được danh sách lời mời kết bạn.
Người dùng cả 2 phía (Phía gửi lời mời và chấp nhận lời mời) xem được danh sách bạn bè mới cập nhật.
Test case 21: Xem playlist của bạn bè
Mô tả: Kiểm tra xem thao tác xóa bạn bè có được thực hiện một cách chính xác bởi hệ thống.
Các bước:
Người dùng đăng nhập vào hệ thống.
Người dùng chọn vào danh mục “Hồ sơ”
Người dùng tiếp tục chọn danh mục “Xem tất cả bạn bè”
Người dùng chọn một thẻ một người bạn bất kỳ.
Kết quả mong đợi:
Hệ thống:
Hệ thống hiển thị danh sách bạn bè hiện có.
Hệ thống hiển thị danh sách nhạc của phía người bạn.
Người dùng:
Người dùng xem được danh sách bạn bè hiện có.
Người dùng xem được danh sách các bản nhạc yêu thích của một người bạn bất kỳ.
Test case 22: Hủy kết bạn
Mô tả: Kiểm tra cách hệ thống xử lý khi người dùng hủy kết bạn.
Các bước:
Đăng nhập vào ứng dụng.
Vào trang hồ sơ, chọn xem tất cả bạn bè.
Chọn một người bạn trong danh sách bạn bè, chọn tùy chọn hủy kết bạn.
Kết quả mong đợi:
Hệ thống 
Xóa người bạn khỏi danh sách bạn bè sau khi người dùng chọn hủy kết bạn.
Cập nhật danh sách bạn bè.
Người dùng không thể thấy bạn đã xóa và xem nhạc yêu thích của họ trong danh sách bạn bè.
Test case 23: Tách lời và giai điệu của bài hát
Mô tả: Kiểm tra cách hệ thống tách lời và giai điệu của bài hát.
Các bước:
Đăng nhập vào ứng dụng.
Nhấn chọn tách nhạc.
Chọn hai bài hát và nhấn xác nhận để tách.
Kết quả mong đợi:
Hệ thống tách và hiển thị từng bài hát thành 4 phần: bass, beat, vocal, drum.
Người dùng thấy được từng phần tách của hai bài hát gốc.
Test case 24: Phát từng thành phần của hai bài hát.
Mô tả: Kiểm tra cách hệ thống phát từng thành phần của hai bài hát đã tách.
Các bước:
Đăng nhập vào ứng dụng.
Nhấn chọn tách nhạc.
Chọn hai bài hát và nhấn xác nhận để tách.
Chọn thành phần đã tách muốn phát và nhấn chọn phát.
Kết quả mong đợi:
Hệ thống phát đồng thời các thành phần đã chọn.
Người dùng có thể nghe được các thành phần đã chọn cùng lúc.
VI. Đánh giá và kết luận
Ưu điểm
Giao diện thân thiện, dễ sử dụng.
Đáp ứng cơ bản chức năng nghe nhạc.
Tích hợp chức năng nâng cao như: tách giọng hát và âm thanh từ hai bài hát gốc và phối chúng lại; chức năng bạn bè.
Nhược điểm
Mới chỉ tách được nhạc cụ gồm trống và bass khỏi giọng hát từ bài hát gốc.
Đôi lúc ứng dụng phản hồi còn chậm
Định hướng phát triển
Phát triển tiếp tính năng tách nhạc để người dùng có thể phối được nhiều nhạc cụ hơn, như piano, violin,...
Phát triển ứng dụng trên iOS
