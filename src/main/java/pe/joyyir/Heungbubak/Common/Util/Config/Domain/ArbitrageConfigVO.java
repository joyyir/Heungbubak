package pe.joyyir.Heungbubak.Common.Util.Config.Domain;

import lombok.Data;
import pe.joyyir.Heungbubak.Common.Const.Coin;

import java.util.List;
import java.util.Map;

@Data
public class ArbitrageConfigVO {
    private long minProfit;
    private Map<Coin, Long> minDiffMap;
    private List<Coin> targetCoin;
    private long maxLoss;
    private long maxWaitingSec;
    private long reverseDiffXRP;
    private double qtyMultiplyNum;
    private long priceDiffXRP;
}
