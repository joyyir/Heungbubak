package pe.joyyir.Heungbubak.Common.Util.Config.Domain;

import lombok.Data;
import pe.joyyir.Heungbubak.Common.Const.Coin;

import java.util.List;
import java.util.Map;

@Data
public class ArbitrageConfigVO {
    /*
        "minProfit" : 1000,
        "minDiff" : {
          "BTC" : 20000,
          "ETH" : 2000,
          "ETC" : 100,
          "XRP" : 1
        },
        "targetCoin" : [ "ETH", "ETC" ]
     */

    private long minProfit;
    private Map<Coin, Long> minDiffMap;
    private List<Coin> targetCoin;
    private long maxLoss;
    private long maxWaitingSec;
    private long reverseDiffXRP;
    private double qtyMultiplyNum;
}
