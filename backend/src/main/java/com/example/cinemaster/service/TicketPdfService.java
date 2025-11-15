package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.TicketPdfRequest;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;

@Service
public class TicketPdfService {

    public byte[] generatePdf(TicketPdfRequest req) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Thermal 80mm size
        Rectangle receipt = new Rectangle(226, 700);
        Document document = new Document(receipt, 10, 10, 10, 10);
        PdfWriter.getInstance(document, baos);

        document.open();

        // ===== FONTS =====
        Font mono = new Font(Font.COURIER, 10, Font.NORMAL, Color.BLACK);
        Font bold = new Font(Font.COURIER, 11, Font.BOLD, Color.BLACK);
        Font bigBold = new Font(Font.COURIER, 13, Font.BOLD, Color.BLACK);

        // ===== LOGO =====
        Paragraph logo = new Paragraph("CINEMASTER\n", bigBold);
        logo.setAlignment(Element.ALIGN_CENTER);
        document.add(logo);

        // ===== TITLE =====
        Paragraph title = new Paragraph("VE XEM PHIM\n", bold);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("Công ty cổ phần phim Thiên Ngân", mono));
        document.add(new Paragraph("MST: 0101595861-004", mono));
        document.add(new Paragraph(req.getBranchName(), mono));

        document.add(line());

        // ===== BRANCH NAME =====
        Paragraph bname = new Paragraph(req.getBranchName() + "\n", bold);
        bname.setAlignment(Element.ALIGN_CENTER);
        document.add(bname);

        // ===== MOVIE =====
        Paragraph movie = new Paragraph(req.getMovieTitle() + "\n", bold);
        movie.setAlignment(Element.ALIGN_CENTER);
        document.add(movie);

        // ===== DATE & TIME =====
        document.add(new Paragraph("Ngày: " + req.getShowDate(), mono));
        document.add(new Paragraph("Suất chiếu: " + req.getShowTime(), mono));
        document.add(space());

        // ===== CINEMA + SEAT =====
        document.add(new Paragraph("Phòng: " + req.getAuditoriumName(), bold));
        document.add(new Paragraph("Ghế: " + req.getSeat(), bold));
        // ===== COMBO =====
        if (req.getCombos() != null && !req.getCombos().isEmpty()) {
            document.add(new Paragraph("Combo: " + String.join(", ", req.getCombos()), bold));
        } else {
            document.add(new Paragraph("Combo: Không có", bold));
        }
        document.add(space());


        // ===== PRICE =====
        Paragraph p = new Paragraph(req.getPaymentMethod() + "     " + req.getPrice() + " VND", bold);
        document.add(p);

        document.add(line());

        // ===== TRANSACTION =====
        document.add(new Paragraph("Trans Time: " + req.getTransactionTime(), mono));

        document.add(line());
        document.add(space());

        // ===== QR CODE (ZXing) =====
        String qrText = req.getMovieTitle() + "|" + req.getSeat() + "|" + req.getShowTime();

        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix matrix = qrWriter.encode(qrText, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);

        Image qrImg = Image.getInstance(pngOut.toByteArray());
        qrImg.scaleAbsolute(110, 110);
        qrImg.setAlignment(Image.ALIGN_CENTER);

        document.add(qrImg);

        // ===== NOTE =====
        Paragraph note = new Paragraph(
                "Vui long quet ma QRCode nay de vao rap\nCam on Quy Khach & hen gap lai.\n",
                mono
        );
        note.setAlignment(Element.ALIGN_CENTER);
        document.add(note);


        document.close();
        return baos.toByteArray();
    }

    private Paragraph line() {
        return new Paragraph("----------------------------------------", new Font(Font.COURIER, 9));
    }

    private Paragraph space() {
        return new Paragraph("\n");
    }
}