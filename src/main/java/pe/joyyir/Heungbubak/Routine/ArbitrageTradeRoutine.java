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
        if (bithumbBuyPrice - coinoneSellPrice >= MIN_DIFF) { // ���濡�� �Ȱ� ���ο����� ���.
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
        } else if (coinoneBuyPrice - bithumbSellPrice >= MIN_DIFF) { // ���ο����� �Ȱ� ���濡�� ���.
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
        double sellQty = sellExchange.getBalance(coin);
        double buyQty = buyExchange.getBalance(Coin.KRW) / sellPrice;
        double qty = Math.min(sellQty, buyQty);
        long expectedProfit = (long) ((sellPrice - buyPrice) * qty);
        if (DEBUG) {
            String debugMsg =
                "\nstep 3. �ŷ� ������ ���� ���� Ȯ��\n" +
                String.format("\t%s���� %f�� �Ǹ� ����, %s���� %f�� ���� ����\n", DEBUG_SELL_EXCHANGE, sellQty, DEBUG_BUY_EXCHANGE, buyQty) +
                String.format("\t=> �ִ� �ŷ���: %f��\n", qty) +
                String.format("\t=> ���� ����: %d KRW\n", expectedProfit);
            appendAndPrint(debugMsg);
        }

        if (expectedProfit < MIN_PROFIT) {
            if (DEBUG) {
                appendAndPrint("\t=> ���� ������ ���غ��� ��� �ŷ����� �ʽ��ϴ�.\n");
            }
            return false;
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
        long realSellPrice = sellArbitPrice.getMaximinimumPrice();
        long realBuyPrice = buyArbitPrice.getMaximinimumPrice();
        long minmaxDiff = realSellPrice - realBuyPrice;
        if (DEBUG) {
            String debugMsg =
                "\nstep 5. ���� �ŷ� ���� ����\n" +
                String.format("\t%f�� �ŷ���,\n", qty) +
                String.format("\t%s���� ��հ� %d, ������ %d�� �Ǹ�\n", DEBUG_SELL_EXCHANGE, sellArbitPrice.getAveragePrice(), sellArbitPrice.getMaximinimumPrice()) +
                String.format("\t%s���� ��հ� %d, �ְ� %d�� ����\n", DEBUG_BUY_EXCHANGE, buyArbitPrice.getAveragePrice(), buyArbitPrice.getMaximinimumPrice()) +
                String.format("\t��հ� ����: %d, �����ְ� ����: %d\n", avgDiff, minmaxDiff);
            appendAndPrint(debugMsg);
        }

        if (avgDiff < MIN_DIFF || minmaxDiff < MIN_DIFF) {
            appendAndPrint("\t=> ������ ����� ���� �����Ƿ� �ŷ��� ����մϴ�.\n");
            return false;
        } else {
            appendAndPrint("\t=> ������ ����ϹǷ� �ŷ��� �����մϴ�.\n");
        }

        if (true) {
            //throw new Exception("���� �������ʹ� ������ �ŷ��� �ǹǷ�, �̸� �����ϴ�.");
        }

        // step 6. �ŷ� ����
        appendAndPrint("\nstep 6. �ŷ� ����\n");
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
            // �ŷ� ����
            appendAndPrint("\t�ŷ� ����!!!\n");
            appendAndPrint("\t�Ǹ� ���: " + sellExchange.getOrderInfo(sellTrade.getOrderId(), coin, OrderType.SELL).toString() + "\n");
            appendAndPrint("\t���� ���: " + buyExchange.getOrderInfo(buyTrade.getOrderId(), coin, OrderType.BUY).toString() + "\n");
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
