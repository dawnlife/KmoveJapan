package com.bank.chiikawa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 계좌
 * 관련요건: 機能-02(계좌목록조회), 機能-03(잔액조회), 機能-09(이체한도)
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer owner;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "transfer_password", nullable = false, length = 100)
    private String transferPassword; // 이체 비밀번호 (機能-06)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    protected Account() {
    }

    public Account(String accountNumber, Customer owner, BigDecimal balance, String transferPassword) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = balance;
        this.transferPassword = transferPassword;
    }

    public void withdraw(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public boolean matchesTransferPassword(String rawPassword) {
        return this.transferPassword.equals(rawPassword);
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Customer getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }

    /** 화면표시용: 소수점 없이 천단위 콤마(#,##0) 포맷 */
    public String getBalanceFormatted() {
        return new java.text.DecimalFormat("#,##0").format(balance);
    }

    public enum AccountStatus { ACTIVE, SUSPENDED }
}
