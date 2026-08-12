package com.bank.chiikawa.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그아웃/뒤로가기 시 브라우저 캐시에 남아있던 이전 화면(계좌잔액, 이체내역 등
 * 민감정보 포함)이 다시 보이는 문제를 막기 위한 필터.
 *
 * 정적 리소스(css/images/js)는 계속 캐시를 허용하고, 그 외 모든 응답에는
 * "저장하지 마라"는 헤더를 붙여서 뒤로가기를 눌러도 브라우저가 캐시된 화면
 * 대신 서버에 재요청하도록 강제한다. 서버는 세션이 끊겨있으면 /login으로
 * 돌려보내므로, 로그아웃 후에는 뒤로가기를 눌러도 로그인 화면이 뜬다.
 */
@Component
public class NoCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        boolean isStaticAsset = uri.startsWith("/css/") || uri.startsWith("/images/") || uri.startsWith("/js/");

        if (!isStaticAsset) {
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
        }

        chain.doFilter(request, response);
    }
}
