package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Comm.CoinoneComm;
import pe.joyyir.Heungbubak.Comm.EmailSender;
import pe.joyyir.Heungbubak.Comm.PoloniexComm;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Util.Config;
import lombok.Setter;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ValueChangeRoutine implements Routine {
    private static long TARGET_WON = -1;
    private static long TARGET_WON_INTERVAL = 200000;
    private static final String MAIL_SUBJECT = "흥부박 알림";

    private CoinoneComm coinone = new CoinoneComm();
    private PoloniexComm poloniex = new PoloniexComm();
    private JSONObject paperWallet = Config.getPaperWallet();
    private boolean isFirst = true;
    @Setter
    private EmailSender emailSender = null;

    public ValueChangeRoutine(EmailSender emailSender) throws Exception {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy.MM.dd HH:mm:ss]");

            long coinoneBal = coinone.getCompleteBalance();
            double poloBal = poloniex.getCompleteBalance();
            double walletBal = poloniex.getMarketPrice(Coin.BTC, Coin.STR) * paperWallet.getDouble(Coin.STR.name());
            double btcPrice = coinone.getLastMarketPrice(Coin.BTC);
            long totalWon = coinoneBal + (long) ((poloBal + walletBal) * btcPrice);
            int increaseRate = (int)((double)totalWon / Config.getInvestment() * 100);

            String strTime = dateFormat.format(date);
            //String strLog = "The program is running";
            String strLog = "coinone: " + coinoneBal
                    + " KRW, poloniex: " + poloBal
                    + " BTC, wallet: " + walletBal
                    + " BTC, total: " + totalWon + " KRW (" + increaseRate + "%)";
            String strLogMail = "< 현재 보유 가치 >\n"
                    + "coinone: " + coinoneBal + " KRW\n"
                    + "poloniex: " + poloBal + " BTC\n"
                    + "wallet: " + walletBal + " BTC\n"
                    + "------------------------------\n"
                    + "total: " + totalWon + " KRW (" + increaseRate + "%)";

            System.out.println(strTime + " " + strLog);
            if(isFirst) {
                TARGET_WON = totalWon + TARGET_WON_INTERVAL;
                emailSender.setStringAndReady("ValueChange", strLogMail);
                isFirst = false;
            }
            if (totalWon >= TARGET_WON) {
                //kakao.sendMessage(strTime, strLog);
                emailSender.setStringAndReady("ValueChange", strLog);
                TARGET_WON += TARGET_WON_INTERVAL;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
