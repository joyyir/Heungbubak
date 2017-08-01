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
            emailSender.setStringAndReady("ArbitrageTrade", "�ܼ� ���� �߻�: " + e.getMessage());
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
            emailSender.setReady(true);
            sellExchange = bithumb;
            sellPrice = bithumbBuyPrice;
            buyExchange = coinone;
            buyPrice = coinoneSellPrice;
            if (DEBUG) {
                DEBUG_SELL_EXCHANGE = "����";
                DEBUG_BUY_EXCHANGE = "���ο�";
                String debugMsg = "\nstep 2. �ŷ� Ÿ�̹����� Ȯ��\n" + "\t���濡�� �Ȱ� ���ο����� ���.\n";
                appendAndPrint(debugMsg);
            }
        } else if (coinoneBuyPrice - bithumbSellPrice >= minDiff) { // ���ο����� �Ȱ� ���濡�� ���.
            isTiming = true;
            emailSender.setReady(true);
            sellExchange = coinone;
            sellPrice = coinoneBuyPrice;
            buyExchange = bithumb;
            buyPrice = bithumbSellPrice;
            if (DEBUG) {
                DEBUG_SELL_EXCHANGE = "���ο�";
                DEBUG_BUY_EXCHANGE = "����";
                String debugMsg = "\nstep 2. �ŷ� Ÿ�̹����� Ȯ��\n" + "\t���ο����� �Ȱ� ���濡�� ���.\n";
                appendAndPrint(debugMsg);
            }
        }

        if (!isTiming) {
            if (DEBUG) {
                String debugMsg = "\nstep 2. �ŷ� Ÿ�̹����� Ȯ��\n" + "\t�ŷ� Ÿ�̹��� �ƴϴ�.\n";
                appendAndPrint(debugMsg);
            }
            return false;
        }

        // step 3. �ŷ� ������ ���� ���� Ȯ��
        double sellKrwBalance = sellExchange.getBalance(Coin.KRW);
        double sellCoinQty = sellExchange.getBalance(coin);

        double buyKrwBalance = buyExchange.getBalance(Coin.KRW);
        double buyCoinQty = buyExchange.getBalance(coin);
        double buyAvailQty = buyKrwBalance / buyPrice;

        double qty = Math.min(sellCoinQty, buyAvailQty);
        long expectedProfit = (long) ((sellPrice - buyPrice) * qty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 3. �ŷ� ������ ���� ���� Ȯ��\n" +
                String.format("\t%s���� %f�� �Ǹ� ����, %s���� %f�� ���� ����\n", DEBUG_SELL_EXCHANGE, sellCoinQty, DEBUG_BUY_EXCHANGE, buyAvailQty) +
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
        double realQty = Math.min(sellCoinQty, buyKrwBalance / realBuyPrice);
        long minmaxDiff = realSellPrice - realBuyPrice;
        long realExpectedProfit = (long) (minmaxDiff * realQty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 5. ���� �ŷ� ���� ����\n" +
                String.format("\t%f�� �ŷ���,\n", realQty) +
                String.format("\t%s���� ��հ� %d, ������ %d�� �Ǹ�\n", DEBUG_SELL_EXCHANGE, sellArbitPrice.getAveragePrice(), sellArbitPrice.getMaximinimumPrice()) +
                String.format("\t%s���� ��հ� %d, �ְ� %d�� ����\n", DEBUG_BUY_EXCHANGE, buyArbitPrice.getAveragePrice(), buyArbitPrice.getMaximinimumPrice()) +
                String.format("\t��հ� ����: %d, �����ְ� ����: %d\n", avgDiff, minmaxDiff) +
                String.format("\t=> ���� ����: %d KRW\n", realExpectedProfit);
            appendAndPrint(debugMsg);
        }

        if (realExpectedProfit < minProfit) {
            if (DEBUG) {
                appendAndPrint("\t=> ���� ������ ���غ��� ��� �ŷ����� �ʽ��ϴ�.\n");
            }
            return false;
        }

        if (true) {
            //throw new Exception("���� �������ʹ� ������ �ŷ��� �ǹǷ�, �̸� �����ϴ�.");
        }

        // step 6. �ŷ� ����
        appendAndPrint("\nstep 6. �ŷ� ����\n");
        ArbitrageTrade sellTrade = new ArbitrageTrade(sellExchange, OrderType.SELL, coin, realSellPrice, realQty);
        ArbitrageTrade buyTrade = new ArbitrageTrade(buyExchange, OrderType.BUY, coin, realBuyPrice, realQty);
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

            double sellKrwBalance2 = sellExchange.getBalance(Coin.KRW); // ����
            double sellCoinQty2 = sellExchange.getBalance(coin); // ����
            double buyKrwBalance2 = buyExchange.getBalance(Coin.KRW); // ����
            double buyCoinQty2 = buyExchange.getBalance(coin); // ����

            // sellExchange���� ����in, ��out
            // buyExchange���� ����out, ��in
            String debugMsg = String.format("\t[sell] %s: %+.0f KRW, %+.4f %s\n\t[buy] %s: %+.0f KRW, %+.4f %s\n", DEBUG_SELL_EXCHANGE, sellKrwBalance2-sellKrwBalance, sellCoinQty2-sellCoinQty, coin.name(), DEBUG_BUY_EXCHANGE, buyKrwBalance2-buyKrwBalance, buyCoinQty2-buyCoinQty, coin.name());
            appendAndPrint(debugMsg);
        }
        else {
            // �ŷ� ����
            appendAndPrint("\t�ŷ� ����!!!\n");
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
