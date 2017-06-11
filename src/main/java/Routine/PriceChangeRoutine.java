package Routine;

import Comm.CoinoneComm;
import Comm.EmailSender;
import Comm.PoloniexComm;
import Util.Config;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

public class PriceChangeRoutine implements Routine {
    private CoinoneComm coinone = new CoinoneComm();
    private PoloniexComm poloniex = new PoloniexComm();
    private JSONArray prevPrice = Config.getPreviousPrice();
    @Setter
    private EmailSender emailSender = null;

    public PriceChangeRoutine() throws Exception { }

    public PriceChangeRoutine(EmailSender sender) throws Exception {
        emailSender = sender;
    }

    private boolean isCoinonePriceAvail(String _coin, String _unit) {
        String coin = _coin.toLowerCase();
        String unit = _unit.toLowerCase();

        if(!(CoinoneComm.COIN_KRW).equals(unit))
            return false;

        String[] coinArr = CoinoneComm.COIN_ARRAY;
        for(int i = 0; i < coinArr.length; i++) {
            if(coinArr[i].equals(coin))
                return true;
        }
        return false;
    }

    @Override
    public void run() {
        try {
            StringBuilder sb = new StringBuilder();
            StringBuilder sbMail = new StringBuilder();
            sb.append("\t");
            sbMail.append("< ���� �ü� >\n");

            for(int i = 0; i < prevPrice.length(); i++) {
                JSONObject obj = prevPrice.getJSONObject(i);
                String coin = obj.getString("coin");
                String unit = obj.getString("unit");

                if(isCoinonePriceAvail(coin, unit)) {
                    int price = obj.getInt("price");
                    int coinPrice = coinone.getMarketPrice(coin);
                    int percent = (int)(coinPrice / (double)price * 100);
                    sb.append("[" + coin + ": " + coinPrice + " " + unit + " (" + percent + "%)]    ");
                    sbMail.append(coin + ": " + coinPrice + " " + unit + " (" + percent + "%)\n");
                }
                else {
                    double price = obj.getDouble("price");
                    double coinPrice = poloniex.getMarketPrice(unit, coin);
                    int percent = (int)(coinPrice / price * 100);
                    sb.append("[" + coin + ": " + coinPrice + " " + unit + " (" + percent + "%)]    ");
                    sbMail.append(coin + ": " + coinPrice + " " + unit + " (" + percent + "%)\n");
                }
            }

            System.out.println(sb.toString());
            emailSender.setString("PriceChange", sbMail.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            PriceChangeRoutine routine = new PriceChangeRoutine();
        }
        catch (Exception e) {}
    }
}
