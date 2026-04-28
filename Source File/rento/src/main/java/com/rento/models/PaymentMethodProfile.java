package com.rento.models;

import org.bson.types.ObjectId;

import java.util.Date;

/**
 * Saved payment methods for every actor in the platform.
 */
public class PaymentMethodProfile {

    public enum MethodType {
        CREDIT_CARD, UPI, CASH_ON_DELIVERY
    }

    private ObjectId id;
    private ObjectId userId;
    private MethodType methodType;
    private String profileName;
    private String holderName;
    private String maskedReference;
    private String providerName;
    private String billingAddress;
    private String nickname;
    private String status;
    private boolean preferred;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;

    public PaymentMethodProfile() {
        this.status = "ACTIVE";
        this.preferred = false;
        this.active = true;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }

    public MethodType getMethodType() { return methodType; }
    public void setMethodType(MethodType methodType) { this.methodType = methodType; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public String getMaskedReference() { return maskedReference; }
    public void setMaskedReference(String maskedReference) { this.maskedReference = maskedReference; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPreferred() { return preferred; }
    public void setPreferred(boolean preferred) { this.preferred = preferred; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
