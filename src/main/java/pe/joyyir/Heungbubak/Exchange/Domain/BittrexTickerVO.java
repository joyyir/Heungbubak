package pe.joyyir.Heungbubak.Exchange.Domain;

import lombok.Data;

@Data
public class BittrexTickerVO {
    private String marketCurrency;
    private String baseCurrency;
    private double baseVolume;
}
