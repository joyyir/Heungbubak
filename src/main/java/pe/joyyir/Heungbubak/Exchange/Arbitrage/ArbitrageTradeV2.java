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
        synchronized (sharedResource) {
            sharedResource.tradeStatus = status;
            sharedResource.notify();
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

    private void waitOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception {
        boolean isSuccess = false;
        Exception finalException = null;

        synchronized (sharedResource) {
            for (int trial = 0; trial < TRIAL; trial++) {
                try {
                    if (exchange.isOrderCompleted(orderId, orderType, coin)) {
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
                throw new Exception("거래가 제한 시간 내에 성사되지 않았습니다. " + ((finalException == null) ? "" : finalException));
            }
        }
    }

    private void tryReverseOrder() throws Exception{
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
            waitOrderCompleted(orderId, reversedOrderType, coin);
        } catch (Exception e) {
            throw new Exception("역거래 실패... " + e);
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

    @Override
    public void run() {
        // TradeStatus.START
        if(isCancelRequired()) {
            cancelTrade();
            return;
        }
        try {
            makeOrder();
        }
        catch (Exception e) {
            log(e.getMessage());
            cancelTrade();
            return;
        }

        // TradeStatus.ORDER_MADE
        if(isCancelRequired()) {
            cancelTrade();
            return;
        }
        try {
            waitOrderCompleted(orderId, orderType, coin);
        }
        catch (Exception e) {
            log(e.getMessage());
            cancelTrade();
            return;
        }

        // TradeStatus.ORDER_COMPLETED
        if(isCancelRequired()) {
            cancelTrade();
            return;
        }
        synchronized (oppositeTrade.sharedResource) {
            while(!isTradeCompleted(oppositeTrade.sharedResource.tradeStatus)) {
                try {
                    log("상대방 거래가 끝날 때까지 대기");
                    oppositeTrade.sharedResource.wait();
                    log("대기 상태 풀림");
                }
                catch (Exception e) { }
            }
        }
        if(isCancelRequired()) {
            cancelTrade();
            return;
        }
        log("종료");
    }

    // 교착상태가 발생할 가능성이 있는 함수
    private void cancelTrade() {
        synchronized (sharedResource) {
            switch (sharedResource.tradeStatus) {
                case START: // 거래 생성 실패
                    setTradeStatus(TradeStatus.ORDER_CANCELED);
                    oppositeTrade.setIsCancelRequired(true, "상대방의 거래 생성 실패");
                    break;
                case ORDER_MADE: // 거래 성사 실패
                    try {
                        cancelOrder();
                        setTradeStatus(TradeStatus.ORDER_CANCELED);
                        log("거래 취소 완료");
                    }
                    catch (Exception e) {
                        log(e.getMessage());
                        setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                    }
                    oppositeTrade.setIsCancelRequired(true, "상대방의 거래 성사 실패");
                    break;
                case ORDER_COMPLETED:
                    try {
                        reverseOrder();
                        setTradeStatus(TradeStatus.ORDER_CANCELED);
                    }
                    catch (Exception e) {
                        log(e.getMessage());
                        setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                    }
                    break;
                default:
                    log("nothing to do: " + sharedResource.tradeStatus.name());
                    break;
            }
        }
        log("완료");
    }

    private void cancelOrder() throws Exception {
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

            throw new Exception("거래 취소 실패 " + ((finalException == null) ? "" : finalException));
        }
    }
}
