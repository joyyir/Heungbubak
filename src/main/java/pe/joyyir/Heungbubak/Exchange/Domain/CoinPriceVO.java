package pe.joyyir.Heungbubak.Exchange.Domain;

import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Common.Const.Coin;

import java.util.HashMap;
import java.util.Map;

public class CoinPriceVO {
    @Getter @Setter
    private Map<Coin, BasicPriceVO> price = new HashMap<>();
}
