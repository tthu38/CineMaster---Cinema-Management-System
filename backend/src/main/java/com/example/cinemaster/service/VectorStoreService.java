package com.example.cinemaster.service;

import com.example.cinemaster.entity.DocumentChunk;
import com.example.cinemaster.util.ChatKnowledgeBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private final EmbeddingService embeddingService;

    private final List<DocumentChunk> vectorStore = new ArrayList<>();

    public VectorStoreService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
        initializeVectorStore();
    }

    // Phương thức giả lập Indexing dữ liệu phi cấu trúc
    private void initializeVectorStore() {
        System.out.println("⏳ Indexing dữ liệu mẫu...");
        ChatKnowledgeBase.getStaticRules().forEach(rule -> addDocumentChunk(rule, "STATIC_RULE"));
        addDocumentChunk("Giá vé cho học sinh sinh viên là 50.000 VNĐ, áp dụng từ Thứ Hai đến Thứ Năm, chỉ cần xuất trình thẻ.", "GIA_VE");
        addDocumentChunk("Khách hàng được phép mang theo nước uống đóng chai, nhưng không được mang đồ ăn nóng bên ngoài vào rạp.", "QUY_TAC");
        addDocumentChunk("Chính sách hoàn tiền: Vé đã mua không được hoàn trả, trừ trường hợp rạp hủy suất chiếu vì lý do bất khả kháng.", "CHINH_SACH_HUY");
        addDocumentChunk("Các chương trình khuyến mãi thường xuyên: Đồng giá cho thành viên vào các ngày thứ 3 hàng tuần.", "KHUYEN_MAI");
        System.out.println("✅ Indexing hoàn tất. " + vectorStore.size() + " chunks đã được nhúng.");
    }

    private void addDocumentChunk(String content, String source) {
        List<Double> vector = embeddingService.embedText(content);
        if (!vector.isEmpty()) {
            DocumentChunk chunk = new DocumentChunk(String.valueOf(vectorStore.size()), content, vector, source);
            vectorStore.add(chunk);
        }
    }

    /**
     * Tìm kiếm Top K đoạn văn bản gần nhất với câu hỏi của người dùng.
     */
    public List<String> searchSimilarDocuments(String query, int topK) {
        if (vectorStore.isEmpty()) return List.of();

        // 1️⃣ Lọc sơ bộ theo từ khóa
        String q = query.toLowerCase();
        List<DocumentChunk> filtered = vectorStore.stream()
                .filter(chunk ->
                        chunk.getContent().toLowerCase().contains(q)
                                || (q.matches(".*(hủy|hoàn tiền|refund).*") && chunk.getSource().contains("CHINH_SACH"))
                                || (q.matches(".*(combo|bắp|nước).*") && chunk.getSource().contains("STATIC_RULE"))
                                || (q.matches(".*(giá vé|sinh viên|giảm giá|khuyến mãi).*") && chunk.getSource().contains("KHUYEN_MAI"))
                                || (q.matches(".*(đặt vé|chọn ghế).*") && chunk.getSource().contains("STATIC_RULE"))
                )
                .collect(Collectors.toList());

        if (filtered.isEmpty()) filtered = vectorStore;

        // 2️⃣ Nhúng câu hỏi người dùng thành vector
        List<Double> queryVector = embeddingService.embedText(query);
        if (queryVector.isEmpty()) return List.of();

        // 3️⃣ Tính điểm tương đồng
        List<DocumentChunk> ranked = filtered.stream()
                .map(chunk -> {
                    double score = cosineSimilarity(queryVector, chunk.getEmbedding());
                    return new DocumentChunk(
                            chunk.getId(),
                            chunk.getContent(),
                            chunk.getEmbedding(),
                            chunk.getSource(),
                            score
                    );
                })
                .sorted(Comparator.comparingDouble(DocumentChunk::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        // ✅ 4️⃣ Trả về danh sách văn bản có gắn nhãn nguồn
        return ranked.stream()
                .map(chunk -> String.format("[Nguồn: %s] %s", chunk.getSource(), chunk.getContent()))
                .collect(Collectors.toList());
    }
    // Hàm hỗ trợ: Tính Cosine Similarity
    private double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        // Kiểm tra kích thước vector
        if (vecA.size() != vecB.size()) return 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            dotProduct += vecA.get(i) * vecB.get(i);
            normA += vecA.get(i) * vecA.get(i);
            normB += vecB.get(i) * vecB.get(i);
        }
        if (normA == 0 || normB == 0) return 0.0;
        // Ép kiểu về double trước khi tính căn bậc hai để tránh lỗi
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public void ingestDocuments(String domain, List<String> docs) {
        if (docs == null || docs.isEmpty()) {
            System.out.println("⚠️ Không có dữ liệu nào để ingest cho domain: " + domain);
            return;
        }

        System.out.println("🚀 Bắt đầu ingest " + docs.size() + " documents vào domain: " + domain);

        for (String content : docs) {
            addDocumentChunk(content, domain);
        }

        System.out.println("✅ Hoàn tất ingest domain: " + domain + " | Tổng số chunk hiện tại: " + vectorStore.size());
    }
}
