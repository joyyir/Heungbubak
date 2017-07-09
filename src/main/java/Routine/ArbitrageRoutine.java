package Routine;

import Comm.BithumbComm;
import Comm.CoinoneComm;
import Comm.EmailSender;
import lombok.Setter;

import java.util.Scanner;

public class ArbitrageRoutine implements Routine{
    private final int MIN_DIFF_BTC = 20000;
    private final int MIN_DIFF_ETH = 3000;
    private final int MIN_DIFF_ETC = 300;
    private final int MIN_DIFF_XRP = 3;

    private final String BITHUMB_BTC_WALLET_ADDRESS = "1AKnnChADG5svVrNbAGnF4xdNdZ515J4oM";
    private final String COINONE_BTC_WALLET_ADDRESS = "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59";
    private final String COINONE_BTC_WALLET_TYPE    = "trade";

    private CoinoneComm coinone = new CoinoneComm();
    private BithumbComm bithumb = new BithumbComm();

    private boolean isTiming = false;
    private int timingCount = 0;

    @Setter
    private EmailSender emailSender = null;

    public ArbitrageRoutine(EmailSender emailSender) throws Exception {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        try {
            // 시세와 개수 가져옴
            long coinoneAmount = 0, bithumbAmount = 0;
            long bithumbPrice = bithumb.getMarketPrice(BithumbComm.COIN_BTC, BithumbComm.PriceType.BUY);
            long coinonePrice = coinone.getMarketPrice(CoinoneComm.COIN_BTC);

            if(isTiming) { // 한번 알림 후에는 60초동안 다시 알림을 주지 않는다.
                timingCount++;
                if(timingCount == 6) {
                    isTiming = false;
                    timingCount = 0;
                }
            }
            else if (Math.abs(coinonePrice - bithumbPrice) >= MIN_DIFF_BTC) {
                isTiming = true;

                String mailMsg =
                    "BTC 거래소 차익 거래 타이밍 입니다.\n" +
                    "Bithumb: " + bithumbPrice + "\n" +
                    "Coinone: " + coinonePrice + "\n" +
                    "차액: " + Math.abs(coinonePrice - bithumbPrice);
                emailSender.setStringAndReady("Arbitrage", mailMsg);
            }

            if (coinonePrice - bithumbPrice >= MIN_DIFF_BTC) {
                // 거래 가능 여부 확인 (충분한 BTC, KRW)

                // 코인원에서 비싸게 팔고, 빗썸에서 싸게 산다.

                // 코인원에서 BTC 판매 (-BTC, +KRW)

                // 빗썸에서 BTC 구매 (송금 수수료만큼 더 사야함)  (+BTC, -KRW)

                // 빗썸->코인원 BTC 송금

                // KRW 송금에 대한 노티
            } else if (bithumbPrice - coinonePrice >= MIN_DIFF_BTC) {
                // 빗썸에서 비싸게 팔고, 코인원에서 싸게 산다.
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            BithumbComm bithumb = new BithumbComm();
            CoinoneComm coinone = new CoinoneComm();

            // step 1. 시세 확인
            long bithumbPrice, coinonePrice;
            bithumbPrice = bithumb.getMarketPrice(BithumbComm.COIN_BTC, BithumbComm.PriceType.BUY);
            coinonePrice = coinone.getMarketPrice(CoinoneComm.COIN_BTC);

            System.out.printf("step 1. 시세\n");
            System.out.printf("\tbithumb(%d) coinone(%d) diff(%d)\n", bithumbPrice, coinonePrice, Math.abs(bithumbPrice-coinonePrice));

            // step 2. 거래 가능액 보유 여부 확인
            double bithumbKRW, bithumbBTC;
            double coinoneKRW, coinoneBTC;
            bithumbKRW = bithumb.getBalance(BithumbComm.COIN_KRW);
            bithumbBTC = bithumb.getBalance(BithumbComm.COIN_BTC);
            coinoneKRW = coinone.getBalance(CoinoneComm.COIN_KRW);
            coinoneBTC = coinone.getBalance(CoinoneComm.COIN_BTC);

            System.out.printf("\nstep 2. 보유액\n");
            System.out.printf("\tbithumb: KRW(%f) BTC(%f)\n", bithumbKRW, bithumbBTC);
            System.out.printf("\tcoinone: KRW(%f) BTC(%f)\n", coinoneKRW, coinoneBTC);

            // step 3. 거래 개수
            // step 3-1. 빗썸에서 팔고 코인원에서 구매
            System.out.printf("\nstep 3. 거래 개수\n");
            System.out.printf("\tstep 3-1. 빗썸에서 팔고 코인원에서 구매\n");
            System.out.printf("\t\t빗썸에서 팔 수 있는 개수: %f\n", bithumbBTC);
            System.out.printf("\t\t코인원에서 살 수 있는 개수: %f\n", coinoneKRW/coinonePrice);
            System.out.printf("\t\t최종 거래할 수 있는 개수: %f\n", Math.min(bithumbBTC, coinoneKRW/coinonePrice));
            System.out.printf("\tstep 3-2. 코인원에서 팔고 빗썸에서 구매\n");
            System.out.printf("\t\t코인원에서 팔 수 있는 개수: %f\n", coinoneBTC);
            System.out.printf("\t\t빗썸에서 살 수 있는 개수: %f\n", bithumbKRW/bithumbPrice);
            System.out.printf("\t\t최종 거래할 수 있는 개수: %f\n", Math.min(coinoneBTC, bithumbKRW/bithumbPrice));

            // step 4. 거래 승인
            System.out.printf("\nstep 4. 거래 승인\n");
            System.out.printf("\t거래를 진행하려 합니다. 동의하십니까?(y/n) : ");
            Scanner sc = new Scanner(System.in);
            String userInput = sc.nextLine();
            if(!userInput.toUpperCase().equals("Y"))
                return;

            // step 5. 판매

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
