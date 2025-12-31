package com.pharmacy.mapper;

import com.pharmacy.dto.response.PaymentResponse;
import com.pharmacy.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setAmount(payment.getAmount());
        response.setRefundedAmount(payment.getRefundedAmount());
        response.setStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        response.setPaymentId(payment.getPaymentId());
        response.setConversationId(payment.getConversationId());
        response.setCardLastFour(payment.getCardLastFour());
        response.setCardBrand(payment.getCardBrand());
        response.setErrorCode(payment.getErrorCode());
        response.setErrorMessage(payment.getErrorMessage());
        response.setPaidAt(payment.getPaidAt());
        response.setRefundedAt(payment.getRefundedAt());
        response.setCreatedAt(payment.getCreatedAt());

        if (payment.getOrder() != null) {
            response.setOrderId(payment.getOrder().getId());
            response.setOrderNumber(payment.getOrder().getOrderNumber());
        }

        return response;
    }

    public PaymentResponse toResponseWith3DSecure(Payment payment, String threeDSecureHtml) {
        PaymentResponse response = toResponse(payment);
        response.setThreeDSecureHtml(threeDSecureHtml);
        return response;
    }

    public PaymentResponse toResponseWithRedirect(Payment payment, String redirectUrl) {
        PaymentResponse response = toResponse(payment);
        response.setRedirectUrl(redirectUrl);
        return response;
    }
}