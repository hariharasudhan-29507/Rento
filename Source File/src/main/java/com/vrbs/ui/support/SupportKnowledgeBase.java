package com.vrbs.ui.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SupportKnowledgeBase {

    private static final Map<String, String> QA = new LinkedHashMap<>();

    static {
        QA.put("How do I book a cab?", "Choose Book Cab, select a service area, confirm the destination, then accept the driver that fits your ETA.");
        QA.put("How does hourly rental work?", "Open Rent, compare the fleet cards, add up to 5 vehicles, choose 1-24 hours, then check out.");
        QA.put("Peak pricing?", "Peak demand applies a 1.5x multiplier. Pricing is shown in the booking summary before you confirm.");
        QA.put("Late return?", "A 1 hour grace period applies first. After that, late fees and insurance adjustments are added automatically.");
        QA.put("Refunds?", "Full refund within 15 minutes of booking. After that, 50% is returned unless the trip is already underway.");
        QA.put("Wallet?", "Wallet balance is shown on the user dashboard and can be used for rentals, cab rides, and priority add-ons.");
        QA.put("Contact supplier?", "Drivers can reach their primary supplier from the driver dashboard and suppliers can monitor dispatch health live.");
        QA.put("GST on invoices?", "Suppliers see GST breakdowns in the finance and vehicle performance sections.");
        QA.put("Account suspended?", "Admins can toggle suspension states. Contact support if your role is blocked unexpectedly.");
        QA.put("Session timeout?", "Sessions time out after inactivity. Use settings to update credentials before the session expires.");
    }

    private SupportKnowledgeBase() {
    }

    public static Map<String, String> entries() {
        return QA;
    }
}
