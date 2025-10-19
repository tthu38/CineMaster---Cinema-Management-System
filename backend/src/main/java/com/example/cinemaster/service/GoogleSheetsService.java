package com.example.cinemaster.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Service
public class GoogleSheetsService {

    private final Sheets sheetsService;
    // thay bằng spreadsheetId của bạn (từ URL)
    private final String spreadsheetId = "1SogFx3pztZTmUroh9ra0zst08l_0AksbrK-RekXuKdM";

    public GoogleSheetsService() throws Exception {
        this.sheetsService = createSheetsService();
    }

    private Sheets createSheetsService() throws Exception {
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = getClass().getResourceAsStream("/credentials/service-account.json");
        if (in == null)
            throw new RuntimeException("⚠️ Không tìm thấy file service-account.json trong resources/credentials/");

        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets.readonly"));
        return new Sheets.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName("CineMaster-GSheets")
                .build();
    }

    /**
     * Tìm xem có giao dịch nào chứa code ở cột Code TT (E) hoặc nội dung (F).
     * Trả về thông tin nếu tìm thấy: (found true) and a map chứa các trường quan trọng.
     */
    public Map<String, String> findTransactionByCode(String code) throws Exception {
        // đọc nhiều cột (A..J) vì sheet của bạn có nhiều trường
        String range = "A:J";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> rows = response.getValues();
        if (rows == null || rows.isEmpty()) return Map.of("found","false");

        // bắt đầu từ row 2 (row 1 là header)
        for (int i = 1; i < rows.size(); i++) {
            List<Object> r = rows.get(i);
            // cột E (index 4) = Code TT, cột F (index 5) = nội dung, cột H (index 7) = số tiền
            String codeTT = r.size() > 4 ? r.get(4).toString() : "";
            String note = r.size() > 5 ? r.get(5).toString() : "";
            String amount = r.size() > 7 ? r.get(7).toString() : "";
            if ((codeTT != null && codeTT.contains(code)) || (note != null && note.contains(code))) {
                return Map.of(
                        "found","true",
                        "rowIndex", String.valueOf(i+1), // người dùng dễ debug
                        "codeTT", codeTT,
                        "note", note,
                        "amount", amount
                );
            }
        }
        return Map.of("found","false");
    }
}
