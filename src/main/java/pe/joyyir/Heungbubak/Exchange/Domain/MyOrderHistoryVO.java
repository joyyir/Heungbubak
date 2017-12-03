package pe.joyyir.Heungbubak.Exchange.Domain;

import lombok.Data;

@Data
public class MyOrderHistoryVO {
    private String coin;
    private String dateString;
    private double beforeUsdPrice;
    private double beforeBtcPrice;
    private double afterUsdPrice;
    private double afterBtcPrice;
    private double rateCoin;
    private double rateBtc;
    private double newBtcPrice15;
    private double newBtcPrice25;
    private double newBtcPrice40;
    private String updateSuccess;

    public MyOrderHistoryVO() {}
    public MyOrderHistoryVO(String coin, String dateString) {
        this.coin = coin;
        this.dateString = dateString;
    }
}
