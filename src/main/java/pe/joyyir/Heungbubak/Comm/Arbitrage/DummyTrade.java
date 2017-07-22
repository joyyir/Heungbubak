package pe.joyyir.Heungbubak.Comm.Arbitrage;

import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.OrderType;
import pe.joyyir.Heungbubak.Const.PriceType;

import java.util.Random;

public class DummyTrade implements ArbitrageExchange {
    @Override
    public long getMarketPrice(Coin coin, PriceType priceType) throws Exception {
        Random random = new Random();
        int number = random.nextInt(100);
        if(number < 20)
            throw new Exception("getMarketPrice 오류");
        return 1;
    }

    @Override
    public double getBalance(Coin coin) throws Exception {
        Random random = new Random();
        int number = random.nextInt(100);
        if(number < 20)
            throw new Exception("getBalance 오류");
        return 1;
    }

    @Override
    public ArbitrageMarketPrice getArbitrageMarketPrice(Coin coin, PriceType priceType, double quantity) throws Exception {
        Random random = new Random();
        int number = random.nextInt(100);
        if(number < 20)
            throw new Exception("getArbitrageMarketPrice 오류");
        return new ArbitrageMarketPrice(0, 0);
    }

    @Override
    public String makeOrder(OrderType orderType, Coin coin, long price, double quantity) throws Exception {
        Random random = new Random();
        int number = random.nextInt(100);
        if(number < 20)
            throw new Exception("makeOrder 오류");
        return "SUCCESS";
    }

    @Override
    public boolean isOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception {
        Random random = new Random();
        int number = random.nextInt(100);
        if(number < 20)
            throw new Exception("isOrderCompleted 오류");
        return (number % 2 == 1) ? true : false;
    }

    @Override
    public void cancelOrder(String orderId, OrderType orderType, Coin coin, long krwPrice, double quantity) throws Exception {
        Random random = new Random();
        int number = random.nextInt(100);
        if(number < 20)
            throw new Exception("cancelOrder 오류");
    }
}
