package com.rento.services;

import com.rento.dao.PaymentMethodDAO;
import com.rento.models.PaymentMethodProfile;
import com.rento.utils.ValidationUtil;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * Service for saved payment methods.
 */
public class PaymentMethodService {

    private final PaymentMethodDAO paymentMethodDAO = new PaymentMethodDAO();

    public String savePaymentMethod(ObjectId userId, PaymentMethodProfile.MethodType methodType, String profileName,
                                    String holderName, String rawReference, String providerName,
                                    String billingAddress, boolean preferred) {
        if (userId == null) {
            return "Please sign in before saving a payment method.";
        }
        if (!ValidationUtil.isNotEmpty(profileName) || !ValidationUtil.isNotEmpty(holderName)) {
            return "Profile name and holder name are required.";
        }
        if (methodType == PaymentMethodProfile.MethodType.CREDIT_CARD) {
            if (!ValidationUtil.isValidCardNumber(rawReference)) {
                return "Please enter a valid credit card number.";
            }
        } else if (methodType == PaymentMethodProfile.MethodType.UPI) {
            if (!ValidationUtil.isValidUpiId(rawReference)) {
                return "Please enter a valid UPI ID.";
            }
        }

        PaymentMethodProfile profile = new PaymentMethodProfile();
        profile.setUserId(userId);
        profile.setMethodType(methodType);
        profile.setProfileName(profileName.trim());
        profile.setHolderName(holderName.trim());
        profile.setMaskedReference(mask(methodType, rawReference));
        profile.setProviderName(providerName != null ? providerName.trim() : "Rento");
        profile.setBillingAddress(billingAddress != null ? billingAddress.trim() : "");
        profile.setNickname(profileName.trim());
        profile.setPreferred(preferred);

        if (preferred) {
            for (PaymentMethodProfile existing : paymentMethodDAO.findByUser(userId)) {
                if (existing.isPreferred()) {
                    existing.setPreferred(false);
                    paymentMethodDAO.update(existing);
                }
            }
        }

        return paymentMethodDAO.insert(profile) ? null : "Unable to save payment method right now.";
    }

    public List<PaymentMethodProfile> getPaymentMethodsForUser(ObjectId userId) {
        return paymentMethodDAO.findByUser(userId);
    }

    private String mask(PaymentMethodProfile.MethodType methodType, String rawReference) {
        if (methodType == PaymentMethodProfile.MethodType.CASH_ON_DELIVERY) {
            return "Cash collected in person";
        }
        if (methodType == PaymentMethodProfile.MethodType.CREDIT_CARD) {
            return ValidationUtil.maskCardNumber(rawReference);
        }
        return ValidationUtil.maskUpiId(rawReference);
    }
}
