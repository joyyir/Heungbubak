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

public class MakeSellReservationRoutine implements Routine {
    @Override
    public void run() {

    }

    public static void main(String[] args) {
//        makeSellReservation("VIB", 0.00000822);
//        makeSellReservation("MTL", 0.00032265);
//        makeSellReservation("RBY", 0.00005829);
//        makeSellReservation("BLOCK", 0.00147036);
//        makeSellReservation("XEL", 0.00001679);
//        makeSellReservation("IOC", 0.00014915);
//        makeSellReservation("EXP", 0.00012972);
//        makeSellReservation("PART", 0.00059761);
//        makeSellReservation("DNT", 0.00000289);
//        makeSellReservation("CVC", 0.00001957);
//        makeSellReservation("SNGLS", 0.00000778);

    }

    public static void makeSellReservation(String coinShortName, double btcPrice) {

        try {
            BittrexService bittrex = new BittrexService();
            Map<String, List<BittrexOrderVO>> openOrderMap = bittrex.getOpenOrder("BTC", "LIMIT_SELL");

            List<BittrexOrderVO> openOrderList = openOrderMap.get(coinShortName);
            if (CmnUtil.isNotEmpty(openOrderList)) {
                double quantity = 0.0;
                for (BittrexOrderVO orderVO : openOrderList) {
                    quantity += orderVO.getQuantity();
                }
                for (BittrexOrderVO orderVO : openOrderList) {
                    bittrex.cancelOrder(orderVO.getOrderUuid());
                }
                double newPrice15 = btcPrice * 1.15;
                double newPrice25 = btcPrice * 1.25;
                double newPrice40 = btcPrice * 1.40;

                double quantityOneThird = Math.floor((quantity / 3) * 100000000) / 100000000;

                bittrex.makeOrder(OrderType.SELL, "BTC", coinShortName.toUpperCase(), newPrice15, quantityOneThird);
                bittrex.makeOrder(OrderType.SELL, "BTC", coinShortName.toUpperCase(), newPrice25, quantityOneThird);
                bittrex.makeOrder(OrderType.SELL, "BTC", coinShortName.toUpperCase(), newPrice40, quantityOneThird);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
