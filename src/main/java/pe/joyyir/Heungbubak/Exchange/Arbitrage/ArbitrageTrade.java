package pe.joyyir.Heungbubak.Exchange.Arbitrage;

import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;

import java.util.Date;

public class ArbitrageTrade implements Runnable {
    // parameters
    final int TRIAL = 10;
    final int TRIAL_TIME_INTERVAL = 1000;

    public enum TradeStatus {
        START, ORDER_MADE, ORDER_CANCELED, ORDER_COMPLETED, ORDER_CANCEL_FAILED
    }
    @Getter
    private TradeStatus tradeStatus;
    @Getter @Setter
    private ArbitrageExchange exchange;
    @Getter @Setter
    private OrderType orderType;
    @Getter @Setter
    private Coin coin;
    @Getter @Setter
    private long price;
    @Getter @Setter
    private double quantity;
    @Getter @Setter
    ArbitrageTrade oppositeTrade;
    @Getter @Setter
    String orderId;
    @Getter
    private Thread thread;
    private Boolean isCancelRequired;

    private double beforeKrwBalance;
    private double beforeCoinBalance;

    @Setter
    private StringBuilder emailStringBuilder = null;

    public void setTradeStatus(TradeStatus status) {
        synchronized (this) {
            tradeStatus = status;
            this.notify();
            if(tradeStatus != null) {
                //log("notify");
            }
        }
    }

    public void setIsCancelRequired(boolean cancelRequired, String cause) {
        cause = (cause != null && !"".equals(cause)) ? cause : "�Ҹ�";
        synchronized (isCancelRequired) {
            if (cancelRequired) {
                //log("�ŷ� ��Ұ� ��û�� (����: " + cause + ")");
            }
            isCancelRequired = cancelRequired;
        }
    }

    public Boolean isCancelRequired() {
        synchronized (isCancelRequired) {
            return isCancelRequired;
        }
    }

    private void log(String str) {
        String indention = orderType == OrderType.SELL ? " " : "\t\t\t\t\t\t\t\t\t\t\t\t\t";
        String logStr = String.format("%s%s[%s] %s\n", CmnUtil.timeToString(new Date()), indention, orderType.name(), str);
        if(emailStringBuilder != null) {
            emailStringBuilder.append(logStr);
        }
        System.out.printf(logStr);
    }

    public ArbitrageTrade(ArbitrageExchange exchange, OrderType orderType, Coin coin, long price, double quantity, double beforeKrwBal, double beforeCoinBal) {
        this.orderType = orderType;
        this.tradeStatus = TradeStatus.START;
        this.exchange = exchange;
        this.coin = coin;
        this.price = price;
        this.quantity = quantity;
        this.oppositeTrade = null;
        this.orderId = null;
        this.thread = new Thread(this);
        this.isCancelRequired = false;
        this.beforeCoinBalance = beforeCoinBal;
        this.beforeKrwBalance = beforeKrwBal;
    }

    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        if(isCancelRequired()) {
            log("�ŷ� ��Ұ� ��û�Ǿ� ����");
            setTradeStatus(TradeStatus.ORDER_CANCELED);
            return;
        }
        makeOrder();

        try {
            if(isCancelRequired()) {
                log("�ŷ� ��Ұ� ��û�Ǿ� �ŷ� ��� �õ�");
                tryCancelOrder();
                log("����");
                return;
            }
            waitOrderCompletedSync();
        }
        catch (Exception e) {
            log(e.getMessage());
            try {
                tryCancelOrder();
            }
            catch (Exception e2) {
                log(e2.getMessage());
                log("����");
                return;
            }
        }

        synchronized (oppositeTrade) {
            while(oppositeTrade.getTradeStatus() != TradeStatus.ORDER_COMPLETED
                    && oppositeTrade.getTradeStatus() != TradeStatus.ORDER_CANCELED
                    && oppositeTrade.getTradeStatus() != TradeStatus.ORDER_CANCEL_FAILED) {
                try {
                    log("���� �ŷ��� ���� ������ ���");
                    oppositeTrade.wait();
                    log("��� ���� Ǯ��");
                    if(tradeStatus == TradeStatus.ORDER_COMPLETED && isCancelRequired()) {
                        log("�ŷ� ��Ұ� ��û�Ǿ� ���ŷ��� �����մϴ�.");
                        try {
                            tryReverseOrder();
                            log("���ŷ� ����!!!");
                            Coin reverseCoin;
                            double diff;
                            if(orderType == OrderType.BUY) {
                                double afterKrwBalance = exchange.getBalance(Coin.KRW);
                                diff = afterKrwBalance-beforeKrwBalance;
                                reverseCoin = Coin.KRW;
                            }
                            else { // OrderType.SELL
                                double afterCoinBalance = exchange.getBalance(coin);
                                diff = afterCoinBalance-beforeCoinBalance;
                                reverseCoin = coin;
                            }

                            if(diff > 0) {
                                String logStr = String.format("������ �̵��̴�! %+.0f %s", diff, reverseCoin.name());
                                log(logStr);
                            }
                            else {
                                String logStr = String.format("�ƽ��Ե� ���ش�... %+.0f %s", diff, reverseCoin.name());
                                log(logStr);
                            }
                        }
                        catch (Exception e) {
                            log(e.getMessage());
                        }
                        log("����");
                        return;
                    }
                }
                catch (InterruptedException e) { }
            }
        }
        log("����");
    }

    private void makeOrder() {
        synchronized (tradeStatus) {
            try {
                double qty = (orderType == OrderType.SELL) ? (0.999 * quantity) : quantity; // �Ĵ� �ʿ����� ������ 0.1% ��ŭ �� �Ǵ�.
                orderId = exchange.makeOrder(orderType, coin, price, qty);
                setTradeStatus(TradeStatus.ORDER_MADE);
                log("�ŷ� ���� �Ϸ�");
            }
            catch (Exception e) {
                log("�ŷ� ���� ���� " + e);
                setIsCancelRequired(true, "�ŷ� ���� ����");
                oppositeTrade.setIsCancelRequired(true, "������ �ŷ� ���� ����");
            }
        }
    }

    private void waitOrderCompletedSync() throws Exception {
        synchronized (tradeStatus) {
            waitOrderCompleted(orderId, orderType, coin, true);
        }
    }

    private void waitOrderCompleted(String orderId, OrderType orderType, Coin coin, boolean isSync) throws Exception {
        boolean isSuccess = false;
        Exception finalException = null;
        for(int trial = 0; trial < TRIAL; trial++) {
            try {
                if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                    if(isSync)
                        setTradeStatus(TradeStatus.ORDER_COMPLETED);
                    log("�ŷ� ����");
                    isSuccess = true;
                    break;
                }
                Thread.sleep(TRIAL_TIME_INTERVAL);
            }
            catch (Exception e) {
                finalException = e;
            }
        }

        if(!isSuccess)
            throw new Exception("�ŷ��� ���� �ð� ���� ������� �ʾҽ��ϴ�. " + ((finalException == null) ? "" : finalException));
    }

    private void tryCancelOrder() throws Exception {
        synchronized (tradeStatus) {
            if (tradeStatus == TradeStatus.ORDER_MADE) {
                boolean isSuccess = false;
                Exception finalException = null;
                for (int trial = 0; trial < TRIAL; trial++) {
                    try {
                        exchange.cancelOrder(orderId, orderType, coin, price, quantity);
                        isSuccess = true;
                        break;
                    } catch (Exception e) {
                        finalException = e;
                    }
                    Thread.sleep(TRIAL_TIME_INTERVAL);
                }
                if(!isSuccess) {
                    // �ѹ� �� �ŷ� ���� Ȯ��
                    boolean isCompleted = exchange.isOrderCompleted(orderId, orderType, coin);
                    if(isCompleted) {
                        setTradeStatus(TradeStatus.ORDER_COMPLETED);
                        finalException = new Exception("�� ���̿� �ŷ��� �����");
                    }
                    else {
                        setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                    }
                    throw new Exception("��� ���� " + ((finalException == null) ? "" : finalException));
                }
            }
            setTradeStatus(TradeStatus.ORDER_CANCELED);
            log("�ŷ� ��� �Ϸ�");
            synchronized (oppositeTrade.isCancelRequired()) {
                oppositeTrade.setIsCancelRequired(true, "������ �ŷ� ���");
            }
        }
    }

    private void tryReverseOrder() throws Exception{
        try {
            double tradeQuantity;
            OrderType reversedOrderType = (orderType == OrderType.BUY) ? OrderType.SELL : OrderType.BUY;
            PriceType reversedPriceType = (orderType == OrderType.BUY) ? PriceType.BUY : PriceType.SELL;
            if(orderType == OrderType.BUY) {
                double afterCoinBalance = exchange.getBalance(coin);
                tradeQuantity = afterCoinBalance - beforeCoinBalance;
            }
            else { // OrderType.SELL
                double afterKrwBalance = exchange.getBalance(Coin.KRW);
                tradeQuantity = exchange.getAvailableBuyQuantity(coin, (long)(afterKrwBalance - beforeKrwBalance));
            }
            ArbitrageMarketPrice marketPrice = exchange.getArbitrageMarketPrice(coin, reversedPriceType, tradeQuantity);
            String orderId = exchange.makeOrder(reversedOrderType, coin, marketPrice.getMaximinimumPrice(), tradeQuantity);
            waitOrderCompleted(orderId, reversedOrderType, coin, false);
        }
        catch (Exception e) {
            throw new Exception("���ŷ� ����... " + e);
        }
    }
}
