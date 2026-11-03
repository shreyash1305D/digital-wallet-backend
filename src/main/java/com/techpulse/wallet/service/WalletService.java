package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.*;
import com.techpulse.wallet.entity.*;
import com.techpulse.wallet.exception.*;
import com.techpulse.wallet.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class WalletService {
    private final WalletRepository wallets; private final WalletTransactionRepository transactions; private final WalletTransactionService operations;
    public WalletService(WalletRepository wallets,WalletTransactionRepository transactions,WalletTransactionService operations) { this.wallets=wallets;this.transactions=transactions;this.operations=operations; }
    public WalletResponse getWallet(String email) { Wallet w=wallets.findByUserEmail(email).orElseThrow(()->new ResourceNotFoundException("Wallet not found"));return new WalletResponse(w.getId(),w.getUser().getEmail(),w.getBalance(),w.getVersion()); }
    public TransactionResponse addMoney(String email,BigDecimal amount,String key) { return execute(email,key,()->operations.addMoney(email,amount,key)); }
    public TransactionResponse transfer(String email,String receiver,BigDecimal amount,String key) { return execute(email,key,()->operations.transfer(email,receiver,amount,key)); }
    private TransactionResponse execute(String ownerEmail,String key,java.util.function.Supplier<WalletTransaction> action) {
        return existingForOwner(key,ownerEmail).orElseGet(()->{
            try { return TransactionResponse.from(action.get()); }
            catch(DataIntegrityViolationException ex) { return existingForOwner(key,ownerEmail).orElseThrow(()->ex); }
        });
    }
    private java.util.Optional<TransactionResponse> existingForOwner(String key,String ownerEmail) {
        return transactions.findDetailedByIdempotencyKey(key).map(tx->{
            String originalOwner=tx.getFromWallet()!=null?tx.getFromWallet().getUser().getEmail():tx.getToWallet().getUser().getEmail();
            if(!ownerEmail.equalsIgnoreCase(originalOwner)) throw new BadRequestException("Idempotency-Key has already been used by another user");
            return TransactionResponse.from(tx);
        });
    }
    public Page<TransactionResponse> userTransactions(String email,Pageable pageable) { return transactions.findForUser(email,pageable).map(TransactionResponse::from); }
    public Page<WalletResponse> allWallets(Pageable pageable) { return wallets.findAll(pageable).map(w->new WalletResponse(w.getId(),w.getUser().getEmail(),w.getBalance(),w.getVersion())); }
    public Page<TransactionResponse> allTransactions(Pageable pageable) { return transactions.findAll(pageable).map(TransactionResponse::from); }
}
