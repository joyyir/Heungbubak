package pe.joyyir.Heungbubak.Exchange.Domain;

import lombok.Data;

@Data
public class CoinmarketcapGraphCurrencyVO {
    Long time;
    Long marketCapByAvailableSupply;
    Double priceUsd;
    Long volumeUsd;
    Double priceBtc;
}
