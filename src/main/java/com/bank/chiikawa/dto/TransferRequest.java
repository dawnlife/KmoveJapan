package com.bank.chiikawa.dto;

/** SCR004 계좌이체 입력화면에서 세션에 임시 보관하는 이체 요청 정보 */
public class TransferRequest {
    private String fromAccountNumber;
    private String toAccountNumber;
    private java.math.BigDecimal amount;
    private String transferType; // IMMEDIATE / RESERVED

    public TransferRequest() {}

    public TransferRequest(String fromAccountNumber, String toAccountNumber,
                            java.math.BigDecimal amount, String transferType) {
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.transferType = transferType;
    }

    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber() { return toAccountNumber; }
    public java.math.BigDecimal getAmount() { return amount; }
    public String getTransferType() { return transferType; }
}
