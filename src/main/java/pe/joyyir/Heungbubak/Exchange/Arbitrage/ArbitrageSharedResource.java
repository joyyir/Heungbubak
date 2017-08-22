package pe.joyyir.Heungbubak.Exchange.Arbitrage;

import lombok.Data;
import pe.joyyir.Heungbubak.Common.Const.OrderType;

public class ArbitrageSharedResource {
    @Data
    public class SharedResource {
        public ArbitrageTradeV2.TradeStatus tradeStatus;
        public Boolean isCancelRequired;

        public SharedResource(ArbitrageTradeV2.TradeStatus tradeStatus, Boolean isCancelRequired) {
            this.tradeStatus = tradeStatus;
            this.isCancelRequired = isCancelRequired;
        }
    }

    private SharedResource buyResource;
    private SharedResource sellResource;

    public void setResource(OrderType orderType, ArbitrageTradeV2.TradeStatus tradeStatus, boolean isCancelRequired) {
        if(orderType == OrderType.BUY) {
            buyResource = new SharedResource(tradeStatus, isCancelRequired);
        }
        else if(orderType == OrderType.SELL) {
            sellResource = new SharedResource(tradeStatus, isCancelRequired);
        }
    }

    public SharedResource getResource(OrderType orderType) {
        if(orderType == OrderType.BUY) {
            return buyResource;
        }
        else { // OrderType.SELL
            return sellResource;
        }
    }
}
