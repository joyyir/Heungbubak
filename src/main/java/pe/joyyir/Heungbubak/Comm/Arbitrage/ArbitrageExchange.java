package pe.joyyir.Heungbubak.Comm.Arbitrage;

import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.OrderType;
import pe.joyyir.Heungbubak.Const.PriceType;

public interface ArbitrageExchange {
    long getMarketPrice(Coin coin, PriceType priceType) throws Exception;
    double getBalance(Coin coin) throws Exception;
    ArbitrageMarketPrice getArbitrageMarketPrice(Coin coin, PriceType priceType, double quantity) throws Exception;
    String makeOrder(OrderType orderType, Coin coin, long price, double quantity) throws Exception;
    boolean isOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception;
    public void cancelOrder(String orderId, OrderType orderType, Coin coin, long krwPrice, double quantity) throws Exception;
}
