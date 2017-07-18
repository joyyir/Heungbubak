package pe.joyyir.Heungbubak.Comm;

import pe.joyyir.Heungbubak.Comm.apikey.CoinoneApiKey;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.OrderType;
import pe.joyyir.Heungbubak.Const.PriceType;
import pe.joyyir.Heungbubak.Util.CmnUtil;
import pe.joyyir.Heungbubak.Util.Encryptor;
import pe.joyyir.Heungbubak.Util.HTTPUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class CoinoneComm implements ArbitrageExchange {
    private final String API_URL = "https://api.coinone.co.kr/";
    private final String TICKER_URL = "ticker?currency=";
    private final String BALANCE_URL = "v2/account/balance/";
    private final String TRANSACTION_URL = "v2/transaction/";

    public static final Coin[] COIN_ARRAY = { Coin.BTC, Coin.ETC, Coin.ETH };

    @Setter @Getter
    private CoinoneApiKey apikey;
    private String accessToken;
    private String secret;

    public CoinoneComm() throws Exception {
        setApikey(new CoinoneApiKey());
        accessToken = getApikey().getAccessToken();
        secret = getApikey().getSecret();
    }

    public int getLastMarketPrice(Coin coin) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+coin.name().toLowerCase());
        return Integer.valueOf(jsonObject.getString("last"));
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
        params.put("qty", quantity);
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
            case "103":
                desc = "Lack of Balance";
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
        else if(orderType == OrderType.SELL)
            url = "v2/order/limit_sell/";
        else
            throw new Exception("Undefined OrderType");

        url = API_URL + url;

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("price", price);
        params.put("qty", quantity);
        params.put("currency", coin.name().toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        errorCheck(result);
        return result.getString("orderId");
    }

    public boolean isOrderComplete(String orderId, Coin coin) throws Exception {
        JSONObject result = getOrderInfo(orderId, coin);
        String status = result.getString("status"); // live, filled, partially_filled
        return status.equals("filled");
    }

    public void cancelOrder(String orderId, int krwPrice, double quantity, boolean isSell, Coin coin) throws Exception {
        long nonce = CmnUtil.nsTime();
        String url = API_URL + "v2/order/cancel/";

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("order_id", orderId);
        params.put("price", krwPrice);
        params.put("qty", quantity);
        int isAsk = isSell ? 1 : 0;
        params.put("is_ask", isAsk);
        params.put("currency", coin.name().toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        errorCheck(result);
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
        if(!"success".equals(result.getString("result")))
            throw new Exception(errorDescription(result.getString("errorCode")));
    }

    public static void main(String[] args) {
        try {
            CoinoneComm comm = new CoinoneComm();
            /*
            int authNumber;
            comm.twoFactorAuth(CoinoneComm.Coin.BTC);
            System.out.print("Coinone OTP ��ȣ�� �Է��ϼ��� : ");
            Scanner sc = new Scanner(System.in);
            authNumber = sc.nextInt();
            comm.sendBTC("1AKnnChADG5svVrNbAGnF4xdNdZ515J4oM", 0.001, authNumber, "trade", "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59");
            */
            /*
            String orderId = comm.makeOrder(OrderType.SELL, Coin.ETC, 1000000, 0.05);
            System.out.println(comm.isOrderComplete(orderId, Coin.ETC));
            comm.cancelOrder(orderId, 1000000, 0.05, true, Coin.ETC);
            */

            //comm.getMarketPrice(Coin.BTC, PriceType.BUY);

            /*
            System.out.println("1�� �춧: " + comm.getAverageMarketPrice(Coin.BTC, PriceType.BUY, 1.0));
            System.out.println("10�� �춧: " + comm.getAverageMarketPrice(Coin.BTC, PriceType.BUY, 10.0));
            System.out.println("10000�� �춧: " + comm.getAverageMarketPrice(Coin.BTC, PriceType.BUY, 10000.0));
            System.out.println("1�� �ȶ�: " + comm.getAverageMarketPrice(Coin.BTC, PriceType.SELL, 1.0));
            System.out.println("10�� �ȶ�: " + comm.getAverageMarketPrice(Coin.BTC, PriceType.SELL, 10.0));
            System.out.println("10000�� �ȶ�: " + comm.getAverageMarketPrice(Coin.BTC, PriceType.SELL, 10000.0));
            */
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
