package pe.joyyir.Heungbubak.Exchange.Arbitrage;

import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;

public interface ArbitrageExchange {
    long getMarketPrice(Coin coin, PriceType priceType) throws Exception;
    double getBalance(Coin coin) throws Exception;
    ArbitrageMarketPrice getArbitrageMarketPrice(Coin coin, PriceType priceType, double quantity) throws Exception;
    double getAvailableBuyQuantity(Coin coin, long krwBalance) throws Exception;
    String makeOrder(OrderType orderType, Coin coin, long price, double quantity) throws Exception;
    boolean isOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception;
    void cancelOrder(String orderId, OrderType orderType, Coin coin, long krwPrice, double quantity) throws Exception;
    JSONObject getOrderInfo(String orderId, Coin coin, OrderType orderType) throws Exception;
}
