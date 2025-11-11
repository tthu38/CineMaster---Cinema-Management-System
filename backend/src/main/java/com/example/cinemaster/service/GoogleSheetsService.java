package com.example.cinemaster.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class GoogleSheetsService {

    private final Sheets sheetsService;
    private final String spreadsheetId = "1SogFx3pztZTmUroh9ra0zst08l_0AksbrK-RekXuKdM";

    public GoogleSheetsService() throws Exception {
        this.sheetsService = createSheetsService();
    }

    private Sheets createSheetsService() throws Exception {
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = getClass().getResourceAsStream("/credentials/service-account.json");
        if (in == null)
            throw new RuntimeException("️ Không tìm thấy file service-account.json trong resources/credentials/");

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
        String range = "A:J";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> rows = response.getValues();
        if (rows == null || rows.isEmpty()) return Map.of("found", "false");

        String normalizedCode = normalize(code);
        System.out.println(" Đang tìm code: " + normalizedCode);

        for (int i = 1; i < rows.size(); i++) {
            List<Object> r = rows.get(i);
            String codeTT = r.size() > 4 ? normalize(r.get(4).toString()) : "";
            String note = r.size() > 5 ? normalize(r.get(5).toString()) : "";
            String amount = r.size() > 7 ? r.get(7).toString() : "";

            // debug log
            System.out.printf(" Dòng %d | CodeTT=[%s] | Note=[%s]%n", i + 1, codeTT, note);

            if (codeTT.contains(normalizedCode) || note.contains(normalizedCode)) {
                System.out.println(" Tìm thấy giao dịch tại dòng " + (i + 1));
                return Map.of(
                        "found", "true",
                        "rowIndex", String.valueOf(i + 1),
                        "codeTT", r.size() > 4 ? r.get(4).toString() : "",
                        "note", r.size() > 5 ? r.get(5).toString() : "",
                        "amount", amount
                );
            }
        }
        System.out.println(" Không tìm thấy giao dịch cho code: " + normalizedCode);

        return Map.of("found", "false");

    }

    private String normalize(String s) {
        if (s == null) return "";
        return s
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase()
                .trim();
    }



}
