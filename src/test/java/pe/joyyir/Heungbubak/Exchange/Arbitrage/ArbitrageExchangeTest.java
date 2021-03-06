package pe.joyyir.Heungbubak.Exchange.Arbitrage;

import org.json.JSONObject;
import org.junit.Test;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Exchange.Service.BithumbService;
import pe.joyyir.Heungbubak.Exchange.Service.CoinoneService;

import static org.junit.Assert.*;

/**
 * Created by 1003880 on 2017. 9. 2..
 */
public class ArbitrageExchangeTest {
    private ArbitrageExchange service;
    private Coin coin;

    public ArbitrageExchangeTest() {
        try {
            this.service = new BithumbService();
            this.coin = Coin.XRP;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAvailableBuyQuantity() throws Exception {
        double qty = service.getAvailableBuyQuantity(coin, 10000);
        System.out.println("getAvailableBuyQuantity: " + qty);
        assertTrue(qty > 0);
    }

    @Test
    public void getArbitrageMarketPrice() throws Exception {
        double qty = service.getAvailableBuyQuantity(coin, 10000);
        System.out.println("getArbitrageMarketPrice: " + qty);
        assertTrue(qty > 0);
    }

    @Test
    public void getMarketPrice() throws Exception {
        double buyPrice = service.getMarketPrice(coin, PriceType.BUY);
        double sellPrice = service.getMarketPrice(coin, PriceType.SELL);
        System.out.println("getMarketPrice: (buy)" + buyPrice + ", (sell)" + sellPrice);
        assertTrue(buyPrice > 0 && sellPrice > 0);
    }

    @Test
    public void getBalance() throws Exception {
        double bal = service.getBalance(coin);
        System.out.println("getBalance: " + bal);
        assertTrue(bal >= 0.0);
    }

    @Test
    public void makeOrder() throws Exception {
        OrderType orderType = OrderType.BUY;
        long krwPrice = 200;
        double qty = 10;
        JSONObject orderInfo;
        System.out.println("makeOrder:");
        String orderId = service.makeOrder(orderType, coin, krwPrice, qty);
        System.out.println("\torder made! (orderId)" + orderId);
        orderInfo = service.getOrderInfo(orderId, coin, orderType);
        System.out.println("\t(orderInfo)" + orderInfo.toString(4));
        if (service.isOrderCompleted(orderId, orderType, coin)) {
            System.out.println("\torder completed!");
            assertTrue(false);
        }
        else {
            System.out.println("\torder not completed!");
            assertTrue(true);
        }
        if (service.isOrderExist(orderId, coin, orderType)) {
            System.out.println("\torder exist");
            assertTrue(true);
        }
        else {
            System.out.println("\torder not exist");
            assertTrue(false);
        }
        Thread.sleep(3000);
        service.cancelOrder(orderId, orderType, coin, krwPrice, qty);
        System.out.println("\tcancel completed!");
        if (service.isOrderExist(orderId, coin, orderType)) {
            System.out.println("\torder exist");
            assertTrue(false);
        }
        else {
            System.out.println("\torder not exist");
            assertTrue(true);
        }
    }

    @Test
    public void isOrderCompleted() throws Exception {
        //assertTrue(service.isOrderCompleted("c726ca93-f165-4918-a107-a80a1b173837", OrderType.SELL, coin)); // Coinone
    }

    @Test
    public void cancelOrder() throws Exception {
    }

    @Test
    public void getOrderInfo() throws Exception {
    }
}