package com.bank.chiikawa.controller;

import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * SCR001 ログイン画面
 * 화면전이조건표 #1: SCR001→SCR002, 트리거=로그인버튼, 이동조건=인증성공
 */
@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        if (session.getAttribute("loginCustomer") != null) {
            return "redirect:/main";
        }
        return "login"; // templates/login.mustache
    }

    @PostMapping("/login")
    public String login(@RequestParam String userId,
                         @RequestParam String password,
                         HttpSession session,
                         Model model) {
        Optional<Customer> customer = authService.authenticate(userId, password);
        if (customer.isEmpty()) {
            // 예외전이 이벤트: LoginError - 자화면에 에러 표시
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "login";
        }
        session.setAttribute("loginCustomer", customer.get());
        return "redirect:/main";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
