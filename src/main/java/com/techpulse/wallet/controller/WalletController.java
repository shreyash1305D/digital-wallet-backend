package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.*;
import com.techpulse.wallet.exception.BadRequestException;
import com.techpulse.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {
    private final WalletService service;

    public WalletController(WalletService service) {
        this.service = service;
    }

    @GetMapping
    public WalletResponse wallet(Authentication auth) {
        return service.getWallet(auth.getName());
    }

    @PostMapping("/add")
    public ResponseEntity<TransactionResponse> add(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody AmountRequest request, Authentication auth) {
        validateKey(key);
        TransactionResponse result = service.addMoney(auth.getName(), request.amount(), key.trim());
        return ResponseEntity.status(result.status() == com.techpulse.wallet.entity
                .TransactionStatus.FAILED ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK).body(result);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest request, Authentication auth) {
        validateKey(key);
        TransactionResponse result = service.transfer(auth.getName(), request.receiverEmail().trim().toLowerCase(), request.amount(), key.trim());
        return ResponseEntity.status(result.status() == com.techpulse.wallet.entity
                .TransactionStatus.FAILED ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK).body(result);
    }

    @GetMapping("/transactions")
    public Page<TransactionResponse> history(Authentication auth, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BadRequestException("page must be >= 0 and size must be between 1 and 100");
        return service.userTransactions(auth.getName(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100)
            throw new BadRequestException("Idempotency-Key must contain 1 to 100 characters");
    }
}
