package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.services.interfaces.ContractService;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ContractServiceImpl implements ContractService {

    @Override
    public byte[] generateContract(Appointment appointment) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("SERVICE BOOKING AGREEMENT", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // 2. Formatters
            // Use this for LocalDateTime (Date + Time)
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            // Use this for LocalDate (Date only) to avoid the "HourOfDay" crash
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 3. Appointment Details
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // "now()" is LocalDateTime, so it works with time
            document.add(new Paragraph("Date of Agreement: " + LocalDateTime.now().format(dateTimeFormatter), regularFont));

            document.add(new Paragraph("Client Name: " + appointment.getAppUser().getName() + " " + appointment.getAppUser().getSurname(), regularFont));
            document.add(new Paragraph("Service: " + appointment.getService().getName(), regularFont));

            // CRITICAL FIX:
            // Check if we can format with time, otherwise fallback to date only
            String scheduledString;
            try {
                // Try formatting with time
                scheduledString = appointment.getScheduledAt().format(dateTimeFormatter);
            } catch (Exception e) {
                // If it fails (because it's just a LocalDate), format as date only
                scheduledString = appointment.getScheduledAt().format(dateFormatter);
            }
            document.add(new Paragraph("Scheduled For: " + scheduledString, regularFont));

            document.add(new Paragraph("Price: " + appointment.getService().getPrice() + " PLN", regularFont));
            document.add(new Paragraph("\n"));

            // 4. Terms & Conditions
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("TERMS AND CONDITIONS", boldFont));
            document.add(new Paragraph("1. The Client agrees to pay the total amount listed above for the services rendered.", regularFont));
            document.add(new Paragraph("2. Cancellations must be made at least 24 hours prior to the scheduled appointment time.", regularFont));
            document.add(new Paragraph("3. Late arrivals of more than 15 minutes may result in a shortened service or cancellation.", regularFont));
            document.add(new Paragraph("\n"));

            // 5. Digital Signature Record
            document.add(new Paragraph("DIGITAL ACCEPTANCE RECORD", boldFont));
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            String signText = "This agreement was accepted digitally by the Client via the Glamour Studio booking platform. " +
                    "By checking the 'I accept Terms & Conditions' box and clicking 'Book', the Client has entered into this binding agreement.";

            document.add(new Paragraph(signText, smallFont));
            document.add(new Paragraph("Timestamp: " + LocalDateTime.now().format(dateTimeFormatter), smallFont));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            // Log the actual error to help debug future issues
            e.printStackTrace();
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }
}