package pe.joyyir.Heungbubak.Exchange.Service;

import pe.joyyir.Heungbubak.Exchange.DAO.BittrexDAO;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexTickerVO;

import java.util.*;

public class BittrexService {
    private BittrexDAO dao;

    public BittrexService() {
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
}
