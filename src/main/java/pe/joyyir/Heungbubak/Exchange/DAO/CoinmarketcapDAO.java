package pe.joyyir.Heungbubak.Exchange.DAO;

import org.json.JSONArray;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.HTTPUtil;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;
import pe.joyyir.Heungbubak.Exchange.Domain.CoinmarketcapGraphCurrencyVO;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CoinmarketcapDAO {
    Map<String, String> coinMapShortToFull;
    Map<String, String> coinMapFullToShort;

    public String getCoinFullName(String shortname) throws Exception {
        if (coinMapShortToFull == null ) {
            initailizeCoinMap();
        }
        return coinMapShortToFull.get(shortname.toLowerCase());
    }

    public String getCoinShortName(String fullname) throws Exception {
        if (coinMapFullToShort == null ) {
            initailizeCoinMap();
        }
        return coinMapFullToShort.get(fullname.toLowerCase());
    }

    public void initailizeCoinMap() throws Exception {
        coinMapShortToFull = new HashMap<>();
        coinMapFullToShort = new HashMap<>();
        JSONObject obj = IOUtil.readJson(Config.getResourcePath("CoinmarketcapCoins.json"));
        JSONArray array = obj.getJSONArray("array");
        for (int i = 0; i < array.length(); i++) {
            JSONArray token = array.getJSONObject(i).getJSONArray("tokens");
            String fullName = array.getJSONObject(i).getString("slug");
            //String fullName = token.getString(0).toLowerCase();
            String shortName = token.getString(1).toLowerCase();
            coinMapShortToFull.put(shortName, fullName);
            coinMapFullToShort.put(fullName, shortName);
        }
    }

    private JSONObject graphCurrency(String coin, String start, String end) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = HTTPUtil.getJSONfromGet(String.format("https://graphs.coinmarketcap.com/currencies/%s/%s/%s/", coin, start, end));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private JSONObject graphCurrency(String coin, Date start, Date end) {
        return graphCurrency(coin, String.valueOf(start.getTime()), String.valueOf(end.getTime()));
    }

    public JSONArray getLastArray(JSONArray array) {
        return array.optJSONArray(array.length()-1);
    }

    public CoinmarketcapGraphCurrencyVO graphCurrency(String coin, Date date) {
        Calendar end = Calendar.getInstance();
        end.setTime(date);

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(end.getTimeInMillis() - 30 * 60 * 1000); // 30분 전

        JSONObject obj = graphCurrency(coin, start.getTime(), end.getTime());
        CoinmarketcapGraphCurrencyVO vo = new CoinmarketcapGraphCurrencyVO();
        if (obj != null) {
            JSONArray supplyArray = obj.optJSONArray("market_cap_by_available_supply");
            JSONArray priceUsdArray = obj.optJSONArray("price_usd");
            JSONArray volumeUsdArray = obj.optJSONArray("volume_usd");
            JSONArray priceBtcArray = obj.optJSONArray("price_btc");

            if (supplyArray != null) {
                JSONArray last = getLastArray(supplyArray);
                vo.setTime(last.getLong(0));
                vo.setMarketCapByAvailableSupply(last.optLong(1));
            }
            if (priceUsdArray != null) {
                JSONArray last = getLastArray(priceUsdArray);
                vo.setTime(last.getLong(0));
                vo.setPriceUsd(last.optDouble(1));
            }
            if (volumeUsdArray != null) {
                JSONArray last = getLastArray(volumeUsdArray);
                vo.setTime(last.getLong(0));
                vo.setVolumeUsd(last.optLong(1));
            }
            if (priceBtcArray != null) {
                JSONArray last = getLastArray(priceBtcArray);
                vo.setTime(last.getLong(0));
                vo.setPriceBtc(last.optDouble(1));
            }
        }

        return vo;
    }

    public static void main(String[] args) {
        //JSONObject obj = new CoinmarketcapDAO().graphCurrency("bitcoin", "1512093253000", "1512093254000");
        //System.out.println(obj.toString(4));

        try {
            new CoinmarketcapDAO().getCoinFullName("b");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
