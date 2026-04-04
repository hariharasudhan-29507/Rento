package com.vrbs.ui.payment;

import com.vrbs.model.SessionUser;
import com.vrbs.util.ReceiptPdfUtil;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Food-app style checkout: bill break-up in INR, UPI / card / wallet / cash on pickup (COD).
 */
public final class PaymentDemoPanel {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));
    private static final AtomicLong ORDER_SEQ = new AtomicLong(8800);

    private PaymentDemoPanel() {
    }

    /**
     * Demo amounts for a mixed cab + convenience fee (like Swiggy/Zomato line items).
     */
    public static VBox create(Stage stage, SessionUser user) {
        Label title = new Label("Payment");
        title.getStyleClass().add("text-primary-accent");

        double trip = 285.0;
        double platformFee = 15.0;
        double gst = Math.round((trip + platformFee) * 0.05 * 100.0) / 100.0; // 5% GST demo on services
        double total = trip + platformFee + gst;

        Label bill = new Label(
                "Trip fare (city cab)\t\t" + formatInr(trip) + "\n"
                        + "Platform & convenience fee\t" + formatInr(platformFee) + "\n"
                        + "GST (5%)\t\t\t" + formatInr(gst) + "\n"
                        + "—\n"
                        + "To pay\t\t\t" + formatInr(total)
        );
        bill.getStyleClass().add("text-muted");
        bill.setStyle("-fx-font-family: monospace; -fx-line-spacing: 4;");

        Label hint = new Label(
                "All amounts in Indian Rupees (INR). "
                        + "Cash on pickup works like COD on food apps — pay the driver in cash at handover."
        );
        hint.setWrapText(true);
        hint.getStyleClass().add("text-muted");

        ToggleGroup methods = new ToggleGroup();
        RadioButton upi = new RadioButton("UPI (GPay / PhonePe / Paytm)");
        upi.setToggleGroup(methods);
        upi.setSelected(true);
        RadioButton card = new RadioButton("Debit / Credit card");
        card.setToggleGroup(methods);
        RadioButton wallet = new RadioButton("VRBS Wallet");
        wallet.setToggleGroup(methods);
        RadioButton cod = new RadioButton("Cash on pickup / drop (pay driver)");
        cod.setToggleGroup(methods);

        Button pay = new Button();
        pay.getStyleClass().add("primary-button");
        Runnable syncPayLabel = () -> {
            boolean codSel = cod.isSelected();
            pay.setText(codSel ? "Confirm order (pay cash to driver)" : ("Pay " + formatInr(total)));
        };
        syncPayLabel.run();
        methods.selectedToggleProperty().addListener((o, a, b) -> syncPayLabel.run());

        pay.setOnAction(e -> {
            RadioButton sel = (RadioButton) methods.getSelectedToggle();
            String mode = sel != null ? sel.getText() : "—";
            long ord = ORDER_SEQ.incrementAndGet();
            String msg = "Order VRBS-" + ord + " placed.\nMode: " + mode + "\nTotal: " + formatInr(total);
            if (cod.isSelected()) {
                msg += "\n\nKeep exact change if possible — same idea as cash on delivery for food.";
            } else {
                msg += "\n\n(Demo) Payment gateway would open here for UPI/card/wallet.";
            }
            new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
        });

        Button savePdf = new Button("Download receipt (PDF)");
        savePdf.getStyleClass().add("secondary-button");
        savePdf.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("vrbs-order-" + ORDER_SEQ.get() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            var file = fc.showSaveDialog(stage);
            if (file != null) {
                try {
                    ReceiptPdfUtil.writeSimpleReceipt(
                            Path.of(file.toURI()),
                            "VRBS — Payment summary (INR)",
                            List.of(
                                    "Customer: " + user.getDisplayName(),
                                    "Trip fare: " + formatInr(trip),
                                    "Platform fee: " + formatInr(platformFee),
                                    "GST (5%): " + formatInr(gst),
                                    "Grand total: " + formatInr(total),
                                    "Mode: "
                                            + (methods.getSelectedToggle() instanceof RadioButton rb
                                            ? rb.getText()
                                            : "—")
                            )
                    );
                    new Alert(Alert.AlertType.INFORMATION, "Saved " + file.getName()).showAndWait();
                } catch (java.io.IOException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
        });

        VBox box = new VBox(12,
                title,
                bill,
                hint,
                new Separator(),
                new Label("Pay using"),
                upi, card, wallet, cod,
                pay,
                savePdf
        );
        box.setPadding(new Insets(4, 0, 0, 0));
        return box;
    }

    private static String formatInr(double amount) {
        return INR.format(amount);
    }
}
