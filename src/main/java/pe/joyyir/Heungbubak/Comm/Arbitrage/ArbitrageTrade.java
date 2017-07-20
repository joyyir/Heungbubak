package pe.joyyir.Heungbubak.Comm.Arbitrage;

import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Const.OrderType;

public class ArbitrageTrade implements Runnable {
    @Getter @Setter
    private boolean isOrderMade;
    @Getter @Setter
    private boolean isOrderCompleted;
    @Getter @Setter
    private ArbitrageExchange exchange, oppositeExchange;
    @Getter @Setter
    private OrderType orderType;
    @Getter @Setter
    private double quantity;

    public ArbitrageTrade(ArbitrageExchange exchange, OrderType orderType, double quantity, ArbitrageExchange oppositeExchange) {
        setOrderMade(false);
        setOrderCompleted(false);
        setExchange(exchange);
        setOrderType(orderType);
        setQuantity(quantity);
        setOppositeExchange(oppositeExchange);
    }

    @Override
    public void run() {

    }
}
