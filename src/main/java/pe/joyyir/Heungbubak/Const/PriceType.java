package pe.joyyir.Heungbubak.Const;

public enum PriceType {
    BUY("bid"), SELL("ask");

    private String type;
    PriceType(String type) { this.type = type; }

    @Override
    public String toString() {
        return type;
    }
}