package pe.joyyir.Heungbubak.Exchange.Service;

import pe.joyyir.Heungbubak.Common.Util.CsvUtil;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;
import pe.joyyir.Heungbubak.Exchange.DAO.CoinmarketcapDAO;
import pe.joyyir.Heungbubak.Exchange.Domain.CoinmarketcapGraphCurrencyVO;
import pe.joyyir.Heungbubak.Exchange.Domain.MyOrderHistoryVO;


import java.text.SimpleDateFormat;
import java.util.*;

public class CoinmarketcapService {
    private CoinmarketcapDAO dao = new CoinmarketcapDAO();

    public void doSomething() {
        final String LOAD_FILE_PATH = "/Users/1003880/Desktop/source.csv";
        final String SAVE_FILE_PATH = "/Users/1003880/Desktop/result_" + new SimpleDateFormat("yyyyMMddHH24mmss").format(Calendar.getInstance().getTime())+ ".csv";
        final MyOrderHistoryVO[] historyArr = {
            new MyOrderHistoryVO("Digibyte", "11/26/2017 03:12:53 PM"),
            new MyOrderHistoryVO("Metal", "11/26/2017 02:58:00 PM"),
            new MyOrderHistoryVO("CapriCoin", "11/26/2017 03:04:40 PM"),
            new MyOrderHistoryVO("Blitzcash", "11/26/2017 03:05:44 PM"),
            new MyOrderHistoryVO("Zclassic", "11/26/2017 03:08:38 PM"),
            new MyOrderHistoryVO("Project Decorum", "11/28/2017 11:04:22 PM"),
            new MyOrderHistoryVO("Viberate", "11/26/2017 10:40:45 PM"),
            new MyOrderHistoryVO("district0x", "11/26/2017 03:02:02 PM"),
            new MyOrderHistoryVO("Monaco", "11/26/2017 03:01:06 PM"),
            new MyOrderHistoryVO("Expanse", "11/26/2017 03:14:35 PM"),
            new MyOrderHistoryVO("CreditBit", "11/26/2017 03:17:30 PM"),
            new MyOrderHistoryVO("Gulden", "11/26/2017 02:59:37 PM"),
            new MyOrderHistoryVO("adToken", "11/26/2017 03:06:43 PM"),
            new MyOrderHistoryVO("GeoCoin", "11/26/2017 02:54:08 PM"),
            new MyOrderHistoryVO("SingularDTV", "11/28/2017 08:12:33 AM"),
            new MyOrderHistoryVO("Elastic", "11/26/2017 03:10:09 PM"),
            new MyOrderHistoryVO("FirstBlood", "11/26/2017 03:15:58 PM")
        };

        try {
            List<MyOrderHistoryVO> historyList = (List<MyOrderHistoryVO>) loadCsvAsObjectList(LOAD_FILE_PATH, MyOrderHistoryVO.class);
            List<MyOrderHistoryVO> changeList = getChangeList(historyList);
            //List<MyOrderHistoryVO> changeList = getChangeList(Arrays.asList(historyArr)); // hard-coded
            saveObjectListAsCsv(SAVE_FILE_PATH, changeList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<MyOrderHistoryVO> getChangeList(List<MyOrderHistoryVO> historyArr) {
        List<MyOrderHistoryVO> changeList = new ArrayList<>();
        for (MyOrderHistoryVO vo : historyArr) {
            MyOrderHistoryVO changeVO = getPriceChange(vo.getDateString(), vo.getCoin());
            changeList.add(changeVO);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return changeList;
    }

    public void saveObjectListAsCsv(String filepath, List<?> changeList) throws Exception {
        IOUtil.writeCsv(filepath, new CsvUtil().convertObjectListToSheet(changeList));
    }

    public List<?> loadCsvAsObjectList(String filepath, Class clazz) throws Exception {
        return new CsvUtil().convertSheetToObjectList(IOUtil.readCsv(filepath), clazz);
    }

    public MyOrderHistoryVO getPriceChange(String formattedDate, String coinName) {
        MyOrderHistoryVO vo = new MyOrderHistoryVO();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a", Locale.US);
            Date date = sdf.parse(formattedDate);
            String modifiedCoinName = coinName.toLowerCase().replace(" ", "-");
            CoinmarketcapGraphCurrencyVO coinBeforeVO = dao.graphCurrency(modifiedCoinName, date);
            CoinmarketcapGraphCurrencyVO coinAfterVO = dao.graphCurrency(modifiedCoinName, Calendar.getInstance().getTime());
            CoinmarketcapGraphCurrencyVO btcBeforeVO = dao.graphCurrency("bitcoin", date);
            CoinmarketcapGraphCurrencyVO btcAfterVO = dao.graphCurrency("bitcoin", Calendar.getInstance().getTime());

            double beforeUsdPrice = coinBeforeVO.getPriceUsd();
            double beforeBtcPrice = coinBeforeVO.getPriceBtc();
            double afterUsdPrice = coinAfterVO.getPriceUsd();
            double afterBtcPrice = coinAfterVO.getPriceBtc();
            double rateCoin = afterUsdPrice/beforeUsdPrice;
            double rateBtc = btcAfterVO.getPriceUsd()/btcBeforeVO.getPriceUsd();

            final int[] RATE_ARR = {15, 25, 40};
            double[] newBtcPriceArr = new double[3];
            for (int i = 0; i < RATE_ARR.length; i++) {
                newBtcPriceArr[i] = beforeUsdPrice*(1/btcAfterVO.getPriceUsd())*(1+(double)RATE_ARR[i]/100);
            }

            vo.setCoin(coinName);
            vo.setDateString(formattedDate);
            vo.setBeforeUsdPrice(beforeUsdPrice);
            vo.setBeforeBtcPrice(beforeBtcPrice);
            vo.setAfterUsdPrice(afterUsdPrice);
            vo.setAfterBtcPrice(afterBtcPrice);
            vo.setRateCoin(rateCoin);
            vo.setRateBtc(rateBtc);
            vo.setNewBtcPrice15(newBtcPriceArr[0]);
            vo.setNewBtcPrice25(newBtcPriceArr[1]);
            vo.setNewBtcPrice40(newBtcPriceArr[2]);

            System.out.println("------------------------------");
            System.out.println(coinName);
            System.out.println(String.format("과거 1개당 %f USD, %.8f BTC", beforeUsdPrice, beforeBtcPrice));
            System.out.println(String.format("현재 1개당 %f USD, %.8f BTC", afterUsdPrice, afterBtcPrice));
            System.out.println(String.format("%s의 USD 변화율 : %.1f%%", coinName, (rateCoin-1)*100));
            System.out.println(String.format("BTC의 USD 변화율 : %.1f%%", (rateBtc-1)*100));

            
            for (int i = 0; i < RATE_ARR.length; i++) {
                System.out.println(String.format("%d%% : %.8f BTC", RATE_ARR[i], newBtcPriceArr[i]));
            }
            System.out.println("\n\n");


        } catch (Exception e) {
            e.printStackTrace();
        }

        return vo;
    }

    public String getCoinFullName(String shortname) throws Exception {
        return dao.getCoinFullName(shortname);
    }

    public String getCoinShortName(String fullname) throws Exception {
        return dao.getCoinShortName(fullname);
    }

    public static void main(String[] args) {
        new CoinmarketcapService().doSomething();
    }
}
