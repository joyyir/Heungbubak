package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Exchange.Service.CoinoneService;
import pe.joyyir.Heungbubak.Common.Util.EmailSender;
import pe.joyyir.Heungbubak.Exchange.Service.PoloniexService;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

public class PriceChangeRoutine implements Routine {
    private CoinoneService coinone = new CoinoneService();
    private PoloniexService poloniex = new PoloniexService();
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

        Coin[] coinArr = CoinoneService.COIN_ARRAY;
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
                Coin enumCoin, unitCoin;
                try {
                    enumCoin = Coin.valueOf(coin.toUpperCase());
                    unitCoin = Coin.valueOf(unit.toUpperCase());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                if(isCoinonePriceAvail(coin, unit)) {
                    int price = obj.getInt("price");
                    int coinPrice = coinone.getLastMarketPrice(enumCoin);
                    int percent = (int)(coinPrice / (double)price * 100);
                    sb.append("[" + coin + ": " + coinPrice + " " + unit + " (" + percent + "%)]    ");
                    sbMail.append(coin + ": " + coinPrice + " " + unit + " (" + percent + "%)\n");
                }
                else {
                    double price = obj.getDouble("price");
                    double coinPrice = poloniex.getMarketPrice(unitCoin, enumCoin);
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
