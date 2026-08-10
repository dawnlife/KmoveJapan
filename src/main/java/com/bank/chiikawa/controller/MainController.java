package com.bank.chiikawa.controller;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.entity.Transaction;
import com.bank.chiikawa.service.AccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * SCR002 メイン画面 (허브 화면)
 */
@Controller
public class MainController {

    private static final int RECENT_HISTORY_LIMIT = 10;

    private final AccountService accountService;

    public MainController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping({"/", "/main"})
    public String main(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loginCustomer");
        if (customer == null) {
            return "redirect:/login";
        }
        List<Account> accounts = accountService.findAccountsOf(customer);
        model.addAttribute("loggedIn", true);
        model.addAttribute("customerName", customer.getName());
        model.addAttribute("accounts", accounts);
        model.addAttribute("hasAccounts", !accounts.isEmpty());

        // "최근이체내역" 탭: 보유한 모든 계좌의 거래내역을 합쳐서 최신순 상위 N건
        List<Transaction> recentHistory = accountService.findRecentHistoryAcrossAccounts(accounts, RECENT_HISTORY_LIMIT);
        model.addAttribute("recentHistory", recentHistory);
        model.addAttribute("hasRecentHistory", !recentHistory.isEmpty());

        return "main"; // templates/main.mustache
    }
}
