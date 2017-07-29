package pe.joyyir.Heungbubak.Exchange.Domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import pe.joyyir.Heungbubak.Common.Const.Coin;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicPriceVO {
    @Getter
    private Coin unit;
    @Getter
    private double sellPrice;
    @Getter
    private double buyPrice;

    public void setUnit(Coin unit) {
        this.unit = unit;
    }

    @JsonSetter("sell_price")
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    @JsonSetter("buy_price")
    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }
}
