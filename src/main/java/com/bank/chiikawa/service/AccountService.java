package com.bank.chiikawa.service;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.entity.Transaction;
import com.bank.chiikawa.repository.AccountRepository;
import com.bank.chiikawa.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 機能-02 口座一覧照会 / 機能-03 口座詳細照会 / 機能-08 振込履歴照会
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Account> findAccountsOf(Customer customer) {
        return accountRepository.findByOwner(customer);
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<Transaction> findHistory(Account account) {
        return transactionRepository.findByAccountOrderByTxDatetimeDesc(account);
    }

    /** 소유권 검증: 이 계좌가 해당 고객 소유인지 확인 (IDOR 방지) */
    public boolean isOwnedBy(Account account, Customer customer) {
        return account != null && customer != null
                && account.getOwner() != null
                && account.getOwner().getId().equals(customer.getId());
    }

    /**
     * SCR002 메인화면 "최근이체내역" 탭용 — 고객이 보유한 모든 계좌의 거래내역을
     * 합쳐서 최신순으로 정렬 후 상위 limit건만 반환.
     */
    public List<Transaction> findRecentHistoryAcrossAccounts(List<Account> accounts, int limit) {
        List<Transaction> merged = new ArrayList<>();
        for (Account account : accounts) {
            merged.addAll(transactionRepository.findByAccountOrderByTxDatetimeDesc(account));
        }
        merged.sort(Comparator.comparing(Transaction::getTxDatetime).reversed());
        if (merged.size() > limit) {
            return merged.subList(0, limit);
        }
        return merged;
    }
}
