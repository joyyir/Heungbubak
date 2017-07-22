package pe.joyyir.Heungbubak.Comm.Arbitrage;

import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.OrderType;
import pe.joyyir.Heungbubak.Util.CmnUtil;

import java.util.Date;

public class ArbitrageTrade implements Runnable {
    private enum ThreadStatus {
        NEW, RUNNING, SUSPENDED, STOPPED;
    }
    private enum TradeStatus {
        START(1), ORDER_MADE(2), ORDER_CANCELED(3), ORDER_COMPLETED(4);

        @Getter
        private int step;
        TradeStatus (int step) {
            this.step = step;
        }
    }
    @Getter @Setter
    private ThreadStatus threadStatus;
    @Getter @Setter
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

    public boolean isOrderMade() {
        return tradeStatus.getStep() >= TradeStatus.ORDER_MADE.getStep();
    }

    public boolean isOrderCompleted() {
        return tradeStatus.getStep() >= TradeStatus.ORDER_COMPLETED.getStep();
    }

    private void log(String str) {
        String indention = orderType == OrderType.SELL ? " " : "\t\t\t";
        System.out.printf("[%s %s]%s%s\n", CmnUtil.timeToString(new Date()), orderType.name(), indention, str);
    }

    public ArbitrageTrade(ArbitrageExchange exchange, OrderType orderType, Coin coin, long price, double quantity) {
        setThreadStatus(ThreadStatus.NEW);
        setTradeStatus(TradeStatus.START);
        this.exchange = exchange;
        this.orderType = orderType;
        this.coin = coin;
        this.price = price;
        this.quantity = quantity;
        this.oppositeTrade = null;
        this.orderId = null;
        this.thread = new Thread(this);
    }

    public void start() {
        setThreadStatus(ThreadStatus.RUNNING);
        thread.start();
    }

    public void stop() {
        setThreadStatus(ThreadStatus.STOPPED);
    }


    @Override
    public void run() {
        try {
            if(threadStatus == ThreadStatus.STOPPED)
                return;
            makeOrder();
        }
        catch (Exception e) {
            log("거래 생성 실패");
            if(oppositeTrade != null) {
                synchronized (oppositeTrade.getTradeStatus()) {
                    switch (oppositeTrade.getTradeStatus()) {
                        case START:
                            oppositeTrade.stop();
                            break;
                        case ORDER_MADE:
                            log("상대편 거래가 진행 중이므로 상대편 거래 취소 진행");
                            try {
                                oppositeTrade.tryCancelOrder();
                            }
                            catch (Exception e3) {
                                e3.printStackTrace();
                            }
                            break;
                        case ORDER_CANCELED:
                            break;
                        case ORDER_COMPLETED:
                            log("상대편 거래가 완료되었으므로 상대편 거래소에서 역 거래 진행");
                            // TODO : oppositeTrade에 대한 역 거래 진행
                            break;
                        default:
                            break;
                    }
                }
                /*
                if(oppositeTrade.isOrderMade()) {
                    if(oppositeTrade.isOrderCompleted()) {
                        log("상대편 거래가 완료되었으므로 상대편 거래소에서 역 거래 진행");
                        // TODO : oppositeTrade에 대한 역 거래 진행
                    }
                    else {
                        try {
                            log("상대편 거래가 진행 중이므로 상대편 거래 취소 진행");
                            oppositeTrade.wait();
                            oppositeTrade.tryCancelOrder();
                            log("상대편 거래 취소 성공");
                        }
                        catch (Exception e2) {
                            log("상대편 거래 취소 실패! 사용자 확인 필요!!!");
                            // TODO : 사용자에게 알림
                        }
                    }
                }
                else {
                    log("상대편 거래가 만들어지지 않음");
                }
                */
            }
            else {
                log("상대편 거래가 설정되지 않음");
            }
            return;
        }

        try {
            waitOrderCompleted();
        }
        catch (Exception e) { // 제한 시간 내에 거래가 성사되지 않음
            try {
                log("거래가 제한 시간 내에 성사되지 않아 거래 취소 진행");
                tryCancelOrder();
            }
            catch (Exception e2) {
                log("거래 취소 실패! 사용자 확인 필요!!!");
                // TODO : 사용자에게 알림
            }
        }
    }

    private void makeOrder() throws Exception {
        synchronized (tradeStatus) {
            orderId = exchange.makeOrder(orderType, coin, price, quantity);
            setTradeStatus(TradeStatus.ORDER_MADE);
            log("거래 생성 완료");
            tradeStatus.notify();
        }
    }

    private void waitOrderCompleted() throws Exception {
        synchronized (tradeStatus) {
            for(int trial = 0; trial < 5; trial++) {
                try {
                    if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                        setTradeStatus(TradeStatus.ORDER_COMPLETED);
                        log("거래 성사 완료");
                        break;
                    }
                }
                catch (Exception e) { }
            }

            if(!isOrderCompleted()) {
                tradeStatus.notify();
                throw new Exception("거래가 제한 시간 내에 성사되지 않았습니다.");
            }

            tradeStatus.notify();
        }
    }

    private void tryCancelOrder() throws Exception {
        synchronized (tradeStatus) {
            for (int trial = 0; trial < 5; trial++) {
                try {
                    exchange.cancelOrder(orderId, orderType, coin, price, quantity);
                    setTradeStatus(TradeStatus.ORDER_CANCELED);
                    log("거래 취소 완료");
                    tradeStatus.notify();
                    return;
                } catch (Exception e) { }
            }
            //tradeStatus.notify();
            throw new Exception("취소 실패");
        }
    }
}
