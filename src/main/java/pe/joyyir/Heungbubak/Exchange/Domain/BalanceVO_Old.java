package pe.joyyir.Heungbubak.Exchange.Domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import pe.joyyir.Heungbubak.Common.Const.Coin;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceVO_Old {
    @Getter
    private double totalBTC;
    @Getter
    private double totalXRP;
    @Getter
    private double totalETH;
    @Getter
    private double totalETC;
    @Getter
    private double totalSTR;
    @Getter
    private long totalKRW;
    @Getter
    private long totalLTC;
    @Getter
    private long totalDASH;

    @JsonSetter("total_btc")
    public void setTotalBTC(double totalBTC) {
        this.totalBTC = totalBTC;
    }

    @JsonSetter("total_xrp")
    public void setTotalXRP(double totalXRP) {
        this.totalXRP = totalXRP;
    }

    @JsonSetter("total_eth")
    public void setTotalETH(double totalETH) {
        this.totalETH = totalETH;
    }

    @JsonSetter("total_etc")
    public void setTotalETC(double totalETC) {
        this.totalETC = totalETC;
    }

    @JsonSetter("total_str")
    public void setTotalSTR(double totalSTR) {
        this.totalSTR = totalSTR;
    }

    @JsonSetter("total_krw")
    public void setTotalKRW(long totalKRW) {
        this.totalKRW = totalKRW;
    }

    public double getTotal(Coin coin) throws Exception {
        switch (coin) {
            case BTC:
                return getTotalBTC();
            case ETC:
                return getTotalETC();
            case ETH:
                return getTotalETH();
            case XRP:
                return getTotalXRP();
            case STR:
                return getTotalSTR();
            case KRW:
                return getTotalKRW();
            case LTC:
                return getTotalLTC();
            case DASH:
                return getTotalDASH();
            default:
                throw new Exception("undefined coin");
        }
    }

    public static void main(String[] args) {
        //new BalanceVO_Old().test();
    }

    public void test() {
        String json = "{\"btc\":\"1.382\"}";
        ObjectMapper mapper = new ObjectMapper();
        try {
        }
        catch(Exception e) {

        }
    }
}
