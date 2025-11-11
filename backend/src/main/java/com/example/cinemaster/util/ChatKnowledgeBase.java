package com.example.cinemaster.util;



import java.util.ArrayList;
import java.util.List;

/**
 * Bộ kiến thức tĩnh (không cần DB) cho CineMaster Chatbot.
 * Dữ liệu này sẽ được load khi khởi động app.
 */
public final class ChatKnowledgeBase {

    private ChatKnowledgeBase() {}

    public static List<String> getStaticRules() {
        List<String> data = new ArrayList<>();

        //  I. Thông tin phim
        data.add("Khách có thể xem trailer và tóm tắt nội dung phim ngay trên trang chi tiết phim tại website CineMaster hoặc ứng dụng di động.");
        data.add("Các phim tại CineMaster được phân loại độ tuổi theo quy định: P (mọi độ tuổi), K (dưới 13 tuổi cần phụ huynh đi cùng), T13 (trên 13 tuổi), T16 (trên 16 tuổi), T18 (trên 18 tuổi).");
        data.add("Phim có thể được chiếu ở nhiều định dạng: 2D, 3D, IMAX, 4DX, Gold Class, L’amour tuỳ rạp.");
        data.add("Mỗi phim có thể có suất chiếu phụ đề hoặc lồng tiếng, bạn có thể chọn loại khi đặt vé.");
        data.add("Phim sẽ được chiếu tại rạp theo khoảng thời gian của từng kỳ chiếu (Screening Period). Khi hết kỳ chiếu, phim sẽ ngừng phát sóng.");

        //  II. Quy trình đặt vé
        data.add("Bạn có thể đặt vé online qua website hoặc ứng dụng CineMaster. Chọn phim, suất chiếu, ghế ngồi và tiến hành thanh toán.");
        data.add("Khách hàng có thể chọn ghế ngồi trước khi thanh toán, hệ thống hiển thị sơ đồ ghế theo thời gian thực.");
        data.add("Một giao dịch online cho phép đặt tối đa 8 ghế.");
        data.add("Sau khi chọn ghế, bạn có 10 phút để hoàn tất thanh toán.");
        data.add("Sau khi thanh toán thành công, hệ thống gửi mã vé (QR code) qua email và mục Vé Của Tôi.");
        data.add("Khi đến rạp, bạn chỉ cần quét QR code hoặc đọc mã vé tại quầy để vào phòng chiếu.");

        //  III. Giá vé, thanh toán & khuyến mãi
        data.add("Giá vé tiêu chuẩn dao động từ 70.000–120.000 VNĐ tùy suất chiếu, rạp và định dạng (2D/3D), nếu muốn áp dụng thì bạn đến mua vé tại quầy nhé.");
        data.add("Sinh viên, học sinh được giảm 20% giá vé vào các ngày trong tuần khi xuất trình thẻ.");
        data.add("Ghế VIP hoặc ghế đôi (Sweetbox) có giá cao hơn 20–30% so với ghế thường.");
        data.add("CineMaster hỗ trợ thanh toán bằng tiền mặt, thẻ ATM, Visa/Mastercard và ví điện tử như Momo, ZaloPay, ShopeePay.");
        data.add("Khách có thể nhập mã giảm giá hoặc voucher tại bước thanh toán online để được giảm giá.");
        data.add("Combo bắp nước được mua trực tiếp tại quầy hoặc cùng với vé khi đặt online.");
        data.add("Thành viên có thể thanh toán bằng điểm tích lũy để mua vé hoặc combo.");

        //  IV. Thay đổi, hủy, hoàn vé
        data.add("Vé đã mua không thể hủy hoặc hoàn tiền, trừ khi rạp hủy suất chiếu vì lý do kỹ thuật hoặc bất khả kháng.");
        data.add("Khách có thể đổi suất chiếu trước 4 tiếng nếu còn ghế trống.");
        data.add("Nếu bạn bị lỡ suất chiếu, vé sẽ không được hoàn hoặc đổi sang suất khác.");
        data.add("Nếu bị mất mã vé, bạn có thể tra lại trong tài khoản thành viên hoặc liên hệ quầy hỗ trợ.");

        //  V. Tài khoản thành viên
        data.add("Bạn có thể đăng ký tài khoản thành viên miễn phí trên website hoặc ứng dụng CineMaster.");
        data.add("Khi đăng nhập và mua vé online, bạn sẽ được tích điểm thưởng tương ứng với giá trị giao dịch.");
        data.add("Bạn có thể kiểm tra điểm và hạng thành viên trong mục Hồ Sơ Cá Nhân.");
        data.add("Các hạng thành viên (Silver, Gold, Platinum) có quyền lợi riêng như giảm giá vé và combo.");

        //  VI. Quy tắc tại rạp
        data.add("Khách hàng không được mang đồ ăn có mùi hoặc thức uống có cồn vào phòng chiếu.");
        data.add("Vui lòng giữ trật tự và tắt chuông điện thoại trong khi xem phim.");
        data.add("Không quay phim, chụp ảnh hoặc ghi âm trong rạp chiếu.");
        data.add("Trẻ em dưới 13 tuổi cần có người giám hộ đi cùng nếu xem phim T13 trở lên.");
        data.add("Nếu gặp sự cố kỹ thuật (âm thanh, hình ảnh), vui lòng báo ngay cho nhân viên để được hỗ trợ.");
        data.add("Check-in vé được thực hiện tại cửa phòng chiếu bằng QR code hoặc mã vé.");

        return data;
    }
}
