package com.techpulse.wallet.dto;

import com.techpulse.wallet.entity.*;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(Long id, Long fromWalletId, Long toWalletId, BigDecimal amount, TransactionType type,
                                  TransactionStatus status, String idempotencyKey, Instant createdAt) {
    public static TransactionResponse from(WalletTransaction t) {
        return new TransactionResponse(t.getId(), t.getFromWallet() == null ? null : t.getFromWallet().getId(), t.getToWallet() == null ? null : t.getToWallet().getId(), t.getAmount(), t.getType(), t.getStatus(), t.getIdempotencyKey(), t.getCreatedAt());
    }
}
