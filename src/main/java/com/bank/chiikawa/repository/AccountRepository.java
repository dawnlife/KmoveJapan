package com.bank.chiikawa.repository;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwner(Customer owner);
    Optional<Account> findByAccountNumber(String accountNumber);
}
