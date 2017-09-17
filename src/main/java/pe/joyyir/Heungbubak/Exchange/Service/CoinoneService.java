package pe.joyyir.Heungbubak.Exchange.Service;

import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageExchange;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageMarketPrice;
import pe.joyyir.Heungbubak.Exchange.ApiKey.CoinoneApiKey;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;
import pe.joyyir.Heungbubak.Common.Util.Encryptor;
import pe.joyyir.Heungbubak.Common.Util.HTTPUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class CoinoneService implements ArbitrageExchange {
    private final String API_URL = "https://api.coinone.co.kr/";
    private final String TICKER_URL = "ticker?currency=";
    private final String BALANCE_URL = "v2/account/balance/";
    private final String TRANSACTION_URL = "v2/transaction/";

    public static final Coin[] COIN_ARRAY = { Coin.BTC, Coin.ETC, Coin.ETH };

    @Setter @Getter
    private CoinoneApiKey apikey;
    private String accessToken;
    private String secret;

    public CoinoneService() throws Exception {
        setApikey(new CoinoneApiKey());
        accessToken = getApikey().getAccessToken();
        secret = getApikey().getSecret();
    }

    public int getLastMarketPrice(Coin coin) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+coin.name().toLowerCase());
        return Integer.valueOf(jsonObject.getString("last"));
    }

    @Override
    public double getAvailableBuyQuantity(Coin coin, long krwBalance) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL + "orderbook/?currency=" + coin.name().toLowerCase());
        JSONArray orders = jsonObject.getJSONArray(PriceType.SELL.toString());
        double sum = 0.0;
        double left = krwBalance;
        for(int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.getJSONObject(i);
            long price = order.getLong("price");
            double qty = order.getDouble("qty");

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
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL + "orderbook/?currency=" + coin.name().toLowerCase());
        JSONArray orders = jsonObject.getJSONArray(priceType.toString());
        double sum = 0.0;
        double left = quantity;
        for(int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.getJSONObject(i);
            long price = order.getLong("price");
            double qty = order.getDouble("qty");
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
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL + "orderbook/?currency=" + coin.name().toLowerCase());
        return jsonObject.getJSONArray(priceType.toString()).getJSONObject(0).getLong("price");
    }

    @Override
    public double getBalance(Coin coin) throws Exception {
        long nonce = CmnUtil.nsTime();
        String url = API_URL + BALANCE_URL;

        JSONObject params = new JSONObject();
        params.put("nonce", nonce);
        params.put("access_token", accessToken);

        JSONObject result = callPrivateApi(url, params);
        String strBalance = result.getJSONObject(coin.name().toLowerCase()).getString("avail");

        return Double.valueOf(strBalance);
    }

    private JSONObject callPrivateApi(String url, JSONObject params) throws Exception {
        String payload = Base64.encodeBase64String(params.toString().getBytes());
        String signature = Encryptor.getHmacSha512(secret.toUpperCase(), payload).toLowerCase();

        Map<String, String> map = new HashMap<>();
        map.put("content-type", "application/json");
        map.put("accept", "application/json");
        map.put("X-COINONE-PAYLOAD", payload);
        map.put("X-COINONE-SIGNATURE", signature);

        return HTTPUtil.getJSONfromPost(url, map, payload);
    }

    public long getCompleteBalance() throws Exception {
        return (long) (getLastMarketPrice(Coin.BTC) * getBalance(Coin.BTC))
                + (long) (getLastMarketPrice(Coin.ETH) * getBalance(Coin.ETH))
                + (long) (getLastMarketPrice(Coin.ETC) * getBalance(Coin.ETC))
                + (long) (getLastMarketPrice(Coin.XRP) * getBalance(Coin.XRP))
                + (long) getBalance(Coin.KRW);
    }

    public void twoFactorAuth(String type) throws Exception {
        long nonce = CmnUtil.nsTime();
        String url = API_URL + TRANSACTION_URL + "auth_number/";

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("type", type.toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        errorCheck(result);
    }

    public void sendBTC(String toAddress, double quantity, int authNumber, String walletType, String fromAddress) throws Exception {
        long nonce = CmnUtil.nsTime();
        String url = API_URL + TRANSACTION_URL + "btc/";

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("address", toAddress);
        params.put("auth_number", authNumber); // 2-Factor Authentication number. (int)
        params.put("qty", String.format("%.4f", Math.floor(quantity*10000)/10000));
        params.put("type", walletType); // Type of wallet. 'trade' or 'normal'
        params.put("from_address", fromAddress);
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        errorCheck(result);
    }

    public String errorDescription(String errorCode) {
        String desc;

        switch (errorCode) {
            case "40":
                desc = "Invalid API permission";
                break;
            case "777":
                desc = "Mobile auth error";
                break;
            case "101":
                desc = "Invalid format";
                break;
            case "103":
                desc = "Lack of Balance";
                break;
            case "104":
                desc = "Order id is not exist";
                break;
            case "107":
                desc = "Parameter error";
                break;
            case "141":
                desc = "Too many limit orders";
                break;
            default:
                desc = errorCode;
                break;
        }

        return desc;
    }

    @Override
    public String makeOrder(OrderType orderType, Coin coin, long price, double quantity) throws Exception {
        long nonce = CmnUtil.nsTime();
        String url = "";
        if(orderType == OrderType.BUY)
            url = "v2/order/limit_buy/";
        else // orderType == OrderType.SELL
            url = "v2/order/limit_sell/";

        url = API_URL + url;

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("price", price);
        params.put("qty", String.format("%.4f", Math.floor(quantity*10000)/10000));
        params.put("currency", coin.name().toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);

        //System.out.println(result.toString());

        errorCheck(result);
        return result.getString("orderId");
    }

    @Override
    public boolean isOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception {
        JSONObject result = getOrderInfo(orderId, coin);
        String status = result.getString("status"); // live, filled, partially_filled
        return status.equals("filled");
    }

    @Override
    public void cancelOrder(String orderId, OrderType orderType, Coin coin, long krwPrice, double quantity) throws Exception {
        long nonce = CmnUtil.nsTime();
        String url = API_URL + "v2/order/cancel/";

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("order_id", orderId);
        params.put("price", krwPrice);
        params.put("qty", String.format("%.4f", Math.floor(quantity*10000)/10000));
        params.put("is_ask", (orderType == OrderType.SELL) ? 1 : 0);
        params.put("currency", coin.name().toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        errorCheck(result);
    }

    @Override
    public boolean isOrderExist(String orderId, Coin coin, OrderType orderType) throws Exception {
        try {
            getOrderInfo(orderId, coin);
            return true;
        }
        catch (ErrorCodeException e) {
            if ("104".equals(e.getErrorCode())) {
                return false;
            }
            throw e;
        }
        catch (Exception e) {
            throw e;
        }
    }

    @Override
    public JSONObject getOrderInfo(String orderId, Coin coin, OrderType orderType) throws Exception {
        return getOrderInfo(orderId, coin);
    }

    public JSONObject getOrderInfo(String orderId, Coin coin) throws Exception {
        long nonce = CmnUtil.nsTime();
        String url = API_URL + "v2/order/order_info/";

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("order_id", orderId);
        params.put("currency", coin.name().toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        errorCheck(result);
        return result;
    }

    private void errorCheck(JSONObject result) throws Exception {
        if(!"success".equals(result.getString("result"))) {
            String errorCode = result.getString("errorCode");
            throw new ErrorCodeException(errorCode, errorDescription(errorCode));
        }
    }

    private class ErrorCodeException extends Exception {
        @Getter
        private String errorCode;
        public ErrorCodeException(String errorCode, String msg) {
            super(msg);
            this.errorCode = errorCode;
        }
    }
}
