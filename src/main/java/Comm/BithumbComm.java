package Comm;

import Comm.apikey.BithumbApiKey;
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

    public static final String COIN_BTC = "BTC";
    public static final String COIN_ETH = "ETH";
    public static final String COIN_ETC = "ETC";
    public static final String COIN_XRP = "XRP";
    public static final String COIN_KRW = "KRW";

    public static final String STATUS_CODE_SUCCESS = "0000";
    public static final String STATUS_CODE_CUSTOM = "5600";

    public enum PriceType { BUY, SELL }
    public enum OrderType { BUY, SELL }
    public enum BankCode {
        SHINHAN("088");

        String bankCode;
        BankCode(String bankCode) {
            this.bankCode = bankCode;
        }

        @Override
        public String toString() {
            return bankCode;
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

    public long getMarketPrice(String coin, PriceType priceType) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+coin);

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

    public double getBalance(String coin) throws Exception {
        double balance;
        String endpoint = "info/balance";
        Map<String, String> params = new HashMap<>();
        params.put("order_currency", coin);
        params.put("payment_currency", "KRW");
        params.put("endpoint", '/' + endpoint);
        JSONObject result = callApi(endpoint, params);

        try {
            balance = result.getJSONObject("data").getDouble("total_" + coin.toLowerCase());
        }
        catch (Exception e) {
            //throw new Exception(coin + "이 존재하지 않습니다.\n" + CmnUtil.getStackTraceString(e));
            balance = 0.0;
        }
        return balance;
    }

    public void sendCoin(String coin, float units, String address, Integer destination) throws Exception {
        String endpoint = "trade/btc_withdrawal";
        Map<String, String> params = new HashMap<>();
        params.put("units", String.valueOf(units));
        params.put("address", address);
        if(coin.equals(COIN_XRP))
            params.put("destination", destination.toString());
        params.put("currency", coin);

        JSONObject result = callApi(endpoint, params);

        errorCheck(result, "Sending coin");
    }

    public void makeOrder(OrderType orderType, String coin, Long price, Float quantity) throws Exception {
        final String endpoint = "trade/place";
        Map<String, String> params = new HashMap<>();
        params.put("order_currency", coin.toUpperCase());
        params.put("Payment_currency", "KRW");
        params.put("units", quantity.toString());
        params.put("price", price.toString());
        String type = "";
        if(orderType.equals(OrderType.BUY))
            type = "bid";
        else if(orderType.equals(OrderType.SELL))
            type = "ask";
        else
            new Exception("Undefined OrderType");
        params.put("type", type);

        JSONObject result = callApi(endpoint, params);

        //System.out.println(result.toString());
        errorCheck(result, "Making order");
        //String orderId = result.getString("order_id");
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

    private JSONObject callApi(String endpoint, Map<String, String> params) throws Exception {
        String key = getApikey().getKey();
        String secret = getApikey().getSecret();
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
            comm.sendCoin(COIN_BTC, 0.001f, "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59", null); // 수수료 포함 0.001 BTC
            */

            /*
            long bithumbBTCprice = comm.getMarketPrice(BithumbComm.COIN_BTC, PriceType.BUY);
            long coinoneBTCprice = coinComm.getMarketPrice(CoinoneComm.COIN_BTC);
            System.out.println("bithumb BTC 시세 : " + bithumbBTCprice);
            System.out.println("coinone BTC 시세 : " + coinoneBTCprice);
            System.out.println("차이 : " + Math.abs(bithumbBTCprice-coinoneBTCprice));
            */

            //comm.makeOrder(OrderType.BUY, COIN_ETC, 19570L, 0.1F);
            //comm.makeOrder(OrderType.SELL, COIN_ETC, 19450L, 0.0998F);

            comm.withdrawalKRW(BankCode.SHINHAN, "110325467846", 10000);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}