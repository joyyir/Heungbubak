import Comm.CoinoneComm;
import Comm.GmailComm;
import Comm.KakaoComm;
import Comm.PoloniexComm;
import Util.IOUtil;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    private static long TARGET_WON = -1;
    private static long TARGET_WON_INTERVAL = 200000;
    private static final String MAIL_SUBJECT = "ÈïºÎ¹Ú ¾Ë¸²";

    public static void main(String[] args) {
        try {
            CoinoneComm coinone = new CoinoneComm();
            PoloniexComm poloniex = new PoloniexComm();
            KakaoComm kakao = new KakaoComm();
            JSONObject paperWallet = IOUtil.getConfig().getJSONObject("paperWallet");
            boolean isFirst = true;
            while (true) {
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

                    Thread.sleep(10000);
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
                    try {
//                    kakao.sendMessage("Error", e.getMessage());
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
