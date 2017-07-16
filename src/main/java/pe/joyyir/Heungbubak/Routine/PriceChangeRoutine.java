package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Comm.CoinoneComm;
import pe.joyyir.Heungbubak.Comm.EmailSender;
import pe.joyyir.Heungbubak.Comm.PoloniexComm;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Util.Config;
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
        String coin = _coin.toUpperCase();
        String unit = _unit.toUpperCase();

        if(!Coin.KRW.name().equals(unit))
            return false;

        Coin[] coinArr = CoinoneComm.COIN_ARRAY;
        for(int i = 0; i < coinArr.length; i++) {
            if(coinArr[i].name().equals(coin))
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
            sbMail.append("< 현재 시세 >\n");

            for(int i = 0; i < prevPrice.length(); i++) {
                JSONObject obj = prevPrice.getJSONObject(i);
                String coin = obj.getString("coin");
                String unit = obj.getString("unit");

                if(isCoinonePriceAvail(coin, unit)) {
                    int price = obj.getInt("price");
                    int coinPrice = coinone.getLastMarketPrice(Coin.valueOf(coin.toUpperCase()));
                    int percent = (int)(coinPrice / (double)price * 100);
                    sb.append("[" + coin + ": " + coinPrice + " " + unit + " (" + percent + "%)]    ");
                    sbMail.append(coin + ": " + coinPrice + " " + unit + " (" + percent + "%)\n");
                }
                else {
                    double price = obj.getDouble("price");
                    double coinPrice = poloniex.getMarketPrice(Coin.valueOf(unit), Coin.valueOf(coin));
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
