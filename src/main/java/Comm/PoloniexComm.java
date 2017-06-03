package Comm;

import Comm.apikey.PoloniexApiKey;
import Util.Encryptor;
import Util.HTTPUtil;
import Util.IOUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PoloniexComm {
    private final String API_URL = "https://poloniex.com/";
    private final String TICKER_URL = "public?command=returnTicker";
    private final String TRADING_URL = "tradingApi";

    public static final String COIN_BTC = "BTC";
    public static final String COIN_ETH = "ETH";
    public static final String COIN_ETC = "ETC";
    public static final String COIN_XRP = "XRP";
    public static final String COIN_USDT = "USDT";
    public static final String COIN_STR = "STR";

    @Getter @Setter
    private PoloniexApiKey apikey;

    public PoloniexComm() throws Exception {
        setApikey(new PoloniexApiKey());
    }

    public double getMarketPrice(String unitCoin, String coin) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL);
        String strPrice = jsonObject.getJSONObject(unitCoin + "_" + coin).getString("last");
        return Double.valueOf(strPrice);
    }

    public double getBalance(String coin) throws Exception {
        String key = getApikey().getKey();
        String secret = getApikey().getSecret();
        int nonce = getApikey().getIncreasedNonce();

        String params = "nonce=" + nonce + "&command=returnBalances";

        Map<String, String> map = new HashMap<>();
        map.put("Accept", "application/json");
        map.put("Key", key);
        map.put("Sign", Encryptor.getHmacSha512(secret, params));

        JSONObject json = HTTPUtil.getJSONfromPost(API_URL + TRADING_URL, map, params);

        return Double.valueOf((String)json.get(coin));
    }

    public double getCompleteBalance() throws Exception {
        String key = getApikey().getKey();
        String secret = getApikey().getSecret();
        int nonce = getApikey().getIncreasedNonce();

        Double completeBal = 0.0;

        try {
            String params = "nonce=" + nonce + "&command=returnCompleteBalances";

            Map<String, String> map = new HashMap<>();
            map.put("Accept", "application/json");
            //map.put("content-type", "application/x-www-form-urlencoded");
            map.put("Key", key);
            map.put("Sign", Encryptor.getHmacSha512(secret, params));

            JSONObject json = HTTPUtil.getJSONfromPost(API_URL + TRADING_URL, map, params);

            //System.out.println(json.get("USDT")); // USDT는 제외됨

            for (int i = 0; i < json.names().length(); i++) {
                String strCoin = json.names().getString(i);
                double btcValue = Double.valueOf(json.getJSONObject(strCoin).getString("btcValue"));
                if (btcValue > 0.0)
                    completeBal += btcValue;
            }
        }
        catch(Exception e) {
            throw e;
        }

        return completeBal;
    }

    public static void main(String[] args){
        try {
            PoloniexComm comm = new PoloniexComm();
            System.out.println("현재 USDT_BTC 시세: " + comm.getMarketPrice(COIN_USDT, COIN_BTC));
            System.out.println("내가 소유한 XRP: " + comm.getBalance(COIN_XRP));
            System.out.println("총 보유 가치 (BTC): " + comm.getCompleteBalance());
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
