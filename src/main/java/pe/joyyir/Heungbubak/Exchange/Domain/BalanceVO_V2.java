package pe.joyyir.Heungbubak.Exchange.Domain;

import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Common.Const.Coin;

import java.util.HashMap;
import java.util.Map;

public class BalanceVO_V2 {
    @Getter @Setter
    private Map<Coin, Double> total = new HashMap<>();
    @Getter @Setter
    private Map<Coin, Double> available = new HashMap<>();
}
