package com.bank.chiikawa.controller;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.service.AccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * SCR003 口座詳細照会画面
 */
@Controller
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/accounts/{accountNumber}")
    public String detail(@PathVariable String accountNumber, HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loginCustomer");
        if (customer == null) {
            return "redirect:/login";
        }
        Account account = accountService.findByAccountNumber(accountNumber)
                .filter(a -> accountService.isOwnedBy(a, customer)) // IDOR 방지: 남의 계좌면 못 찾은 것과 동일하게 처리
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다: " + accountNumber));

        model.addAttribute("loggedIn", true);
        model.addAttribute("customerName", customer.getName());
        model.addAttribute("account", account);
        return "account-detail"; // templates/account-detail.mustache
    }
}
