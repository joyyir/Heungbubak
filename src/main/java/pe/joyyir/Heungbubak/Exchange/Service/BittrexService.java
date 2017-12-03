package pe.joyyir.Heungbubak.Exchange.Service;

import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Exchange.DAO.BittrexDAO;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexOrderVO;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexTickerVO;

import java.util.*;

public class BittrexService {
    private BittrexDAO dao;

    public BittrexService() throws Exception {
        this.dao = new BittrexDAO();
    }

    public List<String> getCandidateCoinList(String baseCurrency, List<String> exclude, List<String> own) {
        List<String> list = new ArrayList<>();
        if (baseCurrency == null) baseCurrency = "";
        if (exclude == null) exclude = new ArrayList<>();
        if (own == null) own = new ArrayList<>();

        try {
            List<BittrexTickerVO> ticker = dao.getTicker(baseCurrency);
            Collections.sort(ticker, new Comparator<BittrexTickerVO>() {
                @Override
                public int compare(BittrexTickerVO o1, BittrexTickerVO o2) {
                    return o1.getBaseVolume() > o2.getBaseVolume() ? 1 : -1;
                }
            });

            Iterator<BittrexTickerVO> iter = ticker.iterator();
            while (iter.hasNext()) {
                BittrexTickerVO vo = iter.next();

                if (!baseCurrency.equals(vo.getBaseCurrency())) {
                    continue;
                }

                if (exclude.contains(vo.getMarketCurrency()) || own.contains(vo.getMarketCurrency())) {
                    iter.remove();
                }
            }

            iter = ticker.iterator();
            while (iter.hasNext()) {
                BittrexTickerVO vo = iter.next();
                list.add(vo.getMarketCurrency());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // baseCoin : BTC, ETH, USDT
    // OrderType : LIMIT_SELL, LIMIT_BUY
    public Map<String, List<BittrexOrderVO>> getOpenOrder(String baseCoin, String OrderType) throws Exception {
        Map<String, List<BittrexOrderVO>> map = new HashMap<>();
        List<BittrexOrderVO> openOrderList = dao.getOpenOrdersV2();
        for (BittrexOrderVO vo : openOrderList) {
            String[] coinArr = vo.getExchange().split("-");
            if (baseCoin.equals(coinArr[0]) && OrderType.equals(vo.getOrderType())) {
                List<BittrexOrderVO> list = map.get(coinArr[1]);
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(coinArr[1], list);
                }
                list.add(vo);
            }
        }
        return map;
    }

    public static void main(String[] args) {
        try {
            //new BittrexService().getOpenOrder("BTC", "LIMIT_SELL");
            //new BittrexService().cancelOrder("c812ccb2-48ef-4226-a2ad-a5b2cfeaf272");
            //new BittrexService().makeOrder(OrderType.SELL, "USDT", "BTC", 50000, 0.01);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelOrder(String orderUuid) throws Exception {
        dao.cancelOrder(orderUuid);
    }

    public void makeOrder(OrderType orderType, String baseCoin, String coin, double price, double quantity) throws Exception {
        dao.makeOrder(orderType, baseCoin, coin, price, quantity);
    }
}
