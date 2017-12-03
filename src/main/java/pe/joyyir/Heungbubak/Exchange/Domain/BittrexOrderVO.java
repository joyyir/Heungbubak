package pe.joyyir.Heungbubak.Exchange.Domain;

import lombok.Data;

@Data
public class BittrexOrderVO {
    String exchange;
    String orderType;
    String uuid;
    String orderUuid;
    Long id;
    Double limit; // bid/ask
    Double quantity;
}
