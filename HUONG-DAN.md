# GPS Đo Đường — ứng dụng Android tiếng Việt

Bản mã nguồn 1.0 · dành cho Android 8.0 trở lên, điện thoại có GPS.

**Bộ này là mã nguồn, chưa có APK cài ngay.** Môi trường tạo bộ nguồn thiếu Android SDK và không tải được công cụ build. Logic đo đã chạy qua 27 kiểm tra; toàn bộ 4 tệp Java đã được kiểm tra cú pháp. Chưa biên dịch toàn bộ bằng Android SDK, chưa thử trên điện thoại hoặc máy ảo. Cần tạo APK và kiểm tra thực tế theo các bước dưới đây.

## Ứng dụng làm gì?

- Hiện tốc độ GPS hiện tại theo km/h, chữ lớn.
- Bấm Bắt đầu tại đầu đoạn, Kết thúc → Lưu kết quả tại cuối đoạn.
- Đo tổng quãng đường đã đi bằng các điểm GPS liên tiếp, hiển thị m hoặc km.
- Tốc độ trung bình, tốc độ cao nhất và thời gian của riêng đoạn đã chọn.
- Tạm dừng / Tiếp tục: bỏ qua thời gian và chuyển động trong lúc tạm dừng.
- Đặt tên đoạn, lưu tối đa 100 kết quả mới nhất trên máy, xuất CSV để mở bằng Excel.
- Có thông báo khi GPS hoạt động; dùng foreground service để tiếp tục đo khi màn hình tắt.
- Lưu tạm mỗi 5 giây. Nếu hệ thống ngắt tiến trình, khi bật GPS lần sau, khôi phục phần đã lưu vào lịch sử và đánh dấu chưa đầy đủ. Không tự tiếp tục đo sau khi bị ngắt.
- Không dùng tài khoản, quảng cáo, bản đồ trực tuyến hay máy chủ. Ứng dụng không xin quyền Internet. Không lưu tọa độ vào lịch sử, chỉ lưu chỉ số tổng hợp và tên đoạn.

## Cách tạo APK trên Windows

Cần làm một lần trên máy tính có Internet, sau đó chép APK sang điện thoại.

1. Cài **Android Studio** từ https://developer.android.com/studio và hoàn tất cài đặt lần đầu.
2. Trong **SDK Manager**, cài **Android 15 / API 35 (SDK Platform)** và **Android SDK Build-Tools 35.0.0**. Ghi nhớ đường dẫn Android SDK. Mặc định là `%LOCALAPPDATA%\Android\Sdk`.
3. Giải nén bộ này vào một thư mục, ví dụ `C:\GPS-DoDuong`. Không chạy bên trong tệp ZIP.
4. Mở `Tao-APK-Windows.cmd`. Chương trình dùng JDK đi kèm Android Studio, tải Gradle 8.11.1 từ nguồn chính thức, kiểm tra SHA256, tạo Gradle wrapper, chạy kiểm tra và build APK.
5. Khi thấy **DA TAO APK**, lấy `GPS-DoDuong.apk` ở thư mục gốc.

Nếu PowerShell chặn tệp tải từ Internet, kiểm tra mã nguồn và hướng dẫn quản lý script trên máy của bạn; không thay đổi chính sách của cơ quan. Bạn có thể dùng cách GitHub bên dưới hoặc nhờ kỹ thuật viên build bằng Gradle đã cài.

Nếu JDK/SDK nằm ở vị trí khác, đặt `JAVA_HOME` trỏ tới JDK 17 trở lên và `ANDROID_HOME` trỏ tới Android SDK trong biến môi trường Windows. Nếu thiếu gói SDK hoặc chưa chấp nhận giấy phép SDK, mở SDK Manager để cài và chấp nhận trước khi chạy lại.

Sau lần chạy script đầu tiên tạo được Gradle wrapper, có thể mở thư mục dự án bằng Android Studio để sửa và build. Các tệp wrapper sẽ được tạo trên máy của bạn; chúng chưa có sẵn trong bộ nguồn này.

## Cách tạo APK bằng GitHub

Dành cho người đã dùng GitHub, không cần cài Android Studio trên máy.

1. Tạo một repository riêng và đưa **nội dung bên trong** thư mục `GPS-DoDuong` vào gốc repository, gồm thư mục `.github`.
2. Mở **Actions → Tao APK Android → Run workflow**.
3. Khi workflow thành công, tải artifact **GPS-DoDuong-APK**, giải nén và lấy `app-debug.apk`.

Workflow đã được viết sẵn nhưng chưa chạy trong phiên tạo nguồn này. Môi trường runner phải có Android SDK command-line tools; runner Ubuntu của GitHub thường cung cấp sẵn. Nếu không có `sdkmanager`, cần cài Android command-line tools trước bước Install SDK components.

## Cài và sử dụng trên điện thoại

1. Chép APK được tạo thành công sang điện thoại, mở tệp và cho phép ứng dụng quản lý tệp cài ứng dụng từ nguồn này khi Android yêu cầu.
2. Mở **GPS Đo Đường**, bật **Vị trí** trên điện thoại, bấm **Bật GPS**.
3. Cấp **Vị trí chính xác → Chỉ cho phép khi dùng ứng dụng**. Android 13 trở lên có thể hỏi thêm quyền thông báo; nên cho phép để thấy trạng thái đo.
4. Ra ngoài trời, chờ dòng **GPS tốt**. Tốc độ chưa xác định sẽ hiện dấu **—**, không giả định là 0.
5. Nhập tên đoạn nếu cần. Tại điểm đầu bấm **Bắt đầu đoạn đường**.
6. Tại điểm cuối bấm **Kết thúc → Lưu kết quả**. Thời điểm kết thúc là lúc bấm **Lưu kết quả** trong hộp thoại.
7. Mở **Lịch sử các đoạn đã đo** để xem hoặc **Xuất CSV**.

Khi rời màn hình ứng dụng mà chưa bắt đầu đo, GPS xem trước sẽ tắt để tiết kiệm pin. Khi đang đo hoặc tạm dừng, dịch vụ GPS được duy trì. Sau khi lưu kết quả và rời ứng dụng, dịch vụ dừng. Trong lúc ứng dụng mở, màn hình được giữ sáng; có thể tắt bằng nút nguồn khi đang đo.

Một số hãng điện thoại có chế độ tiết kiệm pin mạnh có thể làm gián đoạn GPS dù có thông báo. Cần kiểm tra trên máy sử dụng thực tế. Khi bị thu hồi quyền vị trí hoặc buộc dừng ứng dụng, ứng dụng không thể tiếp tục đo.

## Cách tính và giới hạn

- Tốc độ: ưu tiên tốc độ do Android GPS cung cấp (m/s × 3,6). Nếu thiếu tốc độ có thể ước tính từ hai điểm đủ xa so với sai số; nếu không đủ tin cậy hiện **—**.
- Quãng đường: cộng các khoảng cách cầu lớn giữa các điểm GPS được chấp nhận. Không chỉ lấy đường thẳng từ điểm đầu tới điểm cuối; cũng không tự khớp theo bản đồ.
- Trung bình: quãng đường đã ghi nhận / toàn bộ thời gian đang đo, bao gồm lúc đứng yên, loại thời gian tạm dừng.
- Chỉ nhận điểm có sai số vị trí báo cáo tối đa 25 m; bỏ điểm cũ, sai thứ tự, tọa độ không hợp lệ, tốc độ trên 360 km/h hoặc bước nhảy phi lý. Đây là ngưỡng lọc của bản 1.0, không phải cam kết độ chính xác.
- Khi GPS báo tốc độ dưới 0,5 m/s, không cộng dịch chuyển để hạn chế trôi khi đứng yên. Vì vậy di chuyển rất chậm có thể bị đo thiếu.
- Nếu quá 5 giây không có điểm mới đủ tốt, hiển thị mất GPS và đánh dấu đoạn chưa đầy đủ. Không nối tắt qua khoảng mất GPS. Thời gian đo vẫn chạy, nên tốc độ trung bình có thể thấp hơn thực tế; kết quả được cảnh báo rõ.
- Thông số ±m là sai số do Android báo cáo, không phải chứng nhận khoảng cách đo.
- GPS lấy mẫu xấp xỉ mỗi giây: điểm bấm bắt đầu / tiếp tục dùng vị trí hợp lệ gần nhất, nên có sai lệch tại hai đầu và khi vào cua. Các đoạn rất ngắn hoặc nhiều khúc cua sẽ có sai số tương đối cao.
- Lịch sử lưu dữ liệu trong ứng dụng; gỡ ứng dụng sẽ xóa dữ liệu. Khi đủ 100 đoạn, đoạn cũ nhất được thay thế. Xuất CSV nếu cần giữ lâu dài.
- Đây là công cụ ước tính phục vụ cá nhân, chưa hiệu chuẩn. Hãy thao tác khi đã dừng xe an toàn.

## Kiểm tra trước khi dùng thường xuyên

| Tình huống | Kết quả cần kiểm tra trên điện thoại |
|---|---|
| Không cấp quyền / chỉ cấp vị trí gần đúng | Không bắt đầu đo; hướng dẫn cấp vị trí chính xác |
| GPS tắt | Hiện trạng thái GPS tắt; chờ bật lại |
| Đứng yên ngoài trời vài phút | Quãng đường không tăng đáng kể do trôi GPS |
| Đi đoạn có chiều dài đã biết | So sánh sai số thực tế với mốc đo tham chiếu |
| Tạm dừng rồi di chuyển | Không cộng đoạn di chuyển khi tạm dừng |
| Tắt màn hình khi đang đo | Thông báo còn hoạt động, số liệu tiếp tục cập nhật khi mở lại |
| Vào hầm / mất GPS | Cảnh báo đoạn chưa đầy đủ, không nối tắt phần mất tín hiệu |
| Xoay màn hình / mở ứng dụng lại | Phiên đang đo còn nguyên nhờ dịch vụ |
| Kết thúc và khởi động lại | Kết quả xuất hiện trong lịch sử |
| Tiến trình bị hệ thống ngắt | Bật GPS lần sau khôi phục bản lưu tạm và đánh dấu chưa đầy đủ |
| Xuất CSV có tên tiếng Việt | Mở được dấu tiếng Việt trong Excel; nếu cần nhập dạng UTF-8, dấu phân cách phẩy |

## Dành cho kỹ thuật viên

- Native Java, Android framework; không phụ thuộc Google Play Services hoặc thư viện giao diện bên ngoài.
- Android Gradle Plugin 8.9.2, Gradle 8.11.1, compile/target SDK 35, min SDK 26, Java 17.
- Build: `gradle engineTest :app:assembleDebug` với Gradle/SDK/JDK đã cài; hoặc `./gradlew engineTest :app:assembleDebug` sau khi đã tạo wrapper.
- Test logic riêng: `javac -encoding UTF-8 -d out app/src/main/java/vn/gps/doduong/TripEngine.java tests/TripEngineTest.java` rồi `java -cp out TripEngineTest`.
- APK được tạo là bản debug có chữ ký phát triển để cài thử, chưa phải bản phát hành Google Play. Muốn phát hành/cập nhật lâu dài cần khóa ký riêng và kiểm tra yêu cầu cửa hàng tại thời điểm phát hành.
- Workflow GitHub tạo debug key mới trên runner mới; các APK từ các lần chạy khác nhau có thể không cài đè nhau. Muốn cài đè giữ dữ liệu cần dùng cùng khóa ký; xuất lịch sử trước khi gỡ bản cũ.
- Không có APK dựng sẵn và không có tuyên bố đã kiểm thử trên Android thật trong bộ này.

## Tài liệu chính thức đã đối chiếu

- Quyền vị trí và foreground location: https://developer.android.com/develop/sensors-and-location/location/permissions
- Foreground service loại location: https://developer.android.com/develop/background-work/services/fgs/service-types#location
- Android Location: https://developer.android.com/reference/android/location/Location
- Tương thích AGP 8.9: https://developer.android.com/build/releases/agp-8-9-0-release-notes
- Chạy ứng dụng Android: https://developer.android.com/studio/run
- Build Gradle trên GitHub Actions: https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle
