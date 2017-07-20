package pe.joyyir.Heungbubak.Comm.Arbitrage;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.OrderType;
import pe.joyyir.Heungbubak.Util.CmnUtil;

import java.util.Date;

public class ArbitrageTrade implements Runnable {
    private Boolean isOrderMade;
    private Boolean isOrderCompleted;
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

    public boolean isOrderMade() {
        return isOrderMade.booleanValue();
    }

    public boolean isOrderCompleted() {
        return isOrderCompleted.booleanValue();
    }

    private void log(String str) {
        String indention = orderType == OrderType.SELL ? " " : "\t\t\t";
        System.out.printf("[%s %s]%s%s\n", CmnUtil.timeToString(new Date()), orderType.name(), indention, str);
    }

    public ArbitrageTrade(ArbitrageExchange exchange, OrderType orderType, Coin coin, long price, double quantity) {
        isOrderMade = false;
        isOrderCompleted = false;
        this.exchange = exchange;
        this.orderType = orderType;
        this.coin = coin;
        this.price = price;
        this.quantity = quantity;
        this.oppositeTrade = null;
        this.orderId = null;
    }

    @Override
    public void run() {
        try {
            makeOrder();
            log("거래 생성 완료");
        }
        catch (Exception e) {
            log("거래 생성 실패");
            if(oppositeTrade != null) {
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
            }
            else {
                log("상대편 거래가 설정되지 않음");
            }
            return;
        }

        try {
            waitOrderCompleted();
            log("거래 성사 완료");
        }
        catch (Exception e) { // 제한 시간 내에 거래가 성사되지 않음
            try {
                log("거래가 제한 시간 내에 성사되지 않아 거래 취소 진행");
                tryCancelOrder();
                log("거래 취소 완료");
            }
            catch (Exception e2) {
                log("거래 취소 실패! 사용자 확인 필요!!!");
                // TODO : 사용자에게 알림
            }
        }
    }

    private void makeOrder() throws Exception {
        synchronized (isOrderMade) {
            orderId = exchange.makeOrder(orderType, coin, price, quantity);
            isOrderMade = true;
        }
    }

    private void waitOrderCompleted() throws Exception {
        for(int trial = 0; trial < 5; trial++) {
            try {
                synchronized (isOrderCompleted) {
                    if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                        isOrderCompleted = true;
                        break;
                    }
                }
            }
            catch (Exception e) { }
        }
        if(!isOrderCompleted()) {
            throw new Exception("거래가 제한 시간 내에 성사되지 않았습니다.");
        }
    }

    private void tryCancelOrder() throws Exception {
        for(int trial = 0; trial < 5; trial++) {
            try {
                synchronized (isOrderMade) {
                    exchange.cancelOrder(orderId, orderType, coin, price, quantity);
                    isOrderMade = false;
                }
                return;
            }
            catch (Exception e) { }
        }
        throw new Exception("취소 실패");
    }
}
