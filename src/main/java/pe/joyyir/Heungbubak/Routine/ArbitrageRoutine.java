package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Comm.*;
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
                    "차익: " + Math.abs(bithumbPrice - coinonePrice);
                if(emailSender != null)
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

    public void makeMoney() throws Exception {
        final Coin coin = Coin.ETC;
        final int MIN_DIFF = 0;

        final boolean DEBUG = true;
        String DEBUG_SELL_EXCHANGE = "", DEBUG_BUY_EXCHANGE = "";

        ArbitrageExchange bithumb = new BithumbComm();
        ArbitrageExchange coinone = new CoinoneComm();
        ArbitrageExchange sellExchange = null, buyExchange = null;

        boolean isTiming = false;
        long sellPrice = 0, buyPrice = 0;

        // step 1. 시세 확인
        long bithumbBuyPrice = bithumb.getMarketPrice(coin, PriceType.BUY);
        long bithumbSellPrice = bithumb.getMarketPrice(coin, PriceType.SELL);
        long coinoneBuyPrice = coinone.getMarketPrice(coin, PriceType.BUY);
        long coinoneSellPrice = coinone.getMarketPrice(coin, PriceType.SELL);

        if(DEBUG) {
            System.out.printf("step 1. 시세 확인\n");
            System.out.printf("\t[Bithumb] Buy: %d, Sell: %d\n", bithumbBuyPrice, bithumbSellPrice);
            System.out.printf("\t[Coinone] Buy: %d, Sell: %d\n", coinoneBuyPrice, coinoneSellPrice);
            System.out.printf("\t=> 현재 차익: %d\n", Math.max(bithumbBuyPrice-coinoneSellPrice, coinoneBuyPrice-bithumbSellPrice));
        }

        // step 2. 거래 타이밍인지 확인
        if (bithumbBuyPrice - coinoneSellPrice >= MIN_DIFF) { // 빗썸에서 팔고 코인원에서 산다.
            isTiming = true;
            sellExchange = bithumb;
            sellPrice = bithumbBuyPrice;
            buyExchange = coinone;
            buyPrice = coinoneSellPrice;
            if(DEBUG) {
                System.out.printf("\nstep 2. 거래 타이밍인지 확인\n");
                System.out.printf("\t빗썸에서 팔고 코인원에서 산다.\n");
            }
        }
        else if (coinoneBuyPrice - bithumbSellPrice >= MIN_DIFF) { // 코인원에서 팔고 빗썸에서 산다.
            isTiming = true;
            sellExchange = coinone;
            sellPrice = coinoneBuyPrice;
            buyExchange = bithumb;
            buyPrice = bithumbSellPrice;
            if(DEBUG) {
                DEBUG_SELL_EXCHANGE = "코인원";
                DEBUG_BUY_EXCHANGE = "빗썸";
                System.out.printf("\nstep 2. 거래 타이밍인지 확인\n");
                System.out.printf("\t코인원에서 팔고 빗썸에서 산다.\n");
            }
        }

        if(!isTiming) {
            if(DEBUG) {
                DEBUG_SELL_EXCHANGE = "빗썸";
                DEBUG_BUY_EXCHANGE = "코인원";
                System.out.printf("\nstep 2. 거래 타이밍인지 확인\n");
                System.out.printf("\t거래 타이밍이 아니다.\n");
            }
            return;
        }

        // step 3. 거래 가능한 보유 수량 확인
        double sellQty = sellExchange.getBalance(coin);
        double buyQty = sellExchange.getBalance(Coin.KRW) / sellPrice;
        double qty = Math.min(sellQty, buyQty);
        long expectedProfit = (long)((sellPrice-buyPrice) * qty);
        if(DEBUG) {
            System.out.printf("\nstep 3. 거래 가능한 보유 수량 확인\n");
            System.out.printf("\t%s에서 %f개 판매 가능, %s에서 %f개 구매 가능\n", DEBUG_SELL_EXCHANGE, sellQty, DEBUG_BUY_EXCHANGE, buyQty);
            System.out.printf("\t=> 최대 거래량: %f개\n", qty);
            System.out.printf("\t=> 예상 이익: %d KRW\n", expectedProfit);
        }

        /*
        // step 4. 거래 승인
        System.out.printf("\nstep 4. 거래 승인\n");
        System.out.printf("\t거래를 진행하려 합니다. 동의하십니까? (y/n) : ");
        Scanner sc = new Scanner(System.in);
        String userInput = sc.nextLine();
        if(!userInput.toUpperCase().equals("Y")) {
            System.out.printf("\t거래를 취소합니다.\n");
            return;
        }
        else {
            System.out.printf("\t거래를 진행합니다.\n");
        }
        */

        // step 5. 실제 거래 가격 산정
        ArbitrageMarketPrice sellArbitPrice = sellExchange.getArbitrageMarketPrice(coin, PriceType.BUY, qty);
        ArbitrageMarketPrice buyArbitPrice = buyExchange.getArbitrageMarketPrice(coin, PriceType.SELL, qty);
        long avgDiff = sellArbitPrice.getAveragePrice()-buyArbitPrice.getAveragePrice();
        long minmaxDiff = sellArbitPrice.getMaximinimumPrice()-buyArbitPrice.getMaximinimumPrice();
        if(DEBUG) {
            System.out.printf("\nstep 5. 실제 거래 가격 산정\n");
            System.out.printf("\t%f개 거래시,\n", qty);
            System.out.printf("\t%s에서 평균가 %d, 최저가 %d에 판매\n", DEBUG_SELL_EXCHANGE, sellArbitPrice.getAveragePrice(), sellArbitPrice.getMaximinimumPrice());
            System.out.printf("\t%s에서 평균가 %d, 최고가 %d에 구매\n", DEBUG_BUY_EXCHANGE, buyArbitPrice.getAveragePrice(), buyArbitPrice.getMaximinimumPrice());
            System.out.printf("\t평균가 차익: %d, 최저최고가 차익: %d\n", avgDiff, minmaxDiff);
        }

        if(avgDiff < MIN_DIFF || minmaxDiff < MIN_DIFF) {
            System.out.printf("\t=> 차익이 충분히 나지 않으므로 거래를 취소합니다.");
            return;
        }
        else {
            System.out.printf("\t=> 차익이 충분하므로 거래를 진행합니다.");
        }
    }

    public static void main(String[] args) {
        try {
            ArbitrageRoutine arbitrage = new ArbitrageRoutine(null);
            arbitrage.makeMoney();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
