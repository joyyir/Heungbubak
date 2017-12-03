package pe.joyyir.Heungbubak.Exchange.Domain;

import lombok.Data;

@Data
public class BittrexOrderVO {
    String exchange;
    String orderType;
    String orderUuid;
    Double limit; // bid/ask
    Double quantity;
}
