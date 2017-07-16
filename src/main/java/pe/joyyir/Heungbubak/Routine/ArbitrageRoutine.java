package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Comm.BithumbComm;
import pe.joyyir.Heungbubak.Comm.CoinoneComm;
import pe.joyyir.Heungbubak.Comm.EmailSender;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.PriceType;
import lombok.Setter;

import java.util.Scanner;

public class ArbitrageRoutine implements Routine{
    private final int MIN_DIFF_BTC = 20000;
    private final int MIN_DIFF_ETH = 3000;
    private final int MIN_DIFF_ETC = 130;
    private final int MIN_DIFF_XRP = 3;

    private final String BITHUMB_BTC_WALLET_ADDRESS = "1AKnnChADG5svVrNbAGnF4xdNdZ515J4oM";
    private final String COINONE_BTC_WALLET_ADDRESS = "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59";
    private final String COINONE_BTC_WALLET_TYPE    = "trade";

    private CoinoneComm coinone = new CoinoneComm();
    private BithumbComm bithumb = new BithumbComm();

    private boolean canNotice = true;
    private int noticeCount = 0;

    @Setter
    private EmailSender emailSender = null;

    public ArbitrageRoutine(EmailSender emailSender) throws Exception {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        try {
            // config
            final Coin coin = Coin.ETC;
            final int MIN_DIFF = MIN_DIFF_ETC;

            boolean isTiming = false, mustSellBithumb = false;
            long bithumbBuyPrice = bithumb.getMarketPrice(coin, PriceType.BUY);
            long bithumbSellPrice = bithumb.getMarketPrice(coin, PriceType.SELL);
            long coinoneBuyPrice = coinone.getMarketPrice(coin, PriceType.BUY);
            long coinoneSellPrice = coinone.getMarketPrice(coin, PriceType.SELL);
            long coinonePrice = 0, bithumbPrice = 0;

            System.out.printf("[Bithumb] Buy: %d, Sell: %d\n", bithumbBuyPrice, bithumbSellPrice);
            System.out.printf("[Coinone] Buy: %d, Sell: %d\n", coinoneBuyPrice, coinoneSellPrice);
            System.out.printf("[현재 차익] %d\n", Math.max(bithumbBuyPrice-coinoneSellPrice, coinoneBuyPrice-bithumbSellPrice));

            if(!canNotice) {
                noticeCount++;
                if(noticeCount == 6) {
                    noticeCount = 0;
                    canNotice = true;
                }
            }

            if (bithumbBuyPrice - coinoneSellPrice >= MIN_DIFF) {
                isTiming = true;
                mustSellBithumb = true;
                coinonePrice = coinoneSellPrice;
                bithumbPrice = bithumbBuyPrice;
            }
            else if (coinoneBuyPrice - bithumbSellPrice >= MIN_DIFF) {
                isTiming = true;
                mustSellBithumb = false;
                coinonePrice = coinoneBuyPrice;
                bithumbPrice = bithumbSellPrice;
            }

            if(isTiming && canNotice) {
                String mailMsg =
                    coin.name() + " 거래소 차익 거래 타이밍 입니다.\n" +
                    (mustSellBithumb ? "빗썸에서 팔고 코인원에서 사세요.\n" : "코인원에서 팔고 빗썸에서 사세요.\n") +
                    "Bithumb: " + bithumbPrice + "\n" +
                    "Coinone: " + coinonePrice + "\n" +
                    "차액: " + Math.abs(bithumbPrice - coinonePrice);
                emailSender.setStringAndReady("Arbitrage", mailMsg);
                canNotice = false;
                System.out.println(mailMsg);
            }

            System.out.printf("--------------------------------------------------\n");
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
            bithumbPrice = bithumb.getMarketPrice(Coin.BTC, PriceType.BUY);
            coinonePrice = coinone.getLastMarketPrice(Coin.BTC);

            System.out.printf("step 1. 시세\n");
            System.out.printf("\tbithumb(%d) coinone(%d) diff(%d)\n", bithumbPrice, coinonePrice, Math.abs(bithumbPrice-coinonePrice));

            // step 2. 거래 가능액 보유 여부 확인
            double bithumbKRW, bithumbBTC;
            double coinoneKRW, coinoneBTC;
            bithumbKRW = bithumb.getBalance(Coin.KRW);
            bithumbBTC = bithumb.getBalance(Coin.BTC);
            coinoneKRW = coinone.getBalance(Coin.KRW);
            coinoneBTC = coinone.getBalance(Coin.BTC);

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
