package com.techpulse.wallet.service;

import com.techpulse.wallet.entity.*;
import com.techpulse.wallet.exception.*;
import com.techpulse.wallet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class WalletTransactionService {
    private final WalletRepository wallets;
    private final UserRepository users;
    private final WalletTransactionRepository transactions;

    public WalletTransactionService(WalletRepository wallets, UserRepository users, WalletTransactionRepository transactions) {
        this.wallets = wallets;
        this.users = users;
        this.transactions = transactions;
    }

    @Transactional
    public WalletTransaction addMoney(String email, BigDecimal amount, String key) {
        Wallet wallet = wallets.findByUserEmail(email).orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        WalletTransaction tx = new WalletTransaction();
        tx.setToWallet(wallet);
        tx.setAmount(amount);
        tx.setType(TransactionType.ADD);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setIdempotencyKey(key);
        transactions.saveAndFlush(tx);
        Wallet locked = wallets.findByUserEmailForUpdate(email).orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        locked.setBalance(locked.getBalance().add(amount));
        tx.setStatus(TransactionStatus.SUCCESS);
        return transactions.save(tx);
    }

    @Transactional
    public WalletTransaction transfer(String senderEmail, String receiverEmail, BigDecimal amount, String key) {
        if (senderEmail.equalsIgnoreCase(receiverEmail))
            throw new BadRequestException("Sender and receiver must be different");
        Wallet sender = wallets.findByUserEmail(senderEmail).orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));
        Wallet receiver = wallets.findByUserEmail(receiverEmail).orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));
        WalletTransaction tx = new WalletTransaction();
        tx.setFromWallet(sender);
        tx.setToWallet(receiver);
        tx.setAmount(amount);
        tx.setType(TransactionType.TRANSFER);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setIdempotencyKey(key);
        transactions.saveAndFlush(tx);
        List<Wallet> locked = wallets.lockWallets(List.of(sender.getId(), receiver.getId()));
        Map<Long, Wallet> byId = new HashMap<>();
        locked.forEach(w -> byId.put(w.getId(), w));
        Wallet from = byId.get(sender.getId()), to = byId.get(receiver.getId());
        if (from == null || to == null) throw new ResourceNotFoundException("Wallet not found");
        if (from.getBalance().compareTo(amount) < 0) {
            tx.setStatus(TransactionStatus.FAILED);
            return transactions.save(tx);
        }
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        tx.setStatus(TransactionStatus.SUCCESS);
        return transactions.save(tx);
    }
}
