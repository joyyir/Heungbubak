package Comm;

import Comm.apikey.CoinoneApiKey;
import Util.Encryptor;
import Util.HTTPUtil;
import Util.IOUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class CoinoneComm {
    private final String API_URL = "https://api.coinone.co.kr/";
    private final String TICKER_URL = "ticker?currency=";
    private final String BALANCE_URL = "v2/account/balance/";

    public static final String COIN_BTC = "btc";
    public static final String COIN_ETH = "eth";
    public static final String COIN_ETC = "etc";
    public static final String COIN_KRW = "krw";

    @Setter @Getter
    private CoinoneApiKey apikey;

    public CoinoneComm() throws Exception {
        setApikey(new CoinoneApiKey());
    }

    public int getMarketPrice(String coin) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+coin);
        return Integer.valueOf(jsonObject.getString("last"));
    }

    public double getBalance(String coin) throws Exception {
        String accessToken = getApikey().getAccessToken();
        String secret = getApikey().getSecret();
        int nonce = getApikey().getIncreasedNonce();

        String url = API_URL + BALANCE_URL;

        JSONObject params = new JSONObject();
        params.put("nonce", nonce);
        params.put("access_token", accessToken);

        String payload = Base64.encodeBase64String(params.toString().getBytes());
        String signature = Encryptor.getHmacSha512(secret.toUpperCase(), payload).toLowerCase();

        Map<String, String> map = new HashMap<>();
        map.put("content-type", "application/json");
        map.put("accept", "application/json");
        map.put("X-COINONE-PAYLOAD", payload);
        map.put("X-COINONE-SIGNATURE", signature);

        JSONObject result = HTTPUtil.getJSONfromPost(url, map, payload);
        String strBalance = result.getJSONObject(coin).getString("avail");

        return Double.valueOf(strBalance);
    }

    public long getCompleteBalance() throws Exception {
        return (long) (getMarketPrice(COIN_BTC) * getBalance(COIN_BTC))
                + (long) (getMarketPrice(COIN_ETH) * getBalance(COIN_ETH))
                + (long) (getMarketPrice(COIN_ETC) * getBalance(COIN_ETC))
                + (long) getBalance(COIN_KRW);
    }

    public static void main(String[] args) {
        try {
            CoinoneComm comm = new CoinoneComm();
            int btcPrice = comm.getMarketPrice(CoinoneComm.COIN_BTC);
            int ethPrice = comm.getMarketPrice(CoinoneComm.COIN_ETH);
            int etcPrice = comm.getMarketPrice(CoinoneComm.COIN_ETC);

            double ethBalance = comm.getBalance(CoinoneComm.COIN_ETH);
            long totalBal = comm.getCompleteBalance();
            System.out.println(totalBal);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
