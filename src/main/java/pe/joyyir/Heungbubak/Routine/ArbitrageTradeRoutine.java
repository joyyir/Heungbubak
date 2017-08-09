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
            long minProfit = arbitrageConfigVO.getMinProfit();
            if(minProfit < 1000)
                minProfit = 1000;

            for(int i = 0; i < targetCoinArr.size(); i++) {
                final Coin coin = targetCoinArr.get(i);
                long minDiff = arbitrageConfigVO.getMinDiffMap().get(coin);
                if(minDiff < 0)
                    minDiff = 0;
                makeMoney(coin, minDiff, minProfit);

                if(emailSender.isReady()) {
                    emailSender.setString("ArbitrageTrade", sb.toString());
                    emailSender.sendEmail();
                    emailSender.setReady(false);
                }
            }
        }
        catch (Exception e) {
            //emailSender.setStringAndReady("ArbitrageTrade", "�ܼ� ���� �߻�: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void makeMoney(final Coin coin, final long minDiff, final long minProfit) throws Exception {
        final boolean DEBUG = true;
        String sellExchangeName = "", buyExchangeName = "";
        ArbitrageExchange bithumb = new BithumbService();
        ArbitrageExchange coinone = new CoinoneService();
        ArbitrageExchange sellExchange = null, buyExchange = null;

        boolean isTiming = false;
        long sellPrice = 0, buyPrice = 0;
        sb = new StringBuilder();

        // step 1. �ü� Ȯ��
        long bithumbBuyPrice = bithumb.getMarketPrice(coin, PriceType.BUY);
        long bithumbSellPrice = bithumb.getMarketPrice(coin, PriceType.SELL);
        long coinoneBuyPrice = coinone.getMarketPrice(coin, PriceType.BUY);
        long coinoneSellPrice = coinone.getMarketPrice(coin, PriceType.SELL);

        if (DEBUG) {
            String debugMsg =
                "\n--------------------------------------------------\n\n" +
                coin.name() + "�� �ŷ�\n\n" +
                "step 1. �ü� Ȯ��\n" +
                String.format("\t[Bithumb] Buy: %d, Sell: %d\n", bithumbBuyPrice, bithumbSellPrice) +
                String.format("\t[Coinone] Buy: %d, Sell: %d\n", coinoneBuyPrice, coinoneSellPrice) +
                String.format("\t=> ���� ����: %d\n", Math.max(bithumbBuyPrice - coinoneSellPrice, coinoneBuyPrice - bithumbSellPrice));
            appendAndPrint(debugMsg);
        }

        // step 2. �ŷ� Ÿ�̹����� Ȯ��
        if (bithumbBuyPrice - coinoneSellPrice >= minDiff) { // ���濡�� �Ȱ� ���ο����� ���.
            isTiming = true;
            sellExchange = bithumb;
            sellPrice = bithumbBuyPrice;
            buyExchange = coinone;
            buyPrice = coinoneSellPrice;
            if (DEBUG) {
                sellExchangeName = "����";
                buyExchangeName = "���ο�";
                String debugMsg = "\nstep 2. �ŷ� Ÿ�̹����� Ȯ��\n" + "\t���濡�� �Ȱ� ���ο����� ���.\n";
                appendAndPrint(debugMsg);
            }
        } else if (coinoneBuyPrice - bithumbSellPrice >= minDiff) { // ���ο����� �Ȱ� ���濡�� ���.
            isTiming = true;
            sellExchange = coinone;
            sellPrice = coinoneBuyPrice;
            buyExchange = bithumb;
            buyPrice = bithumbSellPrice;
            if (DEBUG) {
                sellExchangeName = "���ο�";
                buyExchangeName = "����";
                String debugMsg = "\nstep 2. �ŷ� Ÿ�̹����� Ȯ��\n" + "\t���ο����� �Ȱ� ���濡�� ���.\n";
                appendAndPrint(debugMsg);
            }
        }

        if (!isTiming) {
            if (DEBUG) {
                String debugMsg = "\nstep 2. �ŷ� Ÿ�̹����� Ȯ��\n" + "\t�ŷ� Ÿ�̹��� �ƴϴ�.\n";
                appendAndPrint(debugMsg);
            }
            return;
        }

        // step 3. �ŷ� ������ ���� ���� Ȯ��
        double sellKrwBalance = sellExchange.getBalance(Coin.KRW);
        double sellCoinBalance = sellExchange.getBalance(coin);

        double buyKrwBalance = buyExchange.getBalance(Coin.KRW);
        double buyCoinBalance = buyExchange.getBalance(coin);

        double buyAvailQty = buyExchange.getAvailableBuyQuantity(coin, (long)buyKrwBalance);

        double krwSum = sellKrwBalance + buyKrwBalance;
        double coinSum = sellCoinBalance + buyCoinBalance;

        double qty = Math.min(sellCoinBalance, buyAvailQty);
        long expectedProfit = (long) ((sellPrice - buyPrice) * qty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 3. �ŷ� ������ ���� ���� Ȯ��\n" +
                String.format("\t%s���� %f�� �Ǹ� ����, %s���� %f�� ���� ����\n", sellExchangeName, sellCoinBalance, buyExchangeName, buyAvailQty) +
                String.format("\t=> �ִ� �ŷ���: %f��\n", qty) +
                String.format("\t=> ���� ����: %d KRW\n", expectedProfit);
            appendAndPrint(debugMsg);
        }

        /*
        // step 4. �ŷ� ����
        System.out.printf("\nstep 4. �ŷ� ����\n");
        System.out.printf("\t�ŷ��� �����Ϸ� �մϴ�. �����Ͻʴϱ�? (y/n) : ");
        Scanner sc = new Scanner(System.in);
        String userInput = sc.nextLine();
        if(!userInput.toUpperCase().equals("Y")) {
            System.out.printf("\t�ŷ��� ����մϴ�.\n");
            return;
        }
        else {
            System.out.printf("\t�ŷ��� �����մϴ�.\n");
        }
        */

        // step 5. ���� �ŷ� ���� ����
        ArbitrageMarketPrice sellArbitPrice = sellExchange.getArbitrageMarketPrice(coin, PriceType.BUY, qty);
        ArbitrageMarketPrice buyArbitPrice = buyExchange.getArbitrageMarketPrice(coin, PriceType.SELL, qty);
        long avgDiff = sellArbitPrice.getAveragePrice() - buyArbitPrice.getAveragePrice();
        long realSellPrice = sellArbitPrice.getMaximinimumPrice(); // (���� ��) �ּ� �ǸŰ� (�־��� ����)
        long realBuyPrice = buyArbitPrice.getMaximinimumPrice(); // (���� ��) �ִ� ���԰� (�־��� ����)
        double realQty = Math.min(sellCoinBalance, buyKrwBalance / realBuyPrice);
        long minmaxDiff = realSellPrice - realBuyPrice;
        long realExpectedProfit = (long) (minmaxDiff * realQty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 5. ���� �ŷ� ���� ����\n" +
                String.format("\t%f�� �ŷ���,\n", realQty) +
                String.format("\t%s���� ��հ� %d, ������ %d�� �Ǹ�\n", sellExchangeName, sellArbitPrice.getAveragePrice(), sellArbitPrice.getMaximinimumPrice()) +
                String.format("\t%s���� ��հ� %d, �ְ� %d�� ����\n", buyExchangeName, buyArbitPrice.getAveragePrice(), buyArbitPrice.getMaximinimumPrice()) +
                String.format("\t��հ� ����: %d, �����ְ� ����: %d\n", avgDiff, minmaxDiff) +
                String.format("\t=> ���� ����: %d KRW\n", realExpectedProfit);
            appendAndPrint(debugMsg);
        }

        if (realExpectedProfit < minProfit) {
            if (DEBUG) {
                appendAndPrint("\t=> ���� ������ ���غ��� ��� �ŷ����� �ʽ��ϴ�.\n");
            }
            return;
        }

        if (true) {
            //throw new Exception("���� �������ʹ� ������ �ŷ��� �ǹǷ�, �̸� �����ϴ�.");
        }

        // step 6. �ŷ� ����
        appendAndPrint("\nstep 6. �ŷ� ����\n");
        emailSender.setReady(true);
        ArbitrageTrade sellTrade = new ArbitrageTrade(sellExchange, OrderType.SELL, coin, realSellPrice, realQty, sellKrwBalance, sellCoinBalance);
        ArbitrageTrade buyTrade = new ArbitrageTrade(buyExchange, OrderType.BUY, coin, realBuyPrice, realQty, buyKrwBalance, buyCoinBalance);
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
            // �ŷ� ����
            appendAndPrint("\t�ŷ� ����!!!\n");
            appendAndPrint("\t�Ǹ� ���: " + sellExchange.getOrderInfo(sellTrade.getOrderId(), coin, OrderType.SELL).toString() + "\n");
            appendAndPrint("\t���� ���: " + buyExchange.getOrderInfo(buyTrade.getOrderId(), coin, OrderType.BUY).toString() + "\n");
        }
        else {
            // �ŷ� ����
            appendAndPrint("\t�ŷ� ����!!!\n");
        }

        double sellKrwBalance2 = sellExchange.getBalance(Coin.KRW); // ����
        double sellCoinQty2 = sellExchange.getBalance(coin); // ����
        double buyKrwBalance2 = buyExchange.getBalance(Coin.KRW); // ����
        double buyCoinQty2 = buyExchange.getBalance(coin); // ����
        double krwSum2 = sellKrwBalance2 + buyKrwBalance2;
        double coinSum2 = sellCoinQty2 + buyCoinQty2;

        // sellExchange���� ����in, ��out
        // buyExchange���� ����out, ��in
        String debugMsg = String.format("\t[SELL] %s: %+.0f KRW, %+.4f %s\n\t[BUY] %s: %+.0f KRW, %+.4f %s\n", sellExchangeName, sellKrwBalance2-sellKrwBalance, sellCoinQty2-sellCoinBalance, coin.name(), buyExchangeName, buyKrwBalance2-buyKrwBalance, buyCoinQty2-buyCoinBalance, coin.name());
        debugMsg += String.format("\tTotal: %.0f KRW (%+.0f), %.4f %s (%+.4f)\n", krwSum2, krwSum2-krwSum, coinSum2, coin.name(), coinSum2-coinSum);
        appendAndPrint(debugMsg);
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
