package pe.joyyir.Heungbubak.Comm;

import pe.joyyir.Heungbubak.Comm.apikey.PoloniexApiKey;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Util.CmnUtil;
import pe.joyyir.Heungbubak.Util.HTTPUtil;
import pe.joyyir.Heungbubak.Util.Encryptor;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PoloniexComm {
    private final String API_URL = "https://poloniex.com/";
    private final String TICKER_URL = "public?command=returnTicker";
    private final String TRADING_URL = "tradingApi";

    @Getter @Setter
    private PoloniexApiKey apikey;
    private String key;
    private String secret;

    public PoloniexComm() throws Exception {
        setApikey(new PoloniexApiKey());
        key = getApikey().getKey();
        secret = getApikey().getSecret();
    }

    public double getMarketPrice(Coin unitCoin, Coin coin) throws Exception {
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL);
        String strPrice = jsonObject.getJSONObject(unitCoin.name() + "_" + coin.name()).getString("last");
        return Double.valueOf(strPrice);
    }

    public double getBalance(Coin coin) throws Exception {
        long nonce = CmnUtil.nsTime();
        String params = "nonce=" + nonce + "&command=returnBalances";

        Map<String, String> map = new HashMap<>();
        map.put("Accept", "application/json");
        map.put("Key", key);
        map.put("Sign", Encryptor.getHmacSha512(secret, params));

        JSONObject json = HTTPUtil.getJSONfromPost(API_URL + TRADING_URL, map, params);

        return Double.valueOf((String)json.get(coin.name()));
    }

    public double getCompleteBalance() throws Exception {
        long nonce = CmnUtil.nsTime();
        Double completeBal = 0.0;

        try {
            String params = "nonce=" + nonce + "&command=returnCompleteBalances";

            Map<String, String> map = new HashMap<>();
            map.put("Accept", "application/json");
            //map.put("content-type", "application/x-www-form-urlencoded");
            map.put("Key", key);
            map.put("Sign", Encryptor.getHmacSha512(secret, params));

            JSONObject json = HTTPUtil.getJSONfromPost(API_URL + TRADING_URL, map, params);

            //System.out.println(json.get("USDT")); // USDT�� ���ܵ�

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
            System.out.println("���� USDT_BTC �ü�: " + comm.getMarketPrice(Coin.USDT, Coin.BTC));
            System.out.println("���� ������ XRP: " + comm.getBalance(Coin.XRP));
            System.out.println("�� ���� ��ġ (BTC): " + comm.getCompleteBalance());
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
