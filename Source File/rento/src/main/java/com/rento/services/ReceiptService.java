package com.rento.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rento.models.Booking;
import com.rento.models.Payment;
import com.rento.models.Rental;
import com.rento.utils.MongoDBConnection;
import com.rento.utils.ValidationUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Receipt generation service (PDF and TXT).
 */
public class ReceiptService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    /**
     * Generate a PDF receipt.
     * @return path to the generated file
     */
    public String generatePDFReceipt(Booking booking, Payment payment, String outputDir) throws Exception {
        String fileName = "Receipt_" + (payment != null ? payment.getTransactionRef() : System.currentTimeMillis()) + ".pdf";
        String filePath = outputDir + File.separator + fileName;

        // Ensure directory exists
        new File(outputDir).mkdirs();

        com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Colors
        BaseColor primaryColor = new BaseColor(110, 72, 170); // Purple
        BaseColor accentColor = new BaseColor(0, 200, 150);

        // Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, primaryColor);
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, primaryColor);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.DARK_GRAY);
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, accentColor);

        // Title
        Paragraph title = new Paragraph("RENTO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Vehicle Rental & Booking System — Payment Receipt", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // Divider line
        document.add(new Paragraph("━".repeat(70), new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.LIGHT_GRAY)));
        document.add(Chunk.NEWLINE);

        // Transaction Details
        document.add(new Paragraph("Transaction Details", headerFont));
        document.add(Chunk.NEWLINE);

        PdfPTable txnTable = new PdfPTable(2);
        txnTable.setWidthPercentage(100);
        txnTable.setWidths(new float[]{1, 2});

        if (payment != null) {
            addTableRow(txnTable, "Transaction ID:", payment.getTransactionRef(), boldFont, normalFont);
            addTableRow(txnTable, "Date:", DATE_FORMAT.format(payment.getPaymentDate() != null ? payment.getPaymentDate() : new Date()), boldFont, normalFont);
            addTableRow(txnTable, "Payment Method:", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name().replace("_", " ") : "N/A", boldFont, normalFont);
            addTableRow(txnTable, "Card:", payment.getCardNumber() != null ? payment.getCardNumber() : "N/A", boldFont, normalFont);
            addTableRow(txnTable, "Status:", payment.getStatus() != null ? payment.getStatus().name() : "N/A", boldFont, normalFont);
        }
        document.add(txnTable);
        document.add(Chunk.NEWLINE);

        // Booking Details
        document.add(new Paragraph("Booking Details", headerFont));
        document.add(Chunk.NEWLINE);

        PdfPTable bookingTable = new PdfPTable(2);
        bookingTable.setWidthPercentage(100);
        bookingTable.setWidths(new float[]{1, 2});

        if (booking != null) {
            addTableRow(bookingTable, "Vehicle:", booking.getVehicleName() != null ? booking.getVehicleName() : "N/A", boldFont, normalFont);
            addTableRow(bookingTable, "Pickup:", booking.getPickupLocation() != null ? booking.getPickupLocation() : "N/A", boldFont, normalFont);
            addTableRow(bookingTable, "Drop-off:", booking.getDropoffLocation() != null ? booking.getDropoffLocation() : "N/A", boldFont, normalFont);
            addTableRow(bookingTable, "Pickup Date:", booking.getPickupDateTime() != null ? DATE_FORMAT.format(booking.getPickupDateTime()) : "N/A", boldFont, normalFont);
            addTableRow(bookingTable, "Return Date:", booking.getReturnDateTime() != null ? DATE_FORMAT.format(booking.getReturnDateTime()) : "N/A", boldFont, normalFont);
            addTableRow(bookingTable, "Duration:", booking.getRentalDays() + " day(s)", boldFont, normalFont);
        }
        document.add(bookingTable);
        document.add(Chunk.NEWLINE);

        // Pricing
        document.add(new Paragraph("Pricing Breakdown", headerFont));
        document.add(Chunk.NEWLINE);

        PdfPTable priceTable = new PdfPTable(2);
        priceTable.setWidthPercentage(60);
        priceTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        if (booking != null) {
            double subtotalVal = booking.getTotalCost() - booking.getTaxAmount() + booking.getDiscountApplied();
            addTableRow(priceTable, "Subtotal:", ValidationUtil.formatCurrency(subtotalVal), boldFont, normalFont);
            if (booking.getDiscountApplied() > 0) {
                addTableRow(priceTable, "Discount:", "- " + ValidationUtil.formatCurrency(booking.getDiscountApplied()), boldFont, normalFont);
            }
            addTableRow(priceTable, "Tax (18% GST):", ValidationUtil.formatCurrency(booking.getTaxAmount()), boldFont, normalFont);
            addTableRow(priceTable, "Deposit:", ValidationUtil.formatCurrency(booking.getDepositAmount()), boldFont, normalFont);
        }
        document.add(priceTable);
        document.add(Chunk.NEWLINE);

        // Total
        Paragraph total = new Paragraph("Total: " + (payment != null ? ValidationUtil.formatCurrency(payment.getTotalAmount()) : ValidationUtil.formatCurrency(booking != null ? booking.getTotalCost() : 0)), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);
        document.add(Chunk.NEWLINE);

        // Footer
        document.add(new Paragraph("━".repeat(70), new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.LIGHT_GRAY)));
        Paragraph footer = new Paragraph("Thank you for choosing Rento! Safe travels.", subtitleFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph contact = new Paragraph("Support: sudanayyappan_bcs28@mepcoeng.ac.in", subtitleFont);
        contact.setAlignment(Element.ALIGN_CENTER);
        document.add(contact);

        document.close();
        recordReceipt("BOOKING", filePath, payment != null ? payment.getTransactionRef() : "BOOKING-" + System.currentTimeMillis(),
            payment != null && payment.getStatus() != null ? payment.getStatus().name() : "COMPLETED",
            payment != null && payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "BOOKING");
        return filePath;
    }

    /**
     * Generate a text receipt.
     */
    public String generateTextReceipt(Booking booking, Payment payment, String outputDir) throws Exception {
        String fileName = "Receipt_" + (payment != null ? payment.getTransactionRef() : System.currentTimeMillis()) + ".txt";
        String filePath = outputDir + File.separator + fileName;

        new File(outputDir).mkdirs();

        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("                     RENTO PAYMENT RECEIPT                  \n");
        sb.append("============================================================\n");
        sb.append("Generated At   : ").append(DATE_FORMAT.format(new Date())).append("\n");
        sb.append("Receipt Type   : Booking Payment\n");
        sb.append("------------------------------------------------------------\n\n");

        if (payment != null) {
            sb.append("TRANSACTION DETAILS\n");
            sb.append("------------------------------------------------------------\n");
            sb.append("Transaction ID : ").append(payment.getTransactionRef()).append("\n");
            sb.append("Date           : ").append(DATE_FORMAT.format(payment.getPaymentDate() != null ? payment.getPaymentDate() : new Date())).append("\n");
            sb.append("Method         : ").append(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "N/A").append("\n");
            sb.append("Card           : ").append(payment.getCardNumber() != null ? payment.getCardNumber() : "N/A").append("\n");
            sb.append("Status         : ").append(payment.getStatus()).append("\n\n");
        }

        if (booking != null) {
            sb.append("BOOKING DETAILS\n");
            sb.append("------------------------------------------------------------\n");
            sb.append("Vehicle        : ").append(booking.getVehicleName()).append("\n");
            sb.append("Pickup         : ").append(booking.getPickupLocation()).append("\n");
            sb.append("Drop-off       : ").append(booking.getDropoffLocation()).append("\n");
            sb.append("Pickup Date    : ").append(booking.getPickupDateTime() != null ? DATE_FORMAT.format(booking.getPickupDateTime()) : "N/A").append("\n");
            sb.append("Return Date    : ").append(booking.getReturnDateTime() != null ? DATE_FORMAT.format(booking.getReturnDateTime()) : "N/A").append("\n");
            sb.append("Duration       : ").append(booking.getRentalDays()).append(" day(s)\n\n");

            sb.append("PRICING\n");
            sb.append("------------------------------------------------------------\n");
            sb.append("Total Cost     : ").append(ValidationUtil.formatCurrency(booking.getTotalCost())).append("\n");
            sb.append("Tax (GST 18%)  : ").append(ValidationUtil.formatCurrency(booking.getTaxAmount())).append("\n");
            sb.append("Deposit        : ").append(ValidationUtil.formatCurrency(booking.getDepositAmount())).append("\n");
        }

        sb.append("\n============================================================\n");
        sb.append("Thank you for choosing Rento. Please keep this receipt for\n");
        sb.append("your travel records and payment verification.\n");
        sb.append("Support: sudanayyappan_bcs28@mepcoeng.ac.in\n");
        sb.append("============================================================\n");

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(sb.toString());
        }
        recordReceipt("BOOKING_TXT", filePath, payment != null ? payment.getTransactionRef() : "BOOKING-" + System.currentTimeMillis(),
            payment != null && payment.getStatus() != null ? payment.getStatus().name() : "COMPLETED",
            payment != null && payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "BOOKING");
        return filePath;
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }

    public String generateRentalReceipt(Rental rental, String outputDir) throws Exception {
        String fileName = "Rental_Receipt_" + System.currentTimeMillis() + ".txt";
        String filePath = outputDir + File.separator + fileName;
        new File(outputDir).mkdirs();
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("============================================================\n");
            writer.write("                     RENTO RENTAL RECEIPT                   \n");
            writer.write("============================================================\n");
            writer.write("Generated At   : " + DATE_FORMAT.format(new Date()) + "\n");
            writer.write("Vehicle        : " + (rental.getVehicleName() != null ? rental.getVehicleName() : "N/A") + "\n");
            writer.write("Renter         : " + (rental.getRenterName() != null ? rental.getRenterName() : "N/A") + "\n");
            writer.write("Supplier       : " + (rental.getSupplierName() != null ? rental.getSupplierName() : "N/A") + "\n");
            writer.write("Duration       : " + rental.getRentalDurationLabel() + "\n");
            writer.write("Base Amount    : " + ValidationUtil.formatCurrency(rental.getTotalAmount()) + "\n");
            writer.write("Penalty        : " + ValidationUtil.formatCurrency(rental.getPenaltyAmount()) + "\n");
            writer.write("Payment Method : " + (rental.getPaymentMethod() != null ? rental.getPaymentMethod() : "N/A") + "\n");
            writer.write("Payment Status : " + (rental.getPaymentStatus() != null ? rental.getPaymentStatus() : "N/A") + "\n");
            writer.write("Status         : " + rental.getStatus() + "\n");
            writer.write("Receipt Path   : " + filePath + "\n");
            writer.write("============================================================\n");
        }
        recordReceipt("RENTAL", filePath, "RENTAL-" + System.currentTimeMillis(),
            rental.getStatus() != null ? rental.getStatus().name() : "COMPLETED",
            rental.getPaymentMethod() != null ? rental.getPaymentMethod() : "RENTAL");
        return filePath;
    }

    private void recordReceipt(String type, String path, String code, String status, String method) {
        try {
            MongoDatabase db = MongoDBConnection.getInstance().getDatabase();
            if (db == null) {
                return;
            }
            MongoCollection<org.bson.Document> col = db.getCollection("receipts");
            col.insertOne(new org.bson.Document("receiptCode", code)
                .append("receiptType", type)
                .append("documentTitle", "Rento " + type + " receipt")
                .append("filePath", path)
                .append("fileFormat", path.endsWith(".pdf") ? "PDF" : "TXT")
                .append("status", status)
                .append("paymentMethod", method)
                .append("issuedBy", "Rento System")
                .append("currency", "INR")
                .append("deliveryChannel", "DOWNLOAD")
                .append("printable", true)
                .append("createdAt", new Date()));
        } catch (Exception ignored) {
        }
    }
}
