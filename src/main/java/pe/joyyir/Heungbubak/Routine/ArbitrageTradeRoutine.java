package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.Config.Domain.ArbitrageConfigVO;
import pe.joyyir.Heungbubak.Common.Util.EmailSender;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageExchange;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageMarketPrice;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageTrade;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.DummyTrade;
import pe.joyyir.Heungbubak.Exchange.Service.BithumbService;
import pe.joyyir.Heungbubak.Exchange.Service.CoinoneService;

import java.util.List;

public class ArbitrageTradeRoutine implements Routine{
    private StringBuilder sb = new StringBuilder();
    private EmailSender emailSender = null;

    public ArbitrageTradeRoutine(EmailSender emailSender) throws Exception {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        try {
            ArbitrageConfigVO arbitrageConfigVO = Config.getArbitrageConfig();
            List<Coin> targetCoinArr = arbitrageConfigVO.getTargetCoin();
            final long minProfit = arbitrageConfigVO.getMinProfit();

            for(int i = 0; i < targetCoinArr.size(); i++) {
                final Coin coin = targetCoinArr.get(i);
                final long minDiff = arbitrageConfigVO.getMinDiffMap().get(coin);
                makeMoney(coin, minDiff, minProfit);

                if(emailSender.isReady()) {
                    emailSender.setString("ArbitrageTrade", sb.toString());
                    emailSender.sendEmail();
                    emailSender.setReady(false);
                }
            }
        }
        catch (Exception e) {
            //emailSender.setStringAndReady("ArbitrageTrade", "단순 에러 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean makeMoney(final Coin coin, final long minDiff, final long minProfit) throws Exception {
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
        if (bithumbBuyPrice - coinoneSellPrice >= minDiff) { // 빗썸에서 팔고 코인원에서 산다.
            isTiming = true;
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
        } else if (coinoneBuyPrice - bithumbSellPrice >= minDiff) { // 코인원에서 팔고 빗썸에서 산다.
            isTiming = true;
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
        double sellKrwBalance = sellExchange.getBalance(Coin.KRW);
        double sellCoinQty = sellExchange.getBalance(coin);

        double buyKrwBalance = buyExchange.getBalance(Coin.KRW);
        double buyCoinQty = buyExchange.getBalance(coin);
        double buyAvailQty = buyExchange.getAvailableBuyQuantity(coin, (long)buyKrwBalance);

        double krwSum = sellKrwBalance + buyKrwBalance;
        double coinSum = sellCoinQty + buyCoinQty;

        double qty = Math.min(sellCoinQty, buyAvailQty);
        long expectedProfit = (long) ((sellPrice - buyPrice) * qty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 3. 거래 가능한 보유 수량 확인\n" +
                String.format("\t%s에서 %f개 판매 가능, %s에서 %f개 구매 가능\n", DEBUG_SELL_EXCHANGE, sellCoinQty, DEBUG_BUY_EXCHANGE, buyAvailQty) +
                String.format("\t=> 최대 거래량: %f개\n", qty) +
                String.format("\t=> 예상 이익: %d KRW\n", expectedProfit);
            appendAndPrint(debugMsg);
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
        long realSellPrice = sellArbitPrice.getMaximinimumPrice(); // (내가 팔) 최소 판매가 (최악의 조건)
        long realBuyPrice = buyArbitPrice.getMaximinimumPrice(); // (내가 살) 최대 구입가 (최악의 조건)
        double realQty = Math.min(sellCoinQty, buyKrwBalance / realBuyPrice);
        long minmaxDiff = realSellPrice - realBuyPrice;
        long realExpectedProfit = (long) (minmaxDiff * realQty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 5. 실제 거래 가격 산정\n" +
                String.format("\t%f개 거래시,\n", realQty) +
                String.format("\t%s에서 평균가 %d, 최저가 %d에 판매\n", DEBUG_SELL_EXCHANGE, sellArbitPrice.getAveragePrice(), sellArbitPrice.getMaximinimumPrice()) +
                String.format("\t%s에서 평균가 %d, 최고가 %d에 구매\n", DEBUG_BUY_EXCHANGE, buyArbitPrice.getAveragePrice(), buyArbitPrice.getMaximinimumPrice()) +
                String.format("\t평균가 차익: %d, 최저최고가 차익: %d\n", avgDiff, minmaxDiff) +
                String.format("\t=> 예상 이익: %d KRW\n", realExpectedProfit);
            appendAndPrint(debugMsg);
        }

        if (realExpectedProfit < minProfit) {
            if (DEBUG) {
                appendAndPrint("\t=> 예상 이익이 기준보다 적어서 거래하지 않습니다.\n");
            }
            return false;
        }
        emailSender.setReady(true);

        if (true) {
            //throw new Exception("다음 절차부터는 실제로 거래가 되므로, 이를 막습니다.");
        }

        // step 6. 거래 진행
        appendAndPrint("\nstep 6. 거래 진행\n");
        ArbitrageTrade sellTrade = new ArbitrageTrade(sellExchange, OrderType.SELL, coin, realSellPrice, realQty, sellKrwBalance, sellCoinQty);
        ArbitrageTrade buyTrade = new ArbitrageTrade(buyExchange, OrderType.BUY, coin, realBuyPrice, realQty, buyKrwBalance, buyCoinQty);
        sellTrade.setEmailStringBuilder(sb);
        buyTrade.setEmailStringBuilder(sb);
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

            double sellKrwBalance2 = sellExchange.getBalance(Coin.KRW); // 증가
            double sellCoinQty2 = sellExchange.getBalance(coin); // 감소
            double buyKrwBalance2 = buyExchange.getBalance(Coin.KRW); // 감소
            double buyCoinQty2 = buyExchange.getBalance(coin); // 증가
            double krwSum2 = sellKrwBalance2 + buyKrwBalance2;
            double coinSum2 = sellCoinQty2 + buyCoinQty2;

            // sellExchange에서 코인in, 돈out
            // buyExchange에서 코인out, 돈in
            String debugMsg = String.format("\t[SELL] %s: %+.0f KRW, %+.4f %s\n\t[BUY] %s: %+.0f KRW, %+.4f %s\n", DEBUG_SELL_EXCHANGE, sellKrwBalance2-sellKrwBalance, sellCoinQty2-sellCoinQty, coin.name(), DEBUG_BUY_EXCHANGE, buyKrwBalance2-buyKrwBalance, buyCoinQty2-buyCoinQty, coin.name());
            debugMsg += String.format("\tTotal: %.0f KRW (%+.0f), %.4f %s (%+.4f)\n", krwSum2, krwSum2-krwSum, coinSum2, coin.name(), coinSum2-coinSum);
            appendAndPrint(debugMsg);
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
        ArbitrageTrade sellTrade = new ArbitrageTrade(new DummyTrade(), OrderType.SELL, Coin.ETC, 100000, 100, 1000000, 50);
        ArbitrageTrade buyTrade = new ArbitrageTrade(new DummyTrade(), OrderType.BUY, Coin.ETC, 1000, 100, 1000000, 50);
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
