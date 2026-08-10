package com.bank.chiikawa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래내역 (이체 1건당 출금측 1행, 입금측 1행 기록)
 * 관련요건: 機能-07(이체결과확인), 機能-08(이체내역조회)
 */
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_number", nullable = false, unique = true, length = 30)
    private String txNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; // 이 거래가 귀속되는 계좌

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 20)
    private TxType txType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "counterpart_account", length = 20)
    private String counterpartAccount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "tx_datetime", nullable = false)
    private LocalDateTime txDatetime;

    protected Transaction() {
    }

    /** 실거래용 — 거래일시를 현재시각으로 자동 설정 */
    public Transaction(String txNumber, Account account, TxType txType, BigDecimal amount,
                        String counterpartAccount, BigDecimal balanceAfter) {
        this(txNumber, account, txType, amount, counterpartAccount, balanceAfter, LocalDateTime.now());
    }

    /** 더미데이터 시딩 등 거래일시를 직접 지정해야 하는 경우 사용 */
    public Transaction(String txNumber, Account account, TxType txType, BigDecimal amount,
                        String counterpartAccount, BigDecimal balanceAfter, LocalDateTime txDatetime) {
        this.txNumber = txNumber;
        this.account = account;
        this.txType = txType;
        this.amount = amount;
        this.counterpartAccount = counterpartAccount;
        this.balanceAfter = balanceAfter;
        this.txDatetime = txDatetime;
    }

    public Long getId() { return id; }
    public String getTxNumber() { return txNumber; }
    public Account getAccount() { return account; }
    public TxType getTxType() { return txType; }
    public BigDecimal getAmount() { return amount; }
    public String getCounterpartAccount() { return counterpartAccount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getTxDatetime() { return txDatetime; }

    /** 화면표시용: 출금성 거래(출금액 컬럼에 표시)인지 여부 */
    public boolean isOutgoing() {
        return txType == TxType.TRANSFER_OUT || txType == TxType.WITHDRAW;
    }

    /** 화면표시용: 거래유형 한글 라벨 */
    public String getTxTypeLabel() {
        return switch (txType) {
            case TRANSFER_OUT -> "이체(출금)";
            case TRANSFER_IN -> "이체(입금)";
            case WITHDRAW -> "출금";
            case DEPOSIT -> "입금";
        };
    }

    /** 화면표시용: 소수점 없이 천단위 콤마(#,##0) 포맷 */
    public String getAmountFormatted() {
        return new java.text.DecimalFormat("#,##0").format(amount);
    }

    public String getBalanceAfterFormatted() {
        return new java.text.DecimalFormat("#,##0").format(balanceAfter);
    }

    public enum TxType { DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN }
}
