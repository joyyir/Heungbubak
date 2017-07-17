package pe.joyyir.Heungbubak.Comm;

import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.PriceType;

public interface ArbitrageExchange {
    long getMarketPrice(Coin coin, PriceType priceType) throws Exception;
    double getBalance(Coin coin) throws Exception;
    ArbitrageMarketPrice getArbitrageMarketPrice(Coin coin, PriceType priceType, double quantity) throws Exception;
}
