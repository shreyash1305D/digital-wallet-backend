package com.techpulse.wallet.dto;

import java.math.BigDecimal;

public record WalletResponse(Long walletId, String ownerEmail, BigDecimal balance, Long version) {
}
