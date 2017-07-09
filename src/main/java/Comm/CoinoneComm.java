package Comm;

import Comm.apikey.CoinoneApiKey;
import Util.Encryptor;
import Util.HTTPUtil;
import Util.IOUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import java.util.*;

public class CoinoneComm {
    private final String API_URL = "https://api.coinone.co.kr/";
    private final String TICKER_URL = "ticker?currency=";
    private final String BALANCE_URL = "v2/account/balance/";
    private final String TRANSACTION_URL = "v2/transaction/";

    public static final String COIN_BTC = "btc";
    public static final String COIN_ETH = "eth";
    public static final String COIN_ETC = "etc";
    public static final String COIN_KRW = "krw";
    public static final String[] COIN_ARRAY = { COIN_BTC, COIN_ETC, COIN_ETH };

    public enum OrderType { BUY, SELL }

    @Setter @Getter
    private CoinoneApiKey apikey;

    private String accessToken;
    private String secret;

    public CoinoneComm() throws Exception {
        setApikey(new CoinoneApiKey());
        accessToken = getApikey().getAccessToken();
        secret = getApikey().getSecret();
    }

    public int getMarketPrice(String coin) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+coin);
        return Integer.valueOf(jsonObject.getString("last"));
    }

    public double getBalance(String coin) throws Exception {
        long nonce = getApikey().getIncreasedNonce();

        String url = API_URL + BALANCE_URL;

        JSONObject params = new JSONObject();
        params.put("nonce", nonce);
        params.put("access_token", accessToken);

        JSONObject result = callPrivateApi(url, params);
        String strBalance = result.getJSONObject(coin).getString("avail");

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
        return (long) (getMarketPrice(COIN_BTC) * getBalance(COIN_BTC))
                + (long) (getMarketPrice(COIN_ETH) * getBalance(COIN_ETH))
                + (long) (getMarketPrice(COIN_ETC) * getBalance(COIN_ETC))
                + (long) getBalance(COIN_KRW);
    }

    public void twoFactorAuth(String type) throws Exception {
        String url = API_URL + TRANSACTION_URL + "auth_number/";
        long nonce = getApikey().getIncreasedNonce();

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("type", type.toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        if(!"success".equals(result.getString("result")))
            throw new Exception(errorDescription(result.getString("errorCode")));
        else
            System.out.println("2-Factor 인증 성공! (type:" + type + ")");
    }

    public void sendBTC(String toAddress, double quantity, int authNumber, String walletType, String fromAddress) throws Exception {
        long nonce = getApikey().getIncreasedNonce();
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
        if(!"success".equals(result.getString("result")))
            throw new Exception(errorDescription(result.getString("errorCode")));
        else
            System.out.println("송금 성공!");
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
                desc = "unknown error";
                break;
        }

        return desc;
    }

    public void makeOrder(OrderType orderType, String coin, long price, double quantity) throws Exception {
        long nonce = getApikey().getIncreasedNonce();
        String url = "";
        if(orderType == OrderType.BUY)
            url = "v2/order/limit_buy/";
        else if(orderType == OrderType.SELL)
            url = "v2/order/limit_sell/";
        else
            new Exception("Undefined OrderType");

        url = API_URL + url;

        JSONObject params = new JSONObject();
        params.put("access_token", accessToken);
        params.put("price", price);
        params.put("qty", quantity);
        params.put("currency", coin.toLowerCase());
        params.put("nonce", nonce);

        JSONObject result = callPrivateApi(url, params);
        if(!"success".equals(result.getString("result")))
            throw new Exception(errorDescription(result.getString("errorCode")));
        else
            System.out.println("Success!");
    }

    public static void main(String[] args) {
        try {
            CoinoneComm comm = new CoinoneComm();
            /*
            int authNumber;
            comm.twoFactorAuth(CoinoneComm.COIN_BTC);
            System.out.print("Coinone OTP 번호를 입력하세요 : ");
            Scanner sc = new Scanner(System.in);
            authNumber = sc.nextInt();
            comm.sendBTC("1AKnnChADG5svVrNbAGnF4xdNdZ515J4oM", 0.001, authNumber, "trade", "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59");
            */
            comm.makeOrder(OrderType.BUY, COIN_ETC, 19790, 0.05);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
