package com.bank.chiikawa.config;

import com.bank.chiikawa.entity.Account;
import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.entity.Transaction;
import com.bank.chiikawa.repository.AccountRepository;
import com.bank.chiikawa.repository.CustomerRepository;
import com.bank.chiikawa.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 요건정의서 DAR-04(초기 테스트 데이터 구축) 대응.
 * 데모용 계정: userId=kim / password=1234
 * 이체내역조회(SCR007) 필터(기간/유형/정렬/건수) 테스트를 위해 최근 2년간 약 100건의
 * 더미 거래내역을 랜덤 생성한다 (Random 시드 고정 — 실행할 때마다 동일한 데이터).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final long SEED = 42L; // 재현 가능하도록 고정 시드
    private static final int TRANSACTION_COUNT = 100;
    private static final int SPAN_DAYS = 730; // 최근 2년

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DataInitializer(CustomerRepository customerRepository,
                            AccountRepository accountRepository,
                            TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) {
        Customer kim = customerRepository.save(new Customer("kim", "1234", "김민제"));
        Customer lee = customerRepository.save(new Customer("lee", "1234", "이서연"));

        // 이체 비밀번호는 별도 4자리 (transferPassword) — 데모: 0000
        Account kimMain = accountRepository.save(new Account("691-910480-39907", kim, new BigDecimal("112742"), "0000"));
        Account kimSub = accountRepository.save(new Account("691-910480-20001", kim, new BigDecimal("500000"), "0000"));
        Account leeMain = accountRepository.save(new Account("761-720033-88123", lee, new BigDecimal("75000"), "1111"));

        int linkedCount = seedTransactions(kimMain, kimSub, leeMain);

        System.out.println("=== 데모 로그인 계정: userId=kim / password=1234 (이체 비밀번호 0000) ===");
        System.out.println("=== 더미 거래내역: kimMain " + TRANSACTION_COUNT + "건 + 연계계좌(kimSub/leeMain) " + linkedCount + "건 시딩 완료 ===");
    }

    /**
     * kimMain(691-910480-39907) 계좌 기준 최근 2년(730일) 내 100건의 거래내역을 랜덤 생성한다.
     * 최신 거래의 balanceAfter가 실제 계좌 잔액(112,742원)과 일치하도록, 최신순으로 만든 뒤
     * 과거로 갈수록 거래 방향(+/-)에 따라 역산하여 잔액을 이어붙였다.
     * kimMain→kimSub(본인계좌), kimMain→leeMain(이서연계좌) 이체 건은 상대 계좌 쪽에도
     * 대응되는 거래를 함께 생성한다.
     *
     * @return 연계계좌(kimSub/leeMain)에 함께 생성된 거래 건수
     */
    private int seedTransactions(Account kimMain, Account kimSub, Account leeMain) {
        Random rnd = new Random(SEED);
        LocalDateTime now = LocalDateTime.now();

        // 출금성 상호명 풀 (WITHDRAW)
        String[] merchants = {
            "스타벅스", "세븐일레븐천안", "버거킹", "CLIP STUDIO", "애니메이트", "쿠팡", "배달의민족",
            "올리브영", "다이소", "CGV", "교보문고", "GS25", "지하철정기권", "택시", "주유소",
            "이마트", "약국", "헬스장", "넷플릭스 구독료", "유튜브프리미엄", "통신비(KT)", "관리비"
        };
        // 외부입금 상호명 풀 (TRANSFER_IN, 우리 시스템 밖 계좌에서 들어오는 것으로 간주)
        String[] incomingLabels = {
            "국민은행 110-2233-4455", "신한은행(7623)", "월급(주식회사 치이카와)", "카카오페이 정산",
            "중고거래 정산", "생일축하금(부모님)", "환급금(국세청)"
        };

        record Seed(int daysAgo, Transaction.TxType type, long amount, String counterpart, Account linkedAccount) {}

        List<Seed> seeds = new ArrayList<>();
        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            // 최근일수록 더 촘촘하게, 과거일수록 더 듬성듬성 나오도록 분포
            int daysAgo = 1 + (int) (Math.pow(rnd.nextDouble(), 1.6) * SPAN_DAYS);

            int typeRoll = rnd.nextInt(100);
            Transaction.TxType type;
            long amount;
            String counterpart;
            Account linkedAccount = null;

            if (typeRoll < 55) {
                // 55%: 카드/현금성 출금
                type = Transaction.TxType.WITHDRAW;
                amount = 1_000 + rnd.nextInt(60_000);
                counterpart = merchants[rnd.nextInt(merchants.length)];
            } else if (typeRoll < 75) {
                // 20%: 계좌이체 출금 — 20건 중 절반 정도는 내부계좌(kimSub/leeMain)로
                type = Transaction.TxType.TRANSFER_OUT;
                amount = 5_000 + rnd.nextInt(80_000);
                boolean toInternal = rnd.nextBoolean();
                if (toInternal) {
                    boolean toSub = rnd.nextBoolean();
                    linkedAccount = toSub ? kimSub : leeMain;
                    counterpart = linkedAccount.getAccountNumber();
                } else {
                    counterpart = "우리은행 1002-" + (100000 + rnd.nextInt(900000));
                }
            } else if (typeRoll < 90) {
                // 15%: 외부입금
                type = Transaction.TxType.TRANSFER_IN;
                amount = 5_000 + rnd.nextInt(50_000);
                counterpart = incomingLabels[rnd.nextInt(incomingLabels.length)];
            } else {
                // 10%: 월급성 큰 입금 (월 1회 정도 빈도 느낌으로 금액만 크게)
                type = Transaction.TxType.DEPOSIT;
                amount = 300_000 + rnd.nextInt(700_000);
                counterpart = "월급(주식회사 치이카와)";
            }
            seeds.add(new Seed(daysAgo, type, amount, counterpart, linkedAccount));
        }
        // 최신순(daysAgo 작은 순)으로 정렬 — balanceAfter 역산을 위해 필요
        seeds.sort((a, b) -> Integer.compare(a.daysAgo(), b.daysAgo()));

        BigDecimal runningBalance = kimMain.getBalance(); // 최신 거래 직후 잔액(현재 잔액)에서 역산 시작
        BigDecimal minBalance = new BigDecimal("1000"); // 역산 중 잔액이 음수/과소해지지 않도록 하한선
        int linkedCount = 0;

        for (Seed s : seeds) {
            BigDecimal amount = new BigDecimal(s.amount());
            BigDecimal balanceAfter = runningBalance;
            LocalDateTime txDatetime = now.minusDays(s.daysAgo()).minusMinutes(rnd.nextInt(1440));
            String txNumber = "TXN" + txDatetime.toLocalDate().toString().replace("-", "")
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

            transactionRepository.save(new Transaction(txNumber, kimMain, s.type(), amount,
                    s.counterpart(), balanceAfter, txDatetime));

            if (s.linkedAccount() != null) {
                boolean outgoing = s.type() == Transaction.TxType.TRANSFER_OUT;
                Transaction.TxType linkedType = outgoing ? Transaction.TxType.TRANSFER_IN : Transaction.TxType.TRANSFER_OUT;
                BigDecimal linkedBalanceAfter = s.linkedAccount().getBalance(); // 데모 단순화: 연계계좌는 현재잔액 그대로 표기
                transactionRepository.save(new Transaction(txNumber + "-R", s.linkedAccount(), linkedType, amount,
                        kimMain.getAccountNumber(), linkedBalanceAfter, txDatetime));
                linkedCount++;
            }

            boolean wasOutgoing = s.type() == Transaction.TxType.TRANSFER_OUT || s.type() == Transaction.TxType.WITHDRAW;
            BigDecimal next = wasOutgoing ? runningBalance.add(amount) : runningBalance.subtract(amount);
            // 과거로 갈수록 입금 누적으로 잔액이 하한선 밑으로 내려가지 않도록 보정
            runningBalance = next.max(minBalance);
        }
        return linkedCount;
    }
}
