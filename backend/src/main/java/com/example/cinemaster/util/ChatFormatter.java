package com.example.cinemaster.util;

/**
 * ChatFormatter giúp chuẩn hóa cách hiển thị Markdown giữa backend ↔ frontend.
 * (Frontend bạn dùng marked.js thì Markdown này hiển thị cực đẹp.)
 */
public final class ChatFormatter {
    private ChatFormatter() {}

    /** In đậm tiêu đề */
    public static String mdTitle(String t) {
        return "\n**" + t + "**\n";
    }

    /** Dạng key-value, hiển thị danh sách đẹp */
    public static String kv(String k, String v) {
        return "- " + k + ": " + (v == null || v.isBlank() ? "N/A" : v) + "\n";
    }

    /** Bọc JSON hoặc dữ liệu cấu trúc vào khối code */
    public static String jsonBlock(String json) {
        return "```json\n" + json + "\n```";
    }

    /** Kẻ phân cách nhẹ */
    public static String divider() {
        return "\n---\n";
    }

    /** Đánh dấu icon */
    public static String emoji(String e, String text) {
        return e + " " + text + "\n";
    }
}
