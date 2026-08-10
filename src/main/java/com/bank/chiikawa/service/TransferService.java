package com.bank.chiikawa.service;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Transaction;
import com.bank.chiikawa.repository.AccountRepository;
import com.bank.chiikawa.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 機能-04 即時振込 / 機能-06 暗証番号確認 / 機能-07 結果確認 / 機能-09 限度額検証
 * 화면전이조건표 6번,7번 규칙(이동조건) 그대로 구현:
 *  - SCR004→SCR005: 입력값 검증 + 1회/1일 한도 이내인 경우만 통과
 *  - SCR005→SCR006: 이체 비밀번호 일치하는 경우만 통과
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Value("${transfer.limit.per-transaction}")
    private BigDecimal perTransactionLimit;

    @Value("${transfer.limit.per-day}")
    private BigDecimal perDayLimit;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /** SCR004 「振込実行」버튼 - 한도 검증만 수행 (실제 이체는 SCR005 확인 후) */
    public void validateLimit(Account fromAccount, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransferException("이체 금액을 올바르게 입력해주세요.");
        }
        if (amount.compareTo(perTransactionLimit) > 0) {
            throw new TransferException("1회 이체한도(" + format(perTransactionLimit) + "원)를 초과했습니다.");
        }
        BigDecimal todayTotal = sumTodayOutgoing(fromAccount).add(amount);
        if (todayTotal.compareTo(perDayLimit) > 0) {
            throw new TransferException("1일 이체한도(" + format(perDayLimit) + "원)를 초과했습니다.");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new TransferException("출금계좌 잔액이 부족합니다.");
        }
    }

    /** SCR005 「確認」버튼 - 비밀번호 검증 후 실제 이체 실행 (機能-06,07) */
    @Transactional
    public Transaction executeTransfer(Account fromAccount, Account toAccount, BigDecimal amount, String transferPassword) {
        if (!fromAccount.matchesTransferPassword(transferPassword)) {
            throw new TransferException("이체 비밀번호가 일치하지 않습니다.");
        }
        validateLimit(fromAccount, amount);

        fromAccount.withdraw(amount);
        toAccount.deposit(amount);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String txNumber = "TXN" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Transaction outTx = new Transaction(txNumber, fromAccount, Transaction.TxType.TRANSFER_OUT,
                amount, toAccount.getAccountNumber(), fromAccount.getBalance());
        Transaction inTx = new Transaction(txNumber + "-R", toAccount, Transaction.TxType.TRANSFER_IN,
                amount, fromAccount.getAccountNumber(), toAccount.getBalance());
        transactionRepository.save(outTx);
        transactionRepository.save(inTx);

        return outTx;
    }

    private BigDecimal sumTodayOutgoing(Account account) {
        List<Transaction> history = transactionRepository.findByAccountOrderByTxDatetimeDesc(account);
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return history.stream()
                .filter(t -> t.getTxType() == Transaction.TxType.TRANSFER_OUT)
                .filter(t -> t.getTxDatetime().isAfter(startOfDay))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String format(BigDecimal v) {
        return String.format("%,d", v.longValue());
    }

    public static class TransferException extends RuntimeException {
        public TransferException(String message) { super(message); }
    }
}
