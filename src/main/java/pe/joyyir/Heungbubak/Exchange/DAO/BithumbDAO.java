package pe.joyyir.Heungbubak.Exchange.DAO;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;
import pe.joyyir.Heungbubak.Common.Util.Encryptor;
import pe.joyyir.Heungbubak.Common.Util.HTTPUtil;
import pe.joyyir.Heungbubak.Exchange.ApiKey.BithumbApiKey;
import pe.joyyir.Heungbubak.Exchange.Domain.BalanceVO;
import pe.joyyir.Heungbubak.Exchange.Domain.BalanceVO_V2;
import pe.joyyir.Heungbubak.Exchange.Domain.BasicPriceVO;
import pe.joyyir.Heungbubak.Exchange.Domain.CoinPriceVO;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BithumbDAO {
    private final String API_URL = "https://api.bithumb.com/";
    private final String TICKER_URL = "public/ticker/";

    @Getter
    @Setter
    private BithumbApiKey apikey;
    private String key;
    private String secret;

    public BithumbDAO() throws Exception {
        setApikey(new BithumbApiKey());
        key = getApikey().getKey();
        secret = getApikey().getSecret();
    }

    public JSONObject callApi(String endpoint, Map<String, String> params) throws Exception {
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

    @Deprecated
    public BalanceVO getBalanceVO() throws Exception {
        String endpoint = "info/balance";
        Map<String, String> params = new HashMap<>();
        //params.put("order_currency", coin.name().toUpperCase());
        params.put("currency", "ALL");
        params.put("payment_currency", "KRW");
        params.put("endpoint", '/' + endpoint);
        JSONObject result = callApi(endpoint, params);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getJSONObject("data").toString(), BalanceVO.class);
    }

    public BalanceVO_V2 getBalanceVO_V2() throws Exception {
        final String endpoint = "info/balance";
        final String TOTAL_PREFIX = "total_";
        final String AVAIL_PREFIX = "available_";
        Map<String, String> params = new HashMap<>();
        //params.put("order_currency", coin.name().toUpperCase());
        params.put("currency", "ALL");
        params.put("payment_currency", "KRW");
        params.put("endpoint", '/' + endpoint);
        JSONObject result = callApi(endpoint, params);

        BalanceVO_V2 vo = new BalanceVO_V2();
        JSONObject data = result.getJSONObject("data");
        Iterator<?> iterator = data.keys();
        while(iterator.hasNext()) {
            String key = (String) iterator.next();
            if(key.startsWith(AVAIL_PREFIX)) {
                int startIdx = AVAIL_PREFIX.length();
                int endIdx = key.length();
                String strCoin = key.substring(startIdx, endIdx).toUpperCase();
                Coin coin;
                try {
                    coin = Coin.valueOf(strCoin);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                double balance = data.getDouble(key);
                vo.getAvailable().put(coin, balance);
            }
            else if(key.startsWith(TOTAL_PREFIX)) {
                int startIdx = TOTAL_PREFIX.length();
                int endIdx = key.length();
                String strCoin = key.substring(startIdx, endIdx).toUpperCase();
                Coin coin;
                try {
                    coin = Coin.valueOf(strCoin);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                double balance = data.getDouble(key);
                vo.getTotal().put(coin, balance);
            }
        }
        return vo;
    }

    public CoinPriceVO getCoinPriceVO() throws Exception {
        CoinPriceVO vo = new CoinPriceVO();
        JSONObject jsonObject = HTTPUtil.getJSONfromGet(API_URL+TICKER_URL+"ALL");
        JSONObject data = jsonObject.getJSONObject("data");
        Iterator<?> keys = data.keys();

        while(keys.hasNext()) {
            String key = (String) keys.next();
            try {
                Coin coin;
                try {
                    coin = Coin.valueOf(key);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                ObjectMapper mapper = new ObjectMapper();
                BasicPriceVO basicVO = mapper.readValue(data.getJSONObject(key).toString(), BasicPriceVO.class);
                basicVO.setUnit(Coin.KRW);
                vo.getPrice().put(coin, basicVO);
            }
            catch (IllegalArgumentException e) {
                continue;
            }
        }

        return vo;
    }

    public static void main(String[] args) {
        try {
            BithumbDAO dao = new BithumbDAO();
            dao.getBalanceVO_V2();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
