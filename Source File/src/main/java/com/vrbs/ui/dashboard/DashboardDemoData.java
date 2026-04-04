package com.vrbs.ui.dashboard;

import java.util.List;

public final class DashboardDemoData {

    public record Metric(String label, String value, String note) {
    }

    public record ActionItem(String title, String detail, String accent) {
    }

    public record VehicleShowcase(String name, String price, String spec, String tag) {
    }

    public record RideRequest(String rider, String category, String eta, String distance, String fare) {
    }

    public record SystemLog(String level, String message) {
    }

    private DashboardDemoData() {
    }

    public static List<Metric> userMetrics() {
        return List.of(
                new Metric("Wallet", "INR 2,450", "Ready for bookings"),
                new Metric("This Month", "12 trips", "3 premium rentals"),
                new Metric("Loyalty", "Gold Tier", "Priority support enabled"),
                new Metric("Digital Key", "2 active", "Audi Q7 and XC60")
        );
    }

    public static List<ActionItem> userUpdates() {
        return List.of(
                new ActionItem("Identity verified", "Your account is cleared for premium bookings.", "primary"),
                new ActionItem("Upcoming pickup", "Volvo S90 Recharge arrives at Terminal 4 in 18 min.", "secondary"),
                new ActionItem("Charging reminder", "Current rental is at 18% charge. Suggested station nearby.", "warning")
        );
    }

    public static List<VehicleShowcase> showroom() {
        return List.of(
                new VehicleShowcase("Audi A4 S-Line", "INR 7,200 / day", "Automatic · 5 seats · Hybrid", "Elite"),
                new VehicleShowcase("Volkswagen Golf 8", "INR 4,500 / day", "Manual · 5 seats · Petrol", "City"),
                new VehicleShowcase("Volvo XC60", "INR 9,800 / day", "Automatic · 7 seats · EV", "Adventure")
        );
    }

    public static List<RideRequest> rideRequests() {
        return List.of(
                new RideRequest("John D.", "Premium Sedan", "Pickup in 8 min", "2.4 miles away", "USD 32.50"),
                new RideRequest("Sarah W.", "Luxury SUV", "Pickup in 12 min", "3.1 miles away", "USD 54.00"),
                new RideRequest("Noah K.", "Airport Transfer", "Pickup in 6 min", "1.8 miles away", "USD 28.20")
        );
    }

    public static List<Metric> driverMetrics() {
        return List.of(
                new Metric("Daily Earnings", "USD 284.12", "8 rides completed today"),
                new Metric("Driver Rating", "4.92", "Gold tier performance"),
                new Metric("Online Hours", "6h 42m", "12h cap not reached"),
                new Metric("Vehicle Range", "312 mi", "82% battery remaining")
        );
    }

    public static List<Metric> supplierMetrics() {
        return List.of(
                new Metric("Fleet Availability", "98.4%", "Operational efficiency"),
                new Metric("Monthly Revenue", "USD 42,850", "12.5% above target"),
                new Metric("Health Score", "92", "3 vehicles need service"),
                new Metric("Safety Score", "0 incidents", "Last 30 days")
        );
    }

    public static List<ActionItem> fleetRows() {
        return List.of(
                new ActionItem("Volvo S90 Hybrid", "VR-9021 · Active · 96.8% efficiency · USD 1,240", "primary"),
                new ActionItem("Tesla Model Y", "VR-4421 · Active · 98.2% efficiency · USD 1,892.50", "primary"),
                new ActionItem("Audi A6 Sedan", "VR-1120 · Maintenance · 42.1% efficiency · USD 450.20", "warning"),
                new ActionItem("BMW X5 SUV", "VR-8832 · Active · 94.5% efficiency · USD 2,110", "secondary")
        );
    }

    public static List<ActionItem> adminUsers() {
        return List.of(
                new ActionItem("Julian Vance", "Executive Plus · London HQ · Active", "primary"),
                new ActionItem("Sarah Sterling", "Standard Fleet · NYC Hub · Pending", "warning"),
                new ActionItem("Marcus Thorne", "Global Enterprise · Berlin Core · Active", "secondary")
        );
    }

    public static List<SystemLog> logs() {
        return List.of(
                new SystemLog("INFO", "Successfully synchronized fleet telemetry from NYC-14 Station."),
                new SystemLog("AUTH", "Global API key generated for region APAC-WEST."),
                new SystemLog("ALERT", "Latency spike detected on Berlin-Core node. Scaling instances."),
                new SystemLog("CLEAN", "Garbage collection completed on primary database cluster."),
                new SystemLog("LOGS", "User ID 94229 updated subscription to Executive Plus.")
        );
    }
}
