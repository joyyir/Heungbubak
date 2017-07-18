package pe.joyyir.Heungbubak.Const;

public enum OrderType {
    BUY("bid"), SELL("ask");

    private String type;
    OrderType(String type) { this.type = type; }

    @Override
    public String toString() {
        return type;
    }
}