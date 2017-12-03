package pe.joyyir.Heungbubak.Exchange.DAO;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;
import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.Encryptor;
import pe.joyyir.Heungbubak.Common.Util.HTTPUtil;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;
import pe.joyyir.Heungbubak.Exchange.ApiKey.BittrexApiKey;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexOrderVO;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexTickerVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BittrexDAO {
    @Setter
    @Getter
    private BittrexApiKey apikey;
    private String key;
    private String secret;

    public BittrexDAO() throws Exception {
        setApikey(new BittrexApiKey());
        this.key = getApikey().getKey();
        this.secret = getApikey().getSecret();
    }

    public List<BittrexTickerVO> getTicker(String baseCurrency) throws Exception {
        List<BittrexTickerVO> list = new ArrayList<>();

        JSONObject jsonObject = null;
        try {
            jsonObject = HTTPUtil.getJSONfromGet("https://bittrex.com/api/v1.1/public/getmarketsummaries");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (jsonObject != null && jsonObject.optJSONArray("result") != null) {
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                String marketName = obj.optString("MarketName");
                String[] currencyArr = marketName.split("-");
                if (currencyArr != null && currencyArr.length == 2 &&
                        (baseCurrency == null || baseCurrency.equals(currencyArr[0])) ) {
                    BittrexTickerVO vo = new BittrexTickerVO();
                    vo.setBaseCurrency(currencyArr[0]);
                    vo.setMarketCurrency(currencyArr[1]);
                    vo.setBaseVolume(obj.optDouble("BaseVolume"));

                    list.add(vo);
                }
            }
        }

        return list;
    }

    public List<BittrexOrderVO> getOpenOrders() throws Exception {
        List<BittrexOrderVO> openOrderList = new ArrayList<>();
        final String filename = "bittrexOpenOrder.json";
        JSONObject response = IOUtil.readJson(Config.getResourcePath(filename));
        JSONArray result = response.getJSONArray("result");
        for (int i = 0; i < result.length(); i++) {
            JSONObject obj = result.getJSONObject(i);
            BittrexOrderVO vo = new BittrexOrderVO();
            vo.setExchange(obj.getString("Exchange"));
            vo.setOrderType(obj.getString("OrderType"));
            vo.setOrderUuid(obj.getString("OrderUuid"));
            vo.setLimit(obj.getDouble("Limit"));
            vo.setQuantity(obj.getDouble("Quantity"));
            openOrderList.add(vo);
        }
        return openOrderList;
    }

    public List<BittrexOrderVO> getOpenOrdersV2() throws Exception {
        List<BittrexOrderVO> openOrderList = new ArrayList<>();

        final String url = String.format("https://bittrex.com/api/v1.1/market/getopenorders?apikey=%s&nonce=%d", key, CmnUtil.nsTime());
        JSONObject response = callPrivateApi(url);
        JSONArray result = response.getJSONArray("result");
        for (int i = 0; i < result.length(); i++) {
            JSONObject obj = result.getJSONObject(i);
            BittrexOrderVO vo = new BittrexOrderVO();
            vo.setExchange(obj.getString("Exchange"));
            vo.setOrderType(obj.getString("OrderType"));
            vo.setOrderUuid(obj.getString("OrderUuid"));
            vo.setLimit(obj.getDouble("Limit"));
            vo.setQuantity(obj.getDouble("Quantity"));
            openOrderList.add(vo);
        }
        return openOrderList;
    }

    public JSONObject callPrivateApi(String url) throws Exception {
        String sign = Encryptor.getHmacSha512(secret, url);

        Map<String, String> map = new HashMap<>();
        map.put("apisign", sign);

        return HTTPUtil.getJSONfromPost(url, map, "");
    }

    public void cancelOrder(String orderUuid) throws Exception {
        String url = String.format("https://bittrex.com/api/v1.1/market/cancel?apikey=%s&nonce=%d&uuid=%s", key, CmnUtil.nsTime(), orderUuid);
        JSONObject result = callPrivateApi(url);
        errorCheck(result);
    }

    public String makeOrder(OrderType orderType, String baseCoin, String coin, double price, double quantity) throws Exception {
        String orderTypeStr;
        if (orderType.equals(OrderType.BUY)) {
            orderTypeStr = "buylimit";
        }
        else { // sell
            orderTypeStr = "selllimit";
        }
        String url = String.format("https://bittrex.com/api/v1.1/market/%s?apikey=%s&nonce=%d&market=%s-%s&quantity=%.8f&rate=%.8f", orderTypeStr, key, CmnUtil.nsTime(), baseCoin, coin, quantity, price);
        JSONObject result = callPrivateApi(url);
        errorCheck(result);
        return result.getJSONObject("result").getString("uuid");
    }

    private void errorCheck(JSONObject result) throws Exception {
        if (result.getBoolean("success") != true) {
            throw new Exception("success가 true가 아닙니다 : " + result.optString("message"));
        }
    }

    public static void main(String[] args) {
        try {
            //new BittrexDAO().getOpenOrders();
            new BittrexDAO().getOpenOrdersV2();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
