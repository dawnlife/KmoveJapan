package com.bank.chiikawa.repository;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountOrderByTxDatetimeDesc(Account account);
}
