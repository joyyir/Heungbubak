package pe.joyyir.Heungbubak.Exchange.Arbitrage;

import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;
import pe.joyyir.Heungbubak.Common.Util.EmailSender;

import java.util.Date;

public class ArbitrageTrade implements Runnable {
    // parameters
    final int TRIAL = 5;
    final int TRIAL_TIME_INTERVAL = 1000;

    public enum TradeStatus {
        START, ORDER_MADE, ORDER_CANCELED, ORDER_COMPLETED
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

    public ArbitrageTrade(ArbitrageExchange exchange, OrderType orderType, Coin coin, long price, double quantity) {
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
    }

    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        try {
            if(isCancelRequired()) {
                log("�ŷ� ��Ұ� ��û�Ǿ� ����");
                return;
            }
            makeOrder();
        }
        catch (Exception e) {
            log("�׷��� ���µ�...���� " + e);
            oppositeTrade.setIsCancelRequired(true, "������ ���� �Ҹ� ����");
            return;
        }

        try {
            if(isCancelRequired()) {
                log("�ŷ� ��Ұ� ��û�Ǿ� �ŷ� ��� �õ�");
                tryCancelOrder();
                log("����");
                return;
            }
            waitOrderCompleted();
        }
        catch (Exception e) {
            log(e.getMessage());
            try {
                tryCancelOrder();
            }
            catch (Exception e2) {
                log(e2.getMessage());
            }
            log("����");
            return;
        }

        synchronized (oppositeTrade) {
            while(oppositeTrade.getTradeStatus() != TradeStatus.ORDER_COMPLETED
                    && oppositeTrade.getTradeStatus() != TradeStatus.ORDER_CANCELED) {
                try {
                    log("���� �ŷ��� ���� ������ ���");
                    oppositeTrade.wait();
                    log("��� ���� Ǯ��");
                    if(isCancelRequired()) {
                        log("�ŷ� ��Ұ� ��û�Ǿ� �� �ŷ� �õ�");
                        try {
                            if((CmnUtil.msTime() % 2 == 1) ? true : false)
                                log("�� �ŷ� ����");
                            else
                                throw new Exception("�� �ŷ� ����... �˾Ƽ� �ϼ�");
                        }
                        catch (Exception e2) {
                            log(e2.getMessage());
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
                orderId = exchange.makeOrder(orderType, coin, price, quantity);
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

    private void waitOrderCompleted() throws Exception {
        synchronized (tradeStatus) {
            boolean isSuccess = false;
            Exception finalException = null;
            for(int trial = 0; trial < TRIAL; trial++) {
                try {
                    if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                        setTradeStatus(TradeStatus.ORDER_COMPLETED);
                        log("�ŷ� ���� �Ϸ�");
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
                throw new Exception("�ŷ��� ���� �ð� ���� ������� �ʾҽ��ϴ�. " + finalException);
        }
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
                    throw new Exception("��� ���� " + finalException);
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
        String orderId = "";
        try {
            OrderType reversedOrderType = (orderType == OrderType.BUY) ? OrderType.SELL : OrderType.BUY;
            PriceType reversedPriceType = (orderType == OrderType.BUY) ? PriceType.SELL : PriceType.BUY;
            double reducedQuantity = quantity * 0.9985; // ������ ����
            ArbitrageMarketPrice marketPrice = exchange.getArbitrageMarketPrice(coin, reversedPriceType, reducedQuantity);
            orderId = exchange.makeOrder(reversedOrderType, coin, marketPrice.getMaximinimumPrice(), reducedQuantity);
            // TODO - �ŷ� ���� ��� -> ����

        }
        catch (Exception e) {
            throw new Exception("���ŷ� ����... " + e);
        }
    }
}
