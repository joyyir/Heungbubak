package pe.joyyir.Heungbubak.Exchange.Service;

import org.json.JSONArray;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageExchange;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageMarketPrice;
import pe.joyyir.Heungbubak.Exchange.ApiKey.BithumbApiKey;
import pe.joyyir.Heungbubak.Common.Const.BankCode;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.HTTPUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Exchange.DAO.BithumbDAO;
import pe.joyyir.Heungbubak.Exchange.Domain.BalanceVO;
import pe.joyyir.Heungbubak.Exchange.Domain.BasicPriceVO;
import pe.joyyir.Heungbubak.Exchange.Domain.CoinPriceVO;

import java.util.HashMap;
import java.util.Map;

public class BithumbService implements ArbitrageExchange {
    public static final String STATUS_CODE_SUCCESS = "0000";
    public static final String STATUS_CODE_CUSTOM = "5600";
    private final String API_URL = "https://api.bithumb.com/";
    private final String TICKER_URL = "public/ticker/";

    public static final Coin[] COIN_ARRAY = { Coin.BTC, Coin.ETC, Coin.ETH, Coin.XRP, Coin.LTC, Coin.DASH };

    @Getter @Setter
    private BithumbApiKey apikey;
    private String key;
    private String secret;

    public BithumbService() throws Exception {
        setApikey(new BithumbApiKey());
        key = getApikey().getKey();
        secret = getApikey().getSecret();
    }

    public static void main(String[] args) {
        try {
            BithumbService comm = new BithumbService();
            CoinoneService coinComm = new CoinoneService();

            double quantity = 0.99992;
            System.out.println(String.format("%+.0f", Math.floor(quantity*10000)/10000));

            /*
            comm.sendCoin(Coin.BTC, 0.001f, "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59", null); // 수수료 포함 0.001 BTC
            */

            /*
            long bithumbBTCprice = comm.getMarketPrice(BithumbService.Coin.BTC, PriceType.BUY);
            long coinoneBTCprice = coinComm.getMarketPrice(CoinoneService.Coin.BTC);
            System.out.println("bithumb BTC 시세 : " + bithumbBTCprice);
            System.out.println("coinone BTC 시세 : " + coinoneBTCprice);
            System.out.println("차이 : " + Math.abs(bithumbBTCprice-coinoneBTCprice));
            */

            //comm.makeOrder(OrderType.BUY, Coin.ETC, 19570L, 0.1F);
            //comm.makeOrder(OrderType.SELL, Coin.ETC, 19450L, 0.0998F);

            //comm.withdrawalKRW(BankCode.SHINHAN, "110325467846", 10000);

            /*
            String orderId = comm.makeOrder(OrderType.BUY, Coin.ETC, 10000, 0.1);
            System.out.println(comm.isOrderCompleted(orderId, OrderType.BUY, Coin.ETC));
            comm.cancelOrder(orderId, OrderType.BUY, Coin.ETC, 1000, 0.1);

            Thread.sleep(100);

            String orderId2 = comm.makeOrder(OrderType.SELL, Coin.ETC, 23700, 0.1);
            System.out.println(comm.isOrderCompleted(orderId2, OrderType.SELL, Coin.ETC));
            comm.cancelOrder(orderId2, OrderType.SELL, Coin.ETC, 23700, 0.1);
            comm.cancelOrder(orderId2, OrderType.SELL, Coin.ETC, 23700, 1);
            */

//            System.out.println(comm.isOrderCompleted(orderId, OrderType.BUY, Coin.ETC));

            //JSONObject result = comm.getOrderInfo("1500384872078", OrderType.BUY, Coin.ETC); // 성사된 거래
            //System.out.println(comm.isOrderCompleted(result));
            //System.out.println(comm.isOrderCompleted("1500384872078", OrderType.BUY, Coin.ETC));

            //comm.getOrderInfo("1500381006079", OrderType.BUY, Coin.ETC, false);
            //System.out.println(comm.isOrderCompleted("1499599864512", OrderType.SELL, Coin.ETC));

            //System.out.println(comm.getBalance(Coin.ETC));
            //System.out.println(comm.getMarketPrice(Coin.ETC, PriceType.BUY));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getAvailableBuyQuantity(Coin coin, long krwBalance) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL + "public/orderbook/" + coin.name().toLowerCase());
        errorCheck(jsonObject, "getAverageMarketPrice");
        JSONArray orders = jsonObject.getJSONObject("data").getJSONArray(PriceType.SELL.toString() + 's');
        double sum = 0.0;
        double left = krwBalance;
        for(int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.getJSONObject(i);
            long price = order.getLong("price");
            double qty = order.getDouble("quantity");

            if(price * qty > left) {
                sum += left / price;
                left = 0.0;
                break;
            }
            else {
                sum += qty;
                left -= price * qty;
            }
        }
        if(left > 0.0)
            throw new Exception("Too much KRW");
        return sum;
    }

    @Override
    public ArbitrageMarketPrice getArbitrageMarketPrice(Coin coin, PriceType priceType, double quantity) throws Exception {
        ArbitrageMarketPrice marketPrice = new ArbitrageMarketPrice(0, 0);
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL + "public/orderbook/" + coin.name().toLowerCase());
        errorCheck(jsonObject, "getAverageMarketPrice");
        JSONArray orders = jsonObject.getJSONObject("data").getJSONArray(priceType.toString() + 's');
        double sum = 0.0;
        double left = quantity;
        for(int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.getJSONObject(i);
            long price = order.getLong("price");
            double qty = order.getDouble("quantity");
            marketPrice.setMaximinimumPrice(price);

            if(qty > left) {
                sum += price * left;
                left = 0.0;
                break;
            }
            else {
                sum += price * qty;
                left -= qty;
            }
        }
        if(left > 0.0)
            throw new Exception("Too much quantity");
        marketPrice.setAveragePrice((long)(sum/quantity));
        return marketPrice;
    }

    @Override
    public long getMarketPrice(Coin coin, PriceType priceType) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+coin.name());

        String key = "";
        switch (priceType) {
            case BUY : // 거래 대기건 최고 구매가
                key = "buy_price";
                break;
            case SELL: // 거래 대기건 최소 판매가
                key = "sell_price";
                break;
        }

        long price;
        try {
            price = Long.valueOf(jsonObject.getJSONObject("data").getString(key));
        }
        catch (NumberFormatException e) {
            price = (long)((double)Double.valueOf(jsonObject.getJSONObject("data").getDouble(key)));
        }

        return price;
    }

    public long getCompleteBalance() throws Exception {
        BithumbDAO dao = new BithumbDAO();
        BalanceVO balanceVO = dao.getBalanceVO();
        CoinPriceVO priceVO = dao.getCoinPriceVO();
        double sum = 0.0;

        for(Coin coin : COIN_ARRAY) {
            double balance = balanceVO.getAvailable().get(coin);
            BasicPriceVO vo = priceVO.getPrice().get(coin);
            if(vo == null) continue;
            double price = vo.getBuyPrice();
            sum += balance * price;
        }
        return (long) (sum + balanceVO.getAvailable().get(Coin.KRW));
    }

    @Override
    public double getBalance(Coin coin) throws Exception {
        BithumbDAO dao = new BithumbDAO();
        BalanceVO vo = dao.getBalanceVO();
        return vo.getAvailable().get(coin);
    }

    public void sendCoin(Coin coin, float units, String address, Integer destination) throws Exception {
        String endpoint = "trade/btc_withdrawal";
        Map<String, String> params = new HashMap<>();
        params.put("units", String.valueOf(units));
        params.put("address", address);
        if(coin == Coin.XRP)
            params.put("destination", destination.toString());
        params.put("currency", coin.name().toUpperCase());

        BithumbDAO dao = new BithumbDAO();
        JSONObject result = dao.callApi(endpoint, params);

        errorCheck(result, "Sending coin");
    }

    @Override
    public String makeOrder(OrderType orderType, Coin coin, long price, double quantity) throws Exception {
        final String endpoint = "trade/place";
        Map<String, String> params = new HashMap<>();
        params.put("order_currency", coin.name().toUpperCase());
        params.put("Payment_currency", "KRW");
        params.put("units", String.format("%.4f", Math.floor(quantity*10000)/10000)); // 코인 수량은 소수점 4째짜리 까지만 입력가능
        params.put("price", String.valueOf(price));
        params.put("type", orderType.toString());

        BithumbDAO dao = new BithumbDAO();
        JSONObject result = dao.callApi(endpoint, params);

        //System.out.println(result.toString());

        errorCheck(result, "Making order");
        return result.getString("order_id");
    }

    // 바로 되는게 아니다.
    // 수수료 건당 1,000원, 출금 최소 금액 5,000원, 일일 출금 한도 5000만원, 월 출금 한도 3억
    public void withdrawalKRW(BankCode bankCode, String account, int quantitiy) throws Exception {
        final String endpoint = "trade/krw_withdrawal";
        Map<String, String> params = new HashMap<>();
        params.put("bank", bankCode.toString());
        params.put("account", account);
        params.put("price", String.valueOf(quantitiy));

        BithumbDAO dao = new BithumbDAO();
        JSONObject result = dao.callApi(endpoint, params);

        //System.out.println(result.toString());
        errorCheck(result, "Withdrawal KRW");
    }

    @Override
    public boolean isOrderExist(String orderId, Coin coin, OrderType orderType) throws Exception {
        JSONObject orderInfo = getOrderInfo(orderId, coin, orderType);
        String status = orderInfo.getString("status");

        if (STATUS_CODE_SUCCESS.equals(status)) {
            return true;
        }
        else if (STATUS_CODE_CUSTOM.equals(status)
                && ("거래 진행중인 내역이 존재하지 않습니다.".equals(orderInfo.getString("message"))) || ("거래 체결내역이 존재하지 않습니다.".equals(orderInfo.getString("message"))) ) {
            return false;
        }
        else {
            errorCheck(orderInfo, "isOrderExist"); // 무조건 예외 발생
            throw new Exception("This will never happen"); // 절대 일어나지 않음
        }
    }

    @Override
    public JSONObject getOrderInfo(String orderId, Coin coin, OrderType orderType) throws Exception {
        final String ENDPOINT_FINISHED = "info/order_detail";
        final String ENDPOINT_IN_PROGRESS = "info/orders";
        final String[] ENDPOINTS = { ENDPOINT_IN_PROGRESS, ENDPOINT_FINISHED };
        JSONObject result = null;

        for(String endpoint : ENDPOINTS) {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("order_id", orderId);
                params.put("type", orderType.toString());
                params.put("currency", coin.name().toUpperCase());

                BithumbDAO dao = new BithumbDAO();
                result = dao.callApi(endpoint, params);
                errorCheck(result, "getOrderInfo");
                break;
            }
            catch (ErrorCodeException e) { }
            catch (Exception e2) {
                throw e2;
            }
        }
        return result;
    }

    public boolean isOrderCompleted(JSONObject result) throws Exception {
        if(result.getJSONArray("data").getJSONObject(0).isNull("total"))
            return false;
        else
            return true;
    }

    @Override
    public boolean isOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception {
        JSONObject result = getOrderInfo(orderId, coin, orderType);
        errorCheck(result, "isOrderCompleted");
        return isOrderCompleted(result);
    }

    @Override
    public void cancelOrder(String orderId, OrderType orderType, Coin coin, long krwPrice, double quantity) throws Exception {
        final String endpoint = "trade/cancel";
        Map<String, String> params = new HashMap<>();
        params.put("type", orderType.toString());
        params.put("order_id", orderId);
        params.put("currency", coin.name().toUpperCase());

        BithumbDAO dao = new BithumbDAO();
        JSONObject result = dao.callApi(endpoint, params);
        errorCheck(result, "Cancel order");
    }

    private void errorCheck(JSONObject result, String funcName) throws Exception {
        String status = result.getString("status");
        if (!status.equals(STATUS_CODE_SUCCESS)) {
            String customMsg = "";
            if(status.equals(STATUS_CODE_CUSTOM)) {
                Object message = result.get("message");
                if(message instanceof String)
                    customMsg = result.getString("message"); // {"message":"최소 구매수량은 0.1ETC 입니다.","status":"5600"}
                else if(message instanceof JSONObject)
                    customMsg = result.getJSONObject("message").getString("message");
            }
            throw new ErrorCodeException(status, funcName + " failed! (tradeStatus: " + (customMsg.equals("") ? statusDescription(status) : customMsg) + ")");
        }
    }

    private String statusDescription(String status) {
        String desc;

        switch (status) {
            case STATUS_CODE_SUCCESS:
                desc = "Success";
                break;
            default:
                desc = status;
                break;
        }

        return desc;
    }

    private class ErrorCodeException extends Exception {
        @Getter
        private String status;
        public ErrorCodeException(String status, String msg) {
            super(msg);
            this.status = status;
        }
    }
}