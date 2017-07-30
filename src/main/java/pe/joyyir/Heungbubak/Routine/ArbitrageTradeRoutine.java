package pe.joyyir.Heungbubak.Routine;

import lombok.Setter;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.EmailSender;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageExchange;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageMarketPrice;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageTrade;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.DummyTrade;
import pe.joyyir.Heungbubak.Exchange.Service.BithumbService;
import pe.joyyir.Heungbubak.Exchange.Service.CoinoneService;

public class ArbitrageTradeRoutine implements Routine{
    private final int MIN_DIFF_BTC = 20000;
    private final int MIN_DIFF_ETH = 2000;
    private final int MIN_DIFF_ETC = 100;//100;
    private final int MIN_DIFF_XRP = 1;

    private final Coin[] COIN_ARR = {Coin.BTC, Coin.ETC, Coin.ETH, Coin.XRP};
    private final int[] DIFF_ARR = {MIN_DIFF_BTC, MIN_DIFF_ETC, MIN_DIFF_ETH, MIN_DIFF_XRP};
    private boolean[] canNoticeArr = {true, true, true, true};
    private int[] noticeCountArr = {0, 0, 0, 0};

    private final String BITHUMB_BTC_WALLET_ADDRESS = "1AKnnChADG5svVrNbAGnF4xdNdZ515J4oM";
    private final String COINONE_BTC_WALLET_ADDRESS = "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59";
    private final String COINONE_BTC_WALLET_TYPE    = "trade";

    private CoinoneService coinone = new CoinoneService();
    private BithumbService bithumb = new BithumbService();

    private StringBuilder sb = new StringBuilder();

    @Setter
    private EmailSender emailSender = null;

    public ArbitrageTradeRoutine(EmailSender emailSender) throws Exception {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        try {
            for(int i = 0; i < COIN_ARR.length; i++) {
                final Coin coin = COIN_ARR[i];
                final int MIN_DIFF = DIFF_ARR[i];
                final long MIN_PROFIT = 1000;
                makeMoney(coin, MIN_DIFF, MIN_PROFIT);

                if(emailSender.isReady()) {
                    emailSender.setString("ArbitrageTrade", sb.toString());
                    emailSender.sendEmail();
                    emailSender.setReady(false);
                }
            }
        }
        catch (Exception e) {
            emailSender.setStringAndReady("ArbitrageTrade", sb.toString());
        }
    }

    public boolean makeMoney(final Coin coin, final int MIN_DIFF, final long MIN_PROFIT) throws Exception {
        final boolean DEBUG = true;
        String DEBUG_SELL_EXCHANGE = "", DEBUG_BUY_EXCHANGE = "";

        ArbitrageExchange bithumb = new BithumbService();
        ArbitrageExchange coinone = new CoinoneService();
        ArbitrageExchange sellExchange = null, buyExchange = null;

        boolean isTiming = false;
        long sellPrice = 0, buyPrice = 0;
        sb = new StringBuilder();

        // step 1. 시세 확인
        long bithumbBuyPrice = bithumb.getMarketPrice(coin, PriceType.BUY);
        long bithumbSellPrice = bithumb.getMarketPrice(coin, PriceType.SELL);
        long coinoneBuyPrice = coinone.getMarketPrice(coin, PriceType.BUY);
        long coinoneSellPrice = coinone.getMarketPrice(coin, PriceType.SELL);

        if (DEBUG) {
            String debugMsg =
                "\n--------------------------------------------------\n\n" +
                coin.name() + "로 거래\n\n" +
                "step 1. 시세 확인\n" +
                String.format("\t[Bithumb] Buy: %d, Sell: %d\n", bithumbBuyPrice, bithumbSellPrice) +
                String.format("\t[Coinone] Buy: %d, Sell: %d\n", coinoneBuyPrice, coinoneSellPrice) +
                String.format("\t=> 현재 차익: %d\n", Math.max(bithumbBuyPrice - coinoneSellPrice, coinoneBuyPrice - bithumbSellPrice));
            appendAndPrint(debugMsg);
        }

        // step 2. 거래 타이밍인지 확인
        if (bithumbBuyPrice - coinoneSellPrice >= MIN_DIFF) { // 빗썸에서 팔고 코인원에서 산다.
            isTiming = true;
            emailSender.setReady(true);
            sellExchange = bithumb;
            sellPrice = bithumbBuyPrice;
            buyExchange = coinone;
            buyPrice = coinoneSellPrice;
            if (DEBUG) {
                DEBUG_SELL_EXCHANGE = "빗썸";
                DEBUG_BUY_EXCHANGE = "코인원";
                String debugMsg = "\nstep 2. 거래 타이밍인지 확인\n" + "\t빗썸에서 팔고 코인원에서 산다.\n";
                appendAndPrint(debugMsg);
            }
        } else if (coinoneBuyPrice - bithumbSellPrice >= MIN_DIFF) { // 코인원에서 팔고 빗썸에서 산다.
            isTiming = true;
            emailSender.setReady(true);
            sellExchange = coinone;
            sellPrice = coinoneBuyPrice;
            buyExchange = bithumb;
            buyPrice = bithumbSellPrice;
            if (DEBUG) {
                DEBUG_SELL_EXCHANGE = "코인원";
                DEBUG_BUY_EXCHANGE = "빗썸";
                String debugMsg = "\nstep 2. 거래 타이밍인지 확인\n" + "\t코인원에서 팔고 빗썸에서 산다.\n";
                appendAndPrint(debugMsg);
            }
        }

        if (!isTiming) {
            if (DEBUG) {
                String debugMsg = "\nstep 2. 거래 타이밍인지 확인\n" + "\t거래 타이밍이 아니다.\n";
                appendAndPrint(debugMsg);
            }
            return false;
        }

        // step 3. 거래 가능한 보유 수량 확인
        double sellQty = sellExchange.getBalance(coin);
        double buyQty = buyExchange.getBalance(Coin.KRW) / sellPrice;
        double qty = Math.min(sellQty, buyQty);
        long expectedProfit = (long) ((sellPrice - buyPrice) * qty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 3. 거래 가능한 보유 수량 확인\n" +
                String.format("\t%s에서 %f개 판매 가능, %s에서 %f개 구매 가능\n", DEBUG_SELL_EXCHANGE, sellQty, DEBUG_BUY_EXCHANGE, buyQty) +
                String.format("\t=> 최대 거래량: %f개\n", qty) +
                String.format("\t=> 예상 이익: %d KRW\n", expectedProfit);
            appendAndPrint(debugMsg);
        }

        if (expectedProfit < MIN_PROFIT) {
            if (DEBUG) {
                appendAndPrint("\t=> 예상 이익이 기준보다 적어서 거래하지 않습니다.\n");
            }
            return false;
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
        long avgDiff = sellArbitPrice.getAveragePrice() - buyArbitPrice.getAveragePrice();
        long realSellPrice = sellArbitPrice.getMaximinimumPrice();
        long realBuyPrice = buyArbitPrice.getMaximinimumPrice();
        long minmaxDiff = realSellPrice - realBuyPrice;
        if (DEBUG) {
            String debugMsg =
                "\nstep 5. 실제 거래 가격 산정\n" +
                String.format("\t%f개 거래시,\n", qty) +
                String.format("\t%s에서 평균가 %d, 최저가 %d에 판매\n", DEBUG_SELL_EXCHANGE, sellArbitPrice.getAveragePrice(), sellArbitPrice.getMaximinimumPrice()) +
                String.format("\t%s에서 평균가 %d, 최고가 %d에 구매\n", DEBUG_BUY_EXCHANGE, buyArbitPrice.getAveragePrice(), buyArbitPrice.getMaximinimumPrice()) +
                String.format("\t평균가 차익: %d, 최저최고가 차익: %d\n", avgDiff, minmaxDiff);
            appendAndPrint(debugMsg);
        }

        if (avgDiff < MIN_DIFF || minmaxDiff < MIN_DIFF) {
            appendAndPrint("\t=> 차익이 충분히 나지 않으므로 거래를 취소합니다.\n");
            return false;
        } else {
            appendAndPrint("\t=> 차익이 충분하므로 거래를 진행합니다.\n");
        }

        if (true) {
            //throw new Exception("다음 절차부터는 실제로 거래가 되므로, 이를 막습니다.");
        }

        // step 6. 거래 진행
        appendAndPrint("\nstep 6. 거래 진행\n");
        ArbitrageTrade sellTrade = new ArbitrageTrade(sellExchange, OrderType.SELL, coin, realSellPrice, qty);
        ArbitrageTrade buyTrade = new ArbitrageTrade(buyExchange, OrderType.BUY, coin, realBuyPrice, qty);
        sellTrade.setOppositeTrade(buyTrade);
        buyTrade.setOppositeTrade(sellTrade);
        sellTrade.start();
        buyTrade.start();
        try {
            sellTrade.getThread().join();
            buyTrade.getThread().join();
        }
        catch (Exception e) {
            appendAndPrint("join exception");
        }

        if(sellTrade.getTradeStatus() == ArbitrageTrade.TradeStatus.ORDER_COMPLETED
                && buyTrade.getTradeStatus() == ArbitrageTrade.TradeStatus.ORDER_COMPLETED) {
            // 거래 성공
            appendAndPrint("\t거래 성공!!!\n");
            appendAndPrint("\t판매 결과: " + sellExchange.getOrderInfo(sellTrade.getOrderId(), coin, OrderType.SELL).toString() + "\n");
            appendAndPrint("\t구매 결과: " + buyExchange.getOrderInfo(buyTrade.getOrderId(), coin, OrderType.BUY).toString() + "\n");
        }
        else {
            // 거래 실패
            appendAndPrint("\t거래 실패!!!\n");
        }
        return true;
    }

    private void appendAndPrint(String debugMsg) {
        sb.append(debugMsg);
        System.out.print(debugMsg);
    }

    private void testTrade() {
        ArbitrageTrade sellTrade = new ArbitrageTrade(new DummyTrade(), OrderType.SELL, Coin.ETC, 100000, 100);
        ArbitrageTrade buyTrade = new ArbitrageTrade(new DummyTrade(), OrderType.BUY, Coin.ETC, 1000, 100);
        sellTrade.setOppositeTrade(buyTrade);
        buyTrade.setOppositeTrade(sellTrade);
        sellTrade.start();
        buyTrade.start();
        try {
            sellTrade.getThread().join();
            buyTrade.getThread().join();
        }
        catch (Exception e) {
            System.out.println("join exception");
        }
    }

    public static void main(String[] args) {
        try {
            ArbitrageTradeRoutine arbitrage = new ArbitrageTradeRoutine(null);
            while(true) {
                try {
                    Thread.sleep(5000);
                    //if (arbitrage.makeMoney())
                    //    break;
                    arbitrage.testTrade();
                    System.out.println("\n------------------------------------------------------------\n");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //arbitrage.testTrade();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
