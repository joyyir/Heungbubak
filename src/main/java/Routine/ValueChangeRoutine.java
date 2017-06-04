package Routine;

import Comm.CoinoneComm;
import Comm.GmailComm;
import Comm.PoloniexComm;
import Util.Config;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ValueChangeRoutine implements Routine {
    private static long TARGET_WON = -1;
    private static long TARGET_WON_INTERVAL = 200000;
    private static final String MAIL_SUBJECT = "ÈïºÎ¹Ú ¾Ë¸²";

    private CoinoneComm coinone;
    private PoloniexComm poloniex;
    private JSONObject paperWallet;
    private boolean isFirst;

    public ValueChangeRoutine() throws Exception {
        coinone = new CoinoneComm();
        poloniex = new PoloniexComm();
        paperWallet = Config.getPaperWallet();
        isFirst = true;
    }

    @Override
    public void run() {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy.MM.dd HH:mm:ss]");

            long coinoneBal = coinone.getCompleteBalance();
            double poloBal = poloniex.getCompleteBalance();
            double walletBal = poloniex.getMarketPrice(PoloniexComm.COIN_BTC, PoloniexComm.COIN_STR) * paperWallet.getDouble(PoloniexComm.COIN_STR);
            double btcPrice = coinone.getMarketPrice(CoinoneComm.COIN_BTC);
            long totalWon = coinoneBal + (long) ((poloBal + walletBal) * btcPrice);

            String strTime = dateFormat.format(date);
            //String strLog = "The program is running";
            String strLog = "coinone: " + coinoneBal
                    + " won, poloniex: " + poloBal
                    + " BTC, wallet: " + walletBal
                    + " BTC, total(won): " + totalWon + " won";

            System.out.println(strTime + " " + strLog);
            if(isFirst) {
                TARGET_WON = totalWon + TARGET_WON_INTERVAL;
                GmailComm.sendEmail(MAIL_SUBJECT, "Heungbubak is started.\n" + strLog);
                isFirst = false;
            }
            if (totalWon >= TARGET_WON) {
                //kakao.sendMessage(strTime, strLog);
                GmailComm.sendEmail(MAIL_SUBJECT, strLog);
                TARGET_WON += TARGET_WON_INTERVAL;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
