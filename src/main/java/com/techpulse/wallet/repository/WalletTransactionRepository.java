package com.techpulse.wallet.repository;

import com.techpulse.wallet.entity.WalletTransaction;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Optional<WalletTransaction> findByIdempotencyKey(String key);

    @Query("select t from WalletTransaction t left join fetch t.fromWallet fw left join fetch fw.user left join fetch t.toWallet tw left join fetch tw.user where t.idempotencyKey=:key")
    Optional<WalletTransaction> findDetailedByIdempotencyKey(@Param("key") String key);

    @Query("select t from WalletTransaction t left join t.fromWallet fw left join fw.user fu left join t.toWallet tw left join tw.user tu where fu.email=:email or tu.email=:email")
    Page<WalletTransaction> findForUser(@Param("email") String email, Pageable pageable);
}
