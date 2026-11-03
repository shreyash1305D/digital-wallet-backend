package com.techpulse.wallet.repository;

import com.techpulse.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<Wallet> findByUserEmail(String email);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Wallet> findAll(org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w join fetch w.user u where u.email=:email")
    Optional<Wallet> findByUserEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w join fetch w.user u where w.id in :ids order by w.id")
    List<Wallet> lockWallets(@Param("ids") Collection<Long> ids);
}
