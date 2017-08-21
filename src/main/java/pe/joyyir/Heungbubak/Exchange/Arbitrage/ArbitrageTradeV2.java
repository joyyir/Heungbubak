package pe.joyyir.Heungbubak.Exchange.Arbitrage;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;

import java.util.Date;

public class ArbitrageTradeV2 implements Runnable {
    // parameters
    final int TRIAL = 10;
    final int TRIAL_TIME_INTERVAL = 1000;

    public enum TradeStatus {
        START, ORDER_MADE, ORDER_CANCELED, ORDER_COMPLETED, ORDER_CANCEL_FAILED
    }
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
    ArbitrageTradeV2 oppositeTrade;
    @Getter @Setter
    String orderId;
    @Getter
    private Thread thread;

    private double beforeKrwBalance;
    private double beforeCoinBalance;

    @Setter
    private StringBuilder emailStringBuilder = null;

    public void setTradeStatus(TradeStatus status) {
        synchronized (this) {
            sharedResource.tradeStatus = status;
            this.notify();
            if(sharedResource.tradeStatus != null) {
                //log("notify");
            }
        }
    }

    public void setIsCancelRequired(boolean cancelRequired, String cause) {
        cause = (cause != null && !"".equals(cause)) ? cause : "불명";
        synchronized (sharedResource) {
            if (cancelRequired) {
                //log("거래 취소가 요청됨 (원인: " + cause + ")");
            }
            sharedResource.isCancelRequired = cancelRequired;
        }
    }

    public Boolean isCancelRequired() {
        synchronized (sharedResource) {
            return sharedResource.isCancelRequired;
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

    public ArbitrageTradeV2(ArbitrageExchange exchange, OrderType orderType, Coin coin, long price, double quantity, double beforeKrwBal, double beforeCoinBal) {
        this.orderType = orderType;
        this.exchange = exchange;
        this.coin = coin;
        this.price = price;
        this.quantity = quantity;
        this.oppositeTrade = null;
        this.orderId = null;
        this.thread = new Thread(this);
        this.beforeCoinBalance = beforeCoinBal;
        this.beforeKrwBalance = beforeKrwBal;
        this.sharedResource = new SharedResource(TradeStatus.START, false);
    }

    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        if(isCancelRequired()) {
            log("거래 취소가 요청되어 종료");
            setTradeStatus(TradeStatus.ORDER_CANCELED);
            return;
        }
        makeOrder();

        try {
            if(isCancelRequired()) {
                log("거래 취소가 요청되어 거래 취소 시도");
                tryCancelOrder();
                log("종료");
                return;
            }
            waitOrderCompleted(orderId, orderType, coin, false);
        }
        catch (Exception e) {
            log(e.getMessage());
            try {
                tryCancelOrder();
            }
            catch (Exception e2) {
                log(e2.getMessage());
                log("종료");
                return;
            }
        }

        boolean didReverseOrder = false;
        if(isCancelRequired()) {
            reverseOrder();
            didReverseOrder = true;
        }

        synchronized (oppositeTrade.sharedResource) {
            while(!isTradeCompleted(oppositeTrade.sharedResource.tradeStatus)) {
                try {
                    log("상대방 거래가 끝날 때까지 대기");
                    oppositeTrade.wait();
                    log("대기 상태 풀림");
                }
                catch (Exception e) { }
            }
        }

        if(isCancelRequired() && !didReverseOrder) {
            reverseOrder();
        }

        log("종료");
    }

    boolean isTradeCompleted(TradeStatus tradeStatus) {
        return  tradeStatus == TradeStatus.ORDER_COMPLETED ||
                tradeStatus == TradeStatus.ORDER_CANCELED ||
                tradeStatus == TradeStatus.ORDER_CANCEL_FAILED;
    }

    private void reverseOrder() {
        log("거래 취소가 요청되어 역거래를 진행합니다.");
        try {
            tryReverseOrder();
            log("역거래 성공!!!");
            Coin reverseCoin;
            double diff;
            if (orderType == OrderType.BUY) {
                double afterKrwBalance = exchange.getBalance(Coin.KRW);
                diff = afterKrwBalance - beforeKrwBalance;
                reverseCoin = Coin.KRW;
            } else { // OrderType.SELL
                double afterCoinBalance = exchange.getBalance(coin);
                diff = afterCoinBalance - beforeCoinBalance;
                reverseCoin = coin;
            }

            if (diff > 0) {
                String logStr = String.format("다행히 이득이다! %+.0f %s", diff, reverseCoin.name());
                log(logStr);
            } else {
                String logStr = String.format("아쉽게도 손해다... %+.0f %s", diff, reverseCoin.name());
                log(logStr);
            }
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    private void makeOrder() throws Exception {
        synchronized (sharedResource) {
            try {
                orderId = exchange.makeOrder(orderType, coin, price, quantity);
                setTradeStatus(TradeStatus.ORDER_MADE);
                log("거래 생성 완료");
            }
            catch (Exception e) {
                throw new Exception("거래 생성 실패 " + e);
            }
        }
    }

    private void waitOrderCompleted(String orderId, OrderType orderType, Coin coin, boolean isReverseTrade) throws Exception {
        boolean isSuccess = false;
        Exception finalException = null;

        synchronized (sharedResource) {
            for (int trial = 0; trial < TRIAL; trial++) {
                try {
                    if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                        if (!isReverseTrade)
                            setTradeStatus(TradeStatus.ORDER_COMPLETED);
                        log("거래 성공");
                        isSuccess = true;
                        break;
                    }
                    Thread.sleep(TRIAL_TIME_INTERVAL);
                } catch (Exception e) {
                    finalException = e;
                }
            }

            if (!isSuccess) {
                if (!isReverseTrade)
                    oppositeTrade.setIsCancelRequired(true, "상대방의 거래 취소");
                throw new Exception("거래가 제한 시간 내에 성사되지 않았습니다. " + ((finalException == null) ? "" : finalException));
            }
        }
    }

    private void tryReverseOrder() throws Exception{
        synchronized (sharedResource) {
            try {
                double tradeQuantity;
                OrderType reversedOrderType = (orderType == OrderType.BUY) ? OrderType.SELL : OrderType.BUY;
                PriceType reversedPriceType = (orderType == OrderType.BUY) ? PriceType.BUY : PriceType.SELL;
                if (orderType == OrderType.BUY) {
                    double afterCoinBalance = exchange.getBalance(coin);
                    tradeQuantity = afterCoinBalance - beforeCoinBalance;
                } else { // OrderType.SELL
                    double afterKrwBalance = exchange.getBalance(Coin.KRW);
                    tradeQuantity = exchange.getAvailableBuyQuantity(coin, (long) (afterKrwBalance - beforeKrwBalance));
                }
                ArbitrageMarketPrice marketPrice = exchange.getArbitrageMarketPrice(coin, reversedPriceType, tradeQuantity);
                String orderId = exchange.makeOrder(reversedOrderType, coin, marketPrice.getMaximinimumPrice(), tradeQuantity);
                waitOrderCompleted(orderId, reversedOrderType, coin, true);
                setTradeStatus(TradeStatus.ORDER_CANCELED);
            } catch (Exception e) {
                setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                throw new Exception("역거래 실패... " + e);
            }
        }
    }

    private void tryCancelOrder() throws Exception {
        synchronized (sharedResource) {
            if (sharedResource.tradeStatus == TradeStatus.ORDER_MADE) {
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
                    try {
                        // 한번 더 거래 성사 확인
                        if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                            finalException = new Exception("그 사이에 거래가 성사됨");
                        }
                    }
                    catch (Exception e) { }

                    setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                    throw new Exception("취소 실패 " + ((finalException == null) ? "" : finalException));
                }

                setTradeStatus(TradeStatus.ORDER_CANCELED);
                log("거래 취소 완료");
            }
        }
    }

    @Data
    private class SharedResource {
        private TradeStatus tradeStatus;
        private Boolean isCancelRequired;

        public SharedResource(TradeStatus tradeStatus, Boolean isCancelRequired) {
            this.tradeStatus = tradeStatus;
            this.isCancelRequired = isCancelRequired;
        }
    }
    private SharedResource sharedResource;

    void run2() {
        if(isCancelRequired()) {
            cancelTrade();
        }
        try {
            makeOrder();
        }
        catch (Exception e) {
            log(e.getMessage());
            cancelTrade();
        }
    }

    private void cancelTrade() {
        synchronized (sharedResource) {
            switch (sharedResource.tradeStatus) {
                case START:
                    setIsCancelRequired(true, "거래 생성 실패");
                    oppositeTrade.setIsCancelRequired(true, "상대방의 거래 생성 실패");
                    break;
                case ORDER_MADE:
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
                        try {
                            Thread.sleep(TRIAL_TIME_INTERVAL);
                        }
                        catch (InterruptedException e) {}
                    }

                    if(!isSuccess) {
                        try {
                            // 한번 더 거래 성사 확인
                            if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                                finalException = new Exception("그 사이에 거래가 성사됨");
                            }
                        }
                        catch (Exception e) { }

                        setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                        throw new Exception("취소 실패 " + ((finalException == null) ? "" : finalException));
                    }

                    setTradeStatus(TradeStatus.ORDER_CANCELED);
                    log("거래 취소 완료");
            }
        }
    }
}
