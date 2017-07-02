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
    public static final String COIN_DASH = "ETC";
    public static final String COIN_XRP = "XRP";
    public static final String COIN_KRW = "KRW";

    public enum PriceType { BUY, SELL }

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
        System.out.println(result.toString());
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

    public static void main(String[] args) {
        try {
            BithumbComm comm = new BithumbComm();
            CoinoneComm coinComm = new CoinoneComm();

            comm.sendCoin(COIN_BTC, 0.001f, "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59", null);
            /*
            long bithumbBTCprice = comm.getMarketPrice(BithumbComm.COIN_BTC, PriceType.BUY);
            long coinoneBTCprice = coinComm.getMarketPrice(CoinoneComm.COIN_BTC);
            System.out.println("bithumb BTC 시세 : " + bithumbBTCprice);
            System.out.println("coinone BTC 시세 : " + coinoneBTCprice);
            System.out.println("차이 : " + Math.abs(bithumbBTCprice-coinoneBTCprice));
            */
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}