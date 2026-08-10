package com.bank.chiikawa.controller;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.entity.Transaction;
import com.bank.chiikawa.service.AccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SCR007 振込履歴照会画面 (機能-08)
 * 조회기간 / 조회내용(전체·출금·입금·이자) / 정렬(건수·순서) 조건 지원.
 */
@Controller
public class HistoryController {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AccountService accountService;

    public HistoryController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/history")
    public String history(@RequestParam(required = false) String accountNumber,
                           @RequestParam(required = false) String dateFrom,
                           @RequestParam(required = false) String dateTo,
                           @RequestParam(defaultValue = "ALL") String txFilter,
                           @RequestParam(defaultValue = "30") int count,
                           @RequestParam(defaultValue = "recent") String sortOrder,
                           HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loginCustomer");
        if (customer == null) return "redirect:/login";

        List<Account> myAccounts = accountService.findAccountsOf(customer);
        model.addAttribute("loggedIn", true);
        model.addAttribute("customerName", customer.getName());
        model.addAttribute("accounts", myAccounts);

        Account target = null;
        if (accountNumber != null) {
            target = accountService.findByAccountNumber(accountNumber).orElse(null);
        } else if (!myAccounts.isEmpty()) {
            target = myAccounts.get(0);
        }

        List<Map<String, Object>> accountOptions = new ArrayList<>();
        for (Account a : myAccounts) {
            boolean isSelected = target != null && a.getAccountNumber().equals(target.getAccountNumber());
            accountOptions.add(Map.of("accountNumber", a.getAccountNumber(), "selected", isSelected));
        }
        model.addAttribute("accountOptions", accountOptions);

        // 조회기간 기본값: 최근 1개월 (참고 화면의 기본 선택값과 동일)
        LocalDate today = LocalDate.now();
        LocalDate fromDate = parseDateOrDefault(dateFrom, today.minusMonths(1));
        LocalDate toDate = parseDateOrDefault(dateTo, today);
        model.addAttribute("dateFrom", fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.addAttribute("dateTo", toDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // 조회기간 프리셋 버튼(당일/3일/.../1년) 중 현재 조건과 일치하는 것을 활성 표시
        long diffDays = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate);
        boolean toDateIsToday = toDate.isEqual(today);
        int[] presetDays = {0, 3, 7, 14, 30, 90, 180, 365};
        Map<Integer, Boolean> presetActive = new java.util.HashMap<>();
        for (int d : presetDays) {
            presetActive.put(d, toDateIsToday && diffDays == d);
        }
        model.addAttribute("preset0", presetActive.get(0));
        model.addAttribute("preset3", presetActive.get(3));
        model.addAttribute("preset7", presetActive.get(7));
        model.addAttribute("preset14", presetActive.get(14));
        model.addAttribute("preset30", presetActive.get(30));
        model.addAttribute("preset90", presetActive.get(90));
        model.addAttribute("preset180", presetActive.get(180));
        model.addAttribute("preset365", presetActive.get(365));

        model.addAttribute("txFilter", txFilter);
        model.addAttribute("txFilterAll", "ALL".equals(txFilter));
        model.addAttribute("txFilterWithdraw", "WITHDRAW".equals(txFilter));
        model.addAttribute("txFilterDeposit", "DEPOSIT".equals(txFilter));
        model.addAttribute("txFilterInterest", "INTEREST".equals(txFilter));

        model.addAttribute("count", count);
        List<Map<String, Object>> countOptions = new ArrayList<>();
        for (int c : new int[]{15, 30, 50, 100}) {
            countOptions.add(Map.of("value", c, "selected", c == count));
        }
        model.addAttribute("counts", countOptions);

        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("sortRecent", "recent".equals(sortOrder));
        model.addAttribute("sortPast", "past".equals(sortOrder));

        if (target != null) {
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay(); // toDate 당일 포함

            List<Transaction> history = accountService.findHistory(target).stream()
                    .filter(t -> !t.getTxDatetime().isBefore(fromDateTime) && t.getTxDatetime().isBefore(toDateTime))
                    .filter(t -> matchesTxFilter(t, txFilter))
                    .collect(Collectors.toList());

            Comparator<Transaction> comparator = Comparator.comparing(Transaction::getTxDatetime);
            if ("recent".equals(sortOrder)) {
                comparator = comparator.reversed();
            }
            history.sort(comparator);

            if (history.size() > count) {
                history = history.subList(0, count);
            }

            model.addAttribute("selectedAccount", target.getAccountNumber());
            model.addAttribute("history", history);
            model.addAttribute("hasHistory", !history.isEmpty());
            model.addAttribute("resultCount", history.size());
        } else {
            model.addAttribute("hasHistory", false);
            model.addAttribute("resultCount", 0);
        }
        return "history"; // templates/history.mustache
    }

    /** 出金履歴(TRANSFER_OUT,WITHDRAW) / 入金履歴(TRANSFER_IN,DEPOSIT) / 利子(미지원, 항상 불일치) / 전체 */
    private boolean matchesTxFilter(Transaction t, String txFilter) {
        return switch (txFilter) {
            case "WITHDRAW" -> t.getTxType() == Transaction.TxType.TRANSFER_OUT
                    || t.getTxType() == Transaction.TxType.WITHDRAW;
            case "DEPOSIT" -> t.getTxType() == Transaction.TxType.TRANSFER_IN
                    || t.getTxType() == Transaction.TxType.DEPOSIT;
            case "INTEREST" -> false; // 이자 거래 유형 자체가 시스템에 없음(미구현) — 항상 0건
            default -> true; // ALL
        };
    }

    private LocalDate parseDateOrDefault(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            // yyyy-MM-dd(HTML date input) 또는 yyyyMMdd(참고화면 안내 형식) 둘 다 허용
            return raw.contains("-") ? LocalDate.parse(raw) : LocalDate.parse(raw, YYYYMMDD);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
