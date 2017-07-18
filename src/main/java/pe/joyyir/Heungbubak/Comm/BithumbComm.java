package pe.joyyir.Heungbubak.Comm;

import org.json.JSONArray;
import pe.joyyir.Heungbubak.Comm.apikey.BithumbApiKey;
import pe.joyyir.Heungbubak.Const.BankCode;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.OrderType;
import pe.joyyir.Heungbubak.Const.PriceType;
import pe.joyyir.Heungbubak.Util.CmnUtil;
import pe.joyyir.Heungbubak.Util.Encryptor;
import pe.joyyir.Heungbubak.Util.HTTPUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BithumbComm implements ArbitrageExchange {
    private final String API_URL = "https://api.bithumb.com/";
    private final String TICKER_URL = "public/ticker/";

    public static final String STATUS_CODE_SUCCESS = "0000";
    public static final String STATUS_CODE_CUSTOM = "5600";

    @Getter @Setter
    private BithumbApiKey apikey;
    private String key;
    private String secret;

    public BithumbComm() throws Exception {
        setApikey(new BithumbApiKey());
        key = getApikey().getKey();
        secret = getApikey().getSecret();
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

        return Long.valueOf(jsonObject.getJSONObject("data").getString(key));
    }

    @Override
    public double getBalance(Coin coin) throws Exception {
        double balance;
        String endpoint = "info/balance";
        Map<String, String> params = new HashMap<>();
        params.put("order_currency", coin.name().toUpperCase());
        params.put("payment_currency", "KRW");
        params.put("endpoint", '/' + endpoint);
        JSONObject result = callApi(endpoint, params);

        try {
            balance = result.getJSONObject("data").getDouble("total_" + coin.name().toLowerCase());
        }
        catch (Exception e) {
            //throw new Exception(coin.name() + "이 존재하지 않습니다.\n" + CmnUtil.getStackTraceString(e));
            balance = 0.0;
        }
        return balance;
    }

    public void sendCoin(Coin coin, float units, String address, Integer destination) throws Exception {
        String endpoint = "trade/btc_withdrawal";
        Map<String, String> params = new HashMap<>();
        params.put("units", String.valueOf(units));
        params.put("address", address);
        if(coin == Coin.XRP)
            params.put("destination", destination.toString());
        params.put("currency", coin.name().toUpperCase());

        JSONObject result = callApi(endpoint, params);

        errorCheck(result, "Sending coin");
    }

    @Override
    public String makeOrder(OrderType orderType, Coin coin, long price, double quantity) throws Exception {
        final String endpoint = "trade/place";
        Map<String, String> params = new HashMap<>();
        params.put("order_currency", coin.name().toUpperCase());
        params.put("Payment_currency", "KRW");
        params.put("units", String.valueOf(quantity));
        params.put("price", String.valueOf(price));
        params.put("type", orderType.toString());

        JSONObject result = callApi(endpoint, params);

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

        JSONObject result = callApi(endpoint, params);

        //System.out.println(result.toString());
        errorCheck(result, "Withdrawal KRW");
    }

    public boolean isOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception {
        try {
            JSONObject result = getOrderInfo(orderId, orderType, coin, true);
            return (result != null);
        }
        catch (Exception e) {
            try {
                JSONObject result = getOrderInfo(orderId, orderType, coin, false);
                return (result == null);
            }
            catch (Exception e2) {
                throw e2;
            }
        }
    }

    public JSONObject getOrderInfo(String orderId, OrderType orderType, Coin coin, boolean finished) throws Exception {
        final String endpoint = finished ? "info/order_detail" : "info/orders";

        Map<String, String> params = new HashMap<>();
        params.put("order_id", orderId);
        params.put("type", orderType.toString());
        params.put("currency", coin.name().toUpperCase());

        JSONObject result = callApi(endpoint, params);
        errorCheck(result, "getOrderInfo");
        return result;
    }

    public void cancelOrder(String orderId, OrderType orderType, Coin coin) throws Exception {
        final String endpoint = "trade/cancel";
        Map<String, String> params = new HashMap<>();
        params.put("type", orderType.toString());
        params.put("order_id", orderId);
        params.put("currency", coin.name().toUpperCase());

        JSONObject result = callApi(endpoint, params);
        errorCheck(result, "Cancel order");
    }

    private JSONObject callApi(String endpoint, Map<String, String> params) throws Exception {
        long nonce = CmnUtil.msTime();

        String strParams = HTTPUtil.paramsBuilder(params);
        String encodedParams = HTTPUtil.encodeURIComponent(strParams);

        String str = "/" + endpoint + ";" + encodedParams + ";" + nonce;

        Map<String, String> map = new HashMap<>();
        map.put("Api-Key", key);
        map.put("Api-Sign", Encryptor.getHmacSha512(secret, str, Encryptor.EncodeType.BASE64));
        map.put("Api-Nonce", String.valueOf(nonce));
        map.put("api-client-type", "2");

        return HTTPUtil.getJSONfromPost(API_URL + endpoint, map, strParams);
    }

    private void errorCheck(JSONObject result, String funcName) throws Exception {
        String status = result.getString("status");
        if (!status.equals(STATUS_CODE_SUCCESS)) {
            String customMsg = "";
            if(status.equals(STATUS_CODE_CUSTOM))
                customMsg = result.getString("message");
            throw new Exception(funcName + " failed! (status:" + (customMsg.equals("") ? statusDescription(status) : customMsg) + ")");
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

    public static void main(String[] args) {
        try {
            BithumbComm comm = new BithumbComm();
            CoinoneComm coinComm = new CoinoneComm();

            /*
            comm.sendCoin(Coin.BTC, 0.001f, "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59", null); // 수수료 포함 0.001 BTC
            */

            /*
            long bithumbBTCprice = comm.getMarketPrice(BithumbComm.Coin.BTC, PriceType.BUY);
            long coinoneBTCprice = coinComm.getMarketPrice(CoinoneComm.Coin.BTC);
            System.out.println("bithumb BTC 시세 : " + bithumbBTCprice);
            System.out.println("coinone BTC 시세 : " + coinoneBTCprice);
            System.out.println("차이 : " + Math.abs(bithumbBTCprice-coinoneBTCprice));
            */

            //comm.makeOrder(OrderType.BUY, Coin.ETC, 19570L, 0.1F);
            //comm.makeOrder(OrderType.SELL, Coin.ETC, 19450L, 0.0998F);

            //comm.withdrawalKRW(BankCode.SHINHAN, "110325467846", 10000);

            double krw = comm.getBalance(Coin.BTC);
            String orderId = comm.makeOrder(OrderType.BUY, Coin.ETC, 10000, 1);
            System.out.println(comm.isOrderCompleted(orderId, OrderType.BUY, Coin.ETC));
            comm.getOrderInfo(orderId, OrderType.BUY, Coin.ETC, false);
            comm.cancelOrder(orderId, OrderType.BUY, Coin.ETC);

            //comm.getOrderInfo("1499599864512", OrderType.SELL, Coin.ETC, true);
            //System.out.println(comm.isOrderCompleted("1499599864512", OrderType.SELL, Coin.ETC));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}