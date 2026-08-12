package com.bank.chiikawa.controller;

import com.bank.chiikawa.dto.TransferRequest;
import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.entity.Transaction;
import com.bank.chiikawa.service.AccountService;
import com.bank.chiikawa.service.TransferService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * SCR004 振込入力画面 → SCR005 振込暗証番号確認画面 → SCR006 振込完了画面
 * 화면전이조건표 #6,#7 규칙 그대로 구현
 */
@Controller
@org.springframework.web.bind.annotation.RequestMapping("/transfer")
public class TransferController {

    private final AccountService accountService;
    private final TransferService transferService;

    public TransferController(AccountService accountService, TransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    /** SCR004: 이체 입력 폼 표시. fromAccount 쿼리파라미터로 SCR002/003에서 초기값 전달 */
    @GetMapping
    public String form(@RequestParam(required = false) String fromAccount,
                        HttpSession session, Model model) {
        Customer customer = requireLogin(session);
        if (customer == null) return "redirect:/login";

        List<Account> myAccounts = accountService.findAccountsOf(customer);
        model.addAttribute("loggedIn", true);
        model.addAttribute("customerName", customer.getName());
        model.addAttribute("accounts", myAccounts);
        model.addAttribute("selectedAccount", fromAccount);
        return "transfer-input"; // templates/transfer-input.mustache
    }

    /** SCR004 「振込実行」버튼: 형식검증+한도검증(機能-09) 통과 시 SCR005로 이동 */
    @PostMapping
    public String submit(@RequestParam String fromAccountNumber,
                          @RequestParam String toAccountNumber,
                          @RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "IMMEDIATE") String transferType,
                          HttpSession session, Model model) {
        Customer customer = requireLogin(session);
        if (customer == null) return "redirect:/login";

        Account fromAccount = accountService.findByAccountNumber(fromAccountNumber).orElse(null);
        // IDOR 방지: 출금계좌가 로그인한 고객 소유가 아니면 이체 자체를 거부 (남의 계좌에서 출금 방지)
        if (fromAccount == null || !accountService.isOwnedBy(fromAccount, customer)) {
            model.addAttribute("error", "출금계좌 정보를 확인할 수 없습니다.");
            model.addAttribute("loggedIn", true);
            model.addAttribute("customerName", customer.getName());
            model.addAttribute("accounts", accountService.findAccountsOf(customer));
            return "transfer-input";
        }

        try {
            transferService.validateLimit(fromAccount, amount);
        } catch (TransferService.TransferException e) {
            // 예외전이: LimitExceeded - 자화면에 에러 표시
            model.addAttribute("error", e.getMessage());
            model.addAttribute("loggedIn", true);
            model.addAttribute("customerName", customer.getName());
            model.addAttribute("accounts", accountService.findAccountsOf(customer));
            return "transfer-input";
        }

        TransferRequest pending = new TransferRequest(fromAccountNumber, toAccountNumber, amount, transferType);
        session.setAttribute("pendingTransfer", pending);
        return "redirect:/transfer/confirm";
    }

    /** SCR005: 이체 비밀번호 확인 화면 */
    @GetMapping("/confirm")
    public String confirmForm(HttpSession session, Model model) {
        Customer customer = requireLogin(session);
        if (customer == null) return "redirect:/login";

        TransferRequest pending = (TransferRequest) session.getAttribute("pendingTransfer");
        if (pending == null) return "redirect:/transfer";

        model.addAttribute("loggedIn", true);
        model.addAttribute("customerName", customer.getName());
        model.addAttribute("fromAccountNumber", pending.getFromAccountNumber());
        model.addAttribute("toAccountNumber", pending.getToAccountNumber());
        model.addAttribute("amount", formatAmount(pending.getAmount()));
        return "transfer-confirm"; // templates/transfer-confirm.mustache
    }

    /** SCR005 「確認」버튼: 비밀번호 일치 시 실제 이체 실행 후 SCR006 */
    @PostMapping("/confirm")
    public String confirm(@RequestParam String transferPassword, HttpSession session, Model model) {
        Customer customer = requireLogin(session);
        if (customer == null) return "redirect:/login";

        TransferRequest pending = (TransferRequest) session.getAttribute("pendingTransfer");
        if (pending == null) return "redirect:/transfer";

        Account fromAccount = accountService.findByAccountNumber(pending.getFromAccountNumber()).orElse(null);
        Account toAccount = accountService.findByAccountNumber(pending.getToAccountNumber()).orElse(null);
        if (fromAccount == null || toAccount == null) {
            model.addAttribute("error", "계좌 정보를 확인할 수 없습니다.");
            return "transfer-confirm";
        }

        try {
            Transaction tx = transferService.executeTransfer(fromAccount, toAccount, pending.getAmount(), transferPassword);
            session.removeAttribute("pendingTransfer");
            session.setAttribute("lastTransaction", tx);
            return "redirect:/transfer/complete";
        } catch (TransferService.TransferException e) {
            // 예외전이: PasswordMismatch - 자화면에 에러 표시, 입력값 초기화
            model.addAttribute("error", e.getMessage());
            model.addAttribute("loggedIn", true);
            model.addAttribute("customerName", customer.getName());
            model.addAttribute("fromAccountNumber", pending.getFromAccountNumber());
            model.addAttribute("toAccountNumber", pending.getToAccountNumber());
            model.addAttribute("amount", formatAmount(pending.getAmount()));
            return "transfer-confirm";
        }
    }

    /** SCR006: 이체 완료 화면 */
    @GetMapping("/complete")
    public String complete(HttpSession session, Model model) {
        Customer customer = requireLogin(session);
        if (customer == null) return "redirect:/login";

        Transaction tx = (Transaction) session.getAttribute("lastTransaction");
        if (tx == null) return "redirect:/main";

        model.addAttribute("loggedIn", true);
        model.addAttribute("customerName", customer.getName());
        model.addAttribute("txNumber", tx.getTxNumber());
        model.addAttribute("amount", tx.getAmountFormatted());
        model.addAttribute("balanceAfter", tx.getBalanceAfterFormatted());
        return "transfer-complete"; // templates/transfer-complete.mustache
    }

    private Customer requireLogin(HttpSession session) {
        return (Customer) session.getAttribute("loginCustomer");
    }

    /** 화면표시용: 소수점 없이 천단위 콤마(#,##0) 포맷 */
    private String formatAmount(java.math.BigDecimal amount) {
        return new java.text.DecimalFormat("#,##0").format(amount);
    }
}
