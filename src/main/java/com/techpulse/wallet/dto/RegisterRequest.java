package com.techpulse.wallet.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(@NotBlank @Email @Size(max = 150) String email,
                              @NotBlank @Size(min = 8, max = 72) String password) {
}
