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
        cause = (cause != null && !"".equals(cause)) ? cause : "불명";
        synchronized (isCancelRequired) {
            if (cancelRequired) {
                //log("거래 취소가 요청됨 (원인: " + cause + ")");
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
                log("거래 취소가 요청되어 종료");
                return;
            }
            makeOrder();
        }
        catch (Exception e) {
            log("그럴리 없는데...종료 " + e);
            oppositeTrade.setIsCancelRequired(true, "상대방의 원인 불명 오류");
            return;
        }

        try {
            if(isCancelRequired()) {
                log("거래 취소가 요청되어 거래 취소 시도");
                tryCancelOrder();
                log("종료");
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
            log("종료");
            return;
        }

        synchronized (oppositeTrade) {
            while(oppositeTrade.getTradeStatus() != TradeStatus.ORDER_COMPLETED
                    && oppositeTrade.getTradeStatus() != TradeStatus.ORDER_CANCELED) {
                try {
                    log("상대방 거래가 끝날 때까지 대기");
                    oppositeTrade.wait();
                    log("대기 상태 풀림");
                    if(isCancelRequired()) {
                        log("거래 취소가 요청되어 역 거래 시도");
                        try {
                            if((CmnUtil.msTime() % 2 == 1) ? true : false)
                                log("역 거래 성공");
                            else
                                throw new Exception("역 거래 실패... 알아서 하셈");
                        }
                        catch (Exception e2) {
                            log(e2.getMessage());
                        }
                        log("종료");
                        return;
                    }
                }
                catch (InterruptedException e) { }
            }
        }
        log("종료");
    }

    private void makeOrder() {
        synchronized (tradeStatus) {
            try {
                orderId = exchange.makeOrder(orderType, coin, price, quantity);
                setTradeStatus(TradeStatus.ORDER_MADE);
                log("거래 생성 완료");
            }
            catch (Exception e) {
                log("거래 생성 실패 " + e);
                setIsCancelRequired(true, "거래 생성 실패");
                oppositeTrade.setIsCancelRequired(true, "상대방의 거래 생성 실패");
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
                        log("거래 성사 완료");
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
                throw new Exception("거래가 제한 시간 내에 성사되지 않았습니다. " + finalException);
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
                    throw new Exception("취소 실패 " + finalException);
                }
            }
            setTradeStatus(TradeStatus.ORDER_CANCELED);
            log("거래 취소 완료");
            synchronized (oppositeTrade.isCancelRequired()) {
                oppositeTrade.setIsCancelRequired(true, "상대방의 거래 취소");
            }
        }
    }

    private void tryReverseOrder() throws Exception{
        String orderId = "";
        try {
            OrderType reversedOrderType = (orderType == OrderType.BUY) ? OrderType.SELL : OrderType.BUY;
            PriceType reversedPriceType = (orderType == OrderType.BUY) ? PriceType.SELL : PriceType.BUY;
            double reducedQuantity = quantity * 0.9985; // 수수료 제외
            ArbitrageMarketPrice marketPrice = exchange.getArbitrageMarketPrice(coin, reversedPriceType, reducedQuantity);
            orderId = exchange.makeOrder(reversedOrderType, coin, marketPrice.getMaximinimumPrice(), reducedQuantity);
            // TODO - 거래 성사 대기 -> 복구

        }
        catch (Exception e) {
            throw new Exception("역거래 실패... " + e);
        }
    }
}
