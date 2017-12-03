package pe.joyyir.Heungbubak.Exchange.DAO;

import org.json.JSONArray;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.HTTPUtil;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexOrderVO;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexTickerVO;

import java.util.ArrayList;
import java.util.List;

public class BittrexDAO {
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
            vo.setUuid(obj.getString("Uuid"));
            vo.setOrderUuid(obj.getString("OrderUuid"));
            vo.setId(obj.getLong("Id"));
            vo.setLimit(obj.getDouble("Limit"));
            vo.setQuantity(obj.getDouble("Quantity"));
            openOrderList.add(vo);
        }
        return openOrderList;
    }

    public static void main(String[] args) {
        try {
            new BittrexDAO().getOpenOrders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
