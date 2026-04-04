package com.vrbs.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed South Indian locations and drivers (no maps — dataset only). Everyday cars, INR pricing context.
 */
public final class CabLocationService {

    public record DriverCard(String id, String name, String vehicle, String area, boolean available) {
    }

    private final Map<String, List<DriverCard>> byArea = new LinkedHashMap<>();

    public CabLocationService() {
        seed();
    }

    private void seed() {
        byArea.put("Chennai — T. Nagar", List.of(
                new DriverCard("d1", "Karthik Venkatesh", "Maruti Dzire · TN09", "Chennai — T. Nagar", true),
                new DriverCard("d2", "Lakshmi Narayanan", "Hyundai Grand i10 · TN06", "Chennai — T. Nagar", true),
                new DriverCard("d3", "Murugan Selvam", "Tata Tigor · TN07", "Chennai — T. Nagar", false)
        ));
        byArea.put("Bengaluru — Koramangala", List.of(
                new DriverCard("d4", "Deepa Krishnan", "Maruti Swift · KA03", "Bengaluru — Koramangala", true),
                new DriverCard("d5", "Arun Balakrishnan", "Tata Nexon · KA05", "Bengaluru — Koramangala", true)
        ));
        byArea.put("Hyderabad — Hitech City", List.of(
                new DriverCard("d6", "Srinivas Reddy", "Hyundai Creta · TG07", "Hyderabad — Hitech City", true),
                new DriverCard("d7", "Priya Kulkarni", "Kia Sonet · TG08", "Hyderabad — Hitech City", true)
        ));
        byArea.put("Kochi — Marine Drive", List.of(
                new DriverCard("d8", "Suresh Nambiar", "Maruti Ertiga · KL07", "Kochi — Marine Drive", true)
        ));
    }

    public List<String> locations() {
        return new ArrayList<>(byArea.keySet());
    }

    public List<DriverCard> driversInArea(String area) {
        if (area == null || !byArea.containsKey(area)) {
            return Collections.emptyList();
        }
        return byArea.get(area);
    }
}
