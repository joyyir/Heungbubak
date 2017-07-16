package Comm;

import Comm.apikey.BithumbApiKey;
import Const.BankCode;
import Const.Coin;
import Util.CmnUtil;
import Util.Encryptor;
import Util.HTTPUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BithumbComm {
    private final String API_URL = "https://api.bithumb.com/";
    private final String TICKER_URL = "public/ticker/";

    public static final String STATUS_CODE_SUCCESS = "0000";
    public static final String STATUS_CODE_CUSTOM = "5600";

    public enum PriceType { BUY, SELL }
    public enum OrderType {
        BUY("bid"), SELL("ask");

        private String type;
        OrderType(String type) { this.type = type; }

        @Override
        public String toString() {
            return type;
        }
    }

    @Getter @Setter
    private BithumbApiKey apikey;
    private String key;
    private String secret;

    public BithumbComm() throws Exception {
        setApikey(new BithumbApiKey());
        key = getApikey().getKey();
        secret = getApikey().getSecret();
    }

    public long getMarketPrice(Coin coin, PriceType priceType) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+coin.name());

        String key = "";
        switch (priceType) {
            case BUY : // �ŷ� ���� �ְ� ���Ű�
                key = "buy_price";
                break;
            case SELL: // �ŷ� ���� �ּ� �ǸŰ�
                key = "sell_price";
                break;
        }

        return Long.valueOf(jsonObject.getJSONObject("data").getString(key));
    }

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
            //throw new Exception(coin.name() + "�� �������� �ʽ��ϴ�.\n" + CmnUtil.getStackTraceString(e));
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

    public String makeOrder(OrderType orderType, Coin coin, Long price, Float quantity) throws Exception {
        final String endpoint = "trade/place";
        Map<String, String> params = new HashMap<>();
        params.put("order_currency", coin.name().toUpperCase());
        params.put("Payment_currency", "KRW");
        params.put("units", quantity.toString());
        params.put("price", price.toString());
        params.put("type", orderType.toString());

        JSONObject result = callApi(endpoint, params);

        //System.out.println(result.toString());
        errorCheck(result, "Making order");
        return result.getString("order_id");
    }

    // �ٷ� �Ǵ°� �ƴϴ�.
    // ������ �Ǵ� 1,000��, ��� �ּ� �ݾ� 5,000��, ���� ��� �ѵ� 5000����, �� ��� �ѵ� 3��
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
        long nonce = CmnUtil.nsTime();

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
            comm.sendCoin(Coin.BTC, 0.001f, "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59", null); // ������ ���� 0.001 BTC
            */

            /*
            long bithumbBTCprice = comm.getMarketPrice(BithumbComm.Coin.BTC, PriceType.BUY);
            long coinoneBTCprice = coinComm.getMarketPrice(CoinoneComm.Coin.BTC);
            System.out.println("bithumb BTC �ü� : " + bithumbBTCprice);
            System.out.println("coinone BTC �ü� : " + coinoneBTCprice);
            System.out.println("���� : " + Math.abs(bithumbBTCprice-coinoneBTCprice));
            */

            //comm.makeOrder(OrderType.BUY, Coin.ETC, 19570L, 0.1F);
            //comm.makeOrder(OrderType.SELL, Coin.ETC, 19450L, 0.0998F);

            //comm.withdrawalKRW(BankCode.SHINHAN, "110325467846", 10000);

            //String orderId = comm.makeOrder(OrderType.BUY, Coin.ETC, 10000L, 1F);
            //System.out.println(comm.isOrderCompleted(orderId, OrderType.BUY, Coin.ETC));
            //comm.getOrderInfo(orderId, OrderType.BUY, Coin.ETC, false);
            //comm.cancelOrder(orderId, OrderType.BUY, Coin.ETC);

            //comm.getOrderInfo("1499599864512", OrderType.SELL, Coin.ETC, true);
            System.out.println(comm.isOrderCompleted("1499599864512", OrderType.SELL, Coin.ETC));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}