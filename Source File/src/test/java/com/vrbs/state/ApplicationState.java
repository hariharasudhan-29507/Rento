package com.vrbs.state;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global flags: maintenance mode and per-entity suspension (admin).
 */
public final class ApplicationState {

    private static final ApplicationState INSTANCE = new ApplicationState();

    private final BooleanProperty maintenanceMode = new SimpleBooleanProperty(false);
    private final Set<String> suspendedUserIds = ConcurrentHashMap.newKeySet();
    private final Set<String> suspendedDriverIds = ConcurrentHashMap.newKeySet();
    private final Set<String> suspendedSupplierIds = ConcurrentHashMap.newKeySet();

    private ApplicationState() {
    }

    public static ApplicationState get() {
        return INSTANCE;
    }

    public BooleanProperty maintenanceModeProperty() {
        return maintenanceMode;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }

    public void setMaintenanceMode(boolean on) {
        maintenanceMode.set(on);
    }

    public void suspendUser(String userId, boolean suspend) {
        if (suspend) {
            suspendedUserIds.add(userId);
        } else {
            suspendedUserIds.remove(userId);
        }
    }

    public void suspendDriver(String driverId, boolean suspend) {
        if (suspend) {
            suspendedDriverIds.add(driverId);
        } else {
            suspendedDriverIds.remove(driverId);
        }
    }

    public void suspendSupplier(String supplierId, boolean suspend) {
        if (suspend) {
            suspendedSupplierIds.add(supplierId);
        } else {
            suspendedSupplierIds.remove(supplierId);
        }
    }

    public boolean isUserSuspended(String userId) {
        return suspendedUserIds.contains(userId);
    }

    public boolean isDriverSuspended(String driverId) {
        return suspendedDriverIds.contains(driverId);
    }

    public boolean isSupplierSuspended(String supplierId) {
        return suspendedSupplierIds.contains(supplierId);
    }
}
