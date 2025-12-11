package com.example.nanoavatar.payment;

import com.example.nanoavatar.user.UserService;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;

import java.util.List;

public class PaymentService {

    private final String providerToken;
    private final int minTopupRub;
    private final int creditsPerRub;
    private final UserService userService;

    public PaymentService(String providerToken, int minTopupRub, int creditsPerRub, UserService userService) {
        this.providerToken = providerToken;
        this.minTopupRub = minTopupRub;
        this.creditsPerRub = creditsPerRub;
        this.userService = userService;
    }

    public SendInvoice createTopupInvoice(long chatId, int amountRub) {
        if (amountRub < minTopupRub) {
            amountRub = minTopupRub;
        }
        int amountKopecks = amountRub * 100;

        return SendInvoice.builder()
                .chatId(chatId)
                .title("Пополнение баланса")
                .description("Оплата доступа к нейро‑фильтрам NanoAvatar")
                .payload("TOPUP_" + amountRub)
                .providerToken(providerToken)
                .currency("RUB")
                .prices(List.of(new LabeledPrice("Пополнение баланса", amountKopecks)))
                .needEmail(false)
                .needName(false)
                .needPhoneNumber(false)
                .isFlexible(false)
                .build();
    }

    public AnswerPreCheckoutQuery handlePreCheckout(String queryId, boolean ok, String error) {
        return AnswerPreCheckoutQuery.builder()
                .preCheckoutQueryId(queryId)
                .ok(ok)
                .errorMessage(error)
                .build();
    }

    public void handleSuccessfulPayment(long chatId, SuccessfulPayment payment) {
        String payload = payment.getInvoicePayload(); // "TOPUP_500" например
        int amountRub = payment.getTotalAmount() / 100;
        int credits = amountRub * creditsPerRub;

        userService.changeBalance(chatId, credits, "TOPUP", payload);
    }
}