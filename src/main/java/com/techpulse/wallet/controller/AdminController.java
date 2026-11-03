package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.*;
import com.techpulse.wallet.service.WalletService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final WalletService service;

    public AdminController(WalletService service) {
        this.service = service;
    }

    @GetMapping("/wallets")
    public Page<WalletResponse> wallets(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.allWallets(PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.ASC, "id")));
    }

    @GetMapping("/transactions")
    public Page<TransactionResponse> transactions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.allTransactions(PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
