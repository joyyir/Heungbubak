package Routine;

import Comm.BithumbComm;
import Comm.CoinoneComm;
import Comm.EmailSender;
import lombok.Setter;

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
}
