package com.techpulse.wallet.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(@NotBlank @Email String receiverEmail,
                              @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
