package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;
import pe.joyyir.Heungbubak.Exchange.Domain.BittrexOrderVO;
import pe.joyyir.Heungbubak.Exchange.Domain.MyOrderHistoryVO;
import pe.joyyir.Heungbubak.Exchange.Service.BittrexService;
import pe.joyyir.Heungbubak.Exchange.Service.CoinmarketcapService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class ReadjustmentRoutine implements Routine {
    @Override
    public void run() {

    }

    public static void main(String[] args) {
        final String LOAD_FILE_PATH = "/Users/1003880/Desktop/source.csv";
        final String SAVE_FILE_PATH = "/Users/1003880/Desktop/result_" + new SimpleDateFormat("yyyyMMddHH24mmss").format(Calendar.getInstance().getTime())+ ".csv";

        try {
            BittrexService bittrex = new BittrexService();
            CoinmarketcapService coinmarketcap = new CoinmarketcapService();

            Map<String, List<BittrexOrderVO>> openOrderMap = bittrex.getOpenOrder("BTC", "LIMIT_SELL");

            List<MyOrderHistoryVO> historyList = (List<MyOrderHistoryVO>) coinmarketcap.loadCsvAsObjectList(LOAD_FILE_PATH, MyOrderHistoryVO.class);
            List<MyOrderHistoryVO> changeList = coinmarketcap.getChangeList(historyList);

            for (MyOrderHistoryVO vo : changeList) {
                String coinShortName = coinmarketcap.getCoinShortName(vo.getCoin()).toUpperCase();
                List<BittrexOrderVO> openOrderList = openOrderMap.get(coinShortName);
                if (CmnUtil.isNotEmpty(openOrderList)) {
                    double quantity = 0.0;
                    for (BittrexOrderVO orderVO : openOrderList) {
                        quantity += orderVO.getQuantity();
                    }
                    for (BittrexOrderVO orderVO : openOrderList) {
                        bittrex.cancelOrder(orderVO.getOrderUuid());
                    }
                    double newPrice15 = vo.getNewBtcPrice15();
                    double newPrice25 = vo.getNewBtcPrice25();
                    double newPrice40 = vo.getNewBtcPrice40();

                    double quantityOneThird = Math.floor((quantity / 3) * 100000000) / 100000000;

                    bittrex.makeOrder(OrderType.SELL, "BTC", coinShortName.toUpperCase(), newPrice15, quantityOneThird);
                    bittrex.makeOrder(OrderType.SELL, "BTC", coinShortName.toUpperCase(), newPrice25, quantityOneThird);
                    bittrex.makeOrder(OrderType.SELL, "BTC", coinShortName.toUpperCase(), newPrice40, quantityOneThird);

                    vo.setUpdateSuccess("Y");
                }
            }

            coinmarketcap.saveObjectListAsCsv(SAVE_FILE_PATH, changeList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
